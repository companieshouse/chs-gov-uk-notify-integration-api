package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;

@Repository
public interface NotificationEmailRequestRepository extends MongoRepository<NotificationEmailRequest, String> {}
