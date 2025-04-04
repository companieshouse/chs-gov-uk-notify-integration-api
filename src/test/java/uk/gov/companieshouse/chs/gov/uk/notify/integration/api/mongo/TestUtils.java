package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.Address;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.EmailDetails;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.LetterDetails;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.RecipientDetailsEmail;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.RecipientDetailsLetter;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.SenderDetails;

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
    
}
