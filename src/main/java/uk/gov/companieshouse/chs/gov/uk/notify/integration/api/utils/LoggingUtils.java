package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils;

import java.util.HashMap;
import java.util.Map;

public class LoggingUtils {

    private LoggingUtils() {}

    public static Map<String, Object> createLogMap(final String xRequestId, final String action) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("xRequestId", xRequestId);
        logMap.put("action", action);
        return logMap;
    }
}
