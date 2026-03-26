package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
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
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.mapper.LetterRequestMapper;
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
    private static final String EXPECTED_TOO_MANY_LETTERS_FOUND_ERROR_MESSAGE =
        "Error in chs-gov-uk-notify-integration-api: Multiple letters found for reference: "
            + TOKEN_REFERENCE;
    private static final String EXPECTED_SECURITY_OK_LOG_MESSAGE =
            "authorised as api key (internal user)";

    private static final String GET_LETTER_DETAILS_BY_REFERENCE_PATH =
            "/gov-uk-notify-integration/letters/reference";
    private static final String VIEW_LETTER_PDF_BY_REFERENCE =
            "/gov-uk-notify-integration/letters/view_by_reference";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationClient notificationClient;

    @MockitoSpyBean
    private HtmlPdfGenerator pdfGenerator;

    @Mock
    private InputStream precompiledPdfInputStream;

    @Test
    void When_RequestingAllEmails_Expect_SuccessfulResponseWithEmailList() throws Exception {
        notificationEmailRequestRepository.deleteAll();

        EmailRequestDao emailRequest = TestUtils.createEmailRequest(TEST_EMAIL);
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
        EmailRequestDao emailRequest = TestUtils.createEmailRequest(TEST_EMAIL);
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
        var letterRequest = TestUtils.createLetterRequestWithReference(TOKEN_REFERENCE);
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

    private NotificationLetterRequest saveLetter(LetterRequestDao request) throws Exception {
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class), anyString())).thenReturn(responseReceived);

        return notificationLetterRequestRepository.save(new NotificationLetterRequest(request));
    }

    private LetterRequestDao createLetter() throws Exception {
        LetterRequestDao letterRequest = TestUtils.createLetterRequestWithReference(TOKEN_REFERENCE);
        return saveLetter(letterRequest).getRequest();
    }

    private NotificationEmailRequest saveEmail(EmailRequestDao request) throws Exception {
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class), anyString())).thenReturn(responseReceived);

        return notificationEmailRequestRepository.save(new NotificationEmailRequest(request));
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

    private static String getExpectedViewLetterInvocationLogMessage(String reference) {
        return   "{\"reference\":\"" + reference + "\","
                + "\"action\":\"view_letter_pdf\","
                + "\"message\":\"Starting viewLetterPdfByReference process\","
                + "\"request_id\":\"X9uND6rXQxfbZNcMVFA7JI4h2KOh\"}";
    }

    private static String getExpectedGetLetterDetailsByReferenceInvocationLogMessage(
            String reference) {
        return   "{\"reference\":\"" + reference + "\","
                + "\"action\":\"get_letter_by_reference\","
                + "\"message\":\"Retrieving letter notifications by reference: " + reference + "\","
                + "\"request_id\":\"X9uND6rXQxfbZNcMVFA7JI4h2KOh\"}";
    }

}
