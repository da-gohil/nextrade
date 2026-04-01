package com.nextrade.common.util;

import java.util.UUID;

public class TraceIdUtil {
    private static final ThreadLocal<String> traceIdHolder = new ThreadLocal<>();

    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    public static void setTraceId(String traceId) {
        traceIdHolder.set(traceId);
    }

    public static String getTraceId() {
        String traceId = traceIdHolder.get();
        if (traceId == null) {
            traceId = generateTraceId();
            traceIdHolder.set(traceId);
        }
        return traceId;
    }

    public static void clear() {
        traceIdHolder.remove();
    }
}
