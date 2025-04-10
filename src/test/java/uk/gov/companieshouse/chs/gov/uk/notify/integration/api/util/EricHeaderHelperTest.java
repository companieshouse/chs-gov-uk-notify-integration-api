package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.API_KEY_IDENTITY_TYPE;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EricHeaderHelperTest {

    @Mock
    private HttpServletRequest request;

    @Test
    @DisplayName("getIdentityType gets identity type from the ERIC identity type header")
    void getsIdentityTypeFromEricIdentityTypeHeader() {
        when(request.getHeader(ERIC_IDENTITY_TYPE)).thenReturn(API_KEY_IDENTITY_TYPE);
        assertEquals(API_KEY_IDENTITY_TYPE, EricHeaderHelper.getIdentityType(request));
    }

    @Test
    @DisplayName("getIdentityType returns null if the ERIC identity type header value is null")
    void getsNullIdentityTypeFromNullEricIdentityTypeHeaderValue() {
        when(request.getHeader(ERIC_IDENTITY_TYPE)).thenReturn(null);
        assertNull(EricHeaderHelper.getIdentityType(request));
    }

    @Test
    @DisplayName("getIdentityType returns null if the ERIC identity type header value is blank")
    void getsNullIdentityTypeFromBlankEricIdentityTypeHeaderValue() {
        when(request.getHeader(ERIC_IDENTITY_TYPE)).thenReturn(" ");
        assertNull(EricHeaderHelper.getIdentityType(request));
    }

}
