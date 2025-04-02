package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection;

import jakarta.validation.constraints.Pattern;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;

import java.util.List;

public interface MongoDataStoreInterface {

    // emails
    DatabaseEmailDetails storeEmail(GovUkEmailDetailsRequest emailDetailsRequest);

    DatabaseEmailDetails getEmail(String id);

    DatabaseEmailDetails updateEmailStatus(String id, String status);

    DatabaseEmailDetails findEmailBId(@Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String emailId);

    List<DatabaseEmailDetails> findAllEmails();

    // letters
    LetterDetails getLetter(String id);

    String storeLetter(GovUkLetterDetailsRequest letterDetails);

    LetterDetails updateLetterStatus(String id, String status);

}
