package com.nio.ocr.ektp.mobile;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import com.googlecode.tesseract.android.TessBaseAPI;
import org.xeustechnologies.jtar.TarEntry;
import org.xeustechnologies.jtar.TarInputStream;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

final class OcrInitAsyncTask extends AsyncTask<String, String, Boolean> {
    private static final String TAG = Constants.TAG;

    private CaptureActivity activity;
    private Context context;
    private TessBaseAPI baseApi;
    private ProgressDialog dialog;
    private ProgressDialog indeterminateDialog;
    private final String languageCode;
    private String languageName;
    private int ocrEngineMode;


    OcrInitAsyncTask(CaptureActivity activity, TessBaseAPI baseApi, ProgressDialog dialog,
                     ProgressDialog indeterminateDialog, String languageCode, String languageName,
                     int ocrEngineMode) {
        this.activity = activity;
        this.context = activity.getBaseContext();
        this.baseApi = baseApi;
        this.dialog = dialog;
        this.indeterminateDialog = indeterminateDialog;
        this.languageCode = languageCode;
        this.languageName = languageName;
        this.ocrEngineMode = ocrEngineMode;

        this.baseApi.setVariable("language_model_penalty_non_freq_dict_word", "0.11");
        this.baseApi.setVariable("language_model_penalty_non_dict_word", "0.15");
//        this.baseApi.setVariable("load_unambig_dawg", "1");//1
//        this.baseApi.setVariable("load_punc_dawg", "1");//1
        this.baseApi.setVariable("load_freq_dawg", "1");//1
        this.baseApi.setVariable("load_system_dawg", "1");//1
        this.baseApi.setVariable("language_model_penalty_punc", "0.8");
//        this.baseApi.setVariable("textord_min_xheight", "6");
        this.baseApi.setVariable("numeric_punctuation", ".-/");
//        this.baseApi.setVariable("unrecognised_char", ".");
//        this.baseApi.setVariable("osp_min_sane_kn_sp", "1.55"); //norm 1.5
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog.setTitle("Please wait");
        dialog.setMessage("Checking for data installation...");
        dialog.setIndeterminate(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.show();
        activity.setButtonVisibility(false);
    }


    protected Boolean doInBackground(String... params) {
        // Check whether we need Cube data or Tesseract data.
        // Example Cube data filename: "tesseract-ocr-3.01.eng.tar"
        // Example Tesseract data filename: "eng.traineddata"
//        String tarFile = languageCode + ".traineddata";
//        tarFile = "tesseract-ocr-3.03." + languageCode + ".tar";
        String tarFile = "tesseract-ocr-3.02." + languageCode + ".tar";

        // Check for, and create if necessary, folder to hold model data
        String destinationDirBase = params[0]; // The storage directory, minus the
        // "tessdata" subdirectory

        File tessdataDir = new File(destinationDirBase + File.separator + "tessdata");


        if (!tessdataDir.exists() && !tessdataDir.mkdirs()) {
            Log.e(TAG, "Couldn't make directory " + tessdataDir);
            return false;
        }

        Log.d(TAG,"tessDataDir : "+tessdataDir.getAbsolutePath());
        // Create a reference to the file to save the download in
        File downloadFile = new File(tessdataDir, tarFile);

        // Check if an incomplete download is present. If a *.download file is there, delete it and
        // any (possibly half-unzipped) Tesseract and Cube data files that may be there.
        File incomplete = new File(tessdataDir, tarFile + ".download");

        File tesseractTestFile = new File(tessdataDir, languageCode + ".traineddata");

        if (incomplete.exists()) {
            incomplete.delete();
            if (tesseractTestFile.exists()) {
                tesseractTestFile.delete();
            }
            //deleteCubeDataFiles(tessdataDir);
        }

        // If language data files are not present, install them
        boolean installSuccess = false;
        if (!tesseractTestFile.exists()) {
            Log.d(TAG, "Language data not found in " + tessdataDir.toString());
            //deleteCubeDataFiles(tessdataDir);

            if (!installSuccess) {
                // File was not packaged in assets, so download it
                Log.d(TAG, "Downloading " + tarFile + ".gz...");
                try {
                    installSuccess = downloadFile(tarFile, downloadFile);
                    if (!installSuccess) {
                        Log.e(TAG, "Download failed");
                        return false;
                    }
                } catch (IOException e) {
                    Log.e(TAG, "IOException received in doInBackground. Is a network connection available?");
                    return false;
                }
            }

            // If we have a tar file at this point because we downloaded v3.01+ data, untar it
            String extension = tarFile.substring(tarFile.lastIndexOf('.'),tarFile.length());

            if (extension.equals(".tar")) {
                try {
                    untar(new File(tessdataDir.toString() + File.separator + tarFile),tessdataDir);
                    installSuccess = true;
                } catch (IOException e) {
                    Log.e(TAG, "Extracting file failed");
                    Log.e(TAG, ""+e.toString());
                    return false;
                }
            }

        } else {
            Log.d(TAG, "Language data already installed in "+ tessdataDir.toString());
            installSuccess = true;
        }

        // Dismiss the progress dialog box, revealing the indeterminate dialog box behind it
        try {
            dialog.dismiss();
        } catch (IllegalArgumentException e) {
            // Catch "View not attached to window manager" error, and continue
        }

        // Initialize the OCR engine
        if (baseApi.init(destinationDirBase + File.separator, languageCode, ocrEngineMode)) {
            return installSuccess;// && osdInstallSuccess;
        }
        return false;
    }

    private boolean downloadFile(String sourceFilenameBase, File destinationFile)
            throws IOException {
        try {
            return downloadGzippedFileHttp(new URL(Constants.DOWNLOAD_BASE + sourceFilenameBase +".gz"),destinationFile);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Bad URL string.");
        }
    }


    private boolean downloadGzippedFileHttp(URL url, File destinationFile)
            throws IOException {
        // Send an HTTP GET request for the file
        Log.d(TAG, "Sending GET request to " + url + "...");
        publishProgress(context.getString(R.string.downloading_fonts), "0");
        HttpURLConnection urlConnection;
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setAllowUserInteraction(false);
        urlConnection.setInstanceFollowRedirects(true);
        urlConnection.setRequestMethod("GET");
        urlConnection.connect();
        if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            Log.e(TAG, "Did not get HTTP_OK response.");
            Log.e(TAG, "Response code: " + urlConnection.getResponseCode());
            Log.e(TAG, "Response message: " + urlConnection.getResponseMessage());
            return false;
        }
        int fileSize = urlConnection.getContentLength();
        InputStream inputStream = urlConnection.getInputStream();
        File tempFile = new File(destinationFile.toString() + ".gz.download");

        // Stream the file contents to a local file temporarily
        Log.d(TAG, "Streaming download to " + destinationFile.toString() + ".gz.download...");
        final int BUFFER = 8192;
        FileOutputStream fileOutputStream = null;
        Integer percentComplete;
        int percentCompleteLast = 0;
        try {
            fileOutputStream = new FileOutputStream(tempFile);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "Exception received when opening FileOutputStream.", e);
        }
        int downloaded = 0;
        byte[] buffer = new byte[BUFFER];
        int bufferLength;
        while ((bufferLength = inputStream.read(buffer, 0, BUFFER)) > 0) {
            fileOutputStream.write(buffer, 0, bufferLength);
            downloaded += bufferLength;
            percentComplete = (int) ((downloaded / (float) fileSize) * 100);
            if (percentComplete > percentCompleteLast) {
                publishProgress(context.getString(R.string.downloading_fonts),percentComplete.toString());
                percentCompleteLast = percentComplete;
            }
        }
        if (fileOutputStream != null) {
            fileOutputStream.close();
        }

        urlConnection.disconnect();

        // Uncompress the downloaded temporary file into place, and remove the temporary file
        try {
            Log.d(TAG, "Unzipping...");
            gUnzip(tempFile,
                    new File(tempFile.toString().replace(".gz.download", "")));
            return true;
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not available for unzipping.");
        } catch (IOException e) {
            Log.e(TAG, "Problem unzipping file.");
        }
        return false;
    }


    private void gUnzip(File zippedFile, File outFilePath)
            throws FileNotFoundException, IOException {
        int uncompressedFileSize = getGzipSizeUncompressed(zippedFile);
        Integer percentComplete;
        int percentCompleteLast = 0;
        int unzippedBytes = 0;
        final Integer progressMin = 0;
        int progressMax = 100 - progressMin;
        publishProgress(context.getString(R.string.data_uncompressing),
                progressMin.toString());

        // If the file is a tar file, just show progress to 50%
        String extension = zippedFile.toString().substring(
                zippedFile.toString().length() - 16);
        if (extension.equals(".tar.gz.download")) {
            progressMax = 50;
        }
        GZIPInputStream gzipInputStream = new GZIPInputStream(new BufferedInputStream(new FileInputStream(zippedFile)));
        OutputStream outputStream = new FileOutputStream(outFilePath);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
                outputStream);

        final int BUFFER = 8192;
        byte[] data = new byte[BUFFER];
        int len;
        while ((len = gzipInputStream.read(data, 0, BUFFER)) > 0) {
            bufferedOutputStream.write(data, 0, len);
            unzippedBytes += len;
            percentComplete = (int) ((unzippedBytes / (float) uncompressedFileSize) * progressMax)
                    + progressMin;

            if (percentComplete > percentCompleteLast) {
                publishProgress(context.getString(R.string.data_uncompressing), percentComplete.toString());
                percentCompleteLast = percentComplete;
            }
        }

        gzipInputStream.close();
        bufferedOutputStream.flush();
        bufferedOutputStream.close();

        if (zippedFile.exists()) {
            zippedFile.delete();
        }
    }


    private int getGzipSizeUncompressed(File zipFile) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(zipFile, "r");
        raf.seek(raf.length() - 4);
        int b4 = raf.read();
        int b3 = raf.read();
        int b2 = raf.read();
        int b1 = raf.read();
        raf.close();
        return (b1 << 24) | (b2 << 16) + (b3 << 8) + b4;
    }


    private void untar(File tarFile, File destinationDir) throws IOException {
        Log.d(TAG, "Untarring...");
        Log.d(TAG, "tarFile..."+(tarFile.exists()?"exist":"doesnt exist"));
        /*Log.d(TAG, "tarFile..."+(tarFile.getAbsolutePath()));
        Log.d(TAG, "destinationDir..."+(destinationDir.exists()?"exist":"doesnt exist"));
        Log.d(TAG, "destinationDir..."+(destinationDir.getAbsolutePath()));
*/

        final int uncompressedSize = getTarSizeUncompressed(tarFile);
        Integer percentComplete;
        int percentCompleteLast = 0;
        int unzippedBytes = 0;
        final Integer progressMin = 50;
        final int progressMax = 100 - progressMin;
        publishProgress(context.getString(R.string.data_uncompressing),progressMin.toString());

        // Extract all the files
        TarInputStream tarInputStream = new TarInputStream(new BufferedInputStream(new FileInputStream(tarFile)));
        TarEntry entry;
        while ((entry = tarInputStream.getNextEntry()) != null) {
            int len;
            final int BUFFER = 8192;
            byte data[] = new byte[BUFFER];
            String pathName = entry.getName();
            String fileName = pathName.substring(pathName.lastIndexOf('/'), pathName.length());
            OutputStream outputStream = new FileOutputStream(destinationDir + fileName);
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);

            Log.d(TAG, "Writing " + fileName.substring(1, fileName.length()) + "...");
            while ((len = tarInputStream.read(data, 0, BUFFER)) != -1) {
                bufferedOutputStream.write(data, 0, len);
                unzippedBytes += len;
                percentComplete = (int) ((unzippedBytes / (float) uncompressedSize) * progressMax)
                        + progressMin;
                if (percentComplete > percentCompleteLast) {
                    publishProgress(context.getString(R.string.data_uncompressing),
                            percentComplete.toString());
                    percentCompleteLast = percentComplete;
                }
            }
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
        }
        tarInputStream.close();

        if (tarFile.exists()) {
            tarFile.delete();
        }
    }


    private int getTarSizeUncompressed(File tarFile) throws IOException {
        int size = 0;
        TarInputStream tis = new TarInputStream(new BufferedInputStream(
                new FileInputStream(tarFile)));
        TarEntry entry;
        while ((entry = tis.getNextEntry()) != null) {
            if (!entry.isDirectory()) {
                size += entry.getSize();
            }
        }
        tis.close();
        return size;
    }

    @Override
    protected void onProgressUpdate(String... message) {
        super.onProgressUpdate(message);
        int percentComplete;

        percentComplete = Integer.parseInt(message[1]);
        dialog.setMessage(message[0]);
        dialog.setProgress(percentComplete);
        dialog.show();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        try {
            indeterminateDialog.dismiss();
        } catch (IllegalArgumentException e) {
            // Catch "View not attached to window manager" error, and continue
        }

        if (result) {
            // Restart recognition
            activity.resumeOCR();
        } else {
            activity.showErrorMessage("Error", context.getString(R.string.network_unreachable)
                    + context.getString(R.string.internet_pls_enable));
        }
    }
}