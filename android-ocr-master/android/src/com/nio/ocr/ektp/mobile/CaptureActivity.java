package com.nio.ocr.ektp.mobile;

import android.*;
import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.text.ClipboardManager;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.*;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.*;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.nio.ocr.ektp.mobile.camera.CameraManager;
import com.nio.ocr.ektp.mobile.camera.ShutterButton;
import com.nio.ocr.ektp.mobile.db.DBHelper;
import com.nio.ocr.ektp.mobile.model.KTPModel;
import com.permissioneverywhere.PermissionEverywhere;
import com.permissioneverywhere.PermissionResponse;
import com.permissioneverywhere.PermissionResultCallback;
import com.purplebrain.adbuddiz.sdk.AdBuddiz;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.nio.ocr.ektp.mobile.Constants.PERMISSIONS_REQUEST_CAMERA;

public final class CaptureActivity extends Activity implements SurfaceHolder.Callback, ShutterButton.OnShutterButtonListener {

    private static final String TAG = Constants.TAG;

    private int currentZoomLevel = 0;

    private DBHelper dbHelper = null;

    private final boolean CONTINUOUS_MODE_ACTIVE = true;
    private static final boolean CONTINUOUS_DISPLAY_METADATA = true;

    private static final boolean DISPLAY_SHUTTER_BUTTON = true;

    private static final int SETTINGS_ID = Menu.FIRST;

    private static final int OPTIONS_COPY_RECOGNIZED_TEXT_ID = Menu.FIRST;
    private static final int OPTIONS_SHARE_RECOGNIZED_TEXT_ID = Menu.FIRST + 1;

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private TextView statusViewBottom;
    private TextView ocrResultView;
    private View cameraButtonView;
    private View resultView;
    private View progressView;
    private SeekBar seekBar;
    private CheckBox cbTouchedImage;
    private OcrResult lastResult;
    private Bitmap lastBitmap;
    private boolean hasSurface;
    private BeepManager beepManager;
    private TessBaseAPI baseApi; // Java interface for the Tesseract OCR engine
    private final static String sourceLanguageCodeOcr="eng"; // ISO 639-3 language code
    private final static String sourceLanguageReadable = "English"; // Language name, for example, "English"
    private final static int pageSegmentationMode = TessBaseAPI.PageSegMode.PSM_SINGLE_COLUMN;
    private final static int ocrEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
    private ShutterButton shutterButton;

    private ProgressDialog dialog; // for initOcr - language download & unzip
    private ProgressDialog indeterminateDialog; // also for initOcr - init OCR engine
    private boolean isEngineReady;
    private boolean isPaused;
    public static float thresholdLevel = 0.2f;
    public static boolean showTouchedImage = false ;

    Handler getHandler() {
        return handler;
    }

    TessBaseAPI getBaseApi() {
        return baseApi;
    }

    CameraManager getCameraManager() {
        return cameraManager;
    }

    @Override
    public void onCreate(Bundle bundle) {

        super.onCreate(bundle);

        checkFirstLaunch();

        dbHelper = DBHelper.getInstance(this.getApplicationContext());

        Window window = getWindow();

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.capture);

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);

        cameraButtonView = findViewById(R.id.camera_button_view);
        resultView = findViewById(R.id.result_view);

        statusViewBottom = (TextView) findViewById(R.id.status_view_bottom);
        registerForContextMenu(statusViewBottom);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setProgress(20);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
                thresholdLevel = progresValue==0?0.2f:progresValue*0.01f;
                Toast.makeText(getApplicationContext(), "Threshold level of image processing has changed to "+thresholdLevel, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getApplicationContext(), "Stopped tracking seekbar", Toast.LENGTH_SHORT).show();
            }
        });

        handler = null;
        lastResult = null;
        hasSurface = false;
        beepManager = new BeepManager(this);

        if (DISPLAY_SHUTTER_BUTTON) {
            shutterButton = (ShutterButton) findViewById(R.id.shutter_button);
            shutterButton.setOnShutterButtonListener(this);
        }

        ocrResultView = (TextView) findViewById(R.id.ocr_result_text_view);
        registerForContextMenu(ocrResultView);

        progressView = findViewById(R.id.indeterminate_progress_indicator_view);

        cbTouchedImage = (CheckBox) findViewById(R.id.cbTouchedImage);

        cbTouchedImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(cbTouchedImage.isChecked()){
                    showTouchedImage = true;
                    Toast.makeText(getApplicationContext(), R.string.touched_image_shown,Toast.LENGTH_SHORT).show();
                }else{
                    showTouchedImage = false;
                }
            }
        });


       /* PermissionEverywhere.getPermission(getApplicationContext(),
                new String[]{Manifest.permission.CAMERA},
                Constants.PERMISSIONS_REQUEST_CAMERA,
                "OCR eKTP requires access to your camera",
                "OCR eKTP requires access to your camera so it could take a snapshot of your eKTP",
                R.drawable.ic_launcher)
                .enqueue(new PermissionResultCallback() {
                    @Override
                    public void onComplete(PermissionResponse permissionResponse) {
                        if (permissionResponse.isGranted()) {
                            Toast.makeText(getApplicationContext(),"Camera access granted to OCR eKTP",Toast.LENGTH_SHORT).show();
                            initCameraManager();
                        } else {
                            Toast.makeText(getApplicationContext(),"Sorry but we really requires your camera access",Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                });*/

        initCameraManager();

        isEngineReady = false;

        //AdBuddiz.setPublisherKey("3ba14240-f49e-4f50-bfbd-327ba193d04f");
        //AdBuddiz.cacheAds(this);

    }


    /*private void init(Bundle bundle) {

        super.onCreate(bundle);

        checkFirstLaunch();

        dbHelper = DBHelper.getInstance(this.getApplicationContext());

        Window window = getWindow();

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.capture);

        viewfinderView = (ViewfinderView) findViewById(R.id.viewfinder_view);

        cameraButtonView = findViewById(R.id.camera_button_view);
        resultView = findViewById(R.id.result_view);

        statusViewBottom = (TextView) findViewById(R.id.status_view_bottom);
        registerForContextMenu(statusViewBottom);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setProgress(20);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
                thresholdLevel = progresValue==0?0.2f:progresValue*0.01f;
                Toast.makeText(getApplicationContext(), "Threshold level of image processing has changed to "+thresholdLevel, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getApplicationContext(), "Started tracking seekbar", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(getApplicationContext(), "Stopped tracking seekbar", Toast.LENGTH_SHORT).show();
            }
        });

        handler = null;
        lastResult = null;
        hasSurface = false;
        beepManager = new BeepManager(this);

        if (DISPLAY_SHUTTER_BUTTON) {
            shutterButton = (ShutterButton) findViewById(R.id.shutter_button);
            shutterButton.setOnShutterButtonListener(this);
        }

        ocrResultView = (TextView) findViewById(R.id.ocr_result_text_view);
        registerForContextMenu(ocrResultView);

        progressView = findViewById(R.id.indeterminate_progress_indicator_view);

        cbTouchedImage = (CheckBox) findViewById(R.id.cbTouchedImage);

        cbTouchedImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(cbTouchedImage.isChecked()){
                    showTouchedImage = true;
                    Toast.makeText(getApplicationContext(), R.string.touched_image_shown,Toast.LENGTH_SHORT).show();
                }else{
                    showTouchedImage = false;
                }
            }
        });


        initCameraManager();
//
        isEngineReady = false;

        //AdBuddiz.setPublisherKey("3ba14240-f49e-4f50-bfbd-327ba193d04f");
        //AdBuddiz.cacheAds(this);

    }*/

    private void initCameraManager() {

        cameraManager = new CameraManager(getApplication(),this);

        viewfinderView.setCameraManager(cameraManager);

        // Set listener to change the size of the viewfinder rectangle.
        viewfinderView.setOnTouchListener(new View.OnTouchListener() {
            int lastX = -1;
            int lastY = -1;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        lastX = -1;
                        lastY = -1;

                        requestDelayedAutoFocus();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int currentX = (int) event.getX();
                        int currentY = (int) event.getY();

                        try {
                            Rect rect = cameraManager.getFramingRect();

                            final int BUFFER = 50;
                            final int BIG_BUFFER = 60;
                            if (lastX >= 0) {
                                // Adjust the size of the viewfinder rectangle. Check if the touch event occurs in the corner areas first, because the regions overlap.
                                if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
                                        && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
                                    // Top left corner: adjust both top and left sides
                                    cameraManager.adjustFramingRect(2 * (lastX - currentX), 2 * (lastY - currentY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER))
                                        && ((currentY <= rect.top + BIG_BUFFER && currentY >= rect.top - BIG_BUFFER) || (lastY <= rect.top + BIG_BUFFER && lastY >= rect.top - BIG_BUFFER))) {
                                    // Top right corner: adjust both top and right sides
                                    cameraManager.adjustFramingRect(2 * (currentX - lastX), 2 * (lastY - currentY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.left - BIG_BUFFER && currentX <= rect.left + BIG_BUFFER) || (lastX >= rect.left - BIG_BUFFER && lastX <= rect.left + BIG_BUFFER))
                                        && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
                                    // Bottom left corner: adjust both bottom and left sides
                                    cameraManager.adjustFramingRect(2 * (lastX - currentX), 2 * (currentY - lastY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.right - BIG_BUFFER && currentX <= rect.right + BIG_BUFFER) || (lastX >= rect.right - BIG_BUFFER && lastX <= rect.right + BIG_BUFFER))
                                        && ((currentY <= rect.bottom + BIG_BUFFER && currentY >= rect.bottom - BIG_BUFFER) || (lastY <= rect.bottom + BIG_BUFFER && lastY >= rect.bottom - BIG_BUFFER))) {
                                    // Bottom right corner: adjust both bottom and right sides
                                    cameraManager.adjustFramingRect(2 * (currentX - lastX), 2 * (currentY - lastY));
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.left - BUFFER && currentX <= rect.left + BUFFER) || (lastX >= rect.left - BUFFER && lastX <= rect.left + BUFFER))
                                        && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
                                    // Adjusting left side: event falls within BUFFER pixels of left side, and between top and bottom side limits
                                    cameraManager.adjustFramingRect(2 * (lastX - currentX), 0);
                                    viewfinderView.removeResultText();
                                } else if (((currentX >= rect.right - BUFFER && currentX <= rect.right + BUFFER) || (lastX >= rect.right - BUFFER && lastX <= rect.right + BUFFER))
                                        && ((currentY <= rect.bottom && currentY >= rect.top) || (lastY <= rect.bottom && lastY >= rect.top))) {
                                    // Adjusting right side: event falls within BUFFER pixels of right side, and between top and bottom side limits
                                    cameraManager.adjustFramingRect(2 * (currentX - lastX), 0);
                                    viewfinderView.removeResultText();
                                } else if (((currentY <= rect.top + BUFFER && currentY >= rect.top - BUFFER) || (lastY <= rect.top + BUFFER && lastY >= rect.top - BUFFER))
                                        && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
                                    // Adjusting top side: event falls within BUFFER pixels of top side, and between left and right side limits
                                    cameraManager.adjustFramingRect(0, 2 * (lastY - currentY));
                                    viewfinderView.removeResultText();
                                } else if (((currentY <= rect.bottom + BUFFER && currentY >= rect.bottom - BUFFER) || (lastY <= rect.bottom + BUFFER && lastY >= rect.bottom - BUFFER))
                                        && ((currentX <= rect.right && currentX >= rect.left) || (lastX <= rect.right && lastX >= rect.left))) {
                                    // Adjusting bottom side: event falls within BUFFER pixels of bottom side, and between left and right side limits
                                    cameraManager.adjustFramingRect(0, 2 * (currentY - lastY));
                                    viewfinderView.removeResultText();
                                }
                            }
                        } catch (NullPointerException e) {
                            Log.e(TAG, "Framing rect not available", e);
                        }
                        v.invalidate();
                        lastX = currentX;
                        lastY = currentY;
                        return true;
                    case MotionEvent.ACTION_UP:
                        lastX = -1;
                        lastY = -1;

                        return true;
                }
                return false;
            }
        });


        ZoomControls zoomControls = (ZoomControls) findViewById(R.id.CAMERA_ZOOM_CONTROLS);

        zoomControls.setIsZoomInEnabled(true);
        zoomControls.setIsZoomOutEnabled(true);

        zoomControls.setOnZoomInClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (currentZoomLevel < cameraManager.getMaxZoom()) {
                    currentZoomLevel++;
                    cameraManager.startZoom(currentZoomLevel);
                }
            }
        });

        zoomControls.setOnZoomOutClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (currentZoomLevel > 0) {
                    currentZoomLevel--;
                    cameraManager.startZoom(currentZoomLevel);
                }
            }
        });
    }


    @Override
    protected void onResume() {

        super.onResume();

            resetStatusView();

            if (cameraManager == null) {
                cameraManager = new CameraManager(getApplication(), this);
            }

            // Set up the camera preview surface.
            surfaceView = (SurfaceView) findViewById(R.id.preview_view);

            surfaceHolder = surfaceView.getHolder();

            if (!hasSurface) {
                Log.d(TAG, "onResume() hasSurface false");
                surfaceHolder.addCallback(this);
                surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
            } else {
                Log.d(TAG, "onResume() hasSurface true");
            }


            // Do OCR engine initialization, if necessary

            if (null == baseApi) {
                Log.d(TAG, "onResume() baseApi null");
                // Initialize the OCR engine
                File storageDirectory = getStorageDirectory();
                if (storageDirectory != null) {
                    initOcrEngine(storageDirectory, sourceLanguageCodeOcr, sourceLanguageReadable);
                }

            } else {
                Log.d(TAG, "onResume() baseApi is null; resuming OCR");
                resumeOCR();
            }

    }

    /**
     * Method to start or restart recognition after the OCR engine has been initialized,
     * or after the app regains focus. Sets state related settings and OCR engine parameters,
     * and requests camera initialization.
     */
    void resumeOCR() {

        Log.d(TAG, "resumeOCR()");

        // This method is called when Tesseract has already been successfully initialized, so set isEngineReady = true here.
        isEngineReady = true;

        isPaused = false;

        if (handler != null) {
            Log.d(TAG,"resumeOCR. handler is not null");
            handler.resetState();
        } else {
            Log.d(TAG,"resumeOCR. handler is null");
        }

        if (baseApi != null) {
            Log.d(TAG,"resumeOCR. baseAPI is not null");
            baseApi.setPageSegMode(pageSegmentationMode);
            baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, Constants.BLACKLIST_CHARS);
            baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, Constants.WHITELIST_CHARS);
        } else {
            Log.d(TAG,"resumeOCR. baseAPI is null");
        }

        if (hasSurface) {
            Log.d(TAG,"resumeOCR. hasSurface true");
            // The activity was paused but not stopped, so the surface still exists. Therefore
            // surfaceCreated() won't be called, so init the camera here.
            initCamera(surfaceHolder);
        } else {
            Log.d(TAG,"resumeOCR. hasSurface false");
        }
    }

    /** Called when the shutter button is pressed in continuous mode. */
    private void onShutterButtonPressContinuous() {
        isPaused = true;

        handler.stop();

        Log.d(TAG,"onShutterButtonPressContinuous() - playing beep");

        beepManager.playBeepSoundAndVibrate();

        if (lastResult != null) {
            handleOcrDecode(lastResult);
        } else {
            Toast toast = Toast.makeText(this, "OCR reads no valid result. Please try again.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
            resumeContinuousDecoding();
        }

        if (System.currentTimeMillis()%4==0) AdBuddiz.showAd(this);

    }

    /** Called to resume recognition after translation in continuous mode. */
    private void resumeContinuousDecoding() {
        isPaused = false;
        resetStatusView();
        setStatusViewForContinuous();
        DecodeHandler.resetDecodeState();
        handler.resetState();
        if (shutterButton != null && DISPLAY_SHUTTER_BUTTON) {
            shutterButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        Log.d(TAG, "surfaceCreated()");

        if (holder == null) {
            Log.e(TAG, "surfaceCreated gave us a null surface");
        }

        // Only initialize the camera if the OCR engine is ready to go.
        if (!hasSurface && isEngineReady) {
            Log.d(TAG, "surfaceCreated(): calling initCamera()...");
            initCamera(holder);
        }
        hasSurface = true;
    }

    /** Initializes the camera and starts the handler to begin previewing. */
    private void initCamera(SurfaceHolder surfaceHolder) {

        Log.d(TAG, "initCamera()");

        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        try {

            // Open and initialize the camera
            cameraManager.initDriver(surfaceHolder);

            // Creating the handler starts the preview, which can also throw a RuntimeException.
            handler = new CaptureActivityHandler(this, cameraManager, CONTINUOUS_MODE_ACTIVE);

        } catch (IOException | RuntimeException ioe) {
            Log.e(TAG,"camera init",ioe);
            showErrorMessage("Error", "Could not initialize camera. Please try restarting device.");
        }
    }


    void stopHandler() {

        Log.d(TAG,"stopHandler()");

        if (handler != null) {
            handler.stop();
        }
    }

    @Override
    protected void onPause() {

        Log.d(TAG,"onPause()");

            if (handler != null) {
                handler.quitSynchronously();
            }

            // Stop using the camera, to avoid conflicting with other camera-based apps
            if (cameraManager != null) cameraManager.closeDriver();

            if (!hasSurface) {
                SurfaceView surfaceView = (SurfaceView) findViewById(R.id.preview_view);
                SurfaceHolder surfaceHolder = surfaceView.getHolder();
                surfaceHolder.removeCallback(this);
            }

        super.onPause();
    }

    @Override
    protected void onDestroy() {

        Log.d(TAG,"onDestroy() ");

        if (baseApi != null) {
            try {
                baseApi.end();
            } catch (Exception e) {
                Log.e(TAG,e.getMessage(),e);
            }
        }

        if (lastBitmap!=null) lastBitmap.recycle(); //recycle the old image if any

        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            // First check if we're paused in continuous mode, and if so, just unpause.
            if (isPaused) {
                Log.d(TAG, "only resuming continuous recognition, not quitting...");
                resumeContinuousDecoding();
                return true;
            }

            // Exit the app if we're not viewing an OCR result.
            if (lastResult == null) {
                setResult(RESULT_CANCELED);
                finish();
                return true;
            } else {
                // Go back to previewing in regular OCR mode.
                resetStatusView();
                if (handler != null) {
                    handler.sendEmptyMessage(R.id.restart_preview);
                }
                return true;
            }
        } else if (keyCode == KeyEvent.KEYCODE_CAMERA) {
            if (CONTINUOUS_MODE_ACTIVE) {
                onShutterButtonPressContinuous();
            } else {
                handler.hardwareShutterButtonClick();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_FOCUS) {
            // Only perform autofocus if user is not holding down the button.
            if (event.getRepeatCount() == 0) {
                //cameraManager.requestAutoFocus(1500L);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //    MenuInflater inflater = getMenuInflater();
        //    inflater.inflate(R.menu.options_menu, menu);
        super.onCreateOptionsMenu(menu);
        menu.add(0, SETTINGS_ID, 0, "Settings").setIcon(android.R.drawable.ic_menu_preferences);
//        menu.add(0, ABOUT_ID, 0, "About").setIcon(android.R.drawable.ic_menu_info_details);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case SETTINGS_ID: {
                intent = new Intent().setClass(this, PreferencesActivity.class);
                startActivity(intent);
                break;
            }
 /*           case ABOUT_ID: {
                intent = new Intent(this, HelpActivity.class);
                intent.putExtra(HelpActivity.REQUESTED_PAGE_KEY, HelpActivity.ABOUT_PAGE);
                startActivity(intent);
                break;
            }*/

        }

        return super.onOptionsItemSelected(item);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG,"surfaceDestroyed()");
        hasSurface = false;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    private File getStorageDirectory() {
        //Log.d(TAG, "getStorageDirectory(): API level is " + Integer.valueOf(android.os.Build.VERSION.SDK_INT));

        String state = null;
        try {
            state = Environment.getExternalStorageState();
        } catch (RuntimeException e) {
            Log.e(TAG, "Is the SD card visible?", e);
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable.");
        }

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            // We can read and write the media
            //    	if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) > 7) {
            // For Android 2.2 and above

            try {
                return getExternalFilesDir(Environment.MEDIA_MOUNTED);
            } catch (NullPointerException e) {
                // We get an error here if the SD card is visible, but full
                Log.e(TAG, "External storage is unavailable");
                showErrorMessage("Error", "Required external storage (such as an SD card) is full or unavailable.");
            }

            //        } else {
            //          // For Android 2.1 and below, explicitly give the path as, for example,
            //          // "/mnt/sdcard/Android/data/edu.sfsu.cs.orange.ocr/files/"
            //          return new File(Environment.getExternalStorageDirectory().toString() + File.separator +
            //                  "Android" + File.separator + "data" + File.separator + getPackageName() +
            //                  File.separator + "files" + File.separator);
            //        }

        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            Log.e(TAG, "External storage is read-only");
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable for data storage.");
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            // to know is we can neither read nor write
            Log.e(TAG, "External storage is unavailable");
            showErrorMessage("Error", "Required external storage (such as an SD card) is unavailable or corrupted.");
        }
        return null;
    }


    private void initOcrEngine(File storageRoot, String languageCode, String languageName) {
        isEngineReady = false;

        // Set up the dialog box for the thermometer-style download progress indicator
        if (dialog != null) {
            dialog.dismiss();
        }
        dialog = new ProgressDialog(this);

        // If our language doesn't support Cube, then set the ocrEngineMode to Tesseract
        //if (ocrEngineMode != TessBaseAPI.OEM_TESSERACT_ONLY) {

        //}

        // Display the name of the OCR engine we're initializing in the indeterminate progress dialog box
        indeterminateDialog = new ProgressDialog(this);
        indeterminateDialog.setTitle("Please wait");
//        String ocrEngineModeName = "Tesseract";
        indeterminateDialog.setMessage("Initializing "+getString(R.string.app_name)+" engine...");
        indeterminateDialog.setCancelable(false);
        indeterminateDialog.show();

        if (handler != null) {
            handler.quitSynchronously();
        }

        // Start AsyncTask to install language data and init OCR
        baseApi = new TessBaseAPI();

        new OcrInitAsyncTask(this, baseApi, dialog, indeterminateDialog, languageCode, languageName, ocrEngineMode).execute(storageRoot.toString());
    }

    boolean handleOcrDecode(OcrResult ocrResult) {

        lastResult = ocrResult;

        // Test whether the result is null
        if (ocrResult.getText() == null || ocrResult.getText().equals("")) {
            Toast toast = Toast.makeText(this, getString(R.string.app_name)+" can't read. \nPlease try again.", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.TOP, 0, 0);
            toast.show();
            return false;
        }

        // Turn off capture-related UI elements
        shutterButton.setVisibility(View.GONE);
        //statusViewBottom.setVisibility(View.GONE);
        cameraButtonView.setVisibility(View.GONE);
        viewfinderView.setVisibility(View.GONE);
        seekBar.setVisibility(View.GONE);
        resultView.setVisibility(View.VISIBLE);

        ImageView bitmapImageView = (ImageView) findViewById(R.id.image_view);

        //TODO TBR
        if (lastBitmap!=null) lastBitmap.recycle(); //recycle the old image if any

        lastBitmap = ocrResult.getBitmap();

        if (lastBitmap == null) {
            bitmapImageView.setImageBitmap(BitmapFactory.decodeResource(getResources(),R.drawable.ic_launcher));
        } else {
            bitmapImageView.setImageBitmap(lastBitmap);
            //save(ocrResult.getBitmap());
        }

        TextView ocrResultTextView = (TextView) findViewById(R.id.ocr_result_text_view);

        ocrResultTextView.setText(postProcess(ocrResult.getText()));

        progressView.setVisibility(View.GONE);

        setProgressBarVisibility(false);

        return true;
    }


//    private final static String provincePattern      = "PR[0OoD]V[I1l]NS[I1l][a-zA-Z510\\s]+";
//    private final static String nikPattern           = "N[IZ:ETt]?K[-:=I](\\w+)";
//    private final static String kementerianPattern   = "KE[MN]ENTERIA[NM]+";
//    private final static String direktoratPattern    = "[D0][Il1]REKT[O0]R[A4]T";
//    private final static String npwpPattern          = "[NZ][Pp][WVw][Pp]:(\\w+)";

    private String postProcess(String result) {

        String[] resultLines = result.split("[\\r\\n]+");
        String trimmedResult = result.toUpperCase().replaceAll(" ","");

        /*Pattern provinceCompiledPattern = Pattern.compile(provincePattern);
        Matcher provinceMatcher = provinceCompiledPattern.matcher(trimmedResult);

        Pattern nikCompiledPattern = Pattern.compile(nikPattern);
        Matcher nikMatcher = nikCompiledPattern.matcher(trimmedResult);

        Pattern kementerianCompiledPattern = Pattern.compile(kementerianPattern);
        Matcher kementerianMatcher = kementerianCompiledPattern.matcher(trimmedResult.toUpperCase());

        Pattern direktoratCompiledPattern = Pattern.compile(direktoratPattern);
        Matcher direktoratMatcher = direktoratCompiledPattern.matcher(trimmedResult.toUpperCase());

        Pattern npwpCompiledPattern = Pattern.compile(npwpPattern);
        Matcher npwpMatcher = npwpCompiledPattern.matcher(trimmedResult);



        if (provinceMatcher.find() || nikMatcher.find() ) {*/

            return postProcessEKTP(result, resultLines,trimmedResult);
/*

        } else if (kementerianMatcher.find() || direktoratMatcher.find() || npwpMatcher.find()) {

            return postProcessNPWP(result);

        } else {
            return "Undefined Document\n"+result;
        }
*/

    }

    private String postProcessEKTP(String result, String[] resultLines, String trimmedResult) {

        KTPModel finalResult = new KTPModel();

        finalResult.setProvince(walkTroughLines(resultLines, Constants.PROVINCE_PATTERN, 0));
        finalResult.setKabupaten(walkTroughLines(resultLines, Constants.KABUPATEN_PATTERN, 0));
        if (finalResult.getKabupaten()==null) finalResult.setKabupaten(walkTroughLines(resultLines, Constants.KOTA_PATTERN,0));
        finalResult.setNik(walkTroughLines(resultLines, Constants.NIK_PATTERN, 1));
        finalResult.setName(walkTroughLines(resultLines, Constants.NAME_PATTERN, 1));
        finalResult.setSex(walkTroughLines(resultLines, Constants.SEX_PATTERN, 1));
        finalResult.setBloodType(walkTroughLines(resultLines, Constants.BLOOD_TYPE_PATTERN, 1));
        finalResult.setBirthPlace(walkTroughLines(resultLines, Constants.BIRTH_PLACE_PATTERN, 1));
        finalResult.setBirthday(walkTroughLines(resultLines, Constants.BIRTH_DAY_PATTERN, 2));
        finalResult.setKecamatan(walkTroughLines(resultLines, Constants.KECAMATAN_PATTERN, 1));
        finalResult.setAddress(walkTroughLines(resultLines, Constants.ADDRESS_PATTERN, 1));
        finalResult.setRtRw(walkTroughLines(resultLines, Constants.RTRW_PATTERN, 1));
        finalResult.setKelurahan(walkTroughLines(resultLines, Constants.DESA_PATTERN, 1));
        finalResult.setReligion(walkTroughLines(resultLines, Constants.RELIGION_PATTERN, 1));
        finalResult.setMaritalStatus(walkTroughLines(resultLines, Constants.MARITAL_STATUS_PATTERN, 1));
        finalResult.setOccupation(walkTroughLines(resultLines, Constants.JOB_PATTERN, 1));
        finalResult.setCitizenship(walkTroughLines(resultLines, Constants.CITIZENSHIP_PATTERN, 1));
        finalResult.setExpiryDate(walkTroughLines(resultLines, Constants.EXPIRED_DATE, 1));

        if (finalResult.getReligion() == null || finalResult.getReligion().isEmpty()) {
            Pattern r = Pattern.compile(Constants.ISLAM_PATTERN);
            Matcher m = r.matcher(trimmedResult);
            if (m.find()) {
                finalResult.setReligion("ISLAM");
            } else {
                r = Pattern.compile(Constants.KRISTEN_PATTERN);
                m = r.matcher(trimmedResult);
                if (m.find()) {
                    finalResult.setReligion("KRISTEN");
                }
            }
        }

        if (finalResult.getOccupation() == null || finalResult.getOccupation().isEmpty()) {
            Pattern r = Pattern.compile(Constants.KARYAWAN_SWASTA_PATTERN);
            Matcher m = r.matcher(trimmedResult);
            if (m.find()) {
                finalResult.setOccupation("KARYAWAN SWASTA");
            } else {

                r = Pattern.compile(Constants.PELAJAR_PATTERN);
                m = r.matcher(trimmedResult);

                if (m.find()) {
                    finalResult.setOccupation("PELAJAR");
                }
            }
        }

        if (finalResult.getCitizenship() == null || finalResult.getCitizenship().isEmpty()) {
            Pattern r = Pattern.compile(Constants.WNI_PATTERN);
            Matcher m = r.matcher(trimmedResult);
            if (m.find()) {
                finalResult.setCitizenship("WNI");
            }
        }

        if (finalResult.getMaritalStatus() == null || finalResult.getMaritalStatus().isEmpty()) {
            Pattern r = Pattern.compile(Constants.KAWIN_PATTERN);
            Matcher m = r.matcher(trimmedResult);
            if (m.find()) {
                finalResult.setMaritalStatus("KAWIN");
            } else {
                r = Pattern.compile(Constants.BELUM_KAWIN_PATTERN);
                m = r.matcher(trimmedResult);
                if (m.find()) {
                    finalResult.setMaritalStatus("BELUM KAWIN");
                }
            }
        }

        if (finalResult.getSex() == null || finalResult.getSex().isEmpty()) {
            Pattern r = Pattern.compile(Constants.LAKI_2_PATTERN);
            Matcher m = r.matcher(trimmedResult);
            if (m.find()) {
                finalResult.setSex("LAKI-LAKI");
            }
        }


        if ((finalResult.getRtRw()==null || finalResult.getRtRw().isEmpty()))
        {
            Pattern r = Pattern.compile(Constants.RT_RW_VALUE_PATTERN);
            Matcher m = r.matcher(trimmedResult.replaceAll(" ",""));
            if (m.find()) {
                finalResult.setRtRw(m.group(0));
            }
        }


        if ((finalResult.getProvince()==null || finalResult.getProvince().isEmpty())
                && finalResult.getNik()!=null && !finalResult.getNik().isEmpty())
        {
            try {
                finalResult.setProvince(dbHelper.lookupProvinsi(finalResult.getNik().trim().substring(0, 2)));
            }catch (Exception e){
                Log.e(TAG,e.getMessage(),e);
            }

        }

        if ((finalResult.getKabupaten()==null || finalResult.getKabupaten().isEmpty())
                && finalResult.getNik()!=null && !finalResult.getNik().trim().isEmpty())
        {
            try {
                finalResult.setKabupaten(dbHelper.lookupKabupaten(finalResult.getNik().trim().substring(0, 4)));
            }catch (Exception e){
                Log.e(TAG,e.getMessage(),e);
            }

        }

        if ((finalResult.getKecamatan()==null || finalResult.getKecamatan().isEmpty())
                && finalResult.getNik()!=null && !finalResult.getNik().isEmpty())
        {
            try {
                finalResult.setKecamatan(dbHelper.lookupKecamatan(finalResult.getNik().trim().substring(0, 6)));
            }catch (Exception e){
                Log.e(TAG,e.getMessage(),e);
            }

        }


        return "RAW DATA:\n"+cleanUp(resultLines)+"\nOBJECT:\n"+ finalResult.toString();

    }

    private String cleanUp(String[] resultLines) {

        StringBuilder sb = new StringBuilder();

        for (String s : resultLines) {

            String tmpStr = s.replaceAll("\\s","");

            if (tmpStr.length()>5) {
                sb.append(s.trim());
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /*private String postProcessNPWP(String result) {

        NPWPModel finalResult = new NPWPModel();

        Pattern compiledNpwpPattern = Pattern.compile(Constants.NPWP_PATTERN);

        Matcher npwpMatcher = compiledNpwpPattern.matcher(result);

        if (npwpMatcher.find()) {
            finalResult.setTaxId(npwpMatcher.group(1));
        }

        Pattern compiledNamePattern = Pattern.compile(Constants.TAX_SUBJECT_NAME_PATTERN);
        Matcher nameMatcher = compiledNamePattern.matcher(result);

        if (nameMatcher.find()) {
            finalResult.setTaxSubject(nameMatcher.group(1));
        }

        Pattern compiledRegisteredDatePattern= Pattern.compile(Constants.REGISTERED_DATE_PATTERN);
        Matcher registeredDateMatcher = compiledRegisteredDatePattern.matcher(result);

        if (registeredDateMatcher.find()) {
            finalResult.setRegisteredDate(registeredDateMatcher.group(1));
        }

        Pattern compiledIssuerPattern= Pattern.compile(Constants.ISSUER_PATTERN);
        Matcher issuerMatcher = compiledIssuerPattern.matcher(result);

        if (issuerMatcher.find()) {
            finalResult.setIssuer(issuerMatcher.group(1));
        }

        return finalResult.toString()
                +"\n-------------------------------"
                +"\nTap to copy data to clipboard.."
//                +"\n";
                + "\n<raw>\n"+result+"\n</raw>" ;

    }*/

    private String walkTroughLines(String[] resultLines, String pattern, int groupIndex) {

        String str=null;

        Pattern r = Pattern.compile(pattern);
        Matcher m;
        try {
            for (String line : resultLines) {
                m = r.matcher(line);

                if (m.find()) {
                    str = m.group(groupIndex);
                    break;
                }
            }

        } catch ( Exception e) {
            Log.e(TAG,e.getMessage());
        }

        return str;

    }

    void handleOcrContinuousDecode(OcrResult ocrResult) {

        Log.d(TAG,"handleOcrContinuousDecode");

        //Log.d(TAG,"Tess Version"+baseApi.getVersion());

        lastResult = ocrResult;

        // Send an OcrResultText object to the ViewfinderView for text rendering
        viewfinderView.addResultText(new OcrResultText(ocrResult.getText(),
                ocrResult.getWordConfidences(),
                ocrResult.getMeanConfidence(),
                ocrResult.getBitmapDimensions(),
                ocrResult.getRegionBoundingBoxes(),
                ocrResult.getTextLineBoundingBoxes(),
                ocrResult.getStripBoundingBoxes(),
                ocrResult.getWordBoundingBoxes(),
                ocrResult.getCharacterBoundingBoxes()), ocrResult.getBitmap());

        Integer meanConfidence = ocrResult.getMeanConfidence();




        if (CONTINUOUS_DISPLAY_METADATA) {
            // Display recognition-related metadata at the bottom of the screen
            statusViewBottom.setText("OCR mean confidence: " +meanConfidence.toString() +" ["+ocrResult.getText().length()+" chars]");
        }
    }

    void handleOcrContinuousDecode(OcrResultFailure obj) {

        lastResult = null;
        viewfinderView.removeResultText();

        if (CONTINUOUS_DISPLAY_METADATA) {
            CharSequence cs = setSpanBetweenTokens(getString(R.string.ocr_reads_no_valid_data), "-", new ForegroundColorSpan(0xFFFF0000));
            statusViewBottom.setText(cs);
        }
    }

    private CharSequence setSpanBetweenTokens(CharSequence text,
                                              String token,
                                              CharacterStyle... cs)
    {
        // Start and end refer to the points where the span will apply
        int tokenLen = token.length();
        int start = text.toString().indexOf(token) + tokenLen;
        int end = text.toString().indexOf(token, start);

        if (start > -1 && end > -1) {
            // Copy the spannable string to a mutable spannable string
            SpannableStringBuilder ssb = new SpannableStringBuilder(text);
            for (CharacterStyle c : cs)
                ssb.setSpan(c, start, end, 0);
            text = ssb;
        }
        return text;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu,
                                    View v,
                                    ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.equals(ocrResultView)) {
            menu.add(Menu.NONE, OPTIONS_COPY_RECOGNIZED_TEXT_ID, Menu.NONE, "Copy text");
            menu.add(Menu.NONE, OPTIONS_SHARE_RECOGNIZED_TEXT_ID, Menu.NONE, "Share text");
        }

    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        switch (item.getItemId()) {

            case OPTIONS_COPY_RECOGNIZED_TEXT_ID:
                clipboardManager.setText(ocrResultView.getText());
                if (clipboardManager.hasText()) {
                    Toast toast = Toast.makeText(this, "Text copied.", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();
                }
                return true;
            case OPTIONS_SHARE_RECOGNIZED_TEXT_ID:
                Intent shareRecognizedTextIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareRecognizedTextIntent.setType("text/plain");
                shareRecognizedTextIntent.putExtra(android.content.Intent.EXTRA_TEXT, ocrResultView.getText());
                startActivity(Intent.createChooser(shareRecognizedTextIntent, "Share via"));
                return true;
            /*case OPTIONS_COPY_TRANSLATED_TEXT_ID:
                //clipboardManager.setText(translationView.getText());
                if (clipboardManager.hasText()) {
                    Toast toast = Toast.makeText(this, "Text copied.", Toast.LENGTH_LONG);
                    toast.setGravity(Gravity.BOTTOM, 0, 0);
                    toast.show();
                }
                return true;
            case OPTIONS_SHARE_TRANSLATED_TEXT_ID:
                Intent shareTranslatedTextIntent = new Intent(android.content.Intent.ACTION_SEND);
                shareTranslatedTextIntent.setType("text/plain");
                //shareTranslatedTextIntent.putExtra(android.content.Intent.EXTRA_TEXT, translationView.getText());
                startActivity(Intent.createChooser(shareTranslatedTextIntent, "Share via"));
                return true;*/
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void resetStatusView() {
        resultView.setVisibility(View.GONE);
        if (CONTINUOUS_DISPLAY_METADATA) {
            statusViewBottom.setText("");
            statusViewBottom.setTextColor(getResources().getColor(R.color.status_text));
            statusViewBottom.setVisibility(View.VISIBLE);
        }

        viewfinderView.setVisibility(View.VISIBLE);

        cameraButtonView.setVisibility(View.VISIBLE);

        seekBar.setVisibility(View.VISIBLE);

        if (DISPLAY_SHUTTER_BUTTON) {
            shutterButton.setVisibility(View.VISIBLE);
        }

        lastResult = null;

        viewfinderView.removeResultText();
    }

    void setStatusViewForContinuous() {
        viewfinderView.removeResultText();
        if (CONTINUOUS_DISPLAY_METADATA) {
            statusViewBottom.setText(R.string.waiting_ocr_reading);
        }
    }

    void setButtonVisibility(boolean visible) {
        if (shutterButton != null && visible && DISPLAY_SHUTTER_BUTTON) {
            shutterButton.setVisibility(View.VISIBLE);
        } else if (shutterButton != null) {
            shutterButton.setVisibility(View.GONE);
        }
    }

    void setShutterButtonClickable(boolean clickable) {
        shutterButton.setClickable(clickable);
    }

    void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    @Override
    public void onShutterButtonClick(ShutterButton b) {
        if (CONTINUOUS_MODE_ACTIVE) {
            onShutterButtonPressContinuous();
        } else {
            if (handler != null) {
                handler.shutterButtonClick();
            }
        }
    }

    private void requestDelayedAutoFocus() {
        cameraManager.requestAutoFocus(350L);
    }

    private boolean checkFirstLaunch() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            int currentVersion = info.versionCode;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            int lastVersion = prefs.getInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, 0);
//            if (lastVersion == 0) {
//                isFirstLaunch = true;
//            } else {
//                isFirstLaunch = false;
//            }

            if (currentVersion > lastVersion) {
                prefs.edit().putInt(PreferencesActivity.KEY_HELP_VERSION_SHOWN, currentVersion).commit();
            }

        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e);
        }
        return false;
    }

    void displayProgressDialog() {
        // Set up the indeterminate progress dialog box
        indeterminateDialog = new ProgressDialog(this);
        indeterminateDialog.setTitle("Please wait");
        indeterminateDialog.setMessage("Performing OCR Reading ...");
        indeterminateDialog.setCancelable(false);
        indeterminateDialog.show();
    }

    ProgressDialog getProgressDialog() {
        return indeterminateDialog;
    }

    void showErrorMessage(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setOnCancelListener(new FinishListener(this))
                .setPositiveButton( "Done", new FinishListener(this))
                .show();
    }

    private void doSave(Bitmap mBitmap) {

        String filename;

        Date date = new Date();

        filename =  Constants.SIMPLE_DATE_FORMAT.format(date);

        try {

            String path = Environment.getExternalStorageDirectory().toString();

            OutputStream fOut;

            File folder = new File(path, Constants.SAVED_IMAGE_LOC);

            if (!folder.exists()) folder.mkdirs();

            File file = new File(path, Constants.SAVED_IMAGE_LOC+filename+".jpg");

            fOut = new FileOutputStream(file);

            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();

            MediaStore.Images.Media.insertImage(getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());

        }catch (Exception e) {
            Log.e(TAG,"error while saving file",e);
        }
    }

    private void save(Bitmap mBitmap) {

        Toast.makeText(getApplicationContext(),"save()",Toast.LENGTH_SHORT).show();

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            int hasCameraPermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

            List<String> permissions = new ArrayList<>();

            if (hasCameraPermission != PackageManager.PERMISSION_GRANTED) {
                permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
            } else {
                doSave(mBitmap);
            }

            if (!permissions.isEmpty()) {
                requestPermissions(permissions.toArray(new String[permissions.size()]), Constants.PERMISSIONS_REQUEST_STORAGE);
            }

        } else {
            doSave(mBitmap);
        }*/

        doSave(mBitmap);

    }

}
