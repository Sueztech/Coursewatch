package com.sueztech.ktec.coursewatch;

class Util {
    static String bytesToHex(byte[] bytes) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
