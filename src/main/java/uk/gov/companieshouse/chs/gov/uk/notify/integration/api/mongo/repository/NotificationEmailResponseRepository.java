package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailResponse;

@Repository
public interface NotificationEmailResponseRepository extends MongoRepository<NotificationEmailResponse, String> {
}
