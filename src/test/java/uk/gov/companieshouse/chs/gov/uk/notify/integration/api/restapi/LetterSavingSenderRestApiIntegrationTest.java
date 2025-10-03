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
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.Constants.DATE_FORMATTER;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.IS_WELSH;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonParser;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
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
            HtmlPdfGenerator.getPdfFilepath("send-letter-request");
    private static final File[] SAVED_LETTERS_TO_DELETE = new File[] {
            new File(SAVED_LETTER_FILEPATH),
            new File(HtmlPdfGenerator.getPdfFilepath("send-new-psc-direction-letter-request")),
            new File(HtmlPdfGenerator.getPdfFilepath("send-transitional-non-director-psc-information-letter-request"))
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationClient notificationClient;

    @BeforeEach
    void setUp() {
        System.getProperties().setProperty("xr.util-logging.loggingEnabled", "true");
        Arrays.stream(SAVED_LETTERS_TO_DELETE).forEach(File::delete);
    }

    @Test
    @DisplayName("Send letter successfully, saving letter PDF for troubleshooting in the process")
    void sendLetterSuccessfully(CapturedOutput log) throws Exception {
        sendLetter("send-letter-request", log);
        verifyLetterPdfContent();
    }

    @Test
    @DisplayName("Send New PSC Direction letter successfully, saving letter PDF for troubleshooting in the process")
    void sendNewPscDirectionLetterSuccessfully(CapturedOutput log) throws Exception {
        sendLetter("send-new-psc-direction-letter-request", log);
    }

    @Test
    @DisplayName("Send Welsh New PSC Direction letter successfully, saving letter PDF for troubleshooting in the process")
    void sendWelshNewPscDirectionLetterSuccessfully(CapturedOutput log) throws Exception {
        sendWelshLetter("send-new-psc-direction-letter-request", log);
    }

    @Test
    @DisplayName("Send Transitional Non-director PSC Information letter successfully, saving letter PDF for troubleshooting in the process")
    void sendTransitionalPscInformationLetterSuccessfully(CapturedOutput log) throws Exception {
        sendLetter("send-transitional-non-director-psc-information-letter-request", log);
    }

    @Test
    @DisplayName("Send Welsh Transitional Non-director PSC Information letter successfully, saving letter PDF for troubleshooting in the process")
    void sendWelshTransitionalPscInformationLetterSuccessfully(CapturedOutput log) throws Exception {
        sendWelshLetter("send-transitional-non-director-psc-information-letter-request", log);
    }

    @Test
    @DisplayName("Send Extension Acceptance letter successfully, saving letter PDF for troubleshooting in the process")
    void sendExtensionAcceptanceLetterSuccessfully(CapturedOutput log) throws Exception {
        sendLetter("send-extension-acceptance-letter-request", log);
    }

    @Test
    @DisplayName("Send Welsh Extension Acceptance letter successfully, saving letter PDF for troubleshooting in the process")
    void sendWelshExtensionAcceptanceLetterSuccessfully(CapturedOutput log) throws Exception {
        sendWelshLetter("send-extension-acceptance-letter-request", log);
    }

    @Test
    @DisplayName("Send Second Extension Acceptance letter successfully, saving letter PDF for troubleshooting in the process")
    void sendSecondExtensionAcceptanceLetterSuccessfully(CapturedOutput log) throws Exception {
        sendLetter("send-second-extension-acceptance-letter-request", log);
    }

    @Test
    @DisplayName("Send Welsh Second Extension Acceptance letter successfully, saving letter PDF for troubleshooting in the process")
    void sendWelshSecondExtensionAcceptanceLetterSuccessfully(CapturedOutput log) throws Exception {
        sendWelshLetter("send-second-extension-acceptance-letter-request", log);
    }

    private void sendLetter(final String requestName, final CapturedOutput log) throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class))).thenReturn(responseReceived);

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
                anyString(), any(InputStream.class))).thenReturn(responseReceived);

        // When and then
        postSendLetterRequest(mockMvc,
                getWelshLetterRequest(getSendLetterRequestBody(requestName)),
                status().isCreated());

        assertThat(log.getAll().contains(getExpectedSavingLetterLogMessage(requestName)), is(true));

        verifyLetterPdfSaved(requestName);
    }

    private static String getExpectedSavingLetterLogMessage(final String requestName) {
        var savedLetterFilepath = HtmlPdfGenerator.getPdfFilepath(requestName);
        return "Saving PDF of letter to " + savedLetterFilepath + ".";
    }

    private static String getValidSendLetterRequestBody() throws IOException {
        return resourceToString("/fixtures/send-letter-request.json", UTF_8);
    }

    private static String getSendLetterRequestBody(final String requestName) throws IOException {
        return resourceToString("/fixtures/" + requestName + ".json", UTF_8);
    }

    private static void verifyLetterPdfSaved(final String requestName) {
        var savedLetterFilepath = Paths.get(HtmlPdfGenerator.getPdfFilepath(requestName));
        assertThat(Files.exists(savedLetterFilepath), is(true));
    }

    private void verifyLetterPdfContent() throws IOException {
        try (var document = Loader.loadPDF(new File(SAVED_LETTER_FILEPATH))) {

            // Substitutions all occur on page 1.
            var page1 = getPageText(document, 1);

            var request = objectMapper.readValue(
                    getValidSendLetterRequestBody(),
                    GovUkLetterDetailsRequest.class);

            // Date
            var date = LocalDate.now().format(DATE_FORMATTER);
            assertThat(page1, containsString(date));

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
            var pscFullName = personalisationDetails.get("psc_full_name");
            assertThat(page1, containsString(pscFullName));
            var companyName = personalisationDetails.get("company_name").toUpperCase();
            assertThat(page1, containsString(companyName));
            var deadlineDate = personalisationDetails.get("deadline_date");
            assertThat(page1, containsString(deadlineDate));
            var extensionDate = personalisationDetails.get("extension_date");
            assertThat(page1, containsString(extensionDate));
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
