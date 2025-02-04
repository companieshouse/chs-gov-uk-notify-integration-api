package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.RequestContextUtil.getXRequestId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mapper.AcspMembershipCollectionMappers;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.AcspMembersDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.repositories.AcspMembersRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserStatusEnum;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class AcspMembersService {

    private static final Logger LOG =
            LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    private final AcspMembersRepository acspMembersRepository;
    private final AcspMembershipCollectionMappers acspMembershipCollectionMappers;

    public AcspMembersService(final AcspMembersRepository acspMembersRepository, final AcspMembershipCollectionMappers acspMembershipCollectionMappers) {
        this.acspMembersRepository = acspMembersRepository;
        this.acspMembershipCollectionMappers = acspMembershipCollectionMappers;
    }

    @Transactional(readOnly = true)
    public AcspMembershipsList findAllByAcspNumberAndRole(final String acspNumber, final AcspProfile acspProfile, final String role, final boolean includeRemoved, final int pageIndex, final int itemsPerPage) {
        final Pageable pageable = PageRequest.of(pageIndex, itemsPerPage);
        Page<AcspMembersDao> acspMemberDaos;

        if (Objects.nonNull(role)) {
            if (includeRemoved) {
                acspMemberDaos =
                        acspMembersRepository.findAllByAcspNumberAndUserRole(acspNumber, role, pageable);
            } else {
                acspMemberDaos =
                        acspMembersRepository.findAllNotRemovedByAcspNumberAndUserRole(
                                acspNumber, role, pageable);
            }
        } else {
            if (includeRemoved) {
                acspMemberDaos = acspMembersRepository.findAllByAcspNumber(acspNumber, pageable);
            } else {
                acspMemberDaos = acspMembersRepository.findAllNotRemovedByAcspNumber(acspNumber, pageable);
            }
        }

        return acspMembershipCollectionMappers.daoToDto(acspMemberDaos, null, acspProfile);
    }

    @Transactional(readOnly = true)
    public List<AcspMembersDao> fetchAcspMembershipDaos(final String userId, final boolean includeRemoved) {
        return includeRemoved ? acspMembersRepository.fetchAllAcspMembersByUserId(userId) : acspMembersRepository.fetchActiveAcspMembersByUserId(userId);
    }

    public AcspMembershipsList fetchAcspMemberships(final User user, final boolean includeRemoved) {
        final var acspMembershipDaos = fetchAcspMembershipDaos(user.getUserId(), includeRemoved);
        final var acspMembershipDtos = acspMembershipCollectionMappers.daoToDto(acspMembershipDaos, user, null);
        return new AcspMembershipsList().items(acspMembershipDtos);
    }

    @Transactional(readOnly = true)
    public Optional<AcspMembersDao> fetchMembershipDao(final String membershipId) {
        return acspMembersRepository.findById(membershipId);
    }

    public Optional<AcspMembership> fetchMembership(final String membershipId) {
        return fetchMembershipDao(membershipId).map(dao -> acspMembershipCollectionMappers.daoToDto(dao, null, null));
    }

    @Transactional(readOnly = true)
    public int fetchNumberOfActiveOwners(final String acspNumber) {
        return acspMembersRepository.fetchNumberOfActiveOwners(acspNumber);
    }

    @Transactional(readOnly = true)
    public Optional<AcspMembersDao> fetchActiveAcspMembership(final String userId, final String acspNumber) {
        return acspMembersRepository.fetchActiveAcspMembership(userId, acspNumber);
    }

    @Transactional
    public void updateMembership(final String membershipId, final UserStatusEnum userStatus, final UserRoleEnum userRole, final String requestingUserId) {
        if (Objects.isNull(membershipId)) {
            LOG.errorContext(getXRequestId(), new Exception("membershipId is null"), null);
            throw new IllegalArgumentException("membershipId cannot be null");
        }

        final var update = new Update();
        update.set("etag", generateEtag());

        if (Objects.nonNull(userRole)) {
            update.set("user_role", userRole.getValue());
        }

        if (Objects.nonNull(userStatus)) {
            update.set("status", userStatus.getValue());
            update.set("removed_by", requestingUserId);
            update.set("removed_at", LocalDateTime.now());
        }

        final var numRecordsUpdated = acspMembersRepository.updateAcspMembership(membershipId, update);
        if (numRecordsUpdated == 0) {
            LOG.errorContext(getXRequestId(), new Exception(String.format("Failed to update Acsp Membership with id: %s", membershipId)), null);
            throw new InternalServerErrorRuntimeException(
                    String.format("Failed to update Acsp Membership %s", membershipId));
        }
    }

    @Transactional(readOnly = true)
    public AcspMembershipsList fetchAcspMemberships(final User user, final boolean includeRemoved, final String acspNumber) {

        List<AcspMembersDao> acspMembers;
        if (includeRemoved) {
            acspMembers =
                    acspMembersRepository.fetchAllAcspMembersByUserIdAndAcspNumber(
                            user.getUserId(), acspNumber);
        } else {
            acspMembers =
                    acspMembersRepository.fetchActiveAcspMembersByUserIdAndAcspNumber(
                            user.getUserId(), acspNumber);
        }

        final var acspMembershipsList = new AcspMembershipsList();
        acspMembershipsList.setItems(acspMembershipCollectionMappers.daoToDto(acspMembers, user, null));
        return acspMembershipsList;
    }

    @Transactional
    public AcspMembersDao addAcspMember(final String userId, final String acspNumber, final AcspMembership.UserRoleEnum userRole, final String addedByUserId) {
        final var now = LocalDateTime.now();
        final var newAcspMembersDao = new AcspMembersDao();
        newAcspMembersDao.setUserId(userId);
        newAcspMembersDao.setAcspNumber(acspNumber);
        newAcspMembersDao.setUserRole(userRole.getValue());
        newAcspMembersDao.setCreatedAt(now);
        newAcspMembersDao.setAddedAt(now);
        newAcspMembersDao.setAddedBy(addedByUserId);
        newAcspMembersDao.setEtag(generateEtag());
        newAcspMembersDao.setStatus(AcspMembership.MembershipStatusEnum.ACTIVE.getValue());
        return acspMembersRepository.insert(newAcspMembersDao);
    }

    public AcspMembership addAcspMembership(final User user, final AcspProfile acspProfile, final String acspNumber, final AcspMembership.UserRoleEnum userRole, final String addedByUserId) {
        final var acspMembersDao = addAcspMember(user.getUserId(), acspNumber, userRole, addedByUserId);
        return acspMembershipCollectionMappers.daoToDto(acspMembersDao, user, acspProfile);
    }
}
