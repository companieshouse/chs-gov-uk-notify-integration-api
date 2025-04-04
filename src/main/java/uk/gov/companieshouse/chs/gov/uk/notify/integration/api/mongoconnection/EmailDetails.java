package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import uk.gov.companieshouse.api.chs_notification_sender.model.RecipientDetailsEmail;
import uk.gov.companieshouse.api.chs_notification_sender.model.SenderDetails;

import java.util.UUID;

@Document("email_details")
public class EmailDetails {

    @Id
    private UUID id;

    private SenderDetails senderDetails;
    private RecipientDetailsEmail recipientDetails;
    private uk.gov.companieshouse.api.chs_notification_sender.model.EmailDetails emailDetails;
    private String createdAt;
}
