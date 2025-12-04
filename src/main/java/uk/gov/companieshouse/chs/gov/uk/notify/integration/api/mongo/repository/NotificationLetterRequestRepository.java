package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;

@Repository
public interface NotificationLetterRequestRepository extends
        MongoRepository<NotificationLetterRequest, String> {

    @Query("{ 'request.senderDetails.reference' : ?0 }")
    List<NotificationLetterRequest> findByReference(String reference);

    @Query("""
            {$and: [
                {'request.letterDetails.personalisationDetails':
                 {$regex: '"psc_name": "?0"'}},
                {'request.letterDetails.personalisationDetails':
                 {$regex: '"company_number": "?1"'}},
                {'request.letterDetails.letterId': ?2},
                {'request.letterDetails.templateId': '?3'},
                {'request.createdAt': { $gte: { $date: '?4'}, $lt: { $date: '?5'} }}
            ]}
            """
    )
    List<NotificationLetterRequest> findByNameCompanyTemplateDate(
              String pscName,
              String companyNumber,
              String letterId,
              String templateId,
              String letterSendingDate,
              String letterSendingDateNextDay);

}
