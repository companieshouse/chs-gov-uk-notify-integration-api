package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;

@Repository
public interface NotificationLetterRequestRepository extends
        MongoRepository<NotificationLetterRequest, String> {

    @Query("{ 'request.senderDetails.reference' : ?0 }")
    List<NotificationLetterRequest> findByReference(String reference);

    // TODO DEEP-546 Can we stop the use of regexp syntax below being reported as a syntax error?
    @Query("{ 'request.senderDetails.reference' : /?0/ }")
    Page<NotificationLetterRequest> findByReference(String reference, Pageable pageable);

    @Query("""
            {$and: [
                {'request.letterDetails.personalisationDetails':
                 {$regex: '"psc_name": "?0"'}},
                {'request.letterDetails.personalisationDetails':
                 {$regex: '"company_number": "?1"'}},
                {'request.letterDetails.templateId': '?2'},
                {'request.createdAt': { $gte: { $date: '?3'}, $lt: { $date: '?4'} }}
            ]}
            """
    )
    List<NotificationLetterRequest> findByNameCompanyTemplateDate(
              String pscName,
              String companyNumber,
              String templateId,
              String letterSendingDate,
              String letterSendingDateNextDay);

    @Query("""
            {$and: [
                {'request.letterDetails.personalisationDetails':
                 {$regex: '"psc_name": "?0"'}},
                {'request.letterDetails.personalisationDetails':
                 {$regex: '"company_number": "?1"'}},
                {'request.letterDetails.templateId': '?2'},
                {'request.createdAt': { $gte: { $date: '?3'}, $lt: { $date: '?4'} }}
            ]}
            """
    )
    Page<NotificationLetterRequest> findByNameCompanyTemplateDate(
            String pscName,
            String companyNumber,
            String templateId,
            String letterSendingDate,
            String letterSendingDateNextDay,
            Pageable pageable);

}
