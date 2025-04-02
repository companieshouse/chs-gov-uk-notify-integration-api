package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailDetailsRepository extends MongoRepository<DatabaseEmailDetails, String> {
//    DatabaseEmailDetails getEmail();
//    EmailDetails updateEmailStatus();
}
