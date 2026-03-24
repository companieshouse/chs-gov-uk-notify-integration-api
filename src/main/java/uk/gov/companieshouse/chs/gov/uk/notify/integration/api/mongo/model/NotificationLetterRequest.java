package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "letter_details")
public class NotificationLetterRequest extends NotificationRequest<LetterRequestDao> {

    public NotificationLetterRequest() {
        super();
    }

    public NotificationLetterRequest(LetterRequestDao request) {
        this();
        setRequest(request);
    }
}
