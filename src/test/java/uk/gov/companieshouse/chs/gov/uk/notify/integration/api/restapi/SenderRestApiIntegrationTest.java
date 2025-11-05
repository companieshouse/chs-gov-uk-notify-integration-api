package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.API_KEY_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.INTERNAL_USER_ROLE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.getValidSendLetterRequestBody;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.postSendLetterRequest;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.COMPANY_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.IDV_START_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.PSC_APPOINTMENT_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.PSC_FULL_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.REFERENCE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService.ERROR_MESSAGE_KEY;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService.NIL_UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.PdfXConformanceException;
import com.lowagie.text.pdf.internal.PdfXConformanceImp;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.letterdispatcher.LetterDispatcher;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterResponseRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator.HtmlPdfGenerator;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator.SvgReplacedElementFactory;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.TemplateLookup;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@Tag("integration-test")
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
                    + "missing for LetterTemplateKey[appId=chips, id=direction_letter_v1].";

    private static final String UNPARSABLE_PERSONALISATION_DETAILS_ERROR_MESSAGE_LINE_1 =
            "Error in chs-gov-uk-notify-integration-api: Failed to parse personalisation details:"
                    + " Unexpected character ('}' (code 125)): was expecting double-quote to "
                    + "start field name";
    private static final String UNPARSABLE_PERSONALISATION_DETAILS_ERROR_MESSAGE_LINE_2 =
            " at [Source: REDACTED (`StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION` disabled); "
                    + "line: 1, column: 137]";
    private static final String UNPARSABLE_PERSONALISATION_DETAILS_ERROR_MESSAGE =
            UNPARSABLE_PERSONALISATION_DETAILS_ERROR_MESSAGE_LINE_1 + "\n"
            + UNPARSABLE_PERSONALISATION_DETAILS_ERROR_MESSAGE_LINE_2;
    private static final String UNKNOWN_APPLICATION_ERROR_MESSAGE =
            "Error in chs-gov-uk-notify-integration-api: Unable to find a valid context for "
                    + "LetterTemplateKey[appId=unknown_application, id=direction_letter_v1]";
    private static final String UNKNOWN_TEMPLATE_ID_ERROR_MESSAGE =
            "Error in chs-gov-uk-notify-integration-api: Unable to find a valid context for "
                    + "LetterTemplateKey[appId=chips, id=new_letter]";
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
                    + "LetterTemplateKey[appId=chips, id=direction_letter_v1].";
    private static final String CREATE_SVG_IMAGE_ERROR_MESSAGE =
            "Error in chs-gov-uk-notify-integration-api: Caught IOException while "
                    + "creating SVG image assets/templates/letters/common/warning.svg: "
                    + "Thrown by test. [cause: null]";
    private static final String SVG_IMAGE_NOT_FOUND_ERROR_MESSAGE =
            "Error in chs-gov-uk-notify-integration-api: SVG image not found: "
                    + "assets/templates/letters/common/warning.svg [cause: null]";
    private static final String PDFX_CONFORMANCE_ERROR_MESSAGE =
            "Error in chs-gov-uk-notify-integration-api: Thrown by test [cause: null]. "
                    + "This PdfXConformanceException could indicate that a font, style or "
                    + "stylesheet cannot be found.";
    private static final String INCORRECTLY_FORMATTED_IDV_START_DATE_ERROR_MESSAGE =
            "Error in chs-gov-uk-notify-integration-api: Format of date 'idv_start_date' "
                    + "'Monday, 30 June 2025' is incorrect.";
    private static final String INCORRECTLY_NAMED_MONTH_IN_PSC_APPOINTMENT_DATE_ERROR_MESSAGE =
            "Error in chs-gov-uk-notify-integration-api: Unknown month 'Jun' found in "
                    + "'psc_appointment_date' date '24 Jun 2025'.";

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
    private HtmlPdfGenerator pdfGenerator;

    @MockitoSpyBean
    private TemplateLookup templateLookup;

    @Mock
    private InputStream precompiledPdfInputStream;

    @Mock
    private SAXSVGDocumentFactory svgDocumentFactory;

    @MockitoSpyBean
    private LetterDispatcher letterDispatcher;

    @MockitoSpyBean
    private GovUkNotifyService govUkNotifyService;

    @MockitoSpyBean
    private SvgReplacedElementFactory svgReplacedElementFactory;

    @Test
    @DisplayName("Send letter successfully")
    void sendLetterSuccessfully(CapturedOutput log) throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        var capturedFileSignature = new StringBuilder();
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class), anyString()))
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
        postSendLetterRequest(mockMvc,
                getValidSendLetterRequestBody(),
                status().isCreated());

        assertThat(log.getAll().contains("\"context\":\"" + REQUEST_ID + "\""), is(true));
        assertThat(log.getAll().contains("authorised as api key (internal user)"), is(true));
        assertThat(log.getAll().contains("\"request_id\":\"" + REQUEST_ID + "\""), is(true));
        assertThat(log.getAll().contains("emailAddress: jbloggs@jbloggs.com"), is(true));

        verifyLetterDetailsRequestStoredCorrectly();
        verifyLetterResponseStoredCorrectly(responseReceived);
        verifyLetterPdfSent(capturedFileSignature);
    }

    @Test
    @DisplayName("Send letter without providing the company name in the personalisation details")
    void sendLetterWithoutCompanyName(CapturedOutput log) throws Exception {

        // Given, when and then
        postSendLetterRequest(mockMvc,
                getRequestWithoutCompanyName(),
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
        postSendLetterRequest(mockMvc,
                getRequestWithoutPscFullName(),
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
        postSendLetterRequest(mockMvc,
                getRequestWithUnparsablePersonalisationDetails(),
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
                anyString(), any(InputStream.class), anyString()))
                .thenThrow(
                        new NotificationClientException(INVALID_GOV_NOTIFY_API_KEY_ERROR_MESSAGE));

        // When and then
        postSendLetterRequest(mockMvc,
                getValidSendLetterRequestBody(),
                status().isInternalServerError());

        assertThat(log.getAll().contains("\"context\":\"" + REQUEST_ID + "\""), is(true));
        assertThat(log.getAll().contains("authorised as api key (internal user)"), is(true));
        assertThat(log.getAll().contains("\"request_id\":\"" + REQUEST_ID + "\""), is(true));
        assertThat(log.getAll().contains("emailAddress: jbloggs@jbloggs.com"), is(true));

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
        postSendLetterRequest(mockMvc,
                resourceToString("/fixtures/send-letter-request-missing-sender-reference-and-app-id.json",
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
    @DisplayName("Send letter reports IOException loading letter PDF with a 500 response")
    void sendLetterReportsPdfIOException(CapturedOutput log) throws Exception {

        // Given
        doNothing().when(pdfGenerator).generatePdfFromHtml(anyString(), any(OutputStream.class));
        when(pdfGenerator.generatePdfFromHtml(anyString(), anyString()))
                .thenThrow(new IOException("Thrown by test."));

        // When and then
        postSendLetterRequest(mockMvc,
                getValidSendLetterRequestBody(),
                status().isInternalServerError());

        assertThat(log.getAll().contains(
                "Failed to load precompiled letter PDF. Caught IOException: Thrown by test."),
                is(true));

        verifyLetterDetailsRequestStoredCorrectly();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter reports IOException closing letter PDF stream with a 500 response")
    void sendLetterReportsPdfIOExceptionInClosingStream(CapturedOutput log) throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class), anyString())).thenReturn(responseReceived);

        doNothing().when(pdfGenerator).generatePdfFromHtml(anyString(), any(OutputStream.class));
        when(pdfGenerator.generatePdfFromHtml(anyString(), anyString()))
                .thenReturn(precompiledPdfInputStream);
        doThrow(new IOException("Thrown by test.")).when(precompiledPdfInputStream).close();

        // When and then
        postSendLetterRequest(mockMvc,
                getValidSendLetterRequestBody(),
                status().isInternalServerError());

        assertThat(log.getAll().contains(
                        "Failed to load precompiled letter PDF. Caught IOException: Thrown by test."),
                is(true));

        assertThat(log.getAll().contains("\"context\":\"" + REQUEST_ID + "\""), is(true));
        assertThat(log.getAll().contains("authorised as api key (internal user)"), is(true));
        assertThat(log.getAll().contains("\"request_id\":\"" + REQUEST_ID + "\""), is(true));
        assertThat(log.getAll().contains("emailAddress: jbloggs@jbloggs.com"), is(true));

        verifyLetterDetailsRequestStoredCorrectly();
        verifyLetterResponseStoredCorrectly(responseReceived);
        verify(precompiledPdfInputStream).close();
    }

    @Test
    @DisplayName("Send letter reports SvgImageException creating an SVG image with a 500 response")
    void sendLetterReportsCreationSvgImageException(CapturedOutput log) throws Exception {

        // Given
        when(svgReplacedElementFactory.getDocumentFactory()).thenReturn(svgDocumentFactory);
        when(svgDocumentFactory.createSVGDocument(anyString())).
                thenThrow(new IOException("Thrown by test."));

        // When and then
        postSendLetterRequest(mockMvc,
                getValidSendLetterRequestBody(),
                status().isInternalServerError())
                .andExpect(content().string(CREATE_SVG_IMAGE_ERROR_MESSAGE));

        assertThat(log.getAll().contains(CREATE_SVG_IMAGE_ERROR_MESSAGE), is(true));

        verifyLetterDetailsRequestStoredCorrectly();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter reports PdfXConformanceException rendering PDF with a 500 response")
    void sendLetterReportsPdfXConformanceException(CapturedOutput log) throws Exception {

        // Given
        try (final var pdfxConformanceChecker = mockStatic(PdfXConformanceImp.class)) {

            pdfxConformanceChecker.when(
                    () -> PdfXConformanceImp.checkPDFXConformance(
                            any(PdfWriter.class), anyInt(), any()))
                    .thenThrow(new PdfXConformanceException("Thrown by test"));

            // When and then
            postSendLetterRequest(mockMvc,
                    getValidSendLetterRequestBody(),
                    status().isInternalServerError())
                    .andExpect(content().string(PDFX_CONFORMANCE_ERROR_MESSAGE));

            assertThat(log.getAll().contains(PDFX_CONFORMANCE_ERROR_MESSAGE), is(true));

            verifyLetterDetailsRequestStoredCorrectly();
            verifyNoLetterResponsesAreStored();
        }
    }

    @Test
    @DisplayName("Send letter reports SvgImageException not finding an SVG image with a 500 response")
    void sendLetterReportsMissingSvgImageException(CapturedOutput log) throws Exception {

        // Given
        when(svgReplacedElementFactory.getResourceUrl(anyString())).thenReturn(null);

        // When and then
        postSendLetterRequest(mockMvc,
                getValidSendLetterRequestBody(),
                status().isInternalServerError())
                .andExpect(content().string(SVG_IMAGE_NOT_FOUND_ERROR_MESSAGE));

        assertThat(log.getAll().contains(SVG_IMAGE_NOT_FOUND_ERROR_MESSAGE), is(true));

        verifyLetterDetailsRequestStoredCorrectly();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter with unknown application ID")
    void sendLetterWithUnknownApplicationId(CapturedOutput log) throws Exception {

        // Given, when and then
        postSendLetterRequest(mockMvc,
                getRequestWithUnknownApplicationId(),
                status().isBadRequest())
                .andExpect(content().string(UNKNOWN_APPLICATION_ERROR_MESSAGE));

        assertThat(log.getAll().contains(UNKNOWN_APPLICATION_ERROR_MESSAGE), is(true));

        verifyLetterDetailsRequestStored();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter with unknown template ID (aka letter)")
    void sendLetterWithUnknownTemplateId(CapturedOutput log) throws Exception {

        // Given, when and then
        postSendLetterRequest(mockMvc,
                getRequestWithUnknownTemplateId(),
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
        postSendLetterRequest(mockMvc,
                getValidSendLetterRequestBody(),
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
        postSendLetterRequest(mockMvc,
                getRequestWithReferenceInPersonalisationDetails(),
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
        postSendLetterRequest(mockMvc,
                getRequestWithTooShortAnAddress(),
                status().isBadRequest())
                .andExpect(content().string(MISSING_ADDRESS_LINES_ERROR_MESSAGE));

        assertThat(log.getAll().contains(MISSING_ADDRESS_LINES_ERROR_MESSAGE), is(true));

        verifyLetterDetailsRequestStored();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter with incorrectly formatted date")
    void sendLetterWithIncorrectlyFormattedDate(CapturedOutput log) throws Exception {

        // Given, when and then
        postSendLetterRequest(mockMvc,
                getRequestWithIncorrectlyFormattedIdvStartDate(),
                status().isBadRequest())
                .andExpect(content().string(INCORRECTLY_FORMATTED_IDV_START_DATE_ERROR_MESSAGE));

        assertThat(log.getAll().contains(INCORRECTLY_FORMATTED_IDV_START_DATE_ERROR_MESSAGE),
                is(true));

        verifyLetterDetailsRequestStored();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter with incorrectly named month in a date")
    void sendLetterWithIncorrectlyNamedMonth(CapturedOutput log) throws Exception {

        // Given, when and then
        postSendLetterRequest(mockMvc,
                getRequestWithIncorrectlyNamedMonthInPscAppointmentDate(),
                status().isBadRequest())
                .andExpect(content()
                        .string(INCORRECTLY_NAMED_MONTH_IN_PSC_APPOINTMENT_DATE_ERROR_MESSAGE));

        assertThat(log.getAll()
                        .contains(INCORRECTLY_NAMED_MONTH_IN_PSC_APPOINTMENT_DATE_ERROR_MESSAGE),
                is(true));

        verifyLetterDetailsRequestStored();
        verifyNoLetterResponsesAreStored();
    }

    @Test
    @DisplayName("Send letter with postage economy for CSIDVDEFLET_v1 or IDVPSCDEFAULT_v1")
    void sendLetterWithEconomyPostage(CapturedOutput log) throws Exception {
        // Arrange
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class), anyString()))
                .thenReturn(responseReceived);

        String csidvdefletRequest = resourceToString("/fixtures/send-csidvdeflet-request.json", UTF_8);
        postSendLetterRequest(mockMvc, csidvdefletRequest, status().isCreated());
        verify(letterDispatcher).sendLetter(
                eq(GovUkNotifyService.ECONOMY_POSTAGE), any(), any(), eq("CSIDVDEFLET_v1"), any(), any(), any());
        verify(govUkNotifyService).sendLetter(
                eq(GovUkNotifyService.ECONOMY_POSTAGE), any(), any()
        );

        // Reset mocks to verify second call independently
        reset(letterDispatcher, govUkNotifyService);

        String idvpscdefaultRequest = resourceToString("/fixtures/send-idvpscdefault-request.json", UTF_8);
        postSendLetterRequest(mockMvc, idvpscdefaultRequest, status().isCreated());
        verify(letterDispatcher).sendLetter(
                eq(GovUkNotifyService.ECONOMY_POSTAGE), any(), any(), eq("IDVPSCDEFAULT_v1"), any(), any(), any());
        verify(govUkNotifyService).sendLetter(
                eq(GovUkNotifyService.ECONOMY_POSTAGE), any(), any()
        );


    }

    @Test
    @DisplayName("Send letter postage second class for all other templates")
    void sendLetterWithSecondClassPostage(CapturedOutput log) throws Exception {
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class), anyString()))
                .thenReturn(responseReceived);
        // Test for other template (should use SECOND_CLASS_POSTAGE)
        String otherRequest = resourceToString("/fixtures/send-letter-request.json", UTF_8);
        postSendLetterRequest(mockMvc, otherRequest, status().isCreated());
        verify(letterDispatcher).sendLetter(
                eq(GovUkNotifyService.SECOND_CLASS_POSTAGE), any(), any(), any(), any(), any(), any());
        verify(govUkNotifyService).sendLetter(
                eq(GovUkNotifyService.SECOND_CLASS_POSTAGE), any(), any()
        );
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

    private String getRequestWithIncorrectlyFormattedIdvStartDate()
            throws IOException {
        var request = objectMapper.readValue(
                getValidSendLetterRequestBody(),
                GovUkLetterDetailsRequest.class);
        var letterDetails = request.getLetterDetails();
        var personalisationDetailsString = letterDetails.getPersonalisationDetails();

        var personalisationDetails = JsonParser
                .parseString(personalisationDetailsString)
                .getAsJsonObject();
        personalisationDetails.addProperty(IDV_START_DATE, "Monday, 30 June 2025");

        letterDetails.setPersonalisationDetails(personalisationDetails.toString());
        return objectMapper.writeValueAsString(request);
    }

    private String getRequestWithIncorrectlyNamedMonthInPscAppointmentDate()
            throws IOException {
        var request = objectMapper.readValue(
                getValidSendLetterRequestBody(),
                GovUkLetterDetailsRequest.class);
        var letterDetails = request.getLetterDetails();
        var personalisationDetailsString = letterDetails.getPersonalisationDetails();

        var personalisationDetails = JsonParser
                .parseString(personalisationDetailsString)
                .getAsJsonObject();
        personalisationDetails.addProperty(PSC_APPOINTMENT_DATE, "24 Jun 2025");

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
