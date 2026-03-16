package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterRequestMapper;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterRequestRepository;

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
    private static final String ERIC_IDENTITY = "ERIC-Identity";
    private static final String ERIC_IDENTITY_VALUE = "65e73495c8e2";
    private static final String ERIC_IDENTITY_OAUTH2_TYPE = "oauth2";

    private static final String TOKEN_REFERENCE = "token reference";

    private static final String EXPECTED_LETTER_NOT_FOUND_ERROR_MESSAGE =
        "Error in chs-gov-uk-notify-integration-api: Letter not found for reference: "
            + TOKEN_REFERENCE;
    private static final String EXPECTED_LETTERS_NOT_FOUND_ERROR_MESSAGE =
            "Error in chs-gov-uk-notify-integration-api: Letter number 1 not found. "
                    + "Total number of matching letters was 0.";
    private static final String EXPECTED_TOO_MANY_LETTERS_FOUND_ERROR_MESSAGE =
        "Error in chs-gov-uk-notify-integration-api: Multiple letters found for reference: "
            + TOKEN_REFERENCE;
    private static final String EXPECTED_SECURITY_OK_LOG_MESSAGE =
            "authorised as api key (internal user)";

    private static final String PSC_NAME = "ANDREWPHILLIPLONGNAME BARROW";
    private static final String NULL_PSC_NAME = null;
    private static final String COMPANY_NUMBER = "00006400";
    private static final String TEST_LETTER_ID = "IDVPSCDIRNEW";
    private static final String TEST_TEMPLATE_ID = "v1.0";
    private static final String LETTER_SENDING_DATE = "2025-04-08";

    private static final String EXPECTED_TOO_MANY_LETTERS_FOUND_ERROR_MESSAGE_2 =
            "Error in chs-gov-uk-notify-integration-api: Multiple letters found for psc name "
                    + PSC_NAME  + ", companyNumber "
                    + COMPANY_NUMBER +  ", letterId "
                    + TEST_LETTER_ID + ", templateId "
                    + TEST_TEMPLATE_ID + ", letter sending date "
                    + LETTER_SENDING_DATE + ".";

    private static final int INVALID_LETTER_0 = 0;
    private static final int LETTER_1 = 1;
    private static final int LETTER_2 = 2;
    private static final int LETTER_3 = 3;
    private static final int LETTER_4 = 4;

    private static final String GET_LETTER_DETAILS_BY_REFERENCE_PATH =
            "/gov-uk-notify-integration/letters/reference";
    private static final String VIEW_LETTER_PDF_BY_REFERENCE =
            "/gov-uk-notify-integration/letters/view_by_reference";
    private static final String VIEW_LETTER_PDFS_BY_REFERENCE =
            "/gov-uk-notify-integration/letters/view_by_reference/paginated_view/";
    private static final String VIEW_LETTER_PDF =
            "/gov-uk-notify-integration/letters/view";
    private static final String VIEW_LETTER_PDFS =
            "/gov-uk-notify-integration/letters/paginated_view/";

    private static final String VIEW_LETTERS_BY_REFERENCE_URI =
            VIEW_LETTER_PDFS_BY_REFERENCE + "1?reference=reference";
    private static final String VIEW_LETTER_BY_SELECTION_CRITERIA_URI =
            VIEW_LETTER_PDF + "?"
            + "letter_sending_date=2025-10-14"
            + "&psc_name=Joe Bloggs"
            + "&company_number=00006400"
            + "&template_id=new_psc_direction_letter_v1";
    private static final String VIEW_LETTERS_BY_SELECTION_CRITERIA_URI =
            VIEW_LETTER_PDFS + "1?"
                    + "letter_sending_date=2025-10-14"
                    + "&psc_name=Joe Bloggs"
                    + "&company_number=00006400"
                    + "&template_id=new_psc_direction_letter_v1";

    private static final String OTHER_PERSONALISATIONS =
            "{ \"idv_start_date\": \"30 June 2025\", "
                    + "\"psc_appointment_date\": \"24 June 2025\", "
                    + "\"idv_verification_due_date\": \"14 July 2025\",  "
                    + "\"company_name\": \"Tŷ'r Cwmnïau\", ";

    @Autowired
    private MockMvc mockMvc;

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

        EmailRequestDao emailRequest = TestUtils.createSampleEmailRequest(TEST_EMAIL);
        saveEmail(emailRequest);

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

        var letterRequest = createLetter();

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
        assertEquals(letterRequest, LetterRequestMapper.toDao(letterResponses.get(0)));
    }

    @Test
    void When_RequestingEmailById_Expect_SuccessfulResponseWithMatchingEmail() throws Exception {
        EmailRequestDao emailRequest = TestUtils.createSampleEmailRequest(TEST_EMAIL);
        NotificationEmailRequest savedEmail = saveEmail(emailRequest);
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
                get(VIEW_LETTER_PDF_BY_REFERENCE
                        + "?reference=letter with a calculated sending date")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF_VALUE)
                        .header(X_REQUEST_ID, CONTEXT_ID))
                .andExpect(status().isUnauthorized());

        assertThat(log.getAll().contains("no authorised identity"), is(true));
    }

    @DisplayName("Reports unauthenticated view letters request as unauthorised")
    @Test
    void viewLettersWithoutAuthIsUnauthorised(CapturedOutput log) throws Exception {
        mockMvc.perform(
                get(VIEW_LETTERS_BY_REFERENCE_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF_VALUE)
                        .header(X_REQUEST_ID, CONTEXT_ID))
                .andExpect(status().isUnauthorized());

        assertThat(log.getAll().contains("no authorised identity"), is(true));
    }

    @DisplayName("Reports unauthenticated view letter request with selection criteria as unauthorised")
    @Test
    void viewLetterByPscCompanyLetterTypeAndDateWithoutAuthIsUnauthorised(CapturedOutput log) throws Exception {
        mockMvc.perform(
                get(VIEW_LETTER_BY_SELECTION_CRITERIA_URI)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF_VALUE)
                        .header(X_REQUEST_ID, CONTEXT_ID))
                .andExpect(status().isUnauthorized());

        assertThat(log.getAll().contains("no authorised identity"), is(true));
    }

    @DisplayName("Reports unauthenticated get letter details by reference request as unauthorised")
    @Test
    void getLetterDetailsWithoutAuthIsUnauthorised(CapturedOutput log) throws Exception {
        mockMvc.perform(
                        get(GET_LETTER_DETAILS_BY_REFERENCE_PATH + "?reference=reference")
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON)
                                .header(X_REQUEST_ID, CONTEXT_ID))
                .andExpect(status().isUnauthorized());

        assertThat(log.getAll().contains("no authorised identity"), is(true));
    }

    @DisplayName("Reports unauthenticated view letters request with selection criteria as unauthorised")
    @Test
    void viewLettersByPscCompanyLetterTypeAndDateWithoutAuthIsUnauthorised(CapturedOutput log) throws Exception {
        mockMvc.perform(
                        get(VIEW_LETTERS_BY_SELECTION_CRITERIA_URI)
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
                get(VIEW_LETTER_PDF_BY_REFERENCE
                        + "?reference=letter with a calculated sending date")
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
        viewLetterPdfByReference(TOKEN_REFERENCE,
                status().isNotFound())
                .andExpect(content().string(EXPECTED_LETTER_NOT_FOUND_ERROR_MESSAGE));

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(TOKEN_REFERENCE)),
                is(true));
        assertThat(log.getAll().contains(EXPECTED_LETTER_NOT_FOUND_ERROR_MESSAGE), is(true));
    }

    @Test
    @DisplayName("Reports fact that there is more than 1 letter with the same reference")
    void unableToViewLetterAsMultipleLettersWithReferenceFound(CapturedOutput log)
            throws Exception {

        // Given
        createLetter();
        createLetter();

        // When and then
        viewLetterPdfByReference(TOKEN_REFERENCE,
                status().isConflict())
                .andExpect(content().string(EXPECTED_TOO_MANY_LETTERS_FOUND_ERROR_MESSAGE));

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                getExpectedViewLetterInvocationLogMessage(TOKEN_REFERENCE)),
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
        var letterRequest = createLetter();

        // When and then
        var letterPdf = viewLetterPdfByReference(TOKEN_REFERENCE,
                status().isOk()).andReturn().getResponse().getContentAsByteArray();

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                getExpectedViewLetterInvocationLogMessage(
                        TOKEN_REFERENCE)),
                is(true));
        var expectedLogMessage =
                "Responding with regenerated letter PDF to view for letter with reference "
                        + TOKEN_REFERENCE;
        assertThat(log.getAll().contains(expectedLogMessage), is(true));

        var document = Loader.loadPDF(letterPdf);

        // Substitutions all occur on page 1.
        var page1 = getPageText(document, 1);

        // Check letter sending date in letter PDF is the calculated date provided
        Map<String,String> personalisationDetails =
                objectMapper.readValue(letterRequest.getLetterDetails().getPersonalisationDetails(),
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
        var letterRequest = TestUtils.createLetterWithReference(TOKEN_REFERENCE);
        letterRequest.getLetterDetails().setLetterId("IDVPSCDIRTRAN"); // requiring todays date
        saveLetter(letterRequest);

        // When and then
        var letterPdf = viewLetterPdfByReference(TOKEN_REFERENCE,
                status().isOk()).andReturn().getResponse().getContentAsByteArray();

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                TOKEN_REFERENCE)),
                is(true));
        var expectedLogMessage =
                "Responding with regenerated letter PDF to view for letter with reference "
                        + TOKEN_REFERENCE;
        assertThat(log.getAll().contains(expectedLogMessage), is(true));

        var document = Loader.loadPDF(letterPdf);

        // Substitutions all occur on page 1.
        var page1 = getPageText(document, 1);

        // Check letter sending date in letter PDF is the original sending date.
        var originalSendingDate = letterRequest.getCreatedAt().format(DATE_FORMATTER);
        assertThat(page1, containsString("Date:\n" + originalSendingDate));
    }

    @Test
    @DisplayName("View letter reports IOException loading letter PDF with a 500 response")
    void viewLetterReportsPdfIOException(CapturedOutput log) throws Exception {

        // Given
        createLetter();

        doNothing().when(pdfGenerator).generatePdfFromHtml(anyString(), any(OutputStream.class));
        when(pdfGenerator.generatePdfFromHtml(anyString(), anyString()))
                .thenThrow(new IOException("Thrown by test."));

        // When and then
        viewLetterPdfByReference(TOKEN_REFERENCE,
                status().isInternalServerError());

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                TOKEN_REFERENCE)),
                is(true));
        assertThat(log.getAll().contains(
                "Failed to load precompiled letter PDF. Caught IOException: Thrown by test."),
        is(true));
    }

    @Test
    @DisplayName("View letter reports IOException closing letter PDF stream with a 500 response")
    void viewLetterReportsPdfIOExceptionInClosingStream(CapturedOutput log) throws Exception {

        // Given
        createLetter();

        doNothing().when(pdfGenerator).generatePdfFromHtml(anyString(), any(OutputStream.class));
        when(pdfGenerator.generatePdfFromHtml(anyString(), anyString()))
                .thenReturn(precompiledPdfInputStream);
        doThrow(new IOException("Thrown by test.")).when(precompiledPdfInputStream).close();

        // When and then
       viewLetterPdfByReference(TOKEN_REFERENCE,
                status().isInternalServerError());

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                TOKEN_REFERENCE)),
                is(true));
        var expectedLogMessage =
                "Responding with regenerated letter PDF to view for letter with reference "
                        + TOKEN_REFERENCE;
        assertThat(log.getAll().contains(expectedLogMessage), is(true));
        assertThat(log.getAll().contains(
                "Failed to load precompiled letter PDF. Caught IOException: Thrown by test."),
                is(true));
    }

    @Test
    @DisplayName("View letter PDF identified by PSC name, company number, letter type and sending date successfully")
    void viewLetterByPscCompanyLetterTypeAndDateSuccessfully(CapturedOutput log) throws Exception {
        // Given
        var letterRequest = createLetter();

        // When and then
        var letterPdf = viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE,
        status().isOk()).andReturn().getResponse().getContentAsByteArray();

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                PSC_NAME,
                                COMPANY_NUMBER,
                                TEST_LETTER_ID,
                                TEST_TEMPLATE_ID,
                                LETTER_SENDING_DATE)),
                is(true));
        var expectedLogMessage =
                "Responding with regenerated letter PDF to view for letter with psc name "
                        + PSC_NAME + ", companyNumber "
                        + COMPANY_NUMBER + ", letterId "
                        + TEST_LETTER_ID + ", templateId "
                        + TEST_TEMPLATE_ID + ", letter sending date "
                        + LETTER_SENDING_DATE + ".";
        assertThat(log.getAll().contains(expectedLogMessage), is(true));

        var document = Loader.loadPDF(letterPdf);

        // Substitutions all occur on page 1.
        var page1 = getPageText(document, 1);

        // Check letter sending date in letter PDF is the calculated date provided
        Map<String,String> personalisationDetails =
                objectMapper.readValue(letterRequest.getLetterDetails().getPersonalisationDetails(),
                        new TypeReference<>() {});
        var calculatedDate = personalisationDetails.get("idv_start_date");
        assertThat(page1, containsString("Date:\n" + calculatedDate));
    }

    @Test
    @DisplayName("View letter PDF identified by letter ID, company number, letter type and sending date successfully")
    void viewLetterByLetterIdCompanyLetterTypeAndDateSuccessfully(CapturedOutput log) throws Exception {

        // Given
        var letterRequest = createCsidvdefletLetter();

        String letterId = letterRequest.getLetterDetails().getLetterId();
        String templateId = letterRequest.getLetterDetails().getTemplateId();

        // When and then
        var letterPdf = viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                letterId,
                templateId,
                LETTER_SENDING_DATE,
                status().isOk()).andReturn().getResponse().getContentAsByteArray();

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                PSC_NAME,
                                COMPANY_NUMBER,
                                letterId,
                                templateId,
                                LETTER_SENDING_DATE)),
                is(true));
        var expectedLogMessage =
                "Responding with regenerated letter PDF to view for letter with psc name "
                        + PSC_NAME + ", companyNumber "
                        + COMPANY_NUMBER + ", letterId "
                        + letterId + ", templateId "
                        + templateId + ", letter sending date "
                        + LETTER_SENDING_DATE + ".";
        assertThat(log.getAll().contains(expectedLogMessage), is(true));

        var document = Loader.loadPDF(letterPdf);

        // Substitutions all occur on page 1.
        var page1 = getPageText(document, 1);

        // Check company number in letter PDF.
        assertThat(page1, containsString(
                "Company number:\n" + COMPANY_NUMBER));

        // Check letter sending date in letter PDF is the original sending date.
        var originalSendingDate = letterRequest.getCreatedAt()
                .format(DATE_FORMATTER);
        assertThat(page1, containsString("Date:\n" + originalSendingDate));
    }


    @Test
    @DisplayName("View letter identified by PSC name, company number, letter type and sending date reports IOException loading letter PDF with a 500 response")
    void viewLetterByPscCompanyLetterTypeAndDateReportsPdfIOException(CapturedOutput log) throws Exception {

        // Given
        createLetter();

        doNothing().when(pdfGenerator).generatePdfFromHtml(anyString(), any(OutputStream.class));
        when(pdfGenerator.generatePdfFromHtml(anyString(), anyString()))
                .thenThrow(new IOException("Thrown by test."));

        // When and then
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE,
                status().isInternalServerError());

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(PSC_NAME,
                                COMPANY_NUMBER,
                                TEST_LETTER_ID,
                                TEST_TEMPLATE_ID,
                                LETTER_SENDING_DATE)),
                is(true));
        assertThat(log.getAll().contains(
                "Failed to load precompiled letter PDF. Caught IOException: Thrown by test."),
                is(true));
    }

    @Test
    @DisplayName("Get letter details by reference successfully")
    void getLetterDetailsByReferenceSuccessfully(CapturedOutput log) throws Exception {

        // Given
        createLetter();

        // When
        mockMvc.perform(get(GET_LETTER_DETAILS_BY_REFERENCE_PATH
                        + "?reference=" + TOKEN_REFERENCE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_REQUEST_ID, CONTEXT_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE))
                .andExpect(status().isOk());

        // Then
        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                getExpectedGetLetterDetailsByReferenceInvocationLogMessage(
                        TOKEN_REFERENCE)),
                is(true));
    }

    @Test
    @DisplayName("Reports letter cannot be found if PSC name does not match")
    void unableToViewLetterAsPscNameNotMatched(CapturedOutput log) throws Exception {
        implementLetterNotFoundTest(
                log,
                PSC_NAME + " additional text",
                COMPANY_NUMBER,
                null,
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE);
    }

    @Test
    @DisplayName("Rejects view letter request if neither the PSC name nor the letter ID have been provided")
    void unableToViewLetterAsNeitherPscNameNorLetterIdProvided(CapturedOutput log) throws Exception {
        implementBadRequestViewLetterTest(
                log,
                NULL_PSC_NAME,
                COMPANY_NUMBER,
                null,
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE);
    }

    @Test
    @DisplayName("Rejects view letter request if neither the PSC name nor the letter ID have been populated")
    void unableToViewLetterAsNeitherPscNameNorLetterIdPopulated(CapturedOutput log) throws Exception {
        implementBadRequestViewLetterTest(
                log,
                /* PSC name */"    ",
                COMPANY_NUMBER,
                /* letter ID */" ",
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE);
    }

    @Test
    @DisplayName("Rejects view letters request if neither the PSC name nor the letter ID have been provided")
    void unableToViewLettersAsNeitherPscNameNorLetterIdProvided(CapturedOutput log) throws Exception {
        implementBadRequestViewLettersTest(
                log,
                NULL_PSC_NAME,
                COMPANY_NUMBER,
                null,
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE,
                LETTER_1);
    }

    @Test
    @DisplayName("Rejects view letters request if neither the PSC name nor the letter ID have been populated")
    void unableToViewLettersAsNeitherPscNameNorLetterIdPopulated(CapturedOutput log) throws Exception {
        implementBadRequestViewLettersTest(
                log,
                /* PSC name */"    ",
                COMPANY_NUMBER,
                /* letter ID */" ",
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE,
                LETTER_1);
    }

    @Test
    @DisplayName("Reports letter cannot be found if PSC name does not match and letter ID is null")
    void unableToViewLetterAsPscNameNotMatchedWhenLetterIdIsNull(CapturedOutput log)
            throws Exception {
        implementLetterNotFoundTest(
                log,
                PSC_NAME + " additional text",
                COMPANY_NUMBER,
                null,
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE);
    }

    @Test
    @DisplayName("Reports letter cannot be found if company number does not match")
    void unableToViewLetterAsCompanyNumberNotMatched(CapturedOutput log) throws Exception {
        implementLetterNotFoundTest(
                log,
                PSC_NAME,
                COMPANY_NUMBER + " additional text",
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE);
    }

    @Test
    @DisplayName("Reports letter cannot be found if letter type (i.e., template ID) does not match")
    void unableToViewLetterAsTemplateIdNotMatched(CapturedOutput log) throws Exception {
        implementLetterNotFoundTest(
                log,
                PSC_NAME,
                COMPANY_NUMBER,
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID + " additional text",
                LETTER_SENDING_DATE);
    }

    @Test
    @DisplayName("Reports letter cannot be found if letter sending date does not match")
    void unableToViewLetterAsLetterSendingDateNotMatched(CapturedOutput log) throws Exception {
        implementLetterNotFoundTest(
                log,
                PSC_NAME,
                COMPANY_NUMBER,
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID,
                "1999-12-30");;
    }

    @Test
    @DisplayName("Rejects view letter request if letter sending date is not parseable")
    void unableToViewLetterAsLetterSendingDateNotParseable() throws Exception {

        String sendingDate = "8 April 2025";
        
        // When and then
        var errorMessage = viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID,
                sendingDate,
                status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        assertThat(errorMessage.contains(
                "Failed to convert 'letter_sending_date' with value: '"
                        + sendingDate + "'"),
                is(true));
    }

    @Test
    @DisplayName("Reports fact that there is more than 1 letter with the same PSC name, company number, letter type and sending date")
    void unableToViewLetterAsMultipleLettersWithPscCompanyLetterTypeAndDateFound(CapturedOutput log)
            throws Exception {

        // Given
        createLetter();
        createLetter();

        // When and then
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE,
                status().isConflict())
                .andExpect(content().string(EXPECTED_TOO_MANY_LETTERS_FOUND_ERROR_MESSAGE_2));

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                getExpectedViewLetterInvocationLogMessage(PSC_NAME,
                        COMPANY_NUMBER,
                        TEST_LETTER_ID,
                        TEST_TEMPLATE_ID,
                        LETTER_SENDING_DATE)),
                is(true));
        assertThat(log.getAll().contains(EXPECTED_TOO_MANY_LETTERS_FOUND_ERROR_MESSAGE_2), is(true));
    }

    @Test
    @DisplayName("View letter PDFs identified by reference successfully")
    void viewLettersByReferenceSuccessfully(CapturedOutput log) throws Exception {

        // Given
        createLetter();

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
        createLetter();

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
        createLetter();

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
    @DisplayName("Reports fact letters cannot be found by reference")
    void unableToViewLettersAsNoLettersWithReferenceFound(CapturedOutput log) throws Exception {
        viewLetterPdfByReference(TOKEN_REFERENCE, LETTER_1,
                status().isNotFound())
                .andExpect(content().string(EXPECTED_LETTERS_NOT_FOUND_ERROR_MESSAGE));

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(TOKEN_REFERENCE, LETTER_1)),
                is(true));
        assertThat(log.getAll().contains(EXPECTED_LETTERS_NOT_FOUND_ERROR_MESSAGE), is(true));
    }

    @Test
    @DisplayName("View letters reports IOException loading letter PDF with a 500 response")
    void viewLettersReportsPdfIOException(CapturedOutput log) throws Exception {

        // Given
        createLetter();

        doNothing().when(pdfGenerator).generatePdfFromHtml(anyString(), any(OutputStream.class));
        when(pdfGenerator.generatePdfFromHtml(anyString(), anyString()))
                .thenThrow(new IOException("Thrown by test."));

        // When and then
        viewLetterPdfByReference(TOKEN_REFERENCE, LETTER_1, status().isInternalServerError());

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                TOKEN_REFERENCE, LETTER_1)),
                is(true));
        assertThat(log.getAll().contains(
                        "Failed to load precompiled letter PDF. Caught IOException: Thrown by test."),
                is(true));
    }

    @Test
    @DisplayName("View letters reports IOException closing letter PDF stream with a 500 response")
    void viewLettersReportsPdfIOExceptionInClosingStream(CapturedOutput log) throws Exception {

        // Given
        createLetter();

        doNothing().when(pdfGenerator).generatePdfFromHtml(anyString(), any(OutputStream.class));
        when(pdfGenerator.generatePdfFromHtml(anyString(), anyString()))
                .thenReturn(precompiledPdfInputStream);
        doThrow(new IOException("Thrown by test.")).when(precompiledPdfInputStream).close();

        // When and then
        viewLetterPdfByReference(TOKEN_REFERENCE, LETTER_1, status().isInternalServerError());

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                TOKEN_REFERENCE, LETTER_1)),
                is(true));
        assertThat(log.getAll().contains(
                    "Failed to load precompiled letter PDF. Caught IOException: Thrown by test."),
                is(true));
    }

    @Test
    @DisplayName("View letter PDFs identified by reference paginates correctly")
    void viewLettersByReferencePaginatesCorrectly() throws Exception {

        // Given
        for (int i = 1; i <= 4; i++) {
            LetterRequestDao letterRequest = TestUtils.createLetterWithReference("Reference " + i);
            saveLetter(letterRequest);
        }

        // When and then
        checkCorrectLetterIsReturned("Reference", "Reference", LETTER_1);
        checkCorrectLetterIsReturned("Reference", "Reference 1", LETTER_2);
        checkCorrectLetterIsReturned("Reference", "Reference 11", LETTER_3);
        checkCorrectLetterIsReturned("Reference", "Reference 111", LETTER_4);
    }

    @Test
    @DisplayName("View letter PDFs identified by PSC name, company number, letter type and sending date successfully")
    void viewLettersByPscCompanyLetterTypeAndDateSuccessfully(CapturedOutput log) throws Exception {

        // Given
        createLetter();

        // When and then
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE,
                LETTER_1,
                status().isOk());

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                PSC_NAME,
                                COMPANY_NUMBER,
                                TEST_LETTER_ID,
                                TEST_TEMPLATE_ID,
                                LETTER_SENDING_DATE,
                                LETTER_1)),
                is(true));
        var expectedLogMessage =
                "Responding with regenerated letter PDF to view for letter with psc name "
                        + PSC_NAME + ", companyNumber "
                        + COMPANY_NUMBER + ", letterId "
                        + TEST_LETTER_ID + ", templateId "
                        + TEST_TEMPLATE_ID + ", letter sending date "
                        + LETTER_SENDING_DATE + ", letter number "
                        + LETTER_1 + ".";
        assertThat(log.getAll().contains(expectedLogMessage), is(true));
    }

    @Test
    @DisplayName("View letter PDFs identified by letter ID, company number, letter type and sending date successfully")
    void viewLettersByLetterIdCompanyLetterTypeAndDateSuccessfully(CapturedOutput log) throws Exception {

        // Given
        var letterRequest = createCsidvdefletLetter();

        String letterId = letterRequest.getLetterDetails().getLetterId();
        String templateId = letterRequest.getLetterDetails().getTemplateId();

        // When and then
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                letterId,
                templateId,
                LETTER_SENDING_DATE,
                LETTER_1,
                status().isOk());

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                PSC_NAME,
                                COMPANY_NUMBER,
                                letterId,
                                templateId,
                                LETTER_SENDING_DATE,
                                LETTER_1)),
                is(true));
        var expectedLogMessage =
                "Responding with regenerated letter PDF to view for letter with psc name "
                        + PSC_NAME + ", companyNumber "
                        + COMPANY_NUMBER + ", letterId "
                        + letterId + ", templateId "
                        + templateId + ", letter sending date "
                        + LETTER_SENDING_DATE + ", letter number "
                        + LETTER_1 + ".";
        assertThat(log.getAll().contains(expectedLogMessage), is(true));
    }

    @Test
    @DisplayName("Unable to view letter PDF 0 identified by PSC name, company number, letter type and sending date")
    void unableToViewLetter0ByPscCompanyLetterTypeAndDate() throws Exception {

        // Given
        createLetter();

        // When and then
        var errorMessage = viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID,
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
        createLetter();

        // When and then
        var errorMessage = viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE,
                LETTER_2,
                status().isNotFound())
                    .andReturn().getResponse().getContentAsString();

        assertThat(errorMessage.contains(
                "Error in chs-gov-uk-notify-integration-api: Letter number " + LETTER_2
                        + " not found. Total number of matching letters was 1."),
                is(true));
    }

    @Test
    @DisplayName("Reports fact letters cannot be found by PSC name, company number, letter type and sending date")
    void unableToViewLettersAsNoLettersWithPscCompanyLetterTypeAndDateFound(CapturedOutput log)
            throws Exception {

        // Given, when and then
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE,
                LETTER_1,
                status().isNotFound())
                .andExpect(content().string(EXPECTED_LETTERS_NOT_FOUND_ERROR_MESSAGE));

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                getExpectedViewLetterInvocationLogMessage(
                        PSC_NAME,
                        COMPANY_NUMBER,
                        TEST_LETTER_ID,
                        TEST_TEMPLATE_ID,
                        LETTER_SENDING_DATE,
                        LETTER_1)),
                is(true));
        assertThat(log.getAll().contains(EXPECTED_LETTERS_NOT_FOUND_ERROR_MESSAGE), is(true));
    }

    @Test
    @DisplayName("View letters identified by PSC name, company number, letter type and sending date reports IOException loading letter PDF with a 500 response")
    void viewLettersByPscCompanyLetterTypeAndDateReportsPdfIOException(CapturedOutput log) throws Exception {

        // Given
        createLetter();

        doNothing().when(pdfGenerator).generatePdfFromHtml(anyString(), any(OutputStream.class));
        when(pdfGenerator.generatePdfFromHtml(anyString(), anyString()))
                .thenThrow(new IOException("Thrown by test."));

        // When and then
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE,
                LETTER_1,
                status().isInternalServerError());

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(PSC_NAME,
                                COMPANY_NUMBER,
                                TEST_LETTER_ID,
                                TEST_TEMPLATE_ID,
                                LETTER_SENDING_DATE,
                                LETTER_1)),
                is(true));
        assertThat(log.getAll().contains(
                        "Failed to load precompiled letter PDF. Caught IOException: Thrown by test."),
                is(true));
    }

    @Test
    @DisplayName("View letters identified by PSC name, company number, letter type and sending date reports IOException closing letter PDF stream with a 500 response")
    void viewLettersByPscCompanyLetterTypeAndDateReportsPdfIOExceptionInClosingStream(CapturedOutput log) throws Exception {

        // Given
        createLetter();

        doNothing().when(pdfGenerator).generatePdfFromHtml(anyString(), any(OutputStream.class));
        when(pdfGenerator.generatePdfFromHtml(anyString(), anyString()))
                .thenReturn(precompiledPdfInputStream);
        doThrow(new IOException("Thrown by test.")).when(precompiledPdfInputStream).close();

        // When and then
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE,
                LETTER_1,
                status().isInternalServerError());

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(PSC_NAME,
                                COMPANY_NUMBER,
                                TEST_LETTER_ID,
                                TEST_TEMPLATE_ID,
                                LETTER_SENDING_DATE,
                                LETTER_1)),
                is(true));
        assertThat(log.getAll().contains(
                        "Failed to load precompiled letter PDF. Caught IOException: Thrown by test."),
                is(true));
    }

    @Test
    @DisplayName("View letter PDFs identified by PSC name, company number, letter type and sending date paginates correctly")
    void viewLettersByPscCompanyLetterTypeAndDatePaginatesCorrectly() throws Exception {

        // Given
        for (int i = 1; i <= 4; i++) {
            LetterRequestDao letterRequest = TestUtils.createLetterWithReference("Reference " + i);
            letterRequest.setCreatedAt(OffsetDateTime.now());
            saveLetter(letterRequest);
        }

        // When and then
        checkCorrectLetterIsReturned("Reference 1", LETTER_1);
        checkCorrectLetterIsReturned("Reference 2", LETTER_2);
        checkCorrectLetterIsReturned("Reference 3", LETTER_3);
        checkCorrectLetterIsReturned("Reference 4", LETTER_4);
    }

    @ParameterizedTest
    @DisplayName("View letter PDF identified by PSC name, company number, etc, where personalisation details spacing varies")
    @ValueSource(strings={
            "\"psc_name\": \"ANDREWPHILLIPLONGNAME BARROW\", \"company_number\": \"00006400\"",
            "\"psc_name\":\"ANDREWPHILLIPLONGNAME BARROW\", \"company_number\":\"00006400\"",
            "\"psc_name\":   \"ANDREWPHILLIPLONGNAME BARROW\", \"company_number\": \"00006400\"",
            "\"psc_name\": \"ANDREWPHILLIPLONGNAME BARROW\", \"company_number\":    \"00006400\""
    })
    void viewLetterByLetterIdCompanyLetterTypeAndDateWithVariousPersonalisationsSpacings(
            String personalisationDetails)
            throws Exception {

        // Given
        LetterRequestDao letterRequest = TestUtils.createLetterWithReference("reference");
        letterRequest.getLetterDetails().setPersonalisationDetails(OTHER_PERSONALISATIONS + personalisationDetails + "}");
        saveLetter(letterRequest);

        // When and then
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE,
                status().isOk());
    }

    @ParameterizedTest
    @DisplayName("View letters PDF identified by PSC name, company number, etc, where personalisation details spacing varies")
    @ValueSource(strings={
            "\"psc_name\": \"ANDREWPHILLIPLONGNAME BARROW\", \"company_number\": \"00006400\"",
            "\"psc_name\":\"ANDREWPHILLIPLONGNAME BARROW\", \"company_number\":\"00006400\"",
            "\"psc_name\":   \"ANDREWPHILLIPLONGNAME BARROW\", \"company_number\": \"00006400\"",
            "\"psc_name\": \"ANDREWPHILLIPLONGNAME BARROW\", \"company_number\":    \"00006400\""
    })
    void viewLettersByLetterIdCompanyLetterTypeAndDateWithVariousPersonalisationsSpacings(
            String personalisationDetails)
            throws Exception {

        // Given
        LetterRequestDao letterRequest = TestUtils.createLetterWithReference("reference");
        letterRequest.getLetterDetails().setPersonalisationDetails(OTHER_PERSONALISATIONS + personalisationDetails + "}");
        saveLetter(letterRequest);

        // When and then
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID,
                LETTER_SENDING_DATE,
                LETTER_1,
                status().isOk());
    }

    private void checkCorrectLetterIsReturned(String referenceExpected,
                                              int letterNumber) throws Exception {
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                PSC_NAME,
                COMPANY_NUMBER,
                TEST_LETTER_ID,
                TEST_TEMPLATE_ID,
                LocalDate.now().toString(),
                letterNumber,
                status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private void checkCorrectLetterIsReturned(String referenceSought,
                                              String referenceExpected,
                                              int letterNumber) throws Exception {
        viewLetterPdfByReference(referenceSought, letterNumber,
                status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private NotificationLetterRequest saveLetter(LetterRequestDao request) throws Exception {
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class), anyString())).thenReturn(responseReceived);

        return notificationLetterRequestRepository.save(new NotificationLetterRequest(null, null, request, null));
    }

    private LetterRequestDao createLetter() throws Exception {
        LetterRequestDao letterRequest = TestUtils.createLetterWithReference(TOKEN_REFERENCE);
        return saveLetter(letterRequest).getRequest();
    }

    private LetterRequestDao createCsidvdefletLetter() throws Exception {
        LetterRequestDao letterRequest = TestUtils.createLetterWithReference(TOKEN_REFERENCE);
        letterRequest.getLetterDetails().setLetterId("CSIDVDEFLET");
        letterRequest.getLetterDetails().setTemplateId(TEST_TEMPLATE_ID);
        letterRequest.getLetterDetails().setPersonalisationDetails("{ \"verification_due_date\": \"17 September 2025\", \"company_name\": \"TEST COMPANY LTD\", \"company_number\": \"00006400\", \"is_llp\": \"no\"}");
        return saveLetter(letterRequest).getRequest();
    }
    
    private NotificationEmailRequest saveEmail(EmailRequestDao request) throws Exception {
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class), anyString())).thenReturn(responseReceived);

        return notificationEmailRequestRepository.save(new NotificationEmailRequest(null, null, request, null));
    }

    private void implementLetterNotFoundTest(CapturedOutput log,
                                             String pscName,
                                             String companyNumber,
                                             String letterId,
                                             String templateId,
                                             String letterSendingDate) throws Exception {

        // Given
        createLetter();

        // When and then
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                pscName,
                companyNumber,
                letterId,
                templateId,
                letterSendingDate,
                status().isNotFound())
                .andExpect(content().string(getExpectedLetterNotFoundErrorMessage(
                        pscName,
                        companyNumber,
                        letterId,
                        templateId,
                        letterSendingDate)));

        assertThat(log.getAll().contains(EXPECTED_SECURITY_OK_LOG_MESSAGE), is(true));
        assertThat(log.getAll().contains(
                        getExpectedViewLetterInvocationLogMessage(
                                pscName,
                                companyNumber,
                                letterId,
                                templateId,
                                letterSendingDate)),
                is(true));
        assertThat(log.getAll().contains(getExpectedLetterNotFoundErrorMessage(
                        pscName,
                        companyNumber,
                        letterId,
                        templateId,
                        letterSendingDate)),
                is(true));

    }

    private void implementBadRequestViewLetterTest(CapturedOutput log,
                                                   String pscName,
                                                   String companyNumber,
                                                   String letterId,
                                                   String templateId,
                                                   String letterSendingDate) throws Exception {

        // Given
        createLetter();

        // When and then
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                pscName,
                companyNumber,
                letterId,
                templateId,
                letterSendingDate,
                status().isBadRequest())
                .andExpect(content().string(getExpectedBadRequestErrorMessage(pscName, letterId)));
        assertThat(log.getAll().contains(getExpectedBadRequestErrorMessage(pscName, letterId)),
                is(true));

    }

    private void implementBadRequestViewLettersTest(CapturedOutput log,
                                                    String pscName,
                                                    String companyNumber,
                                                    String letterId,
                                                    String templateId,
                                                    String letterSendingDate,
                                                    int letterNumber) throws Exception {

        // Given
        createLetter();

        // When and then
        viewLetterPdfByPscCompanyLetterTypeAndDate(
                pscName,
                companyNumber,
                letterId,
                templateId,
                letterSendingDate,
                letterNumber,
                status().isBadRequest())
                .andExpect(content().string(getExpectedBadRequestErrorMessage(pscName, letterId)));
        assertThat(log.getAll().contains(getExpectedBadRequestErrorMessage(pscName, letterId)),
                is(true));

    }


    private ResultActions viewLetterPdfByPscCompanyLetterTypeAndDate(String pscName,
                                                   String companyNumber,
                                                   String letterId,
                                                   String templateId,
                                                   String letterSendingDate,
                                                   ResultMatcher expectedResponseStatus)
            throws Exception {
        return mockMvc.perform(get(VIEW_LETTER_PDF + "?"
                        + (pscName != null ? "&psc_name=" + pscName : "")
                        + "&company_number=" + companyNumber
                        + (letterId!=null ? "&letter_id=" + letterId : "")
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
                                                                     String letterId,
                                                                     String templateId,
                                                                     String letterSendingDate,
                                                                     int letterNumber,
                                                                     ResultMatcher expectedResponseStatus)
            throws Exception {
        return mockMvc.perform(get(VIEW_LETTER_PDFS
                        + letterNumber + "?"
                        + (pscName != null ? "psc_name=" + pscName + "&" : "")
                        + "company_number=" + companyNumber
                        + (letterId!=null ? "&letter_id=" + letterId : "")
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
        return mockMvc.perform(get(VIEW_LETTER_PDF_BY_REFERENCE
                        + "?reference=" + reference)
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
        return mockMvc.perform(
                get(VIEW_LETTER_PDFS_BY_REFERENCE + letterNumber + "?reference=" + reference)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_PDF_VALUE)
                        .header(X_REQUEST_ID, CONTEXT_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE))
                .andExpect(expectedResponseStatus);
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

    private static String getExpectedGetLetterDetailsByReferenceInvocationLogMessage(
            String reference) {
        return   "{\"reference\":\"" + reference + "\","
                + "\"action\":\"get_letter_by_reference\","
                + "\"message\":\"Retrieving letter notifications by reference: " + reference + "\","
                + "\"request_id\":\"X9uND6rXQxfbZNcMVFA7JI4h2KOh\"}";
    }

    private static String getExpectedViewLetterInvocationLogMessage(String pscName,
                                                                    String companyNumber,
                                                                    String letterId,
                                                                    String templateId,
                                                                    String letterSendingDate) {
        return   "{\"letter_sending_date\":\"" + letterSendingDate + "\","
                + "\"company_number\":\"" + companyNumber + "\","
                + "\"psc_name\":\"" + pscName + "\","
                + "\"action\":\"view_letter_pdf\","
                + "\"template_id\":\"" + templateId + "\","
                + (letterId != null ? "\"letter_id\":\"" + letterId + "\"," : "")
                + "\"message\":\"Starting viewLetterPdf process\","
                + "\"request_id\":\"X9uND6rXQxfbZNcMVFA7JI4h2KOh\"}";
    }

    private static String getExpectedViewLetterInvocationLogMessage(String pscName,
                                                                    String companyNumber,
                                                                    String letterId,
                                                                    String templateId,
                                                                    String letterSendingDate,
                                                                    int letterNumber) {
        return   "{\"letter_sending_date\":\"" + letterSendingDate + "\","
                + "\"company_number\":\"" + companyNumber + "\","
                + "\"psc_name\":\"" + pscName + "\","
                + "\"letter\":" + letterNumber + ","
                + "\"action\":\"view_letter_pdfs\","
                + "\"template_id\":\"" + templateId + "\","
                + (letterId != null ? "\"letter_id\":\"" + letterId + "\"," : "")
                + "\"message\":\"Starting viewLetterPdfs process\","
                + "\"request_id\":\"X9uND6rXQxfbZNcMVFA7JI4h2KOh\"}";
    }

    private static String getExpectedLetterNotFoundErrorMessage(String pscName,
                                                                String companyNumber,
                                                                String letterId,
                                                                String templateId,
                                                                String letterSendingDate) {
       return "Error in chs-gov-uk-notify-integration-api: Letter not found for psc name " + pscName
               + ", companyNumber " + companyNumber
               + ", letterId " + letterId
               + ", templateId " + templateId
               + ", letter sending date " + letterSendingDate + ".";
    }

    private static String getExpectedBadRequestErrorMessage(String pscName, String letterId) {
        return "Error in chs-gov-uk-notify-integration-api: PSC name [" + pscName
                + "] and/or letter ID [" + letterId + "] cannot be null or blank.";
    }

}
