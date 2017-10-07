package com.nio.ocr.ektp.mobile;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import com.googlecode.leptonica.android.*;
import com.googlecode.tesseract.android.TessBaseAPI;

import static com.nio.ocr.ektp.mobile.Constants.TAG;

final class DecodeHandler extends Handler {

    private final CaptureActivity activity;
    private boolean running = true;
    private final TessBaseAPI baseApi;
    private BeepManager beepManager;
    private Bitmap bitmap;
    private static boolean isDecodePending;

    DecodeHandler(CaptureActivity activity) {
        this.activity = activity;
        baseApi = activity.getBaseApi();
        beepManager = new BeepManager(activity);
    }

    @Override
    public void handleMessage(Message message) {
        if (!running) {
            return;
        }
        switch (message.what) {
            case R.id.ocr_continuous_decode:
                // Only request a decode if a request is not already pending.
                if (!isDecodePending) {
                    isDecodePending = true;
                    ocrContinuousDecode((byte[]) message.obj, message.arg1, message.arg2);
                }
                break;
            case R.id.ocr_decode:
                ocrDecode((byte[]) message.obj, message.arg1, message.arg2);
                break;
            case R.id.quit:
                running = false;
                Looper.myLooper().quit();
                break;
        }
    }

    static void resetDecodeState() {
        isDecodePending = false;
    }

    /**
     *  Launch an AsyncTask to perform an OCR decode for single-shot mode.
     *
     */
    private void ocrDecode(byte[] data, int width, int height) {

        Log.d(TAG,"ocrDecode() - playing beep");

        beepManager.playBeepSoundAndVibrate();

        activity.displayProgressDialog();

        // Launch OCR asynchronously, so we get the dialog box displayed immediately
        new OcrRecognizeAsyncTask(activity, baseApi, data, width, height).execute();
    }

    private void ocrContinuousDecode(byte[] data, int width, int height) {

        Log.d(TAG,"ocrContinuousDecode()");

        PlanarYUVLuminanceSource source = activity.getCameraManager().buildLuminanceSource(data, width, height);

        if (source == null) {
            sendContinuousOcrFailMessage();
            return;
        }

        Bitmap localBitmap = source.renderCroppedGreyscaleBitmap();

        if (localBitmap==null) {
            return;
        }

        Bitmap originalBitmap = localBitmap.copy(localBitmap.getConfig(), false);

        Pix thresholdedImage = Binarize.otsuAdaptiveThreshold(ReadFile.readBitmap(localBitmap), 36, 36, 7, 7, CaptureActivity.thresholdLevel);

        float rotation = Skew.findSkew(thresholdedImage);
        Pix deskewedImage = Rotate.rotate(thresholdedImage, (rotation), true, true);

        //bitmap = Bitmap.createBitmap(deskewedImage.getWidth(),deskewedImage.getHeight(),  Bitmap.Config.ARGB_4444);
        bitmap = WriteFile.writeBitmap(deskewedImage);

        thresholdedImage.recycle();
        deskewedImage.recycle();
        originalBitmap.recycle();

        OcrResult ocrResult = getOcrResult();

        Handler handler = activity.getHandler();

        if (handler == null) {
            return;
        }

        if (ocrResult == null) {
            try {
                sendContinuousOcrFailMessage();
            } catch (NullPointerException e) {
                activity.stopHandler();
            } finally {
                bitmap.recycle();
                baseApi.clear();
            }
            return;
        }

        try {
            ocrResult.setOriginalBitmap(originalBitmap);
            Message message = Message.obtain(handler, R.id.ocr_continuous_decode_succeeded, ocrResult);
            message.sendToTarget();
        } catch (NullPointerException e) {
            activity.stopHandler();
        } finally {
            localBitmap.recycle();
            baseApi.clear();
        }
    }

    private OcrResult getOcrResult() {
        OcrResult ocrResult;
        String textResult;

        try {

            baseApi.setImage(ReadFile.readBitmap(bitmap));

            textResult = baseApi.getUTF8Text();

            // Check for failure to recognize text
            if (textResult == null || textResult.equals("")) {
                return null;
            }
            ocrResult = new OcrResult();
            ocrResult.setWordConfidences(baseApi.wordConfidences());
            ocrResult.setMeanConfidence( baseApi.meanConfidence());
            if (ViewfinderView.DRAW_REGION_BOXES) {
                ocrResult.setRegionBoundingBoxes(baseApi.getRegions().getBoxRects());
            }
            if (ViewfinderView.DRAW_TEXT_LINE_BOXES) {
                ocrResult.setTextLineBoundingBoxes(baseApi.getTextlines().getBoxRects());
            }
            if (ViewfinderView.DRAW_STRIP_BOXES) {
                ocrResult.setStripBoundingBoxes(baseApi.getStrips().getBoxRects());
            }

            ocrResult.setWordBoundingBoxes(baseApi.getWords().getBoxRects());


            //TODO leonar

            baseApi.getWords().recycle();
            baseApi.getStrips().recycle();
            baseApi.getTextlines().recycle();
            baseApi.getRegions().recycle();

        } catch (RuntimeException e) {
            Log.e("OcrRecognizeAsyncTask", "Caught RuntimeException in request to Tesseract. Setting state to CONTINUOUS_STOPPED.");
            e.printStackTrace();
            try {
                baseApi.clear();
                activity.stopHandler();
            } catch (NullPointerException e1) {
                // Continue
            }
            return null;
        }
        ocrResult.setBitmap(bitmap);
        ocrResult.setText(textResult);
        return ocrResult;
    }

    private void sendContinuousOcrFailMessage() {
        Handler handler = activity.getHandler();
        if (handler != null) {
            Message message = Message.obtain(handler, R.id.ocr_continuous_decode_failed, new OcrResultFailure());
            message.sendToTarget();
        }
    }

}












