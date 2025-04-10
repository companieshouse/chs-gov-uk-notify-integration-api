package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.util;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;

import jakarta.servlet.http.HttpServletRequest;

public class EricHeaderHelper {

    private EricHeaderHelper() { }

    public static String getIdentityType(HttpServletRequest request) {
        return getHeader(request, ERIC_IDENTITY_TYPE);
    }

    private static String getHeader(HttpServletRequest request, String headerName) {
        var headerValue = request.getHeader(headerName);
        if (isNotBlank(headerValue)) {
            return headerValue;
        } else {
            return null;
        }
    }

}
