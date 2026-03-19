package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;

@Repository
public interface NotificationLetterRequestRepository extends
        MongoRepository<NotificationLetterRequest, String> {

    @Query("{ 'request.sender_details.reference' : ?0 }")
    List<NotificationLetterRequest> findByReference(String reference);

    @Query("{ 'request.sender_details.app_id' : ?0, 'request.sender_details.reference' : ?1 }")
    Optional<NotificationLetterRequest> findByUniqueReference(String appId, String reference);

    @Query(value = "{ 'request.sender_details.reference' : { $regex: ?0 }}",
            sort = "{ 'request.created_at' : 1 }")
    Page<NotificationLetterRequest> findByReference(String reference, Pageable pageable);

    @Query("""
            {$and: [
               {$or: [
                    {'request.letter_details.personalisation_details':
                     {$regex: '"psc_name": *"?0"'}},
                    {'request.letter_details.letter_id': ?2},
                 ]
                },
                {'request.letter_details.personalisation_details':
                 {$regex: '"company_number": *"?1"'}},
                {'request.letter_details.template_id': '?3'},
                {'request.createdAt': { $gte: { $date: '?4'}, $lt: { $date: '?5'} }}
            ]}
            """
    )
    List<NotificationLetterRequest> findByPscNameOrLetterAndCompanyTemplateDate(
              String pscName,
              String companyNumber,
              String letterId,
              String templateId,
              String letterSendingDate,
              String letterSendingDateNextDay);

    @Query(value = """
            {$and: [
               {$or: [
                    {'request.letter_details.personalisation_details':
                     {$regex: '"psc_name": *"?0"'}},
                    {'request.letter_details.letter_id': ?2},
                 ]
                },
                {'request.letter_details.personalisation_details':
                 {$regex: '"company_number": *"?1"'}},
                {'request.letter_details.template_id': '?3'},
                {'request.createdAt': { $gte: { $date: '?4'}, $lt: { $date: '?5'} }}
            ]}
            """,
            sort = "{ 'request.createdAt' : 1 }"
    )
    Page<NotificationLetterRequest> findByPscNameOrLetterAndCompanyTemplateDate(
            String pscName,
            String companyNumber,
            String letterId,
            String templateId,
            String letterSendingDate,
            String letterSendingDateNextDay,
            Pageable pageable);

}
