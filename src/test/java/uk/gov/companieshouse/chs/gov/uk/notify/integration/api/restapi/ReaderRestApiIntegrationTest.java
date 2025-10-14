package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
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
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.Constants.DATE_FORMATTER;

import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator.HtmlPdfGenerator;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;

@SpringBootTest(properties =
        {"logging.level.org.springframework.data.mongodb.core.MongoTemplate=DEBUG"})
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
    private static final String TOKEN_REFERENCE = "token reference";

    private static final String EXPECTED_LETTER_NOT_FOUND_ERROR_MESSAGE =
        "Error in chs-gov-uk-notify-integration-api: Letter not found for reference: "
            + REFERENCE_FOR_MISSING_LETTER;
    private static final String EXPECTED_TOO_MANY_LETTERS_FOUND_ERROR_MESSAGE =
        "Error in chs-gov-uk-notify-integration-api: Multiple letters found for reference: "
            + REFERENCE_SHARED_BY_MULTIPLE_LETTERS;
    private static final String EXPECTED_SECURITY_OK_LOG_MESSAGE =
            "authorised as api key (internal user)";

    private static final String PSC_NAME = "Joe Bloggs";
    private static final String COMPANY_NUMBER = "00006400";
    private static final String LETTER_TYPE = "new_psc_direction_letter_v1";
    private static final String LETTER_SENDING_DATE = "2025-04-08";
    private static final String NOT_LETTER_SENDING_DATE = "1999-12-30";
    private static final String UNPARSEABLE_LETTER_SENDING_DATE = "8 April 2025";

    private static final String EXPECTED_TOO_MANY_LETTERS_FOUND_ERROR_MESSAGE_2 =
            "Error in chs-gov-uk-notify-integration-api: Multiple letters found for psc name "
                    + PSC_NAME  + ", companyNumber "
                    + COMPANY_NUMBER + ", templateId "
                    + LETTER_TYPE + ", letter sending date "
                    + LETTER_SENDING_DATE + ".";

    private static final int INVALID_LETTER_0 = 0;
    private static final int LETTER_1 = 1;
    private static final int LETTER_2 = 2;

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

    @MockitoSpyBean
    private HtmlPdfGenerator pdfGenerator;

    @Mock
    private InputStream precompiledPdfInputStream;

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
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(REFERENCE_FOR_MISSING_LETTER)),
                is(true));
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
        assertThat(log.getAll().contains(
                getExpectedViewLetterInvocationLogMessage(REFERENCE_SHARED_BY_MULTIPLE_LETTERS)),
                is(true));
        assertThat(log.getAll().contains(EXPECTED_TOO_MANY_LETTERS_FOUND_ERROR_MESSAGE), is(true));
    }

    /**
     * Sends and views a <code>new_psc_direction_letter_v1</code> letter. Letters of this type
     * populate the letter sending date from the calculated <code>idv_start_date</code>
     * personalisation detail provided.
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
        assertThat(log.getAll().contains(
                getExpectedViewLetterInvocationLogMessage(
                        REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER)),
                is(true));
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
     * Sends and views a <code>transitional_non_director_psc_information_letter_v1</code> letter.
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
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                REFERENCE_FOR_TODAYS_SENDING_DATE_LETTER)),
                is(true));
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
                .format(DATE_FORMATTER);
        assertThat(page1, containsString("Date:\n" + originalSendingDate));
    }

    @Test
    @DisplayName("View letter reports IOException loading letter PDF with a 500 response")
    void viewLetterReportsPdfIOException(CapturedOutput log) throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class))).thenReturn(responseReceived);
        var requestBody = getSendLetterRequestWithReference(
                getValidSendDirectionLetterRequestBody(),
                REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER);
        postSendLetterRequest(mockMvc, requestBody, status().isCreated());

        doNothing().when(pdfGenerator).generatePdfFromHtml(anyString(), any(OutputStream.class));
        when(pdfGenerator.generatePdfFromHtml(anyString(), anyString()))
                .thenThrow(new IOException("Thrown by test."));

        // When and then
        viewLetterPdfByReference(REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER,
                status().isInternalServerError());

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER)),
                is(true));
        assertThat(log.getAll().contains(
                "Failed to load precompiled letter PDF. Caught IOException: Thrown by test."),
        is(true));
    }

    @Test
    @DisplayName("View letter reports IOException closing letter PDF stream with a 500 response")
    void viewLetterReportsPdfIOExceptionInClosingStream(CapturedOutput log) throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class))).thenReturn(responseReceived);
        var requestBody = getSendLetterRequestWithReference(
                getValidSendDirectionLetterRequestBody(),
                REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER);
        postSendLetterRequest(mockMvc, requestBody, status().isCreated());

        doNothing().when(pdfGenerator).generatePdfFromHtml(anyString(), any(OutputStream.class));
        when(pdfGenerator.generatePdfFromHtml(anyString(), anyString()))
                .thenReturn(precompiledPdfInputStream);
        doThrow(new IOException("Thrown by test.")).when(precompiledPdfInputStream).close();

        // When and then
       viewLetterPdfByReference(REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER,
                status().isInternalServerError());

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER)),
                is(true));
        var expectedLogMessage =
                "Responding with regenerated letter PDF to view for letter with reference "
                        + REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER;
        assertThat(log.getAll().contains(expectedLogMessage), is(true));
        assertThat(log.getAll().contains(
                "Failed to load precompiled letter PDF. Caught IOException: Thrown by test."),
                is(true));
    }

    @Test
    @DisplayName("View letter PDF identified by PSC name, company number, letter type and sending date successfully")
    void viewLetterByPscCompanyLetterTypeAndDateSuccessfully(CapturedOutput log) throws Exception {
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
        var letterPdf = viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
            COMPANY_NUMBER,
            LETTER_TYPE,
            LETTER_SENDING_DATE,
        status().isOk()).andReturn().getResponse().getContentAsByteArray();

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                PSC_NAME, COMPANY_NUMBER, LETTER_TYPE, LETTER_SENDING_DATE)),
                is(true));
        var expectedLogMessage =
                "Responding with regenerated letter PDF to view for letter with psc name "
                        + PSC_NAME + ", companyNumber "
                        + COMPANY_NUMBER + ", templateId "
                        + LETTER_TYPE + ", letter sending date "
                        + LETTER_SENDING_DATE + ".";
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

    @Test
    @DisplayName("View letter identified by PSC name, company number, letter type and sending date reports IOException loading letter PDF with a 500 response")
    void viewLetterByPscCompanyLetterTypeAndDateReportsPdfIOException(CapturedOutput log) throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class))).thenReturn(responseReceived);
        var requestBody = getSendLetterRequestWithReference(
                getValidSendDirectionLetterRequestBody(),
                REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER);
        postSendLetterRequest(mockMvc, requestBody, status().isCreated());

        doNothing().when(pdfGenerator).generatePdfFromHtml(anyString(), any(OutputStream.class));
        when(pdfGenerator.generatePdfFromHtml(anyString(), anyString()))
                .thenThrow(new IOException("Thrown by test."));

        // When and then
        viewLetterPdfByReference(REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER,
                status().isInternalServerError());

        viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                LETTER_TYPE,
                LETTER_SENDING_DATE,
                status().isInternalServerError());

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER)),
                is(true));
        assertThat(log.getAll().contains(
                "Failed to load precompiled letter PDF. Caught IOException: Thrown by test."),
                is(true));
    }

    @Test
    @DisplayName("Reports letter cannot be found if PSC name does not match")
    void unableToViewLetterAsPscNameNotMatched(CapturedOutput log) throws Exception {
        implementLetterNotFoundTest(
                log,
                PSC_NAME + " additional text",
                COMPANY_NUMBER,
                LETTER_TYPE,
                LETTER_SENDING_DATE);
    }

    @Test
    @DisplayName("Reports letter cannot be found if company number does not match")
    void unableToViewLetterAsCompanyNumberNotMatched(CapturedOutput log) throws Exception {
        implementLetterNotFoundTest(
                log,
                PSC_NAME,
                COMPANY_NUMBER + " additional text",
                LETTER_TYPE,
                LETTER_SENDING_DATE);
    }

    @Test
    @DisplayName("Reports letter cannot be found if letter type (i.e., template ID) does not match")
    void unableToViewLetterAsTemplateIdNotMatched(CapturedOutput log) throws Exception {
        implementLetterNotFoundTest(
                log,
                PSC_NAME,
                COMPANY_NUMBER,
                LETTER_TYPE + " additional text",
                LETTER_SENDING_DATE);
    }

    @Test
    @DisplayName("Reports letter cannot be found if letter sending date does not match")
    void unableToViewLetterAsLetterSendingDateNotMatched(CapturedOutput log) throws Exception {
        implementLetterNotFoundTest(
                log,
                PSC_NAME,
                COMPANY_NUMBER,
                LETTER_TYPE,
                NOT_LETTER_SENDING_DATE);
    }

    @Test
    @DisplayName("Rejects view letter request if letter sending date is not parseable")
    void unableToViewLetterAsLetterSendingDateNotParseable() throws Exception {

        // When and then
        var errorMessage = viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                LETTER_TYPE,
                UNPARSEABLE_LETTER_SENDING_DATE,
                status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(errorMessage.contains(
                "Failed to convert 'letter_sending_date' with value: '"
                        + UNPARSEABLE_LETTER_SENDING_DATE + "'"),
                is(true));
    }

    @Test
    @DisplayName("Reports fact that there is more than 1 letter with the same PSC name, company number, letter type and sending date")
    void unableToViewLetterAsMultipleLettersWithPscCompanyLetterTypeAndDateFound(CapturedOutput log)
            throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class))).thenReturn(responseReceived);

        var requestBody = getSendLetterRequestWithReference(
                getValidSendDirectionLetterRequestBody(),
                REFERENCE_FOR_CALCULATED_SENDING_DATE_LETTER);
        postSendLetterRequest(mockMvc, requestBody, status().isCreated());
        postSendLetterRequest(mockMvc, requestBody, status().isCreated());

        // When and then
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                LETTER_TYPE,
                LETTER_SENDING_DATE,
                status().isConflict())
                .andExpect(content().string(EXPECTED_TOO_MANY_LETTERS_FOUND_ERROR_MESSAGE_2));

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                getExpectedViewLetterInvocationLogMessage(PSC_NAME,
                        COMPANY_NUMBER,
                        LETTER_TYPE,
                        LETTER_SENDING_DATE)),
                is(true));
        assertThat(log.getAll().contains(EXPECTED_TOO_MANY_LETTERS_FOUND_ERROR_MESSAGE_2), is(true));
    }

    @Test
    @DisplayName("View letter PDFs identified by reference successfully")
    void viewLettersByReferenceSuccessfully(CapturedOutput log) throws Exception {

        // Given
        sendLetterWithReference(TOKEN_REFERENCE);

        // When and then
        viewLetterPdfByReference(TOKEN_REFERENCE, LETTER_1,
                status().isOk())
                .andReturn().getResponse().getContentAsByteArray();

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                TOKEN_REFERENCE, LETTER_1)),
                is(true));
        var expectedLogMessage =
                "Responding with regenerated letter PDF to view for letter number "
                        + LETTER_1 + " with reference "
                        + TOKEN_REFERENCE;
        assertThat(log.getAll().contains(expectedLogMessage), is(true));
    }

    @Test
    @DisplayName("Unable to view letter PDF 0 identified by reference")
    void unableToViewLetter0ByReference() throws Exception {

        // Given
        sendLetterWithReference(TOKEN_REFERENCE);

        // When and then
        var errorMessage = viewLetterPdfByReference(TOKEN_REFERENCE, INVALID_LETTER_0,
                status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(errorMessage.contains(
                        "Error in chs-gov-uk-notify-integration-api: Letter number ("
                                + INVALID_LETTER_0 + ") cannot be less than 1."),
                is(true));
    }

    @Test
    @DisplayName("Unable to view letter PDF 2 identified by reference")
    void unableToViewLetter2ByReference() throws Exception {

        // Given
        sendLetterWithReference(TOKEN_REFERENCE);

        // When and then
        var errorMessage = viewLetterPdfByReference(TOKEN_REFERENCE, LETTER_2,
                status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(errorMessage.contains(
                        "Error in chs-gov-uk-notify-integration-api: Letter number " + LETTER_2
                                + " not found. Total number of matching letters was 1."),
                is(true));
    }

    @Test
    @DisplayName("View letter PDFs identified by PSC name, company number, letter type and sending date successfully")
    void viewLettersByPscCompanyLetterTypeAndDateSuccessfully(CapturedOutput log) throws Exception {

        // Given
        sendLetter();

        // When and then
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                LETTER_TYPE,
                LETTER_SENDING_DATE,
                LETTER_1,
                status().isOk());

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                            PSC_NAME, COMPANY_NUMBER, LETTER_TYPE, LETTER_SENDING_DATE, LETTER_1)),
                is(true));
        var expectedLogMessage =
                "Responding with regenerated letter PDF to view for letter with psc name "
                        + PSC_NAME + ", companyNumber "
                        + COMPANY_NUMBER + ", templateId "
                        + LETTER_TYPE + ", letter sending date "
                        + LETTER_SENDING_DATE + ", letter number "
                        + LETTER_1 + ".";
        assertThat(log.getAll().contains(expectedLogMessage), is(true));
    }

    @Test
    @DisplayName("Unable to view letter PDF 0 identified by PSC name, company number, letter type and sending date")
    void unableToViewLetter0ByPscCompanyLetterTypeAndDate() throws Exception {

        // Given
        sendLetter();

        // When and then
        var errorMessage = viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                LETTER_TYPE,
                LETTER_SENDING_DATE,
                INVALID_LETTER_0,
                status().isBadRequest())
                    .andReturn().getResponse().getContentAsString();

        assertThat(errorMessage.contains(
                "Error in chs-gov-uk-notify-integration-api: Letter number ("
                        + INVALID_LETTER_0 + ") cannot be less than 1."),
                is(true));
    }

    @Test
    @DisplayName("Unable to view letter PDF 2 identified by PSC name, company number, letter type and sending date")
    void unableToViewLetter2ByPscCompanyLetterTypeAndDate() throws Exception {

        // Given
        sendLetter();

        // When and then
        var errorMessage = viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                LETTER_TYPE,
                LETTER_SENDING_DATE,
                LETTER_2,
                status().isNotFound())
                    .andReturn().getResponse().getContentAsString();

        assertThat(errorMessage.contains(
                "Error in chs-gov-uk-notify-integration-api: Letter number " + LETTER_2
                        + " not found. Total number of matching letters was 1."),
                is(true));
    }

    private void sendLetterWithReference(String reference) throws Exception {
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class))).thenReturn(responseReceived);
        var requestBody = getSendLetterRequestWithReference(
                getValidSendDirectionLetterRequestBody(), reference);
        postSendLetterRequest(mockMvc, requestBody, status().isCreated());
    }

    private void sendLetter() throws Exception {
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class))).thenReturn(responseReceived);
        postSendLetterRequest(mockMvc, getValidSendDirectionLetterRequestBody(),
                status().isCreated());
    }


    private void implementLetterNotFoundTest(CapturedOutput log,
                                             String pscName,
                                             String companyNumber,
                                             String templateId,
                                             String letterSendingDate) throws Exception {

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
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                pscName,
                companyNumber,
                templateId,
                letterSendingDate,
                status().isNotFound())
                .andExpect(content().string(getExpectedLetterNotFoundErrorMessage(
                        pscName,
                        companyNumber,
                        templateId,
                        letterSendingDate)));

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                pscName,
                                companyNumber,
                                templateId,
                                letterSendingDate)),
                is(true));
        assertThat(log.getAll().contains(getExpectedLetterNotFoundErrorMessage(
                        pscName,
                        companyNumber,
                        templateId,
                        letterSendingDate)),
                is(true));

    }


    private ResultActions viewLetterPdfByPscCompanyLetterTypeAndDate(String pscName,
                                                   String companyNumber,
                                                   String templateId,
                                                   String letterSendingDate,
                                                   ResultMatcher expectedResponseStatus)
            throws Exception {
        return mockMvc.perform(get("/gov-uk-notify-integration/letters/view"
                        + "?psc_name=" + pscName
                        + "&company_number=" + companyNumber
                        + "&template_id=" + templateId
                        + "&letter_sending_date="+ letterSendingDate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF_VALUE)
                        .header(X_REQUEST_ID, CONTEXT_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE))
                .andExpect(expectedResponseStatus);
    }

    private ResultActions viewLetterPdfByPscCompanyLetterTypeAndDate(String pscName,
                                                                     String companyNumber,
                                                                     String templateId,
                                                                     String letterSendingDate,
                                                                     int letterNumber,
                                                                     ResultMatcher expectedResponseStatus)
            throws Exception {
        return mockMvc.perform(get("/gov-uk-notify-integration/letters/paginated_view/"
                        + letterNumber
                        + "?psc_name=" + pscName
                        + "&company_number=" + companyNumber
                        + "&template_id=" + templateId
                        + "&letter_sending_date="+ letterSendingDate)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF_VALUE)
                        .header(X_REQUEST_ID, CONTEXT_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE))
                .andExpect(expectedResponseStatus);
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

    private ResultActions viewLetterPdfByReference(String reference,
                                                   int letterNumber,
                                                   ResultMatcher expectedResponseStatus)
            throws Exception {
        return mockMvc.perform(get("/gov-uk-notify-integration/letters/paginated_view/"
                        + reference + "/" + letterNumber)
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

    private static String getExpectedViewLetterInvocationLogMessage(String reference) {
        return   "{\"reference\":\"" + reference + "\","
                + "\"action\":\"view_letter_pdf\","
                + "\"message\":\"Starting viewLetterPdfByReference process\","
                + "\"request_id\":\"X9uND6rXQxfbZNcMVFA7JI4h2KOh\"}";
    }

    private static String getExpectedViewLetterInvocationLogMessage(String reference,
                                                                    int letterNumber) {
        return   "{\"reference\":\"" + reference + "\","
                + "\"letter\":" + letterNumber + ","
                + "\"action\":\"view_letter_pdfs\","
                + "\"message\":\"Starting viewLetterPdfsByReference process\","
                + "\"request_id\":\"X9uND6rXQxfbZNcMVFA7JI4h2KOh\"}";
    }

    private static String getExpectedViewLetterInvocationLogMessage(String pscName,
                                                                    String companyNumber,
                                                                    String templateId,
                                                                    String letterSendingDate) {
        return   "{\"letter_sending_date\":\"" + letterSendingDate + "\","
                + "\"company_number\":\"" + companyNumber + "\","
                + "\"psc_name\":\"" + pscName + "\","
                + "\"action\":\"view_letter_pdf\","
                + "\"template_id\":\"" + templateId + "\","
                + "\"message\":\"Starting viewLetterPdf process\","
                + "\"request_id\":\"X9uND6rXQxfbZNcMVFA7JI4h2KOh\"}";
    }

    private static String getExpectedViewLetterInvocationLogMessage(String pscName,
                                                                    String companyNumber,
                                                                    String templateId,
                                                                    String letterSendingDate,
                                                                    int letterNumber) {
        return   "{\"letter_sending_date\":\"" + letterSendingDate + "\","
                + "\"company_number\":\"" + companyNumber + "\","
                + "\"psc_name\":\"" + pscName + "\","
                + "\"letter\":" + letterNumber + ","
                + "\"action\":\"view_letter_pdfs\","
                + "\"template_id\":\"" + templateId + "\","
                + "\"message\":\"Starting viewLetterPdfs process\","
                + "\"request_id\":\"X9uND6rXQxfbZNcMVFA7JI4h2KOh\"}";
    }

    private static String getExpectedLetterNotFoundErrorMessage(String pscName,
                                                                String companyNumber,
                                                                String templateId,
                                                                String letterSendingDate) {
       return "Error in chs-gov-uk-notify-integration-api: Letter not found for psc name " + pscName
               + ", companyNumber " + companyNumber
               + ", templateId " + templateId
               + ", letter sending date " + letterSendingDate + ".";
    }

}
