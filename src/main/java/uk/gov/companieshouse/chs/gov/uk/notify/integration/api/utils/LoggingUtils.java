package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils;

import java.util.Map;
import uk.gov.companieshouse.logging.util.DataMap;

public class LoggingUtils {

    public static final String VIEW_LETTER_PDF = "view_letter_pdf";

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
