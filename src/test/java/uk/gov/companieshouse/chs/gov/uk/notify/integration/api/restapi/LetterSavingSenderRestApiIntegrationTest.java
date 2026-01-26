package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils.postSendLetterRequest;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.IS_WELSH;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator.HtmlPdfGenerator;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@SpringBootTest(properties = {
        "save.letter=true",
        "logging.level.org.thymeleaf=TRACE"})
@AutoConfigureMockMvc
@ExtendWith({SystemStubsExtension.class, OutputCaptureExtension.class})
class LetterSavingSenderRestApiIntegrationTest extends AbstractMongoDBTest {
    
    private static final String SAVED_LETTER_FILEPATH =
            HtmlPdfGenerator.getPdfFilepath("PSCDIR_00006400");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationClient notificationClient;

    @BeforeEach
    void setUp() {
        System.getProperties().setProperty("xr.util-logging.loggingEnabled", "true");
    }

    @Test
    @DisplayName("Send letter successfully, saving letter PDF for troubleshooting in the process")
    void sendLetterSuccessfully(CapturedOutput log) throws Exception {
        String requestName = "send-new-psc-direction-letter-request";
        sendLetter(requestName, log);
        verifyLetterPdfContent();
        deleteLetterPdf(requestName);
    }

    @ParameterizedTest(name = "Send English letter request json: {0}")
    @ValueSource(strings = {
            "send-new-psc-direction-letter-request",
            "send-transitional-non-director-psc-information-letter-request",
            "send-extension-acceptance-letter-request",
            "send-second-extension-acceptance-letter-request",
            "send-csidvdeflet-request",
            "send-idvpscdefault-request"
    })
    void sendAndDeleteLetter(final String requestName, final CapturedOutput log) throws
            Exception {
        sendLetter(requestName, log);
        deleteLetterPdf(requestName);
    }

    @ParameterizedTest(name = "Send Welsh letter request json: {0}")
    @ValueSource(strings = {
            "send-new-psc-direction-letter-request",
            "send-transitional-non-director-psc-information-letter-request",
            "send-extension-acceptance-letter-request",
            "send-second-extension-acceptance-letter-request",
            "send-csidvdeflet-request",
            "send-idvpscdefault-request"
    })
    void sendAndDeleteWelshLetter(final String requestName, final CapturedOutput log) throws
            Exception {
        sendWelshLetter(requestName, log);
        deleteLetterPdf(requestName);
    }

    private void sendLetter(final String requestName, final CapturedOutput log) throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class), anyString())).thenReturn(responseReceived);

        // When and then
        postSendLetterRequest(mockMvc,
                getSendLetterRequestBody(requestName),
                status().isCreated());

        assertThat(log.getAll().contains(getExpectedSavingLetterLogMessage(requestName)), is(true));

        verifyLetterPdfSaved(requestName);
    }

    private void sendWelshLetter(final String requestName, final CapturedOutput log)
            throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class), anyString())).thenReturn(responseReceived);

        // When and then
        postSendLetterRequest(mockMvc,
                getWelshLetterRequest(getSendLetterRequestBody(requestName)),
                status().isCreated());

        assertThat(log.getAll().contains(getExpectedSavingLetterLogMessage(requestName)), is(true));

        verifyLetterPdfSaved(requestName);
    }

    private void deleteLetterPdf(final String requestName) throws Exception {
        var reference = getReference(requestName);
        var file = new File(HtmlPdfGenerator.getPdfFilepath(reference));
        file.deleteOnExit();
    }

    private String getExpectedSavingLetterLogMessage(final String requestName) throws IOException {
        var reference = getReference(requestName);
        var savedLetterFilepath = HtmlPdfGenerator.getPdfFilepath(reference);
        return "Saving PDF of letter to " + savedLetterFilepath + ".";
    }

    private static String getValidSendLetterRequestBody() throws IOException {
        return resourceToString("/fixtures/send-new-psc-direction-letter-request.json", UTF_8);
    }

    private static String getSendLetterRequestBody(final String requestName) throws IOException {
        return resourceToString("/fixtures/" + requestName + ".json", UTF_8);
    }

    private void verifyLetterPdfSaved(final String requestName) throws IOException {
        var reference = getReference(requestName);
        var savedLetterFilepath = Paths.get(HtmlPdfGenerator.getPdfFilepath(reference));
        assertThat(Files.exists(savedLetterFilepath), is(true));
    }

    private String getReference(final String requestName) throws IOException {
        var request = objectMapper.readValue(getSendLetterRequestBody(requestName),
                GovUkLetterDetailsRequest.class);
        if (StringUtils.isBlank(request.getLetterDetails().getLetterId())) {
            // Old letters do not have letter IDs and use just the reference
            return request.getSenderDetails().getReference();
        }
        return String.join("-", request.getSenderDetails().getAppId(),
                request.getLetterDetails().getLetterId(),
                request.getSenderDetails().getReference());
    }

    private void verifyLetterPdfContent() throws IOException {
        try (var document = Loader.loadPDF(new File(SAVED_LETTER_FILEPATH))) {

            // Substitutions all occur on page 1.
            var page1 = getPageText(document, 1);

            var request = objectMapper.readValue(
                    getValidSendLetterRequestBody(),
                    GovUkLetterDetailsRequest.class);

            // Reference
            var reference = request.getSenderDetails().getReference();
            assertThat(page1, containsString(reference));

            // Address block
            var address = request.getRecipientDetails().getPhysicalAddress();
            var addressLine1 = address.getAddressLine1();
            assertThat(page1, containsString(addressLine1));
            var addressLine2 = address.getAddressLine2().toUpperCase(); // company name
            assertThat(page1, containsString(addressLine2));
            var addressLine3 = address.getAddressLine3();
            assertThat(page1, containsString(addressLine3));
            var addressLine4 = address.getAddressLine4();
            assertThat(page1, containsString(addressLine4));
            var addressLine5 = address.getAddressLine5();
            assertThat(page1, containsString(addressLine5));
            var addressLine6 = address.getAddressLine6();
            assertThat(page1, containsString(addressLine6));

            // Personalisation details
            Map<String,String> personalisationDetails =
                    objectMapper.readValue(request.getLetterDetails().getPersonalisationDetails(),
                            new TypeReference<>() {});
            var pscFullName = personalisationDetails.get("psc_name");
            assertThat(page1, containsString(pscFullName));
            var companyName = personalisationDetails.get("company_name").toUpperCase();
            assertThat(page1, containsString(companyName));
            var deadlineDate = personalisationDetails.get("idv_verification_due_date");
            assertThat(page1, containsString(deadlineDate));
            var startDate = personalisationDetails.get("idv_start_date");
            assertThat(page1, containsString(startDate));
        }
    }

    private String getPageText(PDDocument document, int pageNumber) throws IOException {
        var textStripper = new PDFTextStripper();
        textStripper.setStartPage(pageNumber);
        textStripper.setEndPage(pageNumber);
        return textStripper.getText(document);
    }

    private String getWelshLetterRequest(final String letterBody)
            throws IOException {
        var request = objectMapper.readValue(
                letterBody,
                GovUkLetterDetailsRequest.class);
        var letterDetails = request.getLetterDetails();
        var personalisationDetailsString = letterDetails.getPersonalisationDetails();

        var personalisationDetails = JsonParser
                .parseString(personalisationDetailsString)
                .getAsJsonObject();
        personalisationDetails.addProperty(IS_WELSH, true);

        letterDetails.setPersonalisationDetails(personalisationDetails.toString());
        return objectMapper.writeValueAsString(request);
    }

}
