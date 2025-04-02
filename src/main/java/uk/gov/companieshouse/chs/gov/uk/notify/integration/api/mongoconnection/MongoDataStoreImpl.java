package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;

import java.util.List;

@Component
public class MongoDataStoreImpl implements MongoDataStoreInterface {

    private final EmailDetailsRepository emailDetailsRepository;
    private final LetterDetailsRepository letterDetailsRepository;

    public MongoDataStoreImpl(EmailDetailsRepository emailDetailsRepository,
                              final LetterDetailsRepository letterDetailsRepository) {
        this.emailDetailsRepository = emailDetailsRepository;
        this.letterDetailsRepository = letterDetailsRepository;
    }

    @Override
    public DatabaseEmailDetails storeEmail(GovUkEmailDetailsRequest emailDetailsRequest) {
//        return emailDetailsRepository.save(new DatabaseEmailDetails(emailDetailsRequest));
        return null;
    }

    @Override
    public String storeLetter(GovUkLetterDetailsRequest letterDetails) {
        throw new NotImplementedException();
    }

    @Override
    public DatabaseEmailDetails getEmail(String id) {
        //return emailDetailsRepository.findById(id);
        return null;
    }

    @Override
    public LetterDetails getLetter(String id) {
        throw new NotImplementedException();
    }

    @Override
    public DatabaseEmailDetails updateEmailStatus(String id, String status) {
//        Document query = new Document("requestId", requestId.toString());
//        Document update = new Document("$set", new Document("status", newStatus)
//                .append("timestamp", System.currentTimeMillis()));
//        collection.updateOne(query, update);
        return null;
    }

    @Override
    public LetterDetails updateLetterStatus(String id, String status) {
        throw new NotImplementedException();
    }

    @Override
    public DatabaseEmailDetails findEmailBId(String emailId) {
        return emailDetailsRepository.findById(emailId).get();
    }

    @Override
    public List<DatabaseEmailDetails> findAllEmails() {
//        return emailDetailsRepository.findAllEmails();
        return List.of();
    }
}
