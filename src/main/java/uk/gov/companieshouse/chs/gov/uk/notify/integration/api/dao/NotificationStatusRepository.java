package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.dao;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.NotificationStatus;
    
@Repository
public interface NotificationStatusRepository extends MongoRepository<NotificationStatus, String> {
    List<NotificationStatus> findByRequestId(String requestId);
    List<NotificationStatus> findByResponseId(String responseId);
}
