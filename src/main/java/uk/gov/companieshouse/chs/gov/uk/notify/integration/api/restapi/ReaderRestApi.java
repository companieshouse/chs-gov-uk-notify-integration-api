package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import jakarta.validation.constraints.Pattern;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.api.NotificationRetrievalInterface;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection.MongoDataStoreInterface;

import java.util.List;

@Controller
@Validated
public class ReaderRestApi implements NotificationRetrievalInterface {

    MongoDataStoreInterface mongoDataStore;

    /**
     * @param xHeaderId
     * @return
     */
    @Override
    public ResponseEntity<List<GovUkEmailDetailsRequest>> getAllEmails(@Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String xHeaderId) {
        throw new NotImplementedException();
    }

    /**
     * @return
     */
    @Override
    public ResponseEntity<List<GovUkLetterDetailsRequest>> getAllLetters() {
        throw new NotImplementedException();
    }

    /**
     * @param xHeaderId
     * @param emailId
     * @return
     */
    @Override
    public ResponseEntity<GovUkEmailDetailsRequest> getEmailDetailsById(String xHeaderId, @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String emailId) {
        return mongoDataStore.findEmailBId(emailId);
    }

    /**
     * @param xHeaderId
     * @param emailReference
     * @return
     */
    @Override
    public ResponseEntity<List<GovUkEmailDetailsRequest>> getEmailDetailsByReference(String xHeaderId, @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String emailReference) {
        throw new NotImplementedException();
    }

    /**
     * @param xHeaderId
     * @return
     */
    @Override
    public ResponseEntity<GovUkLetterDetailsRequest> getLetterDetails(String xHeaderId) {
        throw new NotImplementedException();
    }

    /**
     * @param xHeaderId
     * @param letterReference
     * @return
     */
    @Override
    public ResponseEntity<List<GovUkLetterDetailsRequest>> getLetterDetailsByReference(String xHeaderId, @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String letterReference) {
        throw new NotImplementedException();
    }
}
