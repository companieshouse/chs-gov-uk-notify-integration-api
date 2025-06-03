package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.API_KEY_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.INTERNAL_USER_ROLE;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator.HtmlPdfGenerator;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@SpringBootTest(properties = {"save.letter=true"})
@AutoConfigureMockMvc
@ExtendWith({SystemStubsExtension.class, OutputCaptureExtension.class})
class LetterSavingSenderRestApiIntegrationTest extends AbstractMongoDBTest {

    private static final String CONTEXT_ID = "X9uND6rXQxfbZNcMVFA7JI4h2KOh";
    private static final String X_REQUEST_ID = "X-Request-ID";
    private static final String ERIC_IDENTITY = "ERIC-Identity";
    private static final String ERIC_IDENTITY_VALUE = "65e73495c8e2";
    private static final String SAVED_LETTER_FILEPATH =
            HtmlPdfGenerator.getPdfFilepath("send-letter-request");
    private static final String SAVING_LETTER_LOG_MESSAGE =
            "Saving PDF of letter to " + SAVED_LETTER_FILEPATH + ".";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationClient notificationClient;

    @BeforeEach
    void setUp() {
        var savedLetter = new File(SAVED_LETTER_FILEPATH);
        savedLetter.delete();
    }

    @Test
    @DisplayName("Send letter successfully, saving letter PDF for troubleshooting in the process")
    void sendLetterSuccessfully(CapturedOutput log) throws Exception {

        // Given
        var responseReceived = new LetterResponse(
                resourceToString("/fixtures/send-letter-response.json", UTF_8));
        when(notificationClient.sendPrecompiledLetterWithInputStream(
                anyString(), any(InputStream.class))).thenReturn(responseReceived);

        // When and then
        postSendLetterRequest(getValidSendLetterRequestBody(),
                status().isCreated());

        assertThat(log.getAll().contains(SAVING_LETTER_LOG_MESSAGE), is(true));

        verifyLetterPdfSaved();
        verifyLetterPdfContent();
    }

    private ResultActions postSendLetterRequest(String requestBody,
                                                ResultMatcher expectedResponseStatus)
            throws Exception {
        return mockMvc.perform(post("/gov-uk-notify-integration/letter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_REQUEST_ID, CONTEXT_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE)
                        .content(requestBody))
                .andExpect(expectedResponseStatus);
    }

    private static String getValidSendLetterRequestBody() throws IOException {
        return resourceToString("/fixtures/send-letter-request.json", UTF_8);
    }


    private static void verifyLetterPdfSaved() {
        var savedLetterFilepath = Paths.get(SAVED_LETTER_FILEPATH);
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
            var format = DateTimeFormatter.ofPattern("dd MMMM yyyy");
            var date = LocalDate.now().format(format);
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

}
