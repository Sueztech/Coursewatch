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

class Requests {

    private static final String TAG = "Requests";

    private static RequestQueue requestQueue;

    static void initQueue(Context context) {
        Log.v(TAG, "initQueue");
        if (requestQueue == null) {
            requestQueue = Volley.newRequestQueue(context);
        }
    }

    private static Request addStringRequest(final int id, int method, String url,
            final Map<String, String> params, final ResponseListener<String> responseListener) {

        Log.v(TAG, "addStringRequest(int, int, String, Map, ResponseListener)");

        StringRequest request = new StringRequest(method, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.d(TAG, "id=" + id + ", response=" + response);
                responseListener.onResponse(id, response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "(id=" + id + ") " + error);
                responseListener.onError(id, error);
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

    private static Request addJsonRequest(final int id, int method, String url,
            final Map<String, String> params, final ResponseListener<JSONObject> responseListener) {

        Log.v(TAG, "addJsonRequest(int, int, String, Map, ResponseListener)");

        return addStringRequest(id, method, url, params, new ResponseListener<String>() {
            @Override
            public void onResponse(int id, String response) {
                try {
                    JSONObject responseJson = new JSONObject(response);
                    responseListener.onResponse(id, responseJson);
                } catch (JSONException e) {
                    Log.e(TAG, "(id=" + id + ") " + e);
                    responseListener.onError(id, e);
                }
            }

            @Override
            public void onError(int id, Exception error) {
                Log.e(TAG, error.toString());
                responseListener.onError(id, error);
            }
        });

    }

    static Request addJsonRequest(final int id, String url, final Map<String, String> params,
            final ResponseListener<JSONObject> responseListener) {
        Log.v(TAG, "addJsonRequest(int, String, Map, ResponseListener)");
        int method = (params == null) ? Request.Method.GET : Request.Method.POST;
        return addJsonRequest(id, method, url, params, responseListener);
    }

    interface ResponseListener<T> {

        void onResponse(int id, T response);

        void onError(int id, Exception error);

    }

}
