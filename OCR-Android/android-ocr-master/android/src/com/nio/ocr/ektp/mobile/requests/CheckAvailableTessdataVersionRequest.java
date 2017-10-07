/*
package com.nio.ocr.ektp.mobile.requests;

import com.android.volley.*;

import java.util.HashMap;
import java.util.Map;

public class CheckAvailableTessdataVersionRequest  extends Request<String> {
    private final Response.Listener<String> mListener;
    private Map<String, String> mParams = new HashMap<String, String>();

    private final static String URL = "http://www.naulinovation.com/tessdata/ocrektp/latestversion.html";
    private final static String TAG = "checkLatestVersion";

    public CheckAvailableTessdataVersionRequest(Response.Listener<String> listener) {
        super(Method.GET, URL, null);
        mListener = listener;
        setTag(TAG);
        setRetryPolicy(new DefaultRetryPolicy(5000, 100, 1.2f));
    }

    @Override
    public Map<String, String> getParams() {
        return mParams;
    }

    @Override
    protected void deliverResponse(String response) {
        mListener.onResponse(response);
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {

        if (response.data != null) {
            return Response.success(new String(response.data), getCacheEntry());
        } else {
            return Response.error(new VolleyError());
        }
    }
}*/
