package com.nio.ocr.ektp.mobile.camera;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.view.SurfaceHolder;
import com.nio.ocr.ektp.mobile.Constants;
import com.nio.ocr.ektp.mobile.PlanarYUVLuminanceSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.nio.ocr.ektp.mobile.Constants.*;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 */
public final class CameraManager {

    private static final String TAG = CameraManager.class.getSimpleName();

    private final Context context;
    private final Activity activity;
    private final CameraConfigurationManager configManager;
    private Camera camera;
    private AutoFocusManager autoFocusManager;
    private Rect framingRect;
    private Rect framingRectInPreview;
    private boolean initialized;
    private boolean previewing;
    private int requestedFramingRectWidth;
    private int requestedFramingRectHeight;

    /**
     * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
     * clear the handler so it will only receive one message.
     */
    private final PreviewCallback previewCallback;

    public CameraManager(Context context, Activity activity) {
        this.context = context;
        this.configManager = new CameraConfigurationManager(context);
        previewCallback = new PreviewCallback(configManager);
        this.activity=activity;
    }

    public synchronized void initDriver(SurfaceHolder holder) throws IOException {

        Log.d(TAG,"initDriver()");

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            int hasCameraPermission = activity.checkSelfPermission(android.Manifest.permission.CAMERA);

            List<String> permissions = new ArrayList<String>();

            if (hasCameraPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.CAMERA);
            } else {
               openDriver(holder);
            }

            if (!permissions.isEmpty()) {
                activity.requestPermissions(permissions.toArray(new String[permissions.size()]), Constants.PERMISSIONS_REQUEST_CAMERA);
            }

        } else {
            openDriver(holder);
        }*/

        openDriver(holder);

    }

    private synchronized void openDriver(SurfaceHolder holder) throws IOException {
            Camera theCamera = camera;
            if (theCamera == null) {
                theCamera = Camera.open();
                if (theCamera == null) {
                    throw new IOException();
                }
                camera = theCamera;
            }
            camera.setPreviewDisplay(holder);
            if (!initialized) {
                initialized = true;
                configManager.initFromCameraParameters(theCamera);
                if (requestedFramingRectWidth > 0 && requestedFramingRectHeight > 0) {
                    adjustFramingRect(requestedFramingRectWidth, requestedFramingRectHeight);
                    requestedFramingRectWidth = 0;
                    requestedFramingRectHeight = 0;
                }
            }
            configManager.setDesiredCameraParameters(theCamera);
    }

    public synchronized void closeDriver() {
        if (camera != null) {
            camera.release();
            camera = null;

            framingRect = null;
            framingRectInPreview = null;
        }
    }

    /**
     * Asks the camera hardware to begin drawing preview frames to the screen.
     */
    public synchronized void startPreview() {

        Camera theCamera = camera;
        if (theCamera != null && !previewing) {
            theCamera.startPreview();
            previewing = true;
            autoFocusManager = new AutoFocusManager(context, camera);
        }
    }

    /**
     * Tells the camera to stop drawing preview frames.
     */
    public synchronized void stopPreview() {

        Log.d(TAG,"stopPreview");

        if (autoFocusManager != null) {
            autoFocusManager.stop();
            autoFocusManager = null;
        }

        if (camera != null && previewing) {
            camera.stopPreview();
            previewCallback.setHandler(null, 0);
            previewing = false;
        }
    }

    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
     * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
     * respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    public synchronized void requestOcrDecode(Handler handler, int message) {
        Camera theCamera = camera;
        if (theCamera != null && previewing) {
            previewCallback.setHandler(handler, message);
            theCamera.setOneShotPreviewCallback(previewCallback);
        }
    }

    /**
     * Asks the camera hardware to perform an autofocus.
     * @param delay Time delay to send with the request
     */
    public synchronized void requestAutoFocus(long delay) {
        Log.d("CameraManager","autofocus requested");
        autoFocusManager.start(delay);
    }

    /**
     * Calculates the framing rect which the UI should draw to show the user where to place the
     * EKTP. This target helps with alignment as well as forces the user to hold the device
     * far enough away to ensure the image will be in focus.
     *
     * @return The rectangle to draw on screen in window coordinates.
     */
    public synchronized Rect getFramingRect() {
        if (framingRect == null) {
            if (camera == null) {
                return null;
            }
            Point screenResolution = configManager.getScreenResolution();

            if (screenResolution == null) {
                return null;
            }

            int width = screenResolution.x * 1;
            if (width < MIN_FRAME_WIDTH) {
                width = MIN_FRAME_WIDTH;
            } else if (width > MAX_FRAME_WIDTH) {
                width = MAX_FRAME_WIDTH;
            }
            int height = screenResolution.y * 6/8;
            if (height < MIN_FRAME_HEIGHT) {
                height = MIN_FRAME_HEIGHT;
            } else if (height > MAX_FRAME_HEIGHT) {
                height = MAX_FRAME_HEIGHT;
            }
            int leftOffset = (screenResolution.x - width) / 2;
            int topOffset = (screenResolution.y - height) / 2;
            framingRect = new Rect(leftOffset, topOffset, leftOffset + width, topOffset + height);
        }
        return framingRect;
    }

    /**
     * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
     * not UI / screen.
     */
    public synchronized Rect getFramingRectInPreview() {
        if (framingRectInPreview == null) {
            Rect rect = new Rect(getFramingRect());
            Point cameraResolution = configManager.getCameraResolution();
            Point screenResolution = configManager.getScreenResolution();
            if (cameraResolution == null || screenResolution == null) {
                // Called early, before init even finished
                return null;
            }
            rect.left = rect.left * cameraResolution.x / screenResolution.x;
            rect.right = rect.right * cameraResolution.x / screenResolution.x;
            rect.top = rect.top * cameraResolution.y / screenResolution.y;
            rect.bottom = rect.bottom * cameraResolution.y / screenResolution.y;
            framingRectInPreview = rect;
        }
        return framingRectInPreview;
    }

    /**
     * Changes the size of the framing rect.
     *
     * @param deltaWidth Number of pixels to adjust the width
     * @param deltaHeight Number of pixels to adjust the height
     */
    public synchronized void adjustFramingRect(int deltaWidth, int deltaHeight) {
        if (initialized) {
            Point screenResolution = configManager.getScreenResolution();

            // Set maximum and minimum sizes
            if ((framingRect.width() + deltaWidth > screenResolution.x - 4) || (framingRect.width() + deltaWidth < 50)) {
                deltaWidth = 0;
            }
            if ((framingRect.height() + deltaHeight > screenResolution.y - 4) || (framingRect.height() + deltaHeight < 50)) {
                deltaHeight = 0;
            }

            int newWidth = framingRect.width() + deltaWidth;
            int newHeight = framingRect.height() + deltaHeight;
            int leftOffset = (screenResolution.x - newWidth) / 2;
            int topOffset = (screenResolution.y - newHeight) / 2;
            framingRect = new Rect(leftOffset, topOffset, leftOffset + newWidth, topOffset + newHeight);
            framingRectInPreview = null;
        } else {
            requestedFramingRectWidth = deltaWidth;
            requestedFramingRectHeight = deltaHeight;
        }
    }

    /**
     * A factory method to build the appropriate LuminanceSource object based on the format
     * of the preview buffers, as described by Camera.Parameters.
     *
     * @param data A preview frame.
     * @param width The width of the image.
     * @param height The height of the image.
     * @return A PlanarYUVLuminanceSource instance.
     */
    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = getFramingRectInPreview();
        if (rect == null) {
            return null;
        }
        // Go ahead and assume it's YUV rather than die.
        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
                rect.width(), rect.height(), false);
    }


    public int getMaxZoom() {
        return camera.getParameters().getMaxZoom();
    }

    public void startZoom(int currentZoomLevel) {

        if (camera.getParameters().isSmoothZoomSupported()) {
            camera.startSmoothZoom(currentZoomLevel);
            camera.startPreview();
        } else if (camera.getParameters().isZoomSupported()) {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setZoom(currentZoomLevel);
            camera.setParameters(parameters);
            camera.startPreview();
        }
    }

}
