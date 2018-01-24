package com.sueztech.ktec.coursewatch;

import android.util.Log;

class Utils {

    private static final String TAG = "Utils";

    static String bytesToHex(byte[] bytes) {
        Log.v(TAG, "bytesToHex");
        final StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

}
