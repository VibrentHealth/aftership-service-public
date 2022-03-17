package com.vibrent.aftership.constants;

public class AfterShipConstants {
    private AfterShipConstants() { }

    public static final String HEADER_HMAC_SHA256_V4_4_4 = "Aftership-Hmac-Sha256";
    public static final String HEADER_HMAC_SHA256_V4_4_3 = "aftership-hmac-sha256";

    public static final String TAG_IN_TRANSIT = "InTransit";
    public static final String TAG_DELIVERED = "Delivered";
    public static final String TAG_EXCEPTION = "Exception";
    public static final String TAG_PENDING = "Pending";
}
