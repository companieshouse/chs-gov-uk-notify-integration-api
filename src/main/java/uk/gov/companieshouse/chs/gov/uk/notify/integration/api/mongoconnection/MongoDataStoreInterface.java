package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection;

import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
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

    ResponseEntity<uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest> findEmailBId(@Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String emailId);
}
