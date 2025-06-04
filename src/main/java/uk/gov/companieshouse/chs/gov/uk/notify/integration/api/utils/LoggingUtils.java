package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils;

import java.util.Map;
import uk.gov.companieshouse.logging.util.DataMap;

public class LoggingUtils {

    private LoggingUtils() {
    }

    public static Map<String, Object> createLogMap(final String requestId, final String action) {
        var logMap = new DataMap.Builder()
                .requestId(requestId)
                .build()
                .getLogMap();
        logMap.put("action", action);
        return logMap;
    }
}
