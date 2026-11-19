package com.cloudlabcopilot.common.trace;

import com.cloudlabcopilot.common.constant.AppConstants;
import org.slf4j.MDC;

public final class TraceIdContext {

    private TraceIdContext() {
    }

    public static void set(String traceId) {
        MDC.put(AppConstants.TRACE_ID_MDC_KEY, traceId);
    }

    public static String get() {
        return MDC.get(AppConstants.TRACE_ID_MDC_KEY);
    }

    public static void clear() {
        MDC.remove(AppConstants.TRACE_ID_MDC_KEY);
    }
}