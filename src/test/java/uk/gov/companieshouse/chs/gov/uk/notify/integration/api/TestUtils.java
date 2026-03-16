package uk.gov.companieshouse.chs.gov.uk.notify.integration.api;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.API_KEY_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.INTERNAL_USER_ROLE;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.AddressDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailDetailsDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRecipientDetailsDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.EmailRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterDetailsDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterRecipientDetailsDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.LetterRequestDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.SenderDetailsDao;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.SendEmailResponse;

public class TestUtils {

    private static final String CONTEXT_ID = "X9uND6rXQxfbZNcMVFA7JI4h2KOh";
    private static final String X_REQUEST_ID = "X-Request-ID";
    private static final String ERIC_IDENTITY = "ERIC-Identity";
    private static final String ERIC_IDENTITY_VALUE = "65e73495c8e2";
    private static final String PERSONALISATION_DETAILS =
            "{ \"idv_start_date\": \"30 June 2025\", "
                    + "\"psc_appointment_date\": \"24 June 2025\", "
                    + "\"idv_verification_due_date\": \"14 July 2025\", "
                    + "\"psc_name\": \"Joe Bloggs\", "
                    + "\"company_name\": \"Tŷ'r Cwmnïau\","
                    + "\"company_number\": \"00006400\"}";

    public static LetterRequestDao createSampleLetterRequest(String addressLine1) {
        return createSampleLetterRequestWithReference(addressLine1, "test-reference");
    }

    public static LetterRequestDao createSampleLetterRequestWithReference(String addressLine1, String reference) {
        SenderDetailsDao senderDetails = new SenderDetailsDao();
        senderDetails.setAppId("chips");
        senderDetails.setReference(reference);
        AddressDao address = new AddressDao();
        address.setAddressLine1(addressLine1);
        address.setAddressLine2("Apt 101");
        address.setAddressLine3("District");
        address.setAddressLine4("City");
        address.setAddressLine5("County");
        LetterRecipientDetailsDao recipientDetails = new LetterRecipientDetailsDao();
        recipientDetails.setName("Test Recipient");
        recipientDetails.setPhysicalAddress(address);
        LetterDetailsDao letterDetails = new LetterDetailsDao();
        letterDetails.setLetterId("IDVPSCDIRNEW");
        letterDetails.setTemplateId("v1.0");
        letterDetails.setPersonalisationDetails(PERSONALISATION_DETAILS);

        LetterRequestDao letterRequest = new LetterRequestDao();
        letterRequest.setSenderDetails(senderDetails);
        letterRequest.setRecipientDetails(recipientDetails);
        letterRequest.setLetterDetails(letterDetails);
        letterRequest.setCreatedAt(OffsetDateTime.of(2025, 4, 8, 4, 49, 12, 0, OffsetDateTime.now().getOffset()));
        return letterRequest;
    }

    public static LetterRequestDao createLetterWithReference(String reference) {
        return createSampleLetterRequestWithReference("Address line 1", reference);
    }

    public static EmailRequestDao createSampleEmailRequest(String email) {
        return createSampleEmailRequestWithReference(email, "test-reference");
    }

    public static EmailRequestDao createSampleEmailRequestWithReference(String email, String reference) {
        SenderDetailsDao senderDetails = new SenderDetailsDao();
        senderDetails.setAppId("chips");
        senderDetails.setReference(reference);
        EmailRecipientDetailsDao recipientDetails = new EmailRecipientDetailsDao();
        recipientDetails.setName("Test User");
        recipientDetails.setEmailAddress(email);
        EmailDetailsDao emailDetails = new EmailDetailsDao();
        emailDetails.setTemplateId("template-123");
        emailDetails.setPersonalisationDetails("Hello {{name}}");

        EmailRequestDao emailRequest = new EmailRequestDao();
        emailRequest.setSenderDetails(senderDetails);
        emailRequest.setRecipientDetails(recipientDetails);
        emailRequest.setEmailDetails(emailDetails);
        emailRequest.setCreatedAt(OffsetDateTime.now());
        return emailRequest;
    }

    public static EmailRequestDao createSampleNotificationRequest() {
        return createSampleEmailRequestWithReference("test@example.com", "test-reference");
    }

    public static SendEmailResponse createSampleEmailResponse() {

        JSONObject templateJson = new JSONObject()
                .put("id", UUID.randomUUID().toString())
                .put("version", 1)
                .put("uri", "https://api.notifications.service.gov.uk/v2/template/abcdefg");

        JSONObject contentJson = new JSONObject()
                .put("body", "Hello World")
                .put("from_email", "service@example.com")
                .put("subject", "Test Email");

        JSONObject responseJson = new JSONObject()
                .put("id", UUID.randomUUID().toString())
                .put("reference", "client-reference")
                .put("content", contentJson)
                .put("template", templateJson);

        return new SendEmailResponse(responseJson.toString());
    }

    public static LetterResponse createSampleLetterResponse() {
        JSONObject json = new JSONObject();
        json.put("id", UUID.randomUUID().toString());
        json.put("reference", "example-reference");
        json.put("postage", "first");
        return new LetterResponse(json.toString());
    }

    public static String getPageText(PDDocument pdf, int pageNumber) throws IOException {
        var textStripper = new PDFTextStripper();
        textStripper.setStartPage(pageNumber);
        textStripper.setEndPage(pageNumber);
        return textStripper.getText(pdf);
    }

    public static String getValidSendLetterRequestBody() throws IOException {
        return resourceToString("/fixtures/send-new-psc-direction-letter-request.json", UTF_8);
    }

    public static ResultActions postSendLetterRequest(MockMvc mockMvc,
                                                      String requestBody,
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
}
