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
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterRequestDao;
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

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationClient notificationClient;

    private LetterRequestDao letterRequest = TestUtils.createLetterRequest();

    @BeforeEach
    void setUp() {
        System.getProperties().setProperty("xr.util-logging.loggingEnabled", "true");
    }

    @Test
    @DisplayName("Send letter successfully, saving letter PDF for troubleshooting in the process")
    void sendLetterSuccessfully(CapturedOutput log) throws Exception {
        saveRequestInDatabase();

        sendLetter(log);
        var filename = letterRequest.getSenderDetails().getAppId() + "-"
                + letterRequest.getLetterDetails().getLetterId() + "-"
                + letterRequest.getSenderDetails().getReference();
        verifyLetterPdfContent(filename);
        deleteLetterPdf();
    }

    @ParameterizedTest(name = "Send English letter request: {0} {1}")
    @CsvSource( value ={
            "null,new_psc_direction_letter_v1,IDVPSCDIRNEW",
            "IDVPSCDIRNEW,v1.0,IDVPSCDIRNEW",
            "null,transitional_non_director_psc_information_letter_v1,IDVPSCDIRTRAN",
            "IDVPSCDIRTRAN,v1.0,IDVPSCDIRTRAN",
            "null,extension_acceptance_letter_v1,IDVPSCEXT",
            "IDVPSCEXT1,v1.0,IDVPSCEXT",
            "null,second_extension_acceptance_letter_v1,IDVPSCEXT",
            "IDVPSCEXT2,v1.0,IDVPSCEXT",
            "CSIDVDEFLET,v1.0,CSIDVDEFLET",
            "CSIDVDEFLET,v1.1,CSIDVDEFLET",
            "IDVPSCDEFAULT,v1.0,IDVPSCDEFAULT",
            "IDVPSCDEFAULT,v1.1,IDVPSCDEFAULT"},
            nullValues = { "null" }
    )
    void sendAndDeleteLetter(final String letterType, final String templateId,
            final String personalisationDetailsFile, final CapturedOutput log) throws Exception {

        // English version
        boolean isWelsh = false;
        testSendAndDelete(letterType, templateId, personalisationDetailsFile, log, isWelsh);

        notificationLetterRequestRepository.deleteAll();

        // Welsh version
        isWelsh = true;
        testSendAndDelete(letterType, templateId, personalisationDetailsFile, log, isWelsh);
    }

    private void testSendAndDelete(final String letterType, final String templateId,
            final String personalisationDetailsFile, final CapturedOutput log, boolean isWelsh)
            throws Exception {
        configureRequest(letterType, templateId, personalisationDetailsFile, isWelsh);
        saveRequestInDatabase();

        sendLetter(log);
        deleteLetterPdf();
    }

    private void configureRequest(final String letterType, final String templateId,
            final String personalisationDetailsFilename, boolean isWelsh) throws IOException {
        var personalisationDetails = JsonParser.parseString(resourceToString(
                "/fixtures/personalisation-details/" + personalisationDetailsFilename + ".json",
                UTF_8)).getAsJsonObject();
        personalisationDetails.addProperty(IS_WELSH, isWelsh);

        var letterDetails = letterRequest.getLetterDetails();

        letterDetails.setLetterId(letterType);
        letterDetails.setTemplateId(templateId);
        letterDetails.setPersonalisationDetails(personalisationDetails.toString());
    }

    private void saveRequestInDatabase() {
        NotificationLetterRequest notificationLetterRequest = new NotificationLetterRequest();
        notificationLetterRequest.setRequest(letterRequest);
        notificationLetterRequestRepository.save(notificationLetterRequest);
    }

    private void sendLetter(final CapturedOutput log) throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class), anyString())).thenReturn(responseReceived);

        // When and then
        postSendLetterRequest(mockMvc,
                getSendLetterRequestBody(),
                status().isCreated());

        assertThat(log.getAll().contains(getExpectedSavingLetterLogMessage()), is(true));

        verifyLetterPdfSaved();
    }

    private void deleteLetterPdf() throws Exception {
        var reference = getReference();
        var file = new File(HtmlPdfGenerator.getPdfFilepath(reference));
        file.deleteOnExit();
    }

    private String getExpectedSavingLetterLogMessage() throws IOException {
        var reference = getReference();
        var savedLetterFilepath = HtmlPdfGenerator.getPdfFilepath(reference);
        return "Saving PDF of letter to " + savedLetterFilepath + ".";
    }

    private static String getSendLetterRequestBody() throws IOException {
        return resourceToString("/fixtures/valid-api-request.json", UTF_8);
    }

    private void verifyLetterPdfSaved() throws IOException {
        var reference = getReference();
        var savedLetterFilepath = Paths.get(HtmlPdfGenerator.getPdfFilepath(reference));
        assertThat(Files.exists(savedLetterFilepath), is(true));
    }

    private String getReference() throws IOException {
        if (StringUtils.isBlank(letterRequest.getLetterDetails().getLetterId())) {
            // Old letters do not have letter IDs and use just the reference
            return letterRequest.getSenderDetails().getReference();
        }
        return String.join("-", letterRequest.getSenderDetails().getAppId(),
                letterRequest.getLetterDetails().getLetterId(),
                letterRequest.getSenderDetails().getReference());
    }

    private void verifyLetterPdfContent(String filename) throws IOException {

        String letterPath = HtmlPdfGenerator.getPdfFilepath(filename);
        try (var document = Loader.loadPDF(new File(letterPath))) {

            // Substitutions all occur on page 1.
            var page1 = getPageText(document, 1);

            // Address block
            var address = letterRequest.getRecipientDetails().getPhysicalAddress();
            var addressLine1 = address.getAddressLine1();
            assertThat(page1, containsString(addressLine1));
            var addressLine2 = address.getAddressLine2(); // company name
            assertThat(page1, containsString(addressLine2));
            var addressLine3 = address.getAddressLine3();
            assertThat(page1, containsString(addressLine3));
            var addressLine4 = address.getAddressLine4();
            assertThat(page1, containsString(addressLine4));
            var addressLine5 = address.getAddressLine5();
            assertThat(page1, containsString(addressLine5));
            var addressLine6 = address.getAddressLine6();
            assertThat(page1, containsString(addressLine6));
            var addressLine7 = address.getAddressLine7();
            assertThat(page1, containsString(addressLine7));

            // Personalisation details
            Map<String,String> personalisationDetails =
                    new ObjectMapper().readValue(letterRequest.getLetterDetails().getPersonalisationDetails(),
                            new TypeReference<>() {});
            var pscFullName = personalisationDetails.get("psc_name");
            assertThat(page1, containsString(pscFullName));
            var companyName = personalisationDetails.get("company_name");
            assertThat(page1, containsString(companyName));
            var deadlineDate = personalisationDetails.get("idv_verification_due_date");
            assertThat(page1, containsString(deadlineDate));
            var startDate = personalisationDetails.get("idv_start_date");
            assertThat(page1, containsString(startDate));
        }
    }

    private static String getPageText(PDDocument document, int pageNumber) throws IOException {
        var textStripper = new PDFTextStripper();
        textStripper.setStartPage(pageNumber);
        textStripper.setEndPage(pageNumber);
        return textStripper.getText(document);
    }

}
