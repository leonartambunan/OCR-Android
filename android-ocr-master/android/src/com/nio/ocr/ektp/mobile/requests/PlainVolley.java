/*
package com.nio.ocr.ektp.mobile.requests;

import android.os.Build;
import android.text.TextUtils;
import android.util.LruCache;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;

public class PlainVolley {

    private static final String TAG = "HttpVolleyPatterns";

    final static int MY_SOCKET_TIMEOUT_MS = 7000;
    final static int MY_MAX_RETRIES = 7;
    final static float MY_BACKOFF_MULTIPLIER = 1.5f;

    */
/**
     * Global request queue for Volley
     *//*


    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;


    */
/**
     * A singleton instance of the application class for easy access in other places
     *//*


    private static PlainVolley mInstance;


    private PlainVolley() {
        mRequestQueue = com.android.volley.toolbox.Volley.newRequestQueue(ConversedApplication.getAppContext());

        if (Build.VERSION.SDK_INT >= 12) {
            mImageLoader = new ImageLoader(this.mRequestQueue, new ImageLoader.ImageCache() {
                private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(50);

                public void putBitmap(String url, Bitmap bitmap) {
                    mCache.put(url, bitmap);
                }

                public Bitmap getBitmap(String url) {
                    return mCache.get(url);
                }
            });

        } else {
            mImageLoader = new ImageLoader(this.mRequestQueue, new ImageLoader.ImageCache() {

                private final android.support.v4.util.LruCache<String, Bitmap> mCache = new android.support.v4.util.LruCache<String, Bitmap>(50);

                public void putBitmap(String url, Bitmap bitmap) {
                    mCache.put(url, bitmap);
                }

                public Bitmap getBitmap(String url) {
                    return mCache.get(url);
                }
            });
        }
    }


    */
/**
     * @return ApplicationController singleton instance
     *//*


    public static synchronized PlainVolley getInstance() {
        if (mInstance == null) {
            mInstance = new PlainVolley();
        }
        return mInstance;
    }


    */
/**
     * @return The Volley Request queue, the queue will be created if it is null
     *//*


    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }


    */
/**
     * Adds the specified request to the global queue, if tag is specified
     * then it is used else Default TAG is used.
     *
     * @param req
     * @param tag
     *//*


    public <T> void addToRequestQueue(Request<T> req, String tag) {
        // set the default tag if tag is empty
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);

        mRequestQueue.add(req);
    }


    */
/**
     * Adds the specified request to the global queue using the Default TAG.
     *
     * @param req
     *//*


    public <T> void addToRequestQueue(Request<T> req) {
        // set the default tag if tag is empty

        if (null == req.getTag()) {
            req.setTag(TAG);
        }

        mRequestQueue.add(req);
    }


    */
/**
     * Cancels all pending requests by the specified TAG, it is important
     * to specify a TAG so that the pending/ongoing requests can be cancelled.
     *
     * @param tag
     *//*


    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
    }


}*/
