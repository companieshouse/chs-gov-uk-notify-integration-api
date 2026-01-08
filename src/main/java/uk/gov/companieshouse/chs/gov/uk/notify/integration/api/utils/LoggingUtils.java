package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils;

import java.util.Map;
import uk.gov.companieshouse.logging.util.DataMap;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.SendEmailResponse;

public class LoggingUtils {

    public static final String VIEW_LETTER_PDF = "view_letter_pdf";
    public static final String VIEW_LETTER_PDFS = "view_letter_pdfs";

    private LoggingUtils() {
    }

    // TODO DEEP-490? action, reference and notificationId should all be
    // structured logging DataMap members.
    private static final String ACTION = "action";
    private static final String REFERENCE = "reference";
    private static final String NOTIFICATION_ID = "notification_id";

    private static final String STORE_LETTER_RESPONSE = "store_letter_response";
    private static final String STORE_EMAIL_RESPONSE = "store_response";

    public static Map<String, Object> createLogMap(final String requestId, final String action) {
        var logMap = new DataMap.Builder()
                .requestId(requestId)
                .build()
                .getLogMap();
        logMap.put(ACTION, action);
        return logMap;
    }

    public static Map<String, Object> createLogMap(final String requestId,
                                                   final LetterResponse notifyResponse) {
        var logMap = createLogMap(requestId, STORE_LETTER_RESPONSE);
        if (notifyResponse != null) {
            logMap.put(REFERENCE, notifyResponse.getReference().orElse("[no reference supplied]"));
            logMap.put(NOTIFICATION_ID, notifyResponse.getNotificationId());
        }
        return logMap;
    }

    public static Map<String, Object> createLogMap(final String requestId,
                                                   final SendEmailResponse notifyResponse) {
        var logMap = createLogMap(requestId, STORE_EMAIL_RESPONSE);
        if (notifyResponse != null) {
            logMap.put(REFERENCE, notifyResponse.getReference().orElse("[no reference supplied]"));
            logMap.put(NOTIFICATION_ID, notifyResponse.getNotificationId());
        }
        return logMap;
    }
}
