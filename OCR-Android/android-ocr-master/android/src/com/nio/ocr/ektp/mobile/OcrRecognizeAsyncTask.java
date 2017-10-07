package com.nio.ocr.ektp.mobile;

import android.content.pm.ApplicationInfo;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.util.Log;
import com.googlecode.leptonica.android.*;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Date;


final class OcrRecognizeAsyncTask extends AsyncTask<Void, Void, Boolean> {

    private CaptureActivity activity;
    private TessBaseAPI baseApi;
    private byte[] data;
    private int width;
    private int height;
    private OcrResult ocrResult;

    OcrRecognizeAsyncTask(CaptureActivity activity, TessBaseAPI baseApi, byte[] data, int width, int height) {
        this.activity = activity;
        this.baseApi = baseApi;
        this.data = data;
        this.width = width;
        this.height = height;
    }


    /*static {
        if (!org.opencv.android.OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }*/


    @Override
    protected Boolean doInBackground(Void... arg0) {

        Bitmap originalBitmap = activity.getCameraManager().buildLuminanceSource(data, width, height).renderCroppedGreyscaleBitmap();

        Bitmap bitmap =  originalBitmap.copy(originalBitmap.getConfig(), true);

        String textResult;

         /*{
            org.opencv.core.Mat srcMat = new org.opencv.core.Mat (bitmap.getWidth(), bitmap.getHeight(), org.opencv.core.CvType.CV_8UC1);
             org.opencv.android.Utils.bitmapToMat(bitmap,srcMat);

             org.opencv.core.Mat convertedTo8UC1 = new org.opencv.core.Mat();
            srcMat.convertTo(convertedTo8UC1, org.opencv.core.CvType.CV_8UC1);

             org.opencv.imgproc.Imgproc.cvtColor(srcMat,convertedTo8UC1,org.opencv.imgproc.Imgproc.COLOR_RGB2GRAY,1);

             org.opencv.core.Mat finalMat = new org.opencv.core.Mat (bitmap.getWidth(), bitmap.getHeight(), org.opencv.core.CvType.CV_8UC1);
             org.opencv.imgproc.Imgproc.threshold(convertedTo8UC1, finalMat, 0, 255, Imgproc.THRESH_BINARY+org.opencv.imgproc.Imgproc.THRESH_OTSU);

             org.opencv.android.Utils.matToBitmap(finalMat, bitmap);
        }*/

        {
//            Pix thresholdedImage = Binarize.otsuAdaptiveThreshold(ReadFile.readBitmap(bitmap), 32, 32, 9, 9, 0.15F);
            Pix thresholdedImage = Binarize.otsuAdaptiveThreshold(ReadFile.readBitmap(bitmap), 36, 36, 10, 10, 0.2f);

            float rotation = Skew.findSkew(thresholdedImage);

            Pix rotatedImage = Rotate.rotate(thresholdedImage, (rotation), true, true);

            bitmap = WriteFile.writeBitmap(rotatedImage);

        }

        if (0 != (activity.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            save(bitmap);
        }

        try {

            baseApi.setImage(ReadFile.readBitmap(bitmap));

            textResult = baseApi.getUTF8Text();

            // Check for failure to recognize text
            if (textResult == null || textResult.equals("")) {
                return false;
            }

            ocrResult = new OcrResult();
            ocrResult.setWordConfidences(baseApi.wordConfidences());
            ocrResult.setMeanConfidence( baseApi.meanConfidence());
            ocrResult.setRegionBoundingBoxes(baseApi.getRegions().getBoxRects());
            ocrResult.setTextLineBoundingBoxes(baseApi.getTextlines().getBoxRects());
            ocrResult.setWordBoundingBoxes(baseApi.getWords().getBoxRects());
            ocrResult.setStripBoundingBoxes(baseApi.getStrips().getBoxRects());
            ocrResult.setOriginalBitmap(originalBitmap);
//            ocrResult.setCharacterBoundingBoxes(baseApi.getCharacters().getBoxRects());
        } catch (RuntimeException e) {
            Log.e(Constants.TAG, "Caught RuntimeException in request to Tesseract. Setting state to CONTINUOUS_STOPPED.",e);
            try {
                baseApi.clear();
                activity.stopHandler();
            } catch (NullPointerException e1) {
                Log.e(Constants.TAG,"NPE while clearing baseApi and stop activity handler",e1);
            }
            return false;
        }

        ocrResult.setBitmap(bitmap);
        ocrResult.setOriginalBitmap(originalBitmap);

        ocrResult.setText(textResult);

        return true;
    }



    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        Handler handler = activity.getHandler();
        if (handler != null) {
            // Send results for single-shot mode recognition.
            if (result) {
                Message message = Message.obtain(handler, R.id.ocr_decode_succeeded, ocrResult);
                message.sendToTarget();
            } else {
                Message message = Message.obtain(handler, R.id.ocr_decode_failed, ocrResult);
                message.sendToTarget();
            }
            activity.getProgressDialog().dismiss();
        }
        if (baseApi != null) {
            baseApi.clear();
        }
    }


    private void save(Bitmap mBitmap) {

        String filename;
        Date date = new Date();

        filename =  com.nio.ocr.ektp.mobile.Constants.SIMPLE_DATE_FORMAT.format(date);

        try{
            String path = Environment.getExternalStorageDirectory().toString();
            OutputStream fOut = null;

            File folder = new File(path, com.nio.ocr.ektp.mobile.Constants.SAVED_IMAGE_LOC);

            if (!folder.exists()) folder.mkdirs();

            File file = new File(path, com.nio.ocr.ektp.mobile.Constants.SAVED_IMAGE_LOC+filename+".jpg");

            fOut = new FileOutputStream(file);

            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fOut);
            fOut.flush();
            fOut.close();

            MediaStore.Images.Media.insertImage(activity.getContentResolver(),file.getAbsolutePath(),file.getName(),file.getName());

        }catch (Exception e) {
            e.printStackTrace();
        }

    }



    /*private void touchImage(Bitmap bitmap) {

            Pix thresholdedImage = Binarize.otsuAdaptiveThreshold(ReadFile.readBitmap(bitmap), 36, 36, 9, 9, 0.15F);

            float rotation = Skew.findSkew(thresholdedImage);

            Pix rotatedImage = Rotate.rotate(thresholdedImage,(rotation),true,true);

            bitmap = WriteFile.writeBitmap(rotatedImage);

        *//*else {
            Mat srcMat = new Mat (bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1);
            Utils.bitmapToMat(bitmap,srcMat);

            Mat convertedTo8UC1 = new Mat();
            srcMat.convertTo(convertedTo8UC1, CvType.CV_8UC1);

            Imgproc.cvtColor(srcMat,convertedTo8UC1,Imgproc.COLOR_RGB2GRAY,1);

            Mat finalMat = new Mat (bitmap.getWidth(), bitmap.getHeight(), CvType.CV_8UC1);
            Imgproc.threshold(convertedTo8UC1, finalMat, 0, 255, Imgproc.THRESH_BINARY+Imgproc.THRESH_OTSU);

            Utils.matToBitmap(finalMat, bitmap);
        }*//*
    }*/
}
