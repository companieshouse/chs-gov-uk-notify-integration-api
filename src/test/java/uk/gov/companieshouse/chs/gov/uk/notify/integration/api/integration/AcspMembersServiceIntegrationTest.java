package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.integration;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.TestDataManager;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mapper.AcspMembershipCollectionMappers;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.AcspMembersDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.repositories.AcspMembersRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspMembersService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspProfileService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserStatusEnum;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class AcspMembersServiceIntegrationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AcspMembersService acspMembersService;

    @Autowired
    private AcspMembersRepository acspMembersRepository;

    @MockBean
    private AcspMembershipCollectionMappers acspMembershipCollectionMappers;

    @MockBean
    private UsersService usersService;

    @MockBean
    private AcspProfileService acspProfileService;

    @MockBean
    private EmailProducer emailProducer;

    @MockBean
    private KafkaProducerFactory kafkaProducerFactory;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    @Nested
    class FetchAcspMembershipsTests {

        @Test
        void fetchAcspMembershipsReturnsAcspMembershipsListWithAllAcspMembersIfIncludeRemovedTrue() {
            final var acspMembersDao = testDataManager.fetchAcspMembersDaos("COM002");
            final var acspMembershipDto = testDataManager.fetchAcspMembershipDtos("COM002").getFirst();
            final var user = testDataManager.fetchUserDtos("COMU002").getFirst();

            acspMembersRepository.insert(acspMembersDao);

            Mockito.doReturn(List.of(acspMembershipDto)).when(acspMembershipCollectionMappers).daoToDto(anyList(), eq(user), isNull());

            final var acspMembershipsList = acspMembersService.fetchAcspMemberships(user, true);

            assertEquals(1, acspMembershipsList.getItems().size());
            assertTrue(acspMembershipsList.getItems().stream().anyMatch(m -> m.getAcspNumber().equals("COMA001")));
        }

        @Test
        void
        fetchAcspMembershipsReturnsAcspMembershipsListWithActiveAcspMembersIfIncludeRemovedFalse() {
            final var acspMembersDao = testDataManager.fetchAcspMembersDaos("COM002");
            final var acspMembershipDto = testDataManager.fetchAcspMembershipDtos("COM002").getFirst();
            final var user = testDataManager.fetchUserDtos("COMU002").getFirst();

            acspMembersRepository.insert(acspMembersDao);

            Mockito.doReturn(List.of(acspMembershipDto)).when(acspMembershipCollectionMappers).daoToDto(anyList(), eq(user), isNull());

            final var acspMembershipsList = acspMembersService.fetchAcspMemberships(user, false);

            assertEquals(1, acspMembershipsList.getItems().size());
            assertTrue(acspMembershipsList.getItems().stream().anyMatch(m -> m.getAcspNumber().equals("COMA001")));
        }

        @Test
        void
        fetchAcspMembershipsReturnsAcspMembershipsListWithEmptyListIfNoMembershipsAndIncludeRemovedTrue() {
            final var user = testDataManager.fetchUserDtos("TSU001").getFirst();
            final var acspMembershipsList = acspMembersService.fetchAcspMemberships(user, true);

            assertTrue(acspMembershipsList.getItems().isEmpty());
        }

        @Test
        void
        fetchAcspMembershipsReturnsAcspMembershipsListWithEmptyListIfNoMembershipsAndIncludeRemovedFalse() {
            final var user = testDataManager.fetchUserDtos("TSU001").getFirst();
            final var acspMembershipsList = acspMembersService.fetchAcspMemberships(user, false);
            assertTrue(acspMembershipsList.getItems().isEmpty());
        }
    }

    @Nested
    class FindAllByAcspNumberAndRoleTests {

        @Test
        void findAllByAcspNumberAndRoleReturnsCorrectResultsWithRoleAndIncludeRemovedTrue() {
            final var acspMembersDaos = testDataManager.fetchAcspMembersDaos("COM002", "COM003", "COM004", "COM005", "COM006", "COM007");
            final var acspMembershipDtos = testDataManager.fetchAcspMembershipDtos("COM006", "COM007");
            final var acspProfile = testDataManager.fetchAcspProfiles("COMA001").getFirst();

            acspMembersRepository.saveAll(acspMembersDaos);

            Mockito.doReturn(new AcspMembershipsList().items(acspMembershipDtos)).when(acspMembershipCollectionMappers).daoToDto(any(Page.class), isNull(), any(AcspProfile.class));

            final var acspMembershipsList = acspMembersService.findAllByAcspNumberAndRole("COMA001", acspProfile, "standard", true, 0, 10);

            assertTrue(acspMembershipsList.getItems().stream().allMatch(m -> m.getUserRole() == AcspMembership.UserRoleEnum.STANDARD));
        }

        @Test
        void findAllByAcspNumberAndRoleReturnsCorrectResultsWithRoleAndIncludeRemovedFalse() {
            final var acspMembersDaos = testDataManager.fetchAcspMembersDaos("COM002", "COM003", "COM004", "COM005", "COM006", "COM007");
            final var acspMembershipDtos = testDataManager.fetchAcspMembershipDtos("COM007");
            final var acspProfile = testDataManager.fetchAcspProfiles("COMA001").getFirst();

            acspMembersRepository.saveAll(acspMembersDaos);

            Mockito.doReturn(new AcspMembershipsList().items(acspMembershipDtos)).when(acspMembershipCollectionMappers).daoToDto(any(Page.class), isNull(), any(AcspProfile.class));

            final var acspMembershipsList = acspMembersService.findAllByAcspNumberAndRole("COMA001", acspProfile, "standard", false, 0, 10);

            assertTrue(acspMembershipsList.getItems().stream().allMatch(m -> m.getUserRole() == AcspMembership.UserRoleEnum.STANDARD));
        }

        @Test
        void findAllByAcspNumberAndRoleReturnsCorrectResultsWithoutRoleAndIncludeRemovedTrue() {
            final var acspMembersDaos = testDataManager.fetchAcspMembersDaos("COM002", "COM003", "COM004", "COM005", "COM006", "COM007");
            final var acspMembershipDtos = testDataManager.fetchAcspMembershipDtos("COM002", "COM003", "COM004", "COM005", "COM006", "COM007");
            final var acspProfile = testDataManager.fetchAcspProfiles("COMA001").getFirst();

            acspMembersRepository.saveAll(acspMembersDaos);

            Mockito.doReturn(new AcspMembershipsList().items(acspMembershipDtos)).when(acspMembershipCollectionMappers).daoToDto(any(Page.class), isNull(), any(AcspProfile.class));

            final var acspMembershipsList = acspMembersService.findAllByAcspNumberAndRole("COMA001", acspProfile, null, true, 0, 10);

            assertFalse(acspMembershipsList.getItems().isEmpty());
        }

        @Test
        void findAllByAcspNumberAndRoleReturnsCorrectResultsWithoutRoleAndIncludeRemovedFalse() {
            final var acspMembersDaos = testDataManager.fetchAcspMembersDaos("COM002", "COM003", "COM004", "COM005", "COM006", "COM007");
            final var acspMembershipDtos = testDataManager.fetchAcspMembershipDtos("COM002", "COM004", "COM005", "COM007");
            final var acspProfile = testDataManager.fetchAcspProfiles("COMA001").getFirst();

            acspMembersRepository.saveAll(acspMembersDaos);

            Mockito.doReturn(new AcspMembershipsList().items(acspMembershipDtos)).when(acspMembershipCollectionMappers).daoToDto(any(Page.class), isNull(), any(AcspProfile.class));

            final var acspMembershipsList = acspMembersService.findAllByAcspNumberAndRole("COMA001", acspProfile, null, false, 0, 10);

            assertFalse(acspMembershipsList.getItems().isEmpty());
        }

        @Test
        void findAllByAcspNumberAndRoleReturnsEmptyListForNonExistentAcsp() {
            final var acspProfile = testDataManager.fetchAcspProfiles("COMA001").getFirst();

            Mockito.doReturn(new AcspMembershipsList().items(List.of())).when(acspMembershipCollectionMappers).daoToDto(any(Page.class), isNull(), any(AcspProfile.class));

            final var acspMembershipsList = acspMembersService.findAllByAcspNumberAndRole("NON_EXISTENT", acspProfile, null, true, 0, 10);

            assertTrue(acspMembershipsList.getItems().isEmpty());
        }
    }

    @Nested
    class FetchMembership {

        @Test
        void fetchMembershipWithNullMembershipIdThrowsIllegalArguemntException() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> acspMembersService.fetchMembership(null));
        }

        @Test
        void fetchMembershipWithMalformedOrNonexistentMembershipIdReturnsEmptyOptional() {
            Assertions.assertFalse(acspMembersService.fetchMembership("$$$").isPresent());
        }

        @Test
        void fetchMembershipRetrievesMembership() {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS001"));

            Mockito.doReturn(new AcspMembership().id("TS001")).when(acspMembershipCollectionMappers).daoToDto(any(AcspMembersDao.class), any(), any());

            Mockito.doReturn(testDataManager.fetchUserDtos("TSU001").getFirst()).when(usersService).fetchUserDetails("TSU001");
            Mockito.doReturn(testDataManager.fetchAcspProfiles("TSA001").getFirst()).when(
                    acspProfileService).fetchAcspProfile("TSA001");

            Assertions.assertTrue(acspMembersService.fetchMembership("TS001").isPresent());
        }
    }

    @Nested
    class FetchAcspMembershipsByAcspNumberTests {

        @Test
        void fetchAcspMemberships_WithoutRemovedMemberships_ReturnsEmptyList() {
            final var user = testDataManager.fetchUserDtos("COMU003").getFirst();
            final var acspMembersList = acspMembersService.fetchAcspMemberships(user, false, "COMA001");

            assertTrue(acspMembersList.getItems().isEmpty());
        }

        @Test
        void fetchAcspMemberships_WithRemovedMemberships_ReturnsRemovedMembership() {
            final var acspMembersDaos = testDataManager.fetchAcspMembersDaos("COM002", "COM003", "COM004", "COM005", "COM006", "COM007");
            final var acspMembershipDtos = testDataManager.fetchAcspMembershipDtos("COM003");
            final var user = testDataManager.fetchUserDtos("COMU003").getFirst();

            acspMembersRepository.saveAll(acspMembersDaos);

            Mockito.doReturn(acspMembershipDtos).when(acspMembershipCollectionMappers).daoToDto(anyList(), any(User.class), isNull());

            final var acspMembersList = acspMembersService.fetchAcspMemberships(user, true, "COMA001");
            final var acspMembership = acspMembersList.getItems().getFirst();

            assertEquals("COMA001", acspMembership.getAcspNumber());
            assertEquals(AcspMembership.MembershipStatusEnum.REMOVED, acspMembership.getMembershipStatus());
        }

        @Test
        void fetchAcspMemberships_WithActiveMembership_ReturnsActiveMembership() {
            final var acspMembersDaos = testDataManager.fetchAcspMembersDaos("COM002", "COM003", "COM004", "COM005", "COM006", "COM007");
            final var acspMembershipDtos = testDataManager.fetchAcspMembershipDtos("COM002");
            final var user = testDataManager.fetchUserDtos("COMU002").getFirst();

            acspMembersRepository.saveAll(acspMembersDaos);

            Mockito.doReturn(acspMembershipDtos).when(acspMembershipCollectionMappers).daoToDto(anyList(), any(User.class), isNull());

            final var acspMembershipsList = acspMembersService.fetchAcspMemberships(user, true, "COMA001");
            final var acspMembership = acspMembershipsList.getItems().getFirst();

            assertEquals("COMA001", acspMembership.getAcspNumber());
            assertEquals(AcspMembership.MembershipStatusEnum.ACTIVE, acspMembership.getMembershipStatus());
        }
    }

    @Test
    void fetchMembershipDaoWithNullMembershipIdThrowsIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> acspMembersService.fetchMembershipDao(null));
    }

    @Test
    void fetchMembershipDaoWithMalformedOrNonExistentMembershipIdReturnsEmptyOptional() {
        Assertions.assertFalse(acspMembersService.fetchMembershipDao("£££").isPresent());
        Assertions.assertFalse(acspMembersService.fetchMembershipDao("TS001").isPresent());
    }

    @Test
    void fetchMembershipDaoRetrievesMembership() {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS001"));
        Assertions.assertEquals("TS001", acspMembersService.fetchMembershipDao("TS001").get().getId());
    }

    @Test
    void fetchNumberOfActiveOwnersWithNullOrMalformedOrNonexistentAcspNumberReturnsZero() {
        Assertions.assertEquals(0, acspMembersService.fetchNumberOfActiveOwners(null));
        Assertions.assertEquals(0, acspMembersService.fetchNumberOfActiveOwners("£££"));
        Assertions.assertEquals(0, acspMembersService.fetchNumberOfActiveOwners("TS001"));
    }

    @Test
    void fetchNumberOfActiveOwnersRetrievesNumberOfActiveOwnersAtAcsp() {
        acspMembersRepository.saveAll(testDataManager.fetchAcspMembersDaos("COM002", "COM003", "COM004", "COM005", "COM006", "COM007"));
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS001", "COM001"));
        Assertions.assertEquals(1, acspMembersService.fetchNumberOfActiveOwners("COMA001"));
    }

    @Test
    void
    fetchActiveAcspMembershipWithNullOrMalformedOrNonexistentUserIdOrAcspNumberReturnsEmptyOptional() {
        Assertions.assertFalse(acspMembersService.fetchActiveAcspMembership(null, "TSA001").isPresent());
        Assertions.assertFalse(acspMembersService.fetchActiveAcspMembership("£££", "TSA001").isPresent());
        Assertions.assertFalse(acspMembersService.fetchActiveAcspMembership("TSU001", null).isPresent());
        Assertions.assertFalse(acspMembersService.fetchActiveAcspMembership("TSU001", "£££").isPresent());
        Assertions.assertFalse(acspMembersService.fetchActiveAcspMembership("TSU001", "TSA001").isPresent());
    }

    @Test
    void fetchActiveAcspMembershipAppliedToInactiveMembershipReturnsEmptyOptional() {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS002"));
        Assertions.assertFalse(acspMembersService.fetchActiveAcspMembership("TSU002", "TSA001").isPresent());
    }

    @Test
    void fetchActiveAcspMembershipRetrievesMembership() {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS001"));
        Assertions.assertEquals("TS001", acspMembersService.fetchActiveAcspMembership("TSU001", "TSA001").get().getId());
    }

    @Test
    void updateMembershipWithNullMembershipIdThrowsIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> acspMembersService.updateMembership(null, UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002"));
    }

    @Test
    void
    updateMembershipWithMalformedOrNonexistentMembershipIdThrowsInternalServerErrorRuntimeException() {
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> acspMembersService.updateMembership("£££", UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002"));
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> acspMembersService.updateMembership("TS001", UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002"));
    }

    @Test
    void updateMembershipWithNullUserStatusAndNullUserRoleOnlyUpdatesEtag() {
        final var originalDao = testDataManager.fetchAcspMembersDaos("TS001").getFirst();
        acspMembersRepository.insert(originalDao);

        acspMembersService.updateMembership("TS001", null, null, "TSU002");
        final var updatedDao = acspMembersRepository.findById("TS001").get();

        Assertions.assertNotEquals(originalDao.getEtag(), updatedDao.getEtag());
        Assertions.assertEquals(originalDao.getUserRole(), updatedDao.getUserRole());
        Assertions.assertEquals(originalDao.getStatus(), updatedDao.getStatus());
        Assertions.assertEquals(originalDao.getRemovedAt(), updatedDao.getRemovedAt());
        Assertions.assertEquals(originalDao.getRemovedBy(), updatedDao.getRemovedBy());
    }

    @Test
    void updateMembershipWithNullUserStatusAndNotNullUserRoleOnlyUpdatesEtagAndRole() {
        final var originalDao = testDataManager.fetchAcspMembersDaos("TS001").getFirst();
        acspMembersRepository.insert(originalDao);

        acspMembersService.updateMembership("TS001", null, UserRoleEnum.STANDARD, "TSU002");
        final var updatedDao = acspMembersRepository.findById("TS001").get();

        Assertions.assertNotEquals(originalDao.getEtag(), updatedDao.getEtag());
        Assertions.assertEquals(UserRoleEnum.STANDARD.getValue(), updatedDao.getUserRole());
        Assertions.assertEquals(originalDao.getStatus(), updatedDao.getStatus());
        Assertions.assertEquals(originalDao.getRemovedAt(), updatedDao.getRemovedAt());
        Assertions.assertEquals(originalDao.getRemovedBy(), updatedDao.getRemovedBy());
    }

    @Test
    void
    updateMembershipWithNotNullUserStatusAndNullUserRoleOnlyUpdatesEtagAndStatusAndRemovedAtAndRemovedBy() {
        final var originalDao = testDataManager.fetchAcspMembersDaos("TS001").getFirst();
        acspMembersRepository.insert(originalDao);

        acspMembersService.updateMembership("TS001", UserStatusEnum.REMOVED, null, "TSU002");
        final var updatedDao = acspMembersRepository.findById("TS001").get();

        Assertions.assertNotEquals(originalDao.getEtag(), updatedDao.getEtag());
        Assertions.assertEquals(originalDao.getUserRole(), updatedDao.getUserRole());
        Assertions.assertEquals(UserStatusEnum.REMOVED.getValue(), updatedDao.getStatus());
        Assertions.assertNotEquals(originalDao.getRemovedAt(), updatedDao.getRemovedAt());
        Assertions.assertEquals("TSU002", updatedDao.getRemovedBy());
    }

    @Test
    void updateMembershipWithNotNullUserStatusAndNotNullUserRoleOnlyUpdatesEverything() {
        final var originalDao = testDataManager.fetchAcspMembersDaos("TS001").getFirst();
        acspMembersRepository.insert(originalDao);

        acspMembersService.updateMembership("TS001", UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, "TSU002");
        final var updatedDao = acspMembersRepository.findById("TS001").get();

        Assertions.assertNotEquals(originalDao.getEtag(), updatedDao.getEtag());
        Assertions.assertEquals(UserRoleEnum.STANDARD.getValue(), updatedDao.getUserRole());
        Assertions.assertEquals(UserStatusEnum.REMOVED.getValue(), updatedDao.getStatus());
        Assertions.assertNotEquals(originalDao.getRemovedAt(), updatedDao.getRemovedAt());
        Assertions.assertEquals("TSU002", updatedDao.getRemovedBy());
    }

    @Test
    void updateMembershipWithNullRequestingUserIdSetsRemovedByToNull() {
        final var originalDao = testDataManager.fetchAcspMembersDaos("TS001").getFirst();
        acspMembersRepository.insert(originalDao);

        acspMembersService.updateMembership("TS001", UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, null);
        final var updatedDao = acspMembersRepository.findById("TS001").get();

        Assertions.assertNotEquals(originalDao.getEtag(), updatedDao.getEtag());
        Assertions.assertEquals(UserRoleEnum.STANDARD.getValue(), updatedDao.getUserRole());
        Assertions.assertEquals(UserStatusEnum.REMOVED.getValue(), updatedDao.getStatus());
        Assertions.assertNotEquals(originalDao.getRemovedAt(), updatedDao.getRemovedAt());
        Assertions.assertNull(updatedDao.getRemovedBy());
    }

    @Nested
    class AddAcspMember {

        @Test
        void addAcspMemberReturnsAddedAcspMembersDao() {
            final var user = testDataManager.fetchUserDtos("TSU001").getFirst();
            final var userRole = AcspMembership.UserRoleEnum.ADMIN;

            final var result = acspMembersService.addAcspMember(user.getUserId(), "TS001", userRole, "COMU001");

            assertEquals(user.getUserId(), result.getUserId());
            assertEquals("TS001", result.getAcspNumber());
            assertEquals(userRole.getValue(), result.getUserRole());
            assertEquals("COMU001", result.getAddedBy());
            assertEquals(AcspMembership.MembershipStatusEnum.ACTIVE.getValue(), result.getStatus());
            assertFalse(result.getEtag().isEmpty());
        }
    }

    @Nested
    class AddAcspMembership {

        @Test
        void addAcspMembershipReturnsAddedAcspMembership() {
            final var acspMembershipDto = testDataManager.fetchAcspMembershipDtos("TS001").getFirst();
            final var userDto = testDataManager.fetchUserDtos("TSU001").getFirst();
            final var acspProfile = testDataManager.fetchAcspProfiles("TSA001").getFirst();

            Mockito.doReturn(acspMembershipDto).when(acspMembershipCollectionMappers).daoToDto(any(AcspMembersDao.class), any(), any());

            final var acspMembership = acspMembersService.addAcspMembership(userDto, acspProfile, "TSA001", AcspMembership.UserRoleEnum.OWNER, null);

            assertEquals(userDto.getUserId(), acspMembership.getUserId());
            assertEquals(userDto.getEmail(), acspMembership.getUserEmail());
            assertEquals(DEFAULT_DISPLAY_NAME, acspMembership.getUserDisplayName());
            assertEquals(AcspMembership.UserRoleEnum.OWNER, acspMembership.getUserRole());
            assertNull(acspMembership.getAddedBy());
            assertEquals("TSA001", acspMembership.getAcspNumber());
        }
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AcspMembersDao.class);
    }
}
