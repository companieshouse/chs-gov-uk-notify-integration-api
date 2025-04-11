package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;

@Repository
public interface NotificationEmailRequestRepository extends MongoRepository<NotificationEmailRequest, String> {
    @Query("{ 'request.senderDetails.reference' : ?0 }")
    List<NotificationEmailRequest> findByReference(String reference);
}
