package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.API_KEY_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.INTERNAL_USER_ROLE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.COMPANY_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.PSC_FULL_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.REFERENCE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService.ERROR_MESSAGE_KEY;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService.NIL_UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Objects;
import org.json.JSONObject;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterResponseRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.letterdispatcher.LetterDispatcher;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.TemplateLookup;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith({SystemStubsExtension.class, OutputCaptureExtension.class})
class SenderRestApiIntegrationTest extends AbstractMongoDBTest {

    private static final String REQUEST_ID = "X9uND6rXQxfbZNcMVFA7JI4h2KOh";
    private static final String INVALID_REQUEST_ID = "X9uND6rXQxfbZ:cMVFA7JI4h2KOh";
    private static final String INVALID_REQUEST_ID_ERROR_MESSAGE_PREFIX =
            "Error in chs-gov-uk-notify-integration-api: request ID (X-Request-ID): must match ";
    private static final String REQUEST_ID_PATTERN = "[0-9A-Za-z-_]{8,32}";
    private static final String INVALID_REQUEST_ID_ERROR_MESSAGE =
            INVALID_REQUEST_ID_ERROR_MESSAGE_PREFIX + "\"" + REQUEST_ID_PATTERN + "\"";
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
    private static final String MISSING_COMPANY_NAME_ERROR_MESSAGE =
            "Error in chs-gov-uk-notify-integration-api: No company name found in the "
                    + "letter personalisation details.";
    private static final String MISSING_PSC_FULL_NAME_ERROR_MESSAGE =
            "Error in chs-gov-uk-notify-integration-api: Context variable(s) [psc_full_name] "
                    + "missing for LetterTemplateKey[appId=chips, id=direction_letter, version=1].";

    private static final String UNPARSABLE_PERSONALISATION_DETAILS_ERROR_MESSAGE_LINE_1 =
            "Error in chs-gov-uk-notify-integration-api: Failed to parse personalisation details:"
                    + " Unexpected character ('}' (code 125)): was expecting double-quote to "
                    + "start field name";
    private static final String UNPARSABLE_PERSONALISATION_DETAILS_ERROR_MESSAGE_LINE_2 =
            " at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); "
                    + "line: 1, column: 142]";
    private static final String UNPARSABLE_PERSONALISATION_DETAILS_ERROR_MESSAGE =
            UNPARSABLE_PERSONALISATION_DETAILS_ERROR_MESSAGE_LINE_1 + "\n"
            + UNPARSABLE_PERSONALISATION_DETAILS_ERROR_MESSAGE_LINE_2;
    private static final String UNKNOWN_APPLICATION_ERROR_MESSAGE =
            "Error in chs-gov-uk-notify-integration-api: Unable to find a valid context for "
                    + "LetterTemplateKey[appId=unknown_application, id=direction_letter, version=1]";
    private static final String UNKNOWN_TEMPLATE_VERSION_ERROR_MESSAGE =
            "Error in chs-gov-uk-notify-integration-api: Unable to find a valid context for "
                    + "LetterTemplateKey[appId=chips, id=direction_letter, version=2147483647]";
    private static final String UNKNOWN_TEMPLATE_ID_ERROR_MESSAGE =
            "Error in chs-gov-uk-notify-integration-api: Unable to find a valid context for "
                    + "LetterTemplateKey[appId=chips, id=new_letter, version=1]";
    private static final String TEMPLATE_NOT_FOUND_ERROR_MESSAGE =
    "Error in chs-gov-uk-notify-integration-api: An error happened during template parsing "
            + "(template: \"unknown_directory/chips/direction_letter_v1.html\") "
            + "[cause: java.io.FileNotFoundException: ClassLoader resource "
            + "\"unknown_directory/chips/direction_letter_v1.html\" could not be resolved]";
    private static final String REFERENCE_IN_PERSONALISATIONS_ERROR_MESSAGE =
            "Error in chs-gov-uk-notify-integration-api: The key field reference must not "
                    + "appear in the personalisation details.";
    private static final String MISSING_ADDRESS_LINES_ERROR_MESSAGE =
            "Error in chs-gov-uk-notify-integration-api: Context variable(s) "
                    + "[address_line_2, address_line_3] missing for "
                    + "LetterTemplateKey[appId=chips, id=direction_letter, version=1].";

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
    private LetterDispatcher letterDispatcher;

    @MockitoSpyBean
    private TemplateLookup templateLookup;

    @Mock
    private InputStream precompiledPdfInputStream;

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
        postSendLetterRequest(getValidSendLetterRequestBody(),
                status().isCreated());

        assertThat(log.getAll().contains("\"context\":\"" + REQUEST_ID + "\""), is(true));
        assertThat(log.getAll().contains("authorised as api key (internal user)"), is(true));
        assertThat(log.getAll().contains("\"xRequestId\":\"" + REQUEST_ID + "\""), is(true));
        assertThat(log.getAll().contains("emailAddress: vjackson1@companieshouse.gov.uk"), is(true));

        verifyLetterDetailsRequestStoredCorrectly();
        verifyLetterResponseStoredCorrectly(responseReceived);
        verifyLetterPdfSent(capturedFileSignature);
    }

    @Test
    @DisplayName("Send letter successfully with template version 1.0")
    void sendLetterSuccessfullyWithTemplateVersion1pt0() throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class))).thenReturn(responseReceived);

        // When and then
        postSendLetterRequest(getRequestWithTemplateVersion1pt0(),
                status().isCreated());
    }

    @Test
    @DisplayName("Send letter without providing the company name in the personalisation details")
    void sendLetterWithoutCompanyName(CapturedOutput log) throws Exception {

        // Given, when and then
        postSendLetterRequest(getRequestWithoutCompanyName(),
                status().isBadRequest())
                .andExpect(content().string(MISSING_COMPANY_NAME_ERROR_MESSAGE));

        assertThat(log.getAll().contains(MISSING_COMPANY_NAME_ERROR_MESSAGE), is(true));

        verifyLetterDetailsRequestStored();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter without providing the psc full name in the personalisation details")
    void sendLetterWithoutPscFullName(CapturedOutput log) throws Exception {

        // Given, when and then
        postSendLetterRequest(getRequestWithoutPscFullName(),
                status().isBadRequest())
                .andExpect(content().string(MISSING_PSC_FULL_NAME_ERROR_MESSAGE));

        assertThat(log.getAll().contains(MISSING_PSC_FULL_NAME_ERROR_MESSAGE), is(true));

        verifyLetterDetailsRequestStored();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter with unparsable personalisation details")
    void sendLetterWithUnparsablePersonalisationDetails(CapturedOutput log) throws Exception {

        // Given, when and then
        postSendLetterRequest(getRequestWithUnparsablePersonalisationDetails(),
                status().isBadRequest())
                .andExpect(content().string(UNPARSABLE_PERSONALISATION_DETAILS_ERROR_MESSAGE));

        assertThat(log.getAll().contains(UNPARSABLE_PERSONALISATION_DETAILS_ERROR_MESSAGE_LINE_1),
                is(true));
        assertThat(log.getAll().contains(UNPARSABLE_PERSONALISATION_DETAILS_ERROR_MESSAGE_LINE_2),
                is(true));

        verifyLetterDetailsRequestStored();
        verifyNoLetterResponsesAreStored();
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
        postSendLetterRequest(getValidSendLetterRequestBody(),
                status().isInternalServerError());

        assertThat(log.getAll().contains("\"context\":\"" + REQUEST_ID + "\""), is(true));
        assertThat(log.getAll().contains("authorised as api key (internal user)"), is(true));
        assertThat(log.getAll().contains("\"xRequestId\":\"" + REQUEST_ID + "\""), is(true));
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
                        .header(X_REQUEST_ID, INVALID_REQUEST_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE)
                        .content(getValidSendLetterRequestBody()))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(INVALID_REQUEST_ID_ERROR_MESSAGE));

        assertThat(log.getAll().contains(INVALID_REQUEST_ID_ERROR_MESSAGE_PREFIX), is(true));
        assertThat(log.getAll().contains(REQUEST_ID_PATTERN), is(true));

        verifyNoLetterDetailsRequestsAreStored();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter with an invalid request")
    void sendLetterWithInvalidRequest(CapturedOutput log) throws Exception {

        // When and then
        postSendLetterRequest(resourceToString("/fixtures/send-letter-request-missing-sender-reference-and-app-id.json",
                        UTF_8),
                status().isBadRequest())
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
                        .header(X_REQUEST_ID, REQUEST_ID)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE)
                        .content(getValidSendLetterRequestBody()))
                .andExpect(status().isUnauthorized());

        assertThat(log.getAll().contains("\"context\":\"" + REQUEST_ID + "\""), is(true));
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
                        .header(X_REQUEST_ID, REQUEST_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE)
                        .content(getValidSendLetterRequestBody()))
                .andExpect(status().isForbidden());

        assertThat(log.getAll().contains("\"context\":\"" + REQUEST_ID + "\""), is(true));
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
                        .header(X_REQUEST_ID, REQUEST_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .content(getValidSendLetterRequestBody()))
                .andExpect(status().isForbidden());

        assertThat(log.getAll().contains("\"context\":\"" + REQUEST_ID + "\""), is(true));
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
                        .header(X_REQUEST_ID, REQUEST_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, NON_INTERNAL_USER_ROLES)
                        .content(getValidSendLetterRequestBody()))
                .andExpect(status().isForbidden());

        assertThat(log.getAll().contains("\"context\":\"" + REQUEST_ID + "\""), is(true));
        assertThat(log.getAll().contains("user does not have internal user privileges"), is(true));

        verifyNoLetterDetailsRequestsAreStored();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter gracefully handles IOException loading letter PDF")
    void sendLetterHandlesPdfIOException(CapturedOutput log) throws Exception {

        // Given
        when(letterDispatcher.getPrecompiledPdf()).thenReturn(precompiledPdfInputStream);
        doThrow(new IOException("Thrown by test.")).when(precompiledPdfInputStream).close();

        // When and then
        postSendLetterRequest(getValidSendLetterRequestBody(),
                status().isInternalServerError());

        assertThat(log.getAll().contains(
                "Failed to load precompiled letter PDF. Caught IOException: Thrown by test."),
                is(true));

        verifyLetterDetailsRequestStoredCorrectly();
        verifyLetterResponseStored();
    }

    @Test
    @DisplayName("Send letter with unknown application ID")
    void sendLetterWithUnknownApplicationId(CapturedOutput log) throws Exception {

        // Given, when and then
        postSendLetterRequest(getRequestWithUnknownApplicationId(),
                status().isBadRequest())
                .andExpect(content().string(UNKNOWN_APPLICATION_ERROR_MESSAGE));

        assertThat(log.getAll().contains(UNKNOWN_APPLICATION_ERROR_MESSAGE), is(true));

        verifyLetterDetailsRequestStored();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter with unknown template version")
    void sendLetterWithUnknownTemplateVersion(CapturedOutput log) throws Exception {

        // Given, when and then
        postSendLetterRequest(getRequestWithUnknownTemplateVersion(),
                status().isBadRequest())
                .andExpect(content().string(UNKNOWN_TEMPLATE_VERSION_ERROR_MESSAGE));

        assertThat(log.getAll().contains(UNKNOWN_TEMPLATE_VERSION_ERROR_MESSAGE), is(true));

        verifyLetterDetailsRequestStored();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter with unknown template ID (aka letter)")
    void sendLetterWithUnknownTemplateId(CapturedOutput log) throws Exception {

        // Given, when and then
        postSendLetterRequest(getRequestWithUnknownTemplateId(),
                status().isBadRequest())
                .andExpect(content().string(UNKNOWN_TEMPLATE_ID_ERROR_MESSAGE));

        assertThat(log.getAll().contains(UNKNOWN_TEMPLATE_ID_ERROR_MESSAGE), is(true));

        verifyLetterDetailsRequestStored();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter cannot find letter template")
    void sendLetterCannotFindTemplate(CapturedOutput log) throws Exception {

        // Given, when and then
        when(templateLookup.getLetterTemplatesRootDirectory()).thenReturn("unknown_directory/");
        postSendLetterRequest(getValidSendLetterRequestBody(),
                status().isInternalServerError())
                .andExpect(content().string(TEMPLATE_NOT_FOUND_ERROR_MESSAGE));

        assertThat(log.getAll().contains(
                TEMPLATE_NOT_FOUND_ERROR_MESSAGE.replace("\"", "\\\"")),
                is(true));

        verifyLetterDetailsRequestStored();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter with reference in personalisation details")
    void sendLetterWithReferenceInPersonalisationDetails(CapturedOutput log) throws Exception {

        // Given, when and then
        postSendLetterRequest(getRequestWithReferenceInPersonalisationDetails(),
                status().isBadRequest())
                .andExpect(content().string(REFERENCE_IN_PERSONALISATIONS_ERROR_MESSAGE));

        assertThat(log.getAll().contains(REFERENCE_IN_PERSONALISATIONS_ERROR_MESSAGE), is(true));

        verifyLetterDetailsRequestStored();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter with too short an address")
    void sendLetterWithTooShortAnAddress(CapturedOutput log) throws Exception {

        // Given, when and then
        postSendLetterRequest(getRequestWithTooShortAnAddress(),
                status().isBadRequest())
                .andExpect(content().string(MISSING_ADDRESS_LINES_ERROR_MESSAGE));

        assertThat(log.getAll().contains(MISSING_ADDRESS_LINES_ERROR_MESSAGE), is(true));

        verifyLetterDetailsRequestStored();
        verifyNoLetterResponsesAreStored();
    }

    private ResultActions postSendLetterRequest(String requestBody,
                                                ResultMatcher expectedResponseStatus)
            throws Exception {
        return mockMvc.perform(post("/gov-uk-notify-integration/letter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_REQUEST_ID, REQUEST_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE)
                        .content(requestBody))
                .andExpect(expectedResponseStatus);
    }

    private static String getValidSendLetterRequestBody() throws IOException {
        return resourceToString("/fixtures/send-letter-request.json", UTF_8);
    }

    @SuppressWarnings("java:S1135") // TODO left in place intentionally for MVP.
    // TODO Post MVP Ideally this would use the letter ID returned in the HTTP
    // response payload to fetch the letter created.
    private void verifyLetterDetailsRequestStoredCorrectly() throws IOException {
        var sentRequest = objectMapper.readValue(
                getValidSendLetterRequestBody(),
                GovUkLetterDetailsRequest.class);
        assertThat(notificationDatabaseService.findAllLetters().isEmpty(), is(false));
        var storedRequest = notificationDatabaseService.findAllLetters().getFirst().getRequest();
        assertThat(storedRequest, is(sentRequest));
    }

    private void verifyLetterDetailsRequestStored() {
        assertThat(notificationDatabaseService.findAllLetters().size(), is(1));
    }

    private void verifyNoLetterDetailsRequestsAreStored() {
        assertThat(notificationDatabaseService.findAllLetters().isEmpty(), is(true));
    }

    private void verifyLetterResponseStoredCorrectly(LetterResponse receivedResponse) {
        assertThat(notificationLetterResponseRepository.findAll().isEmpty(), is(false));
        var storedResponse = notificationLetterResponseRepository.findAll().getFirst().getResponse();
        // Unfortunately SendLetter does not implement equals() and hashCode().
        assertThat(storedResponse.toString(), is(receivedResponse.toString()));
    }

    private void verifyLetterResponseStored() {
        assertThat(notificationLetterResponseRepository.findAll().isEmpty(), is(false));
    }

    private void verifyNoLetterResponsesAreStored() {
        assertThat(notificationLetterResponseRepository.findAll().isEmpty(), is(true));
    }

    private void verifyLetterErrorResponseStored() {
        assertThat(notificationLetterResponseRepository.findAll().isEmpty(), is(false));
        var storedResponse = notificationLetterResponseRepository.findAll().getFirst();

        // Ensure the stored response contains information about the error encountered.
        assertThat(storedResponse.getId(), is(notNullValue()));
        assertThat(storedResponse.getResponse(), is(notNullValue()));
        assertThat(storedResponse.getResponse().getNotificationId(), is(NIL_UUID));
        assertThat(storedResponse.getResponse().getReference().isPresent(), is(true));
        assertThat(storedResponse.getResponse().getReference().get(), is("send-letter-request"));
        assertThat(storedResponse.getResponse().getData(), is(notNullValue()));
        var data = storedResponse.getResponse().getData();
        assertThat(data.get("data"), is(notNullValue()));
        var map = ((JSONObject) data.get("data")).get("map");
        assertThat(map, is(notNullValue()));
        assertThat(((JSONObject) map).get("id"), is(NIL_UUID.toString()));
        assertThat(((JSONObject) map).get(ERROR_MESSAGE_KEY),
                is(INVALID_GOV_NOTIFY_API_KEY_ERROR_MESSAGE));
    }

    private static void verifyLetterPdfSent(StringBuilder fileSignature) {
        assertThat(Objects.equals(fileSignature.toString(), PDF_FILE_SIGNATURE), is(true));
    }

    private String getRequestWithoutCompanyName() throws IOException {
        return getRequestWithoutPersonalisation(COMPANY_NAME);
    }

    private String getRequestWithoutPscFullName() throws IOException {
        return getRequestWithoutPersonalisation(PSC_FULL_NAME);
    }

    private String getRequestWithTemplateVersion1pt0()
            throws IOException {
        var request = objectMapper.readValue(
                getValidSendLetterRequestBody(),
                GovUkLetterDetailsRequest.class);
        var letterDetails = request.getLetterDetails();
        letterDetails.setTemplateVersion(new BigDecimal("1.0"));
        return objectMapper.writeValueAsString(request);
    }

    private String getRequestWithoutPersonalisation(String personalisationName)
            throws IOException {
        var request = objectMapper.readValue(
                getValidSendLetterRequestBody(),
                GovUkLetterDetailsRequest.class);
        var letterDetails = request.getLetterDetails();
        var personalisationDetailsString = letterDetails.getPersonalisationDetails();

        var personalisationDetails = JsonParser
                .parseString(personalisationDetailsString)
                .getAsJsonObject();
        personalisationDetails.remove(personalisationName);

        letterDetails.setPersonalisationDetails(personalisationDetails.toString());
        return objectMapper.writeValueAsString(request);
    }

    private String getRequestWithReferenceInPersonalisationDetails()
            throws IOException {
        var request = objectMapper.readValue(
                getValidSendLetterRequestBody(),
                GovUkLetterDetailsRequest.class);
        var letterDetails = request.getLetterDetails();
        var personalisationDetailsString = letterDetails.getPersonalisationDetails();

        var personalisationDetails = JsonParser
                .parseString(personalisationDetailsString)
                .getAsJsonObject();
        personalisationDetails.addProperty(REFERENCE, "Test reference");

        letterDetails.setPersonalisationDetails(personalisationDetails.toString());
        return objectMapper.writeValueAsString(request);
    }

    private String getRequestWithUnparsablePersonalisationDetails()
            throws IOException {
        var request = objectMapper.readValue(
                getValidSendLetterRequestBody(),
                GovUkLetterDetailsRequest.class);
        var letterDetails = request.getLetterDetails();
        var personalisationDetailsString = letterDetails
                .getPersonalisationDetails()
                .replace("}",",}"); // this comma makes it unparsable
        letterDetails.setPersonalisationDetails(personalisationDetailsString);
        return objectMapper.writeValueAsString(request);
    }

    private String getRequestWithUnknownApplicationId()
            throws IOException {
        var request = objectMapper.readValue(
                getValidSendLetterRequestBody(),
                GovUkLetterDetailsRequest.class);
        request.getSenderDetails().setAppId("unknown_application");
        return objectMapper.writeValueAsString(request);
    }

    private String getRequestWithUnknownTemplateVersion()
            throws IOException {
        var request = objectMapper.readValue(
                getValidSendLetterRequestBody(),
                GovUkLetterDetailsRequest.class);
        request.getLetterDetails().setTemplateVersion(new BigDecimal(Integer.MAX_VALUE));
        return objectMapper.writeValueAsString(request);
    }

    private String getRequestWithUnknownTemplateId()
            throws IOException {
        var request = objectMapper.readValue(
                getValidSendLetterRequestBody(),
                GovUkLetterDetailsRequest.class);
        request.getLetterDetails().setTemplateId("new_letter");
        return objectMapper.writeValueAsString(request);
    }

    private String getRequestWithTooShortAnAddress()
            throws IOException {
        var request = objectMapper.readValue(
                getValidSendLetterRequestBody(),
                GovUkLetterDetailsRequest.class);
        request.getRecipientDetails().setPhysicalAddress(
                new Address().addressLine1("Recipient Name Only"));
        return objectMapper.writeValueAsString(request);
    }

}
