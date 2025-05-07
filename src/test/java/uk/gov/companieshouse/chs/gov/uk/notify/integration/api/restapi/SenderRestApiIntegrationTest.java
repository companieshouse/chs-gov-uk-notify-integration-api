package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
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
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService.ERROR_MESSAGE_KEY;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService.NIL_UUID;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterResponseRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

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
    private static final String INVALID_GOV_NOTIFY_API_KEY_ERROR_MESSAGE =
            "Invalid token: service has no API keys";
    private static final String PDF_FILE_SIGNATURE = "%PDF-";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationDatabaseService notificationDatabaseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationLetterResponseRepository notificationLetterResponseRepository;

    @MockitoBean
    private NotificationClient notificationClient;

    @MockitoSpyBean
    private SenderRestApi senderRestApi;

    @Test
    @DisplayName("Send letter successfully")
    void sendLetterSuccessfully(CapturedOutput log) throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        var capturedFileSignature = new StringBuilder();
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class)))
                .thenAnswer(invocation -> {
                    InputStream inputStream = invocation.getArgument(1);
                    byte[] bytes = new byte[5];
                    if (inputStream.read(bytes) == 5) {
                        String header = new String(bytes);
                        capturedFileSignature.append(header);
                    }
                    return responseReceived;
                });

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

        verifyLetterDetailsRequestStoredCorrectly();
        verifyLetterResponseStoredCorrectly(responseReceived);
        verifyLetterPdfSent(capturedFileSignature);
    }

    @Test
    @DisplayName("Send letter with an invalid Gov Notify API key")
    void sendLetterWithInvalidApiKey(CapturedOutput log) throws Exception {

        // Given
        // Not exactly what happens in reality, as this mocking results in an exception with a 400
        // status code rather than the 403 status code expected. Unfortunately, the relevant
        // NotificationClientException constructor is package accessible only. For the purposes
        // of this test however, it's good enough.
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class)))
                .thenThrow(
                        new NotificationClientException(INVALID_GOV_NOTIFY_API_KEY_ERROR_MESSAGE));

        // When and then
        mockMvc.perform(post("/gov-uk-notify-integration/letter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_REQUEST_ID, CONTEXT_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE)
                        .content(resourceToString("/fixtures/send-letter-request.json", UTF_8)))
                .andExpect(status().isInternalServerError());

        assertThat(log.getAll().contains("\"context\":\"" + CONTEXT_ID + "\""), is(true));
        assertThat(log.getAll().contains("authorised as api key (internal user)"), is(true));
        assertThat(log.getAll().contains("\"contextId\":\"" + CONTEXT_ID + "\""), is(true));
        assertThat(log.getAll().contains("emailAddress: vjackson1@companieshouse.gov.uk"), is(true));

        verifyLetterDetailsRequestStoredCorrectly();
        verifyLetterErrorResponseStored();
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

        verifyNoLetterDetailsRequestsAreStored();
        verifyNoLetterResponsesAreStored();
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

        verifyNoLetterDetailsRequestsAreStored();
        verifyNoLetterResponsesAreStored();
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

        verifyNoLetterDetailsRequestsAreStored();
        verifyNoLetterResponsesAreStored();
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

        verifyNoLetterDetailsRequestsAreStored();
        verifyNoLetterResponsesAreStored();
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

        verifyNoLetterDetailsRequestsAreStored();
        verifyNoLetterResponsesAreStored();
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

        verifyNoLetterDetailsRequestsAreStored();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter gracefully handles IOException loading letter PDF")
    void sendLetterHandlesPdfIOException(CapturedOutput log) throws Exception {

        // Given
        when(senderRestApi.getPrecompiledPdf()).thenThrow(new IOException("Thrown by test."));

        // When and then
        mockMvc.perform(post("/gov-uk-notify-integration/letter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_REQUEST_ID, CONTEXT_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE)
                        .content(resourceToString("/fixtures/send-letter-request.json", UTF_8)))
                .andExpect(status().isInternalServerError());

        assertThat(log.getAll().contains(
                "Failed to load precompiled letter PDF. Caught IOException: Thrown by test."),
                is(true));

        verifyLetterDetailsRequestStoredCorrectly();
        verifyNoLetterResponsesAreStored();
    }


    // TODO Post MVP Ideally this would use the letter ID returned in the HTTP
    // response payload to fetch the letter created.
    private void verifyLetterDetailsRequestStoredCorrectly() throws IOException {
        var sentRequest = objectMapper.readValue(
                resourceToString("/fixtures/send-letter-request.json", UTF_8),
                GovUkLetterDetailsRequest.class);
        assertThat(notificationDatabaseService.findAllLetters().isEmpty(), is(false));
        var storedRequest = notificationDatabaseService.findAllLetters().getFirst().request();
        assertThat(storedRequest, is(sentRequest));
    }

    private void verifyNoLetterDetailsRequestsAreStored() {
        assertThat(notificationDatabaseService.findAllLetters().isEmpty(), is(true));
    }

    private void verifyLetterResponseStoredCorrectly(LetterResponse receivedResponse) {
        assertThat(notificationLetterResponseRepository.findAll().isEmpty(), is(false));
        var storedResponse = notificationLetterResponseRepository.findAll().getFirst().response();
        // Unfortunately SendLetter does not implement equals() and hashCode().
        assertThat(storedResponse.toString(), is(receivedResponse.toString()));
    }

    private void verifyNoLetterResponsesAreStored() {
        assertThat(notificationLetterResponseRepository.findAll().isEmpty(), is(true));
    }

    private void verifyLetterErrorResponseStored() {
        assertThat(notificationLetterResponseRepository.findAll().isEmpty(), is(false));
        var storedResponse = notificationLetterResponseRepository.findAll().getFirst();

        // Ensure the stored response contains information about the error encountered.
        assertThat(storedResponse.id(), is(notNullValue()));
        assertThat(storedResponse.response(), is(notNullValue()));
        assertThat(storedResponse.response().getNotificationId(), is(NIL_UUID));
        assertThat(storedResponse.response().getReference().isPresent(), is(true));
        assertThat(storedResponse.response().getReference().get(), is("send-letter-request"));
        assertThat(storedResponse.response().getData(), is(notNullValue()));
        var data = storedResponse.response().getData();
        assertThat(data.get("data"), is(notNullValue()));
        var map = ((JSONObject) data.get("data")).get("map");
        assertThat(map, is(notNullValue()));
        assertThat(((JSONObject) map).get("id"), is(NIL_UUID.toString()));
        assertThat(((JSONObject) map).get(ERROR_MESSAGE_KEY),
                is(INVALID_GOV_NOTIFY_API_KEY_ERROR_MESSAGE));
    }

    private void verifyLetterPdfSent(StringBuilder fileSignature) {
        assertThat(Objects.equals(fileSignature.toString(), PDF_FILE_SIGNATURE), is(true));
    }

}
