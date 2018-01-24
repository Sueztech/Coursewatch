package com.sueztech.ktec.coursewatch;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

class Utils {

    private static final String TAG = "Utils";
    private static RequestQueue requestQueue;

    static String bytesToHex(byte[] bytes) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    static void initRequestQueue(Context context) {
        if (requestQueue != null) {
            requestQueue = Volley.newRequestQueue(context);
        }
    }

    private static Request addStringRequest(final int id, int method, String url,
            final Map<String, String> params, final ResponseListener<String> responseListener) {
        StringRequest request = new StringRequest(method, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, response);
                responseListener.onResponse(id, response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, error.toString());
                responseListener.onErrorResponse(id, error);
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                return params;
            }
        };
        requestQueue.add(request);
        return request;
    }

    static Request addJsonRequest(final int id, int method, String url,
            final Map<String, String> params, final ResponseListener<JSONObject> responseListener) {
        return addStringRequest(id, method, url, params, new ResponseListener<String>() {
            @Override
            public void onResponse(int id, String response) {
                try {
                    JSONObject responseJson = new JSONObject(response);
                    responseListener.onResponse(id, responseJson);
                } catch (JSONException e) {
                    Log.e(TAG, e.toString());
                    responseListener.onErrorResponse(id, new VolleyError(e));
                }
            }

            @Override
            public void onErrorResponse(int id, VolleyError error) {
                Log.e(TAG, error.toString());
                responseListener.onErrorResponse(id, error);
            }
        });
    }

    interface ResponseListener<T> {
        void onResponse(int id, T response);

        void onErrorResponse(int id, VolleyError error);
    }

}
