package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.TestDataManager;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspProfileService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.UsersService;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;

import java.time.LocalDateTime;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.DateUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.DateUtils.reduceTimestampResolution;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AcspMembershipMapperTest {

    @Mock
    private UsersService usersService;

    @Mock
    private AcspProfileService acspProfileService;

    private AcspMembershipMapper acspMembershipMapper;

    private final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    private static final String DEFAULT_KIND = "acsp-membership";

    @BeforeEach
    void setup() {
        acspMembershipMapper = new AcspMembershipMapperImpl();
        acspMembershipMapper.usersService = usersService;
        acspMembershipMapper.acspProfileService = acspProfileService;
    }

    @Test
    void localDateTimeToOffsetDateTimeWithNullReturnsNull() {
        Assertions.assertNull(acspMembershipMapper.localDateTimeToOffsetDateTime(null));
    }

    @Test
    void localDateTimeToOffsetDateTimeReturnsCorrectTimestamp() {
        final var inputDate = LocalDateTime.now();
        final var outputDate = acspMembershipMapper.localDateTimeToOffsetDateTime(inputDate);
        Assertions.assertEquals(localDateTimeToNormalisedString(inputDate), reduceTimestampResolution(outputDate.toString()));
    }

    @Test
    void enrichWithUserDetailsEnrichesEmailAndDisplayName() {
        final var acspMembership = new AcspMembership().userId("TSU002");
        final var userDetails = testDataManager.fetchUserDtos("TSU002").getFirst();
        acspMembershipMapper.enrichWithUserDetails(acspMembership, userDetails);
        Assertions.assertEquals("woody@toystory.com", acspMembership.getUserEmail());
        Assertions.assertEquals("Woody", acspMembership.getUserDisplayName());
    }

    @Test
    void enrichWithUserDetailsEnrichesEmailAndDefaultDisplayName() {
        final var acspMembership = new AcspMembership().userId("TSU001");
        final var userDetails = testDataManager.fetchUserDtos("TSU001").getFirst();
        acspMembershipMapper.enrichWithUserDetails(acspMembership, userDetails);
        Assertions.assertEquals("buzz.lightyear@toystory.com", acspMembership.getUserEmail());
        Assertions.assertEquals(DEFAULT_DISPLAY_NAME, acspMembership.getUserDisplayName());
    }

    @Test
    void enrichWithUserDetailsWithoutUserDetailsRetrievesEmailAndDisplayName() {
        final var acspMembership = new AcspMembership().userId("TSU002");
        final var userDetails = testDataManager.fetchUserDtos("TSU002").getFirst();

        Mockito.doReturn(userDetails).when(usersService).fetchUserDetails("TSU002");

        acspMembershipMapper.enrichWithUserDetails(acspMembership, null);

        Assertions.assertEquals("woody@toystory.com", acspMembership.getUserEmail());
        Assertions.assertEquals("Woody", acspMembership.getUserDisplayName());
    }

    @Test
    void enrichWithAcspProfileEnrichesWithNameAndStatus() {
        final var acspMembership = new AcspMembership().userId("TSU002");
        final var acspProfile = testDataManager.fetchAcspProfiles("TSA001").getFirst();

        acspMembershipMapper.enrichWithAcspProfile(acspMembership, acspProfile);

        Assertions.assertEquals("Toy Story", acspMembership.getAcspName());
        Assertions.assertEquals(AcspStatusEnum.ACTIVE, acspMembership.getAcspStatus());
    }

    @Test
    void enrichWithAcspProfileWithoutAcspProfileRetrievesNameAndStatus() {
        final var acspMembership = new AcspMembership().acspNumber("TSA001");
        final var acspProfile = testDataManager.fetchAcspProfiles("TSA001").getFirst();

        Mockito.doReturn(acspProfile).when(acspProfileService).fetchAcspProfile("TSA001");

        acspMembershipMapper.enrichWithAcspProfile(acspMembership, null);

        Assertions.assertEquals("Toy Story", acspMembership.getAcspName());
        Assertions.assertEquals(AcspStatusEnum.ACTIVE, acspMembership.getAcspStatus());
    }

    @Test
    void daoToDtoWithNullInputReturnsNull() {
        Assertions.assertNull(acspMembershipMapper.daoToDto(null, null, null));
    }

    @Test
    void daoToDtoAppliedToPartialDaoSuccessfullyMapsToDto() {
        final var dao = testDataManager.fetchAcspMembersDaos("TS001").getFirst();
        final var acspProfile = testDataManager.fetchAcspProfiles("TSA001").getFirst();
        final var userData = testDataManager.fetchUserDtos("TSU001").getFirst();

        Mockito.doReturn(acspProfile).when(acspProfileService).fetchAcspProfile("TSA001");
        Mockito.doReturn(userData).when(usersService).fetchUserDetails("TSU001");

        final var dto = acspMembershipMapper.daoToDto(dao, null, null);

        Assertions.assertEquals(dao.getEtag(), dto.getEtag());
        Assertions.assertEquals("TS001", dto.getId());
        Assertions.assertEquals("TSU001", dto.getUserId());
        Assertions.assertEquals(DEFAULT_DISPLAY_NAME, dto.getUserDisplayName());
        Assertions.assertEquals("buzz.lightyear@toystory.com", dto.getUserEmail());
        Assertions.assertEquals(UserRoleEnum.OWNER, dto.getUserRole());
        Assertions.assertEquals("TSA001", dto.getAcspNumber());
        Assertions.assertEquals("Toy Story", dto.getAcspName());
        Assertions.assertEquals("active", dto.getAcspStatus().getValue());
        Assertions.assertEquals(localDateTimeToNormalisedString(dao.getAddedAt()), reduceTimestampResolution(dto.getAddedAt().toString()));
        Assertions.assertNull(dto.getAddedBy());
        Assertions.assertNull(dto.getRemovedBy());
        Assertions.assertNull(dto.getRemovedAt());
        Assertions.assertEquals(MembershipStatusEnum.ACTIVE, dto.getMembershipStatus());
        Assertions.assertEquals(DEFAULT_KIND, dto.getKind());
    }

    @Test
    void daoToDtoAppliedToCompleteDaoSuccessfullyMapsToDto() {
        final var dao = testDataManager.fetchAcspMembersDaos("TS002").getFirst();
        final var acspProfile = testDataManager.fetchAcspProfiles("TSA001").getFirst();
        final var userData = testDataManager.fetchUserDtos("TSU002").getFirst();

        Mockito.doReturn(acspProfile).when(acspProfileService).fetchAcspProfile("TSA001");
        Mockito.doReturn(userData).when(usersService).fetchUserDetails("TSU002");

        final var dto = acspMembershipMapper.daoToDto(dao, null, null);

        Assertions.assertEquals(dao.getEtag(), dto.getEtag());
        Assertions.assertEquals("TS002", dto.getId());
        Assertions.assertEquals("TSU002", dto.getUserId());
        Assertions.assertEquals("Woody", dto.getUserDisplayName());
        Assertions.assertEquals("woody@toystory.com", dto.getUserEmail());
        Assertions.assertEquals(UserRoleEnum.ADMIN, dto.getUserRole());
        Assertions.assertEquals("TSA001", dto.getAcspNumber());
        Assertions.assertEquals("Toy Story", dto.getAcspName());
        Assertions.assertEquals("active", dto.getAcspStatus().getValue());
        Assertions.assertEquals(localDateTimeToNormalisedString(dao.getAddedAt()), reduceTimestampResolution(dto.getAddedAt().toString()));
        Assertions.assertEquals("TSU001", dto.getAddedBy());
        Assertions.assertEquals("TSU001", dto.getRemovedBy());
        Assertions.assertEquals(localDateTimeToNormalisedString(dao.getRemovedAt()), reduceTimestampResolution(dto.getRemovedAt().toString()));
        Assertions.assertEquals(MembershipStatusEnum.REMOVED, dto.getMembershipStatus());
        Assertions.assertEquals(DEFAULT_KIND, dto.getKind());
    }

    @Test
    void daoToDtoWithThreeParametersAppliedToPartialDaoSuccessfullyMapsToDto() {
        final var dao = testDataManager.fetchAcspMembersDaos("TS001").getFirst();
        final var acspProfile = testDataManager.fetchAcspProfiles("TSA001").getFirst();
        final var userData = testDataManager.fetchUserDtos("TSU001").getFirst();

        final var dto = acspMembershipMapper.daoToDto(dao, userData, acspProfile);

        Assertions.assertEquals(dao.getEtag(), dto.getEtag());
        Assertions.assertEquals("TS001", dto.getId());
        Assertions.assertEquals(userData.getUserId(), dto.getUserId());
        Assertions.assertEquals(DEFAULT_DISPLAY_NAME, dto.getUserDisplayName());
        Assertions.assertEquals(userData.getEmail(), dto.getUserEmail());
        Assertions.assertEquals(UserRoleEnum.OWNER, dto.getUserRole());
        Assertions.assertEquals("TSA001", dto.getAcspNumber());
        Assertions.assertEquals(acspProfile.getName(), dto.getAcspName());
        Assertions.assertEquals(acspProfile.getStatus().getValue(), dto.getAcspStatus().getValue());
        Assertions.assertEquals(localDateTimeToNormalisedString(dao.getAddedAt()), reduceTimestampResolution(dto.getAddedAt().toString()));
        Assertions.assertNull(dto.getAddedBy());
        Assertions.assertNull(dto.getRemovedBy());
        Assertions.assertNull(dto.getRemovedAt());
        Assertions.assertEquals(MembershipStatusEnum.ACTIVE, dto.getMembershipStatus());
        Assertions.assertEquals(DEFAULT_KIND, dto.getKind());
    }

    @Test
    void daoToDtoWithThreeParametersAppliedToCompleteDaoSuccessfullyMapsToDto() {
        final var dao = testDataManager.fetchAcspMembersDaos("TS002").getFirst();
        final var acspProfile = testDataManager.fetchAcspProfiles("TSA001").getFirst();
        final var userData = testDataManager.fetchUserDtos("TSU002").getFirst();

        final var dto = acspMembershipMapper.daoToDto(dao, userData, acspProfile);

        Assertions.assertEquals(dao.getEtag(), dto.getEtag());
        Assertions.assertEquals("TS002", dto.getId());
        Assertions.assertEquals(userData.getUserId(), dto.getUserId());
        Assertions.assertEquals(userData.getDisplayName(), dto.getUserDisplayName());
        Assertions.assertEquals(userData.getEmail(), dto.getUserEmail());
        Assertions.assertEquals(UserRoleEnum.ADMIN, dto.getUserRole());
        Assertions.assertEquals("TSA001", dto.getAcspNumber());
        Assertions.assertEquals(acspProfile.getName(), dto.getAcspName());
        Assertions.assertEquals(acspProfile.getStatus().getValue(), dto.getAcspStatus().getValue());
        Assertions.assertEquals(localDateTimeToNormalisedString(dao.getAddedAt()), reduceTimestampResolution(dto.getAddedAt().toString()));
        Assertions.assertEquals("TSU001", dto.getAddedBy());
        Assertions.assertEquals("TSU001", dto.getRemovedBy());
        Assertions.assertEquals(localDateTimeToNormalisedString(dao.getRemovedAt()), reduceTimestampResolution(dto.getRemovedAt().toString()));
        Assertions.assertEquals(MembershipStatusEnum.REMOVED, dto.getMembershipStatus());
        Assertions.assertEquals(DEFAULT_KIND, dto.getKind());
    }

}
