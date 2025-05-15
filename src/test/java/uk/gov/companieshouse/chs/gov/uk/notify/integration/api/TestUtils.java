package uk.gov.companieshouse.chs.gov.uk.notify.integration.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.json.JSONObject;
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

public class TestUtils {

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
        LetterDetails letterDetails = new LetterDetails("template-456", new BigDecimal("1.0"), "Dear {{name}}");

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
        LetterDetails letterDetails = new LetterDetails("template-456", new BigDecimal("1.0"), "Dear {{name}}");

        return new GovUkLetterDetailsRequest()
                .senderDetails(senderDetails)
                .recipientDetails(recipientDetails)
                .letterDetails(letterDetails)
                .createdAt(OffsetDateTime.now());
    }

    public static GovUkEmailDetailsRequest createSampleEmailRequest(String email) {
        SenderDetails senderDetails = new SenderDetails("test-app-id", "test-reference");
        RecipientDetailsEmail recipientDetails = new RecipientDetailsEmail("Test User", email);
        EmailDetails emailDetails = new EmailDetails("template-123", new BigDecimal("1.0"), "Hello {{name}}");

        return new GovUkEmailDetailsRequest()
                .senderDetails(senderDetails)
                .recipientDetails(recipientDetails)
                .emailDetails(emailDetails)
                .createdAt(OffsetDateTime.now());
    }

    public static GovUkEmailDetailsRequest createSampleEmailRequestWithReference(String email, String reference) {
        SenderDetails senderDetails = new SenderDetails("test-app-id", reference);
        RecipientDetailsEmail recipientDetails = new RecipientDetailsEmail("Test User", email);
        EmailDetails emailDetails = new EmailDetails("template-123", new BigDecimal("1.0"), "Hello {{name}}");

        return new GovUkEmailDetailsRequest()
                .senderDetails(senderDetails)
                .recipientDetails(recipientDetails)
                .emailDetails(emailDetails)
                .createdAt(OffsetDateTime.now());
    }

    public static NotificationEmailRequest createSampleNotificationRequest() {
        SenderDetails senderDetails = new SenderDetails("test-app-id", "test-reference");
        RecipientDetailsEmail recipientDetails = new RecipientDetailsEmail("Test User", "test@example.com");
        EmailDetails emailDetails = new EmailDetails("template-123", new BigDecimal("1.0"), "Hello {{name}}");

        GovUkEmailDetailsRequest emailRequest = new GovUkEmailDetailsRequest()
                .senderDetails(senderDetails)
                .recipientDetails(recipientDetails)
                .emailDetails(emailDetails)
                .createdAt(OffsetDateTime.now());

        return new NotificationEmailRequest(LocalDateTime.now(), LocalDateTime.now().plusHours(1), emailRequest, "1");
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
}
