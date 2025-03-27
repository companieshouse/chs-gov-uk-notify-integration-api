package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chs_notification_sender.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_notification_sender.model.GovUkLetterDetailsRequest;

import java.util.List;
import java.util.UUID;

@Component
public class MongoDataStoreImpl implements MongoDataStoreInterface {

    EmailDetailsRepository emailDetailsRepository;
    LetterDetailsRepository letterDetailsRepository;

    public MongoDataStoreImpl(EmailDetailsRepository emailDetailsRepository, LetterDetailsRepository letterDetailsRepository) {
        this.emailDetailsRepository = emailDetailsRepository;
        this.letterDetailsRepository = letterDetailsRepository;
    }

    /**
     * @param emailDetails
     * @return
     */
    @Override
    public UUID storeEmail(GovUkEmailDetailsRequest emailDetails) {
        throw new NotImplementedException();
    }

    /**
     * @param letterDetails
     * @return
     */
    @Override
    public UUID storeLetter(GovUkLetterDetailsRequest letterDetails) {
        throw new NotImplementedException();
    }

    /**
     * @param id
     * @return
     */
    @Override
    public EmailDetails getEmail(UUID id) {
        throw new NotImplementedException();
    }

    /**
     * @param id
     * @return
     */
    @Override
    public LetterDetails getLetter(UUID id) {
        throw new NotImplementedException();
    }

    /**
     * @param id
     * @param status
     * @return
     */
    @Override
    public EmailDetails updateEmailStatus(UUID id, String status) {
        throw new NotImplementedException();
    }

    /**
     * @param id
     * @param status
     * @return
     */
    @Override
    public LetterDetails updateLetterStatus(UUID id, String status) {
        throw new NotImplementedException();
    }

    /**
     * @param emailId
     * @return
     */
    @Override
    public EmailDetails findEmailBId(String emailId) {
        throw new NotImplementedException();
    }

    /**
     * @return
     */
    @Override
    public List<EmailDetails> findAllEmails() {
        return emailDetailsRepository.findAll();
    }
}
