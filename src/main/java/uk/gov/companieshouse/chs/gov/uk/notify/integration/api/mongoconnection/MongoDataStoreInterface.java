package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chs_notification_sender.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_notification_sender.model.GovUkLetterDetailsRequest;

@Component
public interface MongoDataStoreInterface {

    void storeEmail(GovUkEmailDetailsRequest emailDetails);

    void storeLetter(GovUkLetterDetailsRequest letterDetails);

    EmailDetails getEmail(String id);

    LetterDetails getLetter(String id);

    EmailDetails updateEmailStatus(String id, String status);

    LetterDetails updateLetterStatus(String id, String status);
}
