package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.EmailDetails;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.RecipientDetailsLetter;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.SenderDetails;


@Document(collection = "letter_details")
public class LetterDetails {

    @Id
    private String id;
    private SenderDetails senderDetails;
    private RecipientDetailsLetter recipientDetails;
    private EmailDetails emailDetails;
    private String createdAt;

}
