package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("email_details")
public class EmailDetails {

    @Id
    private String id;
}
