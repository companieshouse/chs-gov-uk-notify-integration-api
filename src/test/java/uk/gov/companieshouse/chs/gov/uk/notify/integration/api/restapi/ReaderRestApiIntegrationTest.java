package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;

import static com.google.common.net.HttpHeaders.X_REQUEST_ID;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.API_KEY_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.INTERNAL_USER_ROLE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.getPageText;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.getValidSendLetterRequestBody;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.postSendLetterRequest;

import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ReaderRestApiIntegrationTest extends AbstractMongoDBTest {

    private static final String CONTEXT_ID = "X9uND6rXQxfbZNcMVFA7JI4h2KOh";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_ADDRESS_LINE = "123 Test Street";
    private static final String ERIC_IDENTITY = "ERIC-Identity";
    private static final String ERIC_IDENTITY_VALUE = "65e73495c8e2";
    private static final String ERIC_IDENTITY_OAUTH2_TYPE = "oauth2";

    private static final String REFERENCE_FOR_MISSING_LETTER = "never sent";
    private static final String REFERENCE_SHARED_BY_MULTIPLE_LETTERS =
            "more than 1 letter sent with this reference";
    private static final String REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER =
            "calculated sending date letter";
    private static final String REFERENCE_FOR_TODAYS_SENDING_DATE_LETTER =
            "today's sending date letter";


    private static final String EXPECTED_LETTER_NOT_FOUND_ERROR_MESSAGE =
        "Error in chs-gov-uk-notify-integration-api: Letter not found for reference: "
            + REFERENCE_FOR_MISSING_LETTER;
    private static final String EXPECTED_TOO_MANY_LETTERS_FOUND_ERROR_MESSAGE =
        "Error in chs-gov-uk-notify-integration-api: Multiple letters found for reference: "
            + REFERENCE_SHARED_BY_MULTIPLE_LETTERS;
    private static final String EXPECTED_SECURITY_OK_LOG_MESSAGE =
            "authorised as api key (internal user)";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationDatabaseService notificationDatabaseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationEmailRequestRepository notificationEmailRequestRepository;

    @Autowired
    private NotificationLetterRequestRepository notificationLetterRequestRepository;

    @MockitoBean
    private NotificationClient notificationClient;

    @Test
    void When_RequestingAllEmails_Expect_SuccessfulResponseWithEmailList() throws Exception {
        notificationEmailRequestRepository.deleteAll();

        GovUkEmailDetailsRequest emailRequest = TestUtils.createSampleEmailRequest(TEST_EMAIL);
        notificationDatabaseService.storeEmail(emailRequest);

        MvcResult result = mockMvc.perform(get("/gov-uk-notify-integration/emails")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", CONTEXT_ID))
                .andExpect(status().isOk())
                .andReturn();

        List<GovUkEmailDetailsRequest> emailResponses = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );

        assertNotNull(emailResponses);
        assertEquals(1, emailResponses.size());
        assertEquals(TEST_EMAIL, emailResponses.get(0).getRecipientDetails().getEmailAddress());
    }

    @Test
    void When_RequestingAllLetters_Expect_SuccessfulResponseWithLetterList() throws Exception {
        notificationLetterRequestRepository.deleteAll();
        
        GovUkLetterDetailsRequest letterRequest = TestUtils.createSampleLetterRequest(TEST_ADDRESS_LINE);
        notificationDatabaseService.storeLetter(letterRequest);

        MvcResult result = mockMvc.perform(get("/gov-uk-notify-integration/letters")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", CONTEXT_ID))
                .andExpect(status().isOk())
                .andReturn();

        List<GovUkLetterDetailsRequest> letterResponses = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );

        assertNotNull(letterResponses);
        assertEquals(1, letterResponses.size());
        assertEquals(TEST_ADDRESS_LINE, letterResponses.get(0).getRecipientDetails().getPhysicalAddress().getAddressLine1());
    }

    @Test
    void When_RequestingEmailById_Expect_SuccessfulResponseWithMatchingEmail() throws Exception {
        GovUkEmailDetailsRequest emailRequest = TestUtils.createSampleEmailRequest(TEST_EMAIL);
        NotificationEmailRequest savedEmail = notificationDatabaseService.storeEmail(emailRequest);
        String emailId = savedEmail.getId();

        MvcResult result = mockMvc.perform(get("/gov-uk-notify-integration/email/" + emailId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", CONTEXT_ID))
                .andExpect(status().isOk())
                .andReturn();

        GovUkEmailDetailsRequest emailResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                GovUkEmailDetailsRequest.class);

        assertNotNull(emailResponse);
        assertEquals(TEST_EMAIL, emailResponse.getRecipientDetails().getEmailAddress());
    }

    @Test
    void When_RequestingNonExistentEmailById_Expect_NotFoundResponse() throws Exception {
        String nonExistentId = "nonexistent123456789";

        mockMvc.perform(get("/gov-uk-notify-integration/email/" + nonExistentId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", CONTEXT_ID))
                .andExpect(status().isNotFound());
    }

    @DisplayName("Reports unauthenticated view letter request as unauthorised")
    @Test
    void viewLetterWithoutAuthIsUnauthorised(CapturedOutput log) throws Exception {
        mockMvc.perform(
                get("/gov-uk-notify-integration/letters/view/letter with a calculated sending date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF_VALUE)
                        .header(X_REQUEST_ID, CONTEXT_ID))
                .andExpect(status().isUnauthorized());

        assertThat(log.getAll().contains("no authorised identity"), is(true));
    }

    @DisplayName("Reports authenticated user view letter request as forbidden")
    @Test
    void viewLetterWithUserAuthIsForbidden(CapturedOutput log) throws Exception {
        mockMvc.perform(
                get("/gov-uk-notify-integration/letters/view/letter with a calculated sending date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF_VALUE)
                        .header(X_REQUEST_ID, CONTEXT_ID)
                        .header(ERIC_IDENTITY_TYPE, ERIC_IDENTITY_OAUTH2_TYPE)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());

        assertThat(log.getAll().contains("invalid identity type [oauth2]"), is(true));
    }

    @Test
    @DisplayName("Reports fact letter cannot be found by reference")
    void unableToViewLetterAsLetterWithReferenceNotFound(CapturedOutput log) throws Exception {
        viewLetterPdfByReference(REFERENCE_FOR_MISSING_LETTER,
                status().isNotFound())
                .andExpect(content().string(EXPECTED_LETTER_NOT_FOUND_ERROR_MESSAGE));

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(EXPECTED_LETTER_NOT_FOUND_ERROR_MESSAGE), is(true));
    }

    @Test
    @DisplayName("Reports fact that there is more than 1 letter with the same reference")
    void unableToViewLetterAsMultipleLettersWithReferenceFound(CapturedOutput log)
            throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class))).thenReturn(responseReceived);
        postSendLetterRequest(mockMvc,
                getSendLetterRequestWithReference(
                getValidSendLetterRequestBody(), REFERENCE_SHARED_BY_MULTIPLE_LETTERS),
                status().isCreated());
        postSendLetterRequest(mockMvc,
                getSendLetterRequestWithReference(
                getValidSendLetterRequestBody(), REFERENCE_SHARED_BY_MULTIPLE_LETTERS),
                status().isCreated());

        // When and then
        viewLetterPdfByReference(REFERENCE_SHARED_BY_MULTIPLE_LETTERS,
                status().isConflict())
                .andExpect(content().string(EXPECTED_TOO_MANY_LETTERS_FOUND_ERROR_MESSAGE));

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(EXPECTED_TOO_MANY_LETTERS_FOUND_ERROR_MESSAGE), is(true));
    }

    /**
     * Sends and views a <code>new_psc_direction_letter</code> letter. Letters of this type populate
     * the letter sending date from the calculated <code>idv_start_date</code> personalisation
     * detail provided.
     *
     * @param log the captured log output to be checked for relevant log messages
     * @throws Exception should something unexpected happen in the test
     */
    @Test
    @DisplayName("Responds with regenerated PDF for letter with calculated sending date")
    void viewLetterWithCalculatedLetterSendingDate(CapturedOutput log) throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class))).thenReturn(responseReceived);
        var requestBody = getSendLetterRequestWithReference(
                getValidSendDirectionLetterRequestBody(),
                REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER);
        postSendLetterRequest(mockMvc, requestBody, status().isCreated());

        // When and then
        var letterPdf = viewLetterPdfByReference(REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER,
                status().isOk()).andReturn().getResponse().getContentAsByteArray();

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        var expectedLogMessage =
                "Responding with regenerated letter PDF to view for letter with reference "
                        + REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER;
        assertThat(log.getAll().contains(expectedLogMessage), is(true));

        var document = Loader.loadPDF(letterPdf);

        // Substitutions all occur on page 1.
        var page1 = getPageText(document, 1);

        // Check reference in letter PDF.
        assertThat(page1, containsString(
                "Reference:\n" + REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER));

        // Check letter sending date in letter PDF is the calculated date provided
        var request = objectMapper.readValue(requestBody, GovUkLetterDetailsRequest.class);
        Map<String,String> personalisationDetails =
                objectMapper.readValue(request.getLetterDetails().getPersonalisationDetails(),
                        new TypeReference<>() {});
        var calculatedDate = personalisationDetails.get("idv_start_date");
        assertThat(page1, containsString("Date:\n" + calculatedDate));

    }

    /**
     * Sends and views a <code>transitional_non_director_psc_information_letter</code> letter.
     * Letters of this type populate the letter sending date from today's date at the time of
     * sending. However, when the letter PDF is recreated for viewing, the original sending
     * date must be used instead of today's date.
     *
     * @param log the captured log output to be checked for relevant log messages
     * @throws Exception should something unexpected happen in the test
     */
    @Test
    @DisplayName("Responds with regenerated PDF for letter with original sending date")
    void viewLetterWithOriginalTodaysLetterSendingDate(CapturedOutput log) throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class))).thenReturn(responseReceived);
        var requestBody = getSendLetterRequestWithReference(
                getValidSendInformationLetterRequestBody(),
                REFERENCE_FOR_TODAYS_SENDING_DATE_LETTER);
        postSendLetterRequest(mockMvc, requestBody, status().isCreated());

        // When and then
        var letterPdf = viewLetterPdfByReference(REFERENCE_FOR_TODAYS_SENDING_DATE_LETTER,
                status().isOk()).andReturn().getResponse().getContentAsByteArray();

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        var expectedLogMessage =
                "Responding with regenerated letter PDF to view for letter with reference "
                        + REFERENCE_FOR_TODAYS_SENDING_DATE_LETTER;
        assertThat(log.getAll().contains(expectedLogMessage), is(true));

        var document = Loader.loadPDF(letterPdf);

        // Substitutions all occur on page 1.
        var page1 = getPageText(document, 1);

        // Check reference in letter PDF.
        assertThat(page1, containsString(
                "Reference:\n" + REFERENCE_FOR_TODAYS_SENDING_DATE_LETTER));

        // Check letter sending date in letter PDF is the original sending date.
        var request = objectMapper.readValue(requestBody, GovUkLetterDetailsRequest.class);
        var originalSendingDate = request.getCreatedAt()
                .format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));
        assertThat(page1, containsString("Date:\n" + originalSendingDate));
    }

    private ResultActions viewLetterPdfByReference(String reference,
                                                   ResultMatcher expectedResponseStatus)
            throws Exception {
        return mockMvc.perform(get("/gov-uk-notify-integration/letters/view/" + reference)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF_VALUE)
                        .header(X_REQUEST_ID, CONTEXT_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE))
                .andExpect(expectedResponseStatus);
    }

    private String getSendLetterRequestWithReference(String requestBody, String reference)
            throws IOException {
        var request = objectMapper.readValue(requestBody, GovUkLetterDetailsRequest.class);
        request.getSenderDetails().setReference(reference);
        return objectMapper.writeValueAsString(request);
    }

    private static String getValidSendDirectionLetterRequestBody() throws IOException {
        return resourceToString("/fixtures/send-new-psc-direction-letter-request.json", UTF_8);
    }

    private static String getValidSendInformationLetterRequestBody() throws IOException {
        return resourceToString(
                "/fixtures/send-transitional-non-director-psc-information-letter-request.json",
                UTF_8);
    }

}
