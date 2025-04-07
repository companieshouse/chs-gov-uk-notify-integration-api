package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.dao;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.NotificationLetterRequest;

@Repository
public interface NotificationLetterRequestRepository extends MongoRepository<NotificationLetterRequest, String> {}
