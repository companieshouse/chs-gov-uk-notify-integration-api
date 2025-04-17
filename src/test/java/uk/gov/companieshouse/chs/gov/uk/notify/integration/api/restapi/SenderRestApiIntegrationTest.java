package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.API_KEY_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.INTERNAL_USER_ROLE;

import java.io.InputStream;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@Tag("integration-test")
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith({SystemStubsExtension.class, OutputCaptureExtension.class})
class SenderRestApiIntegrationTest extends AbstractMongoDBTest {

    private static final String CONTEXT_ID = "X9uND6rXQxfbZNcMVFA7JI4h2KOh";
    private static final String INVALID_CONTEXT_ID = "X9uND6rXQxfbZ:cMVFA7JI4h2KOh";
    private static final String INVALID_CONTEXT_ID_ERROR_MESSAGE_PREFIX =
            "Error in chs-gov-uk-notify-integration-api: context ID (X-Request-ID): must match ";
    private static final String CONTEXT_ID_PATTERN = "[0-9A-Za-z-_]{8,32}";
    private static final String INVALID_CONTEXT_ID_ERROR_MESSAGE =
            INVALID_CONTEXT_ID_ERROR_MESSAGE_PREFIX + "\"" + CONTEXT_ID_PATTERN + "\"";
    private static final String EXPECTED_NULL_FIELDS_ERRORS =
            "Error(s) in chs-gov-uk-notify-integration-api: "
            + "[govUkLetterDetailsRequest senderDetails.appId must not be null, "
            + "govUkLetterDetailsRequest senderDetails.reference must not be null]";
    private static final String NON_INTERNAL_USER_ROLES = "role1 role2";
    private static final String X_REQUEST_ID = "X-Request-ID";
    private static final String ERIC_IDENTITY = "ERIC-Identity";
    private static final String ERIC_IDENTITY_VALUE = "65e73495c8e2";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationClient notificationClient;

    @Mock
    private LetterResponse letterResponse;

    @Test
    @DisplayName("Send letter successfully")
    void sendLetterSuccessfully(CapturedOutput log) throws Exception {

        // Given
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class)))
                .thenReturn(letterResponse);
        when(letterResponse.getNotificationId()).thenReturn(UUID.randomUUID());

        // When and then
        mockMvc.perform(post("/gov-uk-notify-integration/letter")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                        .header(X_REQUEST_ID, CONTEXT_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE)
                .content(resourceToString("/fixtures/send-letter-request.json", UTF_8)))
                .andExpect(status().isCreated());

        assertThat(log.getAll().contains("\"context\":\"" + CONTEXT_ID + "\""), is(true));
        assertThat(log.getAll().contains("authorised as api key (internal user)"), is(true));
        assertThat(log.getAll().contains("\"contextId\":\"" + CONTEXT_ID + "\""), is(true));
        assertThat(log.getAll().contains("emailAddress: vjackson1@companieshouse.gov.uk"), is(true));
    }

    @Test
    @DisplayName("Send letter with an invalid context ID")
    void sendLetterWithInvalidContextId(CapturedOutput log) throws Exception {

        // When and then
        mockMvc.perform(post("/gov-uk-notify-integration/letter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_REQUEST_ID, INVALID_CONTEXT_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE)
                        .content(resourceToString("/fixtures/send-letter-request.json", UTF_8)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(INVALID_CONTEXT_ID_ERROR_MESSAGE));

        assertThat(log.getAll().contains(INVALID_CONTEXT_ID_ERROR_MESSAGE_PREFIX), is(true));
        assertThat(log.getAll().contains(CONTEXT_ID_PATTERN), is(true));
    }

    @Test
    @DisplayName("Send letter with an invalid request")
    void sendLetterWithInvalidRequest(CapturedOutput log) throws Exception {

        // When and then
        mockMvc.perform(post("/gov-uk-notify-integration/letter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_REQUEST_ID, INVALID_CONTEXT_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE)
                        .content(resourceToString("/fixtures/send-letter-request-missing-sender-reference-and-app-id.json",
                                UTF_8)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(EXPECTED_NULL_FIELDS_ERRORS));

        assertThat(log.getAll().contains(EXPECTED_NULL_FIELDS_ERRORS), is(true));
    }

    @Test
    @DisplayName("Send letter without required ERIC identity header")
    void sendLetterWithoutEricIdentityHeader(CapturedOutput log) throws Exception {

        // When and then
        mockMvc.perform(post("/gov-uk-notify-integration/letter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_REQUEST_ID, CONTEXT_ID)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE)
                        .content(resourceToString("/fixtures/send-letter-request.json", UTF_8)))
                .andExpect(status().isUnauthorized());

        assertThat(log.getAll().contains("\"context\":\"" + CONTEXT_ID + "\""), is(true));
        assertThat(log.getAll().contains("no authorised identity"), is(true));
    }

    @Test
    @DisplayName("Send letter without required ERIC identity type header")
    void sendLetterWithoutEricIdentityTypeHeader(CapturedOutput log) throws Exception {

        // When and then
        mockMvc.perform(post("/gov-uk-notify-integration/letter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_REQUEST_ID, CONTEXT_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE)
                        .content(resourceToString("/fixtures/send-letter-request.json", UTF_8)))
                .andExpect(status().isForbidden());

        assertThat(log.getAll().contains("\"context\":\"" + CONTEXT_ID + "\""), is(true));
        assertThat(log.getAll().contains("invalid identity type [null]"), is(true));
    }

    @Test
    @DisplayName("Send letter without required ERIC authorised key roles header")
    void sendLetterWithoutEricAuthorisedKeyRolesHeader(CapturedOutput log) throws Exception {

        // When and then
        mockMvc.perform(post("/gov-uk-notify-integration/letter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_REQUEST_ID, CONTEXT_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .content(resourceToString("/fixtures/send-letter-request.json", UTF_8)))
                .andExpect(status().isForbidden());

        assertThat(log.getAll().contains("\"context\":\"" + CONTEXT_ID + "\""), is(true));
        assertThat(log.getAll().contains("user does not have internal user privileges"), is(true));
    }

    @Test
    @DisplayName("Send letter with insufficient privileges")
    void sendLetterWithInsufficientPrivileges(CapturedOutput log) throws Exception {

        // When and then
        mockMvc.perform(post("/gov-uk-notify-integration/letter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_REQUEST_ID, CONTEXT_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, NON_INTERNAL_USER_ROLES)
                        .content(resourceToString("/fixtures/send-letter-request.json", UTF_8)))
                .andExpect(status().isForbidden());

        assertThat(log.getAll().contains("\"context\":\"" + CONTEXT_ID + "\""), is(true));
        assertThat(log.getAll().contains("user does not have internal user privileges"), is(true));
    }

}
