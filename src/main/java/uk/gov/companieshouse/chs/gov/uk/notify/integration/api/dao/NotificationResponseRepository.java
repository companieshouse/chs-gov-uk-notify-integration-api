package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.dao;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.NotificationResponse;

@Repository
public interface NotificationResponseRepository extends MongoRepository<NotificationResponse, String> {
    List<NotificationResponse> findByRequestId(String requestId);
}
