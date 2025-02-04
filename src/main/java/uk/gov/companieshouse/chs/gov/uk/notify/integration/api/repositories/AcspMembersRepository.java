package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.AcspMembersDao;

import java.util.List;
import java.util.Optional;

@Repository
public interface AcspMembersRepository extends MongoRepository<AcspMembersDao, String> {

    @Query("{ 'acsp_number': ?0, 'status': { $ne: 'removed' } }")
    Page<AcspMembersDao> findAllNotRemovedByAcspNumber(
            final String acspNumber, final Pageable pageable);

    @Query("{ 'acsp_number': ?0 }")
    Page<AcspMembersDao> findAllByAcspNumber(final String acspNumber, final Pageable pageable);

    @Query("{ 'acsp_number': ?0, 'status': { $ne: 'removed' }, 'user_role': ?1 }")
    Page<AcspMembersDao> findAllNotRemovedByAcspNumberAndUserRole(
            final String acspNumber, final String userRole, final Pageable pageable);

    @Query("{ 'acsp_number': ?0, 'user_role': ?1 }")
    Page<AcspMembersDao> findAllByAcspNumberAndUserRole(
            final String acspNumber, final String userRole, final Pageable pageable);

    @Query(value = "{ 'user_id': ?0 }")
    List<AcspMembersDao> fetchAllAcspMembersByUserId(final String userId);

    @Query(value = "{ 'user_id': ?0, 'status': 'active' }")
    List<AcspMembersDao> fetchActiveAcspMembersByUserId(final String userId);

    @Query(value = "{ 'acsp_number': ?0, 'user_role': 'owner', 'status': 'active' }", count = true)
    int fetchNumberOfActiveOwners(final String acspNumber);

    @Query("{ 'user_id': ?0, 'acsp_number': ?1, 'status': 'active' }")
    Optional<AcspMembersDao> fetchActiveAcspMembership(final String userId, final String acspNumber);

    @Query("{ '_id': ?0 }")
    int updateAcspMembership(final String acspMembershipId, final Update update);

    @Query(value = "{ 'user_id': ?0, 'acsp_number': ?1 }")
    List<AcspMembersDao> fetchAllAcspMembersByUserIdAndAcspNumber(
            final String userId, final String acspNumber);

    @Query(value = "{ 'user_id': ?0, 'acsp_number': ?1, 'status': 'active' }")
    List<AcspMembersDao> fetchActiveAcspMembersByUserIdAndAcspNumber(
            final String userId, final String acspNumber);
}
