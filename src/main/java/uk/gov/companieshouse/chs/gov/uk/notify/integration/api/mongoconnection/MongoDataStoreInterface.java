package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chs_notification_sender.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_notification_sender.model.GovUkLetterDetailsRequest;

import java.util.UUID;

@Component
public interface MongoDataStoreInterface {

    UUID storeEmail(GovUkEmailDetailsRequest emailDetails);

    UUID storeLetter(GovUkLetterDetailsRequest letterDetails);

    EmailDetails getEmail(UUID id);

    LetterDetails getLetter(UUID id);

    EmailDetails updateEmailStatus(UUID id, String status);

    LetterDetails updateLetterStatus(UUID id, String status);
}
