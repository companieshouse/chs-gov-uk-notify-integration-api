package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongoconnection;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LetterDetailsRepository extends MongoRepository<LetterDetails, UUID> {

}
