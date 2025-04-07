package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import jakarta.validation.constraints.Pattern;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.api.NotificationRetrievalInterface;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;

@Controller
@Validated
public class ReaderRestApi implements NotificationRetrievalInterface {

    private final Logger logger = LoggerFactory.getLogger(ReaderRestApi.class.getName());

    private final NotificationDatabaseService notificationDatabaseService;

    public ReaderRestApi(NotificationDatabaseService notificationDatabaseService) {
        this.notificationDatabaseService = notificationDatabaseService;
    }
    
    @Override
    public ResponseEntity<List<GovUkEmailDetailsRequest>> getAllEmails(@Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String xHeaderId) {
        throw new NotImplementedException();
    }
    
    @Override
    public ResponseEntity<List<GovUkLetterDetailsRequest>> getAllLetters() {
        throw new NotImplementedException();
    }
    
    @Override
    public ResponseEntity<GovUkEmailDetailsRequest> getEmailDetailsById(String xHeaderId, @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String emailId) {
        throw new NotImplementedException();
    }
    
    @Override
    public ResponseEntity<List<GovUkEmailDetailsRequest>> getEmailDetailsByReference(String xHeaderId, @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String emailReference) {
        throw new NotImplementedException();
    }
    
    @Override
    public ResponseEntity<GovUkLetterDetailsRequest> getLetterDetails(String xHeaderId) {
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<List<GovUkLetterDetailsRequest>> getLetterDetailsByReference(String xHeaderId, @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String letterReference) {
        throw new NotImplementedException();
    }
    
}
