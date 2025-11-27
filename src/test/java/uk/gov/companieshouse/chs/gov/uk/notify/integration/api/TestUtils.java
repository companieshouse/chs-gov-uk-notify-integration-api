package uk.gov.companieshouse.chs.gov.uk.notify.integration.api;

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
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.api.chs.notification.model.EmailDetails;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.LetterDetails;
import uk.gov.companieshouse.api.chs.notification.model.RecipientDetailsEmail;
import uk.gov.companieshouse.api.chs.notification.model.RecipientDetailsLetter;
import uk.gov.companieshouse.api.chs.notification.model.SenderDetails;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.SendEmailResponse;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.API_KEY_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.INTERNAL_USER_ROLE;

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

    public static GovUkLetterDetailsRequest createSampleLetterRequest(String addressLine1) {
        SenderDetails senderDetails = new SenderDetails("test-app-id", "test-reference");
        Address address = new Address()
                .addressLine1(addressLine1)
                .addressLine2("Apt 101")
                .addressLine3("District")
                .addressLine4("City")
                .addressLine5("County");
        RecipientDetailsLetter recipientDetails = new RecipientDetailsLetter()
                .name("Test Recipient")
                .physicalAddress(address);
        LetterDetails letterDetails = new LetterDetails("template-456", PERSONALISATION_DETAILS);

        return new GovUkLetterDetailsRequest()
                .senderDetails(senderDetails)
                .recipientDetails(recipientDetails)
                .letterDetails(letterDetails)
                .createdAt(OffsetDateTime.now());
    }

    public static GovUkLetterDetailsRequest createSampleLetterRequestWithReference(String addressLine1, String reference) {
        SenderDetails senderDetails = new SenderDetails("test-app-id", reference);
        Address address = new Address()
                .addressLine1(addressLine1)
                .addressLine2("Apt 101")
                .addressLine3("District")
                .addressLine4("City")
                .addressLine5("County");
        RecipientDetailsLetter recipientDetails = new RecipientDetailsLetter()
                .name("Test Recipient")
                .physicalAddress(address);
        LetterDetails letterDetails = new LetterDetails("template-456", PERSONALISATION_DETAILS);

        return new GovUkLetterDetailsRequest()
                .senderDetails(senderDetails)
                .recipientDetails(recipientDetails)
                .letterDetails(letterDetails)
                .createdAt(OffsetDateTime.now());
    }

    public static GovUkLetterDetailsRequest createLetterWithReference(String reference) {
        return createSampleLetterRequestWithReference("Address line 1", reference);
    }

    public static GovUkLetterDetailsRequest createSampleLetterRequestWithTemplateId(String appId, String templateId) {
        SenderDetails senderDetails = new SenderDetails(appId, "test-reference");
        Address address = new Address()
                .addressLine1("Test Address Line 1")
                .addressLine2("Apt 101")
                .addressLine3("District")
                .addressLine4("City")
                .addressLine5("County");
        RecipientDetailsLetter recipientDetails = new RecipientDetailsLetter()
                .name("Test Recipient")
                .physicalAddress(address);
        LetterDetails letterDetails = new LetterDetails(templateId, "Dear {{name}}");

        return new GovUkLetterDetailsRequest()
                .senderDetails(senderDetails)
                .recipientDetails(recipientDetails)
                .letterDetails(letterDetails)
                .createdAt(OffsetDateTime.now());
    }

    public static GovUkEmailDetailsRequest createSampleEmailRequest(String email) {
        SenderDetails senderDetails = new SenderDetails("test-app-id", "test-reference");
        RecipientDetailsEmail recipientDetails = new RecipientDetailsEmail("Test User", email);
        EmailDetails emailDetails = new EmailDetails("template-123", "Hello {{name}}");

        return new GovUkEmailDetailsRequest()
                .senderDetails(senderDetails)
                .recipientDetails(recipientDetails)
                .emailDetails(emailDetails)
                .createdAt(OffsetDateTime.now());
    }

    public static GovUkEmailDetailsRequest createSampleEmailRequestWithReference(String email, String reference) {
        SenderDetails senderDetails = new SenderDetails("test-app-id", reference);
        RecipientDetailsEmail recipientDetails = new RecipientDetailsEmail("Test User", email);
        EmailDetails emailDetails = new EmailDetails("template-123", "Hello {{name}}");

        return new GovUkEmailDetailsRequest()
                .senderDetails(senderDetails)
                .recipientDetails(recipientDetails)
                .emailDetails(emailDetails)
                .createdAt(OffsetDateTime.now());
    }

    public static NotificationEmailRequest createSampleNotificationRequest() {
        SenderDetails senderDetails = new SenderDetails("test-app-id", "test-reference");
        RecipientDetailsEmail recipientDetails = new RecipientDetailsEmail("Test User", "test@example.com");
        EmailDetails emailDetails = new EmailDetails("template-123", "Hello {{name}}");

        GovUkEmailDetailsRequest emailRequest = new GovUkEmailDetailsRequest()
                .senderDetails(senderDetails)
                .recipientDetails(recipientDetails)
                .emailDetails(emailDetails)
                .createdAt(OffsetDateTime.now());

        return new NotificationEmailRequest(null, null, emailRequest, "1");
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
        return resourceToString("/fixtures/send-letter-request.json", UTF_8);
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
