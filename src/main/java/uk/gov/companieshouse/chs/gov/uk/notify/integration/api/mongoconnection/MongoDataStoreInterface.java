package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection;

import jakarta.validation.constraints.Pattern;
import uk.gov.companieshouse.api.chs_notification_sender.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_notification_sender.model.GovUkLetterDetailsRequest;

import java.util.List;
import java.util.UUID;

public interface MongoDataStoreInterface {

    UUID storeEmail(GovUkEmailDetailsRequest emailDetails);

    UUID storeLetter(GovUkLetterDetailsRequest letterDetails);

    EmailDetails getEmail(UUID id);

    LetterDetails getLetter(UUID id);

    EmailDetails updateEmailStatus(UUID id, String status);

    LetterDetails updateLetterStatus(UUID id, String status);

    EmailDetails findEmailBId(@Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String emailId);

    List<EmailDetails> findAllEmails();
}
