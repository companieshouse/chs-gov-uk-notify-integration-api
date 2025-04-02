package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.EmailDetails;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.RecipientDetailsEmail;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.SenderDetails;


@Document(collection = "email_details")
public class DatabaseEmailDetails {
// find a way to seralise the localdatetime, or parse it. 
// public class DatabaseEmailDetails extends GovUkEmailDetailsRequest {
    

    @Id
    private String id;
    
    // remove these, this class can extend, temporary to check we can talk to docker
    private SenderDetails senderDetails;
    private RecipientDetailsEmail recipientDetails;
    private EmailDetails emailDetails;

    public DatabaseEmailDetails(
            SenderDetails senderDetails,
            RecipientDetailsEmail recipientDetails,
            EmailDetails emailDetails) {
        this.senderDetails = senderDetails;
        this.recipientDetails = recipientDetails;
        this.emailDetails = emailDetails;
    }

}

