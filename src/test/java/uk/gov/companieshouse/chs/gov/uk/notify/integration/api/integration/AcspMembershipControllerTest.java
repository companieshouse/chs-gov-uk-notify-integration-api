package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.integration;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.TestDataManager;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.AcspMembersDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email.YourRoleAtAcspHasChangedToAdminEmailData;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email.YourRoleAtAcspHasChangedToOwnerEmailData;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email.YourRoleAtAcspHasChangedToStandardEmailData;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.repositories.AcspMembersRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspProfileService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.UsersService;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.MembershipStatusEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserStatusEnum;
import uk.gov.companieshouse.api.acspprofile.Status;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.DateUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.DateUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.MessageType.*;

@AutoConfigureMockMvc
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class AcspMembershipControllerTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsersService usersService;

    @MockBean
    private AcspProfileService acspProfileService;

    @Autowired
    private AcspMembersRepository acspMembersRepository;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @MockBean
    private EmailProducer emailProducer;

    @MockBean
    private KafkaProducerFactory kafkaProducerFactory;

    @Value("${signin.url}")
    private String signinUrl;

    private CountDownLatch latch;

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    private static final String DEFAULT_KIND = "acsp-membership";

    private void setEmailProducerCountDownLatch(int countdown) {
        latch = new CountDownLatch(countdown);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(emailProducer).sendEmail(any(), any());
    }

    private void mockFetchUserDetailsFor(final String... userIds) {
        Arrays.stream(userIds).forEach(userId -> Mockito.doReturn(testDataManager.fetchUserDtos(userId).getFirst()).when(usersService).fetchUserDetails(userId));
    }

    @Test
    void getAcspMembershipForAcspAndIdWithoutXRequestIdReturnsBadRequest() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("WIT004"));

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ");

        mockMvc.perform(get("/acsps/memberships/TS001")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAcspMembershipForAcspAndIdWithMalformedMembershipIdReturnsBadRequest() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("WIT004"));

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ");

        mockMvc.perform(get("/acsps/memberships/$$$")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAcspMembershipForAcspAndIdWithNonexistentMembershipIdReturnsNotFound() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("WIT004"));

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ");

        mockMvc.perform(get("/acsps/memberships/TS001")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAcspMembershipForAcspAndIdRetrievesAcspMembership() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("WIT004"));

        final var dao = testDataManager.fetchAcspMembersDaos("TS001").getFirst();
        acspMembersRepository.insert(dao);

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(testDataManager.fetchUserDtos("TSU001").getFirst()).when(usersService).fetchUserDetails("TSU001");
        Mockito.doReturn(testDataManager.fetchAcspProfiles("TSA001").getFirst()).when(
                acspProfileService).fetchAcspProfile("TSA001");

        final var response =
                mockMvc.perform(get("/acsps/memberships/TS001")
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*")
                                .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004")))
                        .andExpect(status().isOk());

        final var acspMembership = parseResponseTo(response, AcspMembership.class);

        Assertions.assertEquals(dao.getEtag(), acspMembership.getEtag());
        Assertions.assertEquals("TS001", acspMembership.getId());
        Assertions.assertEquals("TSU001", acspMembership.getUserId());
        Assertions.assertEquals(DEFAULT_DISPLAY_NAME, acspMembership.getUserDisplayName());
        Assertions.assertEquals("buzz.lightyear@toystory.com", acspMembership.getUserEmail());
        Assertions.assertEquals(UserRoleEnum.OWNER.getValue(), acspMembership.getUserRole().getValue());
        Assertions.assertEquals("TSA001", acspMembership.getAcspNumber());
        Assertions.assertEquals("Toy Story", acspMembership.getAcspName());
        Assertions.assertEquals("active", acspMembership.getAcspStatus().getValue());
        Assertions.assertEquals(localDateTimeToNormalisedString(dao.getAddedAt()), reduceTimestampResolution(acspMembership.getAddedAt().toString()));
        Assertions.assertNull(acspMembership.getAddedBy());
        Assertions.assertNull(acspMembership.getRemovedBy());
        Assertions.assertNull(acspMembership.getRemovedAt());
        Assertions.assertEquals(MembershipStatusEnum.ACTIVE, acspMembership.getMembershipStatus());
        Assertions.assertEquals(DEFAULT_KIND, acspMembership.getKind());
    }

    @Test
    void getAcspMembershipForAcspAndIdWithApiKeySucceeds() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("WIT004", "TS001"));

        Mockito.doReturn(testDataManager.fetchUserDtos("TSU001").getFirst()).when(usersService).fetchUserDetails("TSU001");
        Mockito.doReturn(testDataManager.fetchAcspProfiles("TSA001").getFirst()).when(
                acspProfileService).fetchAcspProfile("TSA001");

        mockMvc.perform(get("/acsps/memberships/TS001")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004")))
                .andExpect(status().isOk());
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithNullXRequestIdThrowsBadRequest() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("WIT004"));

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ");

        mockMvc.perform(patch("/acsps/memberships/WIT001")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_status\":\"removed\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithMalformedMembershipIdThrowsBadRequest() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("WIT004"));

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ");

        mockMvc.perform(patch("/acsps/memberships/£££")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_status\":\"removed\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithNonexistentMembershipIdReturnsNotFound() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("WIT004"));

        Mockito.doReturn(testDataManager.fetchUserDtos("67ZeMsvAEgkBWs7tNKacdrPvOmQ").getFirst()).when(usersService).fetchUserDetails("67ZeMsvAEgkBWs7tNKacdrPvOmQ");

        mockMvc.perform(patch("/acsps/memberships/WIT001")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_status\":\"removed\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithoutRequestBodyReturnsBadRequest() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("WIT004"));

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ");

        mockMvc.perform(patch("/acsps/memberships/WIT001")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004"))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }


    static Stream<Arguments> updateAcspMembershipForAcspAndIdWithMalformedBodyTestData() {
        return Stream.of(
                Arguments.of("{}"),
                Arguments.of("{\"user_status\":\"complicated\"}"),
                Arguments.of("{\"user_role\":\"jester\"}")
        );
    }

    @ParameterizedTest
    @MethodSource("updateAcspMembershipForAcspAndIdWithMalformedBodyTestData")
    void updateAcspMembershipForAcspAndIdWithEmptyRequestBodyReturnsBadRequest(final String requestBody) throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("WIT004"));

        Mockito.doReturn(testDataManager.fetchUserDtos("67ZeMsvAEgkBWs7tNKacdrPvOmQ").getFirst()).when(usersService).fetchUserDetails("67ZeMsvAEgkBWs7tNKacdrPvOmQ");

        mockMvc.perform(patch("/acsps/memberships/WIT001")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }


    @Test
    void updateAcspMembershipForAcspAndIdWithOAuth2ReturnsBadRequestWhenAttemptingToRemoveLastOwner() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("WIT004"));

        Mockito.doReturn(testDataManager.fetchUserDtos("67ZeMsvAEgkBWs7tNKacdrPvOmQ").getFirst()).when(usersService).fetchUserDetails("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(testDataManager.fetchAcspProfiles("WITA001").getFirst()).when(acspProfileService).fetchAcspProfile("WITA001");

        mockMvc.perform(patch("/acsps/memberships/WIT004")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_status\":\"removed\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithApiKeyAndActiveAcspReturnsBadRequestWhenAttemptingToRemoveLastOwner() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("WIT004"));

        Mockito.doReturn(testDataManager.fetchUserDtos("67ZeMsvAEgkBWs7tNKacdrPvOmQ").getFirst()).when(usersService).fetchUserDetails("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(testDataManager.fetchAcspProfiles("WITA001").getFirst()).when(acspProfileService).fetchAcspProfile("WITA001");

        mockMvc.perform(patch("/acsps/memberships/WIT004")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_status\":\"removed\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithApiKeyAndCeasedAcspSucceedsWhenAttemptingToRemoveLastOwner() throws Exception {
        final var acspProfile = testDataManager.fetchAcspProfiles("WITA001").getFirst();
        acspProfile.setStatus(Status.CEASED);

        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("WIT004"));

        Mockito.doReturn(testDataManager.fetchUserDtos("67ZeMsvAEgkBWs7tNKacdrPvOmQ").getFirst()).when(usersService).fetchUserDetails("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(acspProfile).when(acspProfileService).fetchAcspProfile("WITA001");

        mockMvc.perform(patch("/acsps/memberships/WIT004")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_status\":\"removed\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithInactiveCallerReturnsForbidden() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM001", "COM004"));

        Mockito.doReturn(testDataManager.fetchUserDtos("COMU001").getFirst()).when(usersService).fetchUserDetails("COMU001");

        mockMvc.perform(patch("/acsps/memberships/COM004")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU001")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM001"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_status\":\"removed\"}"))
                .andExpect(status().isForbidden());
    }

    private static Stream<Arguments> membershipRemovalSuccessScenarios() {
        return Stream.of(
                Arguments.of("WIT004", "WIT001", testDataManager.fetchTokenPermissions("WIT004")),
                Arguments.of("WIT004", "WIT002", testDataManager.fetchTokenPermissions("WIT004")),
                Arguments.of("WIT004", "WIT003", testDataManager.fetchTokenPermissions("WIT004")),
                Arguments.of("NEI004", "NEI002", testDataManager.fetchTokenPermissions("NEI004")),
                Arguments.of("NEI004", "NEI003", testDataManager.fetchTokenPermissions("NEI004"))
        );
    }

    @ParameterizedTest
    @MethodSource("membershipRemovalSuccessScenarios")
    void updateAcspMembershipForAcspAndIdWithPrivilegedCallerSuccessfullyRemovesMembership(final String requestingUserMembershipId, final String targetUserMembershipId, final String tokenPermissions) throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos(requestingUserMembershipId, targetUserMembershipId);
        final var originalDao = acspMembersDaos.getLast();
        final var requestUserId = acspMembersDaos.getFirst().getUserId();

        acspMembersRepository.insert(acspMembersDaos);
        Mockito.doReturn(testDataManager.fetchUserDtos(requestUserId).getFirst()).when(usersService).fetchUserDetails(requestUserId);

        mockMvc.perform(patch(String.format("/acsps/memberships/%s", targetUserMembershipId))
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", requestUserId)
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", tokenPermissions)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_status\":\"removed\"}"))
                .andExpect(status().isOk());

        final var updatedDao = acspMembersRepository.findById(targetUserMembershipId).get();
        Assertions.assertNotEquals(originalDao.getEtag(), updatedDao.getEtag());
        Assertions.assertEquals(originalDao.getUserRole(), updatedDao.getUserRole());
        Assertions.assertEquals(UserStatusEnum.REMOVED.getValue(), updatedDao.getStatus());
        Assertions.assertNotEquals(originalDao.getRemovedAt(), updatedDao.getRemovedAt());
        Assertions.assertEquals(requestUserId, updatedDao.getRemovedBy());
    }

    private static Stream<Arguments> membershipRemovalFailureScenarios() {
        return Stream.of(
                Arguments.of("NEI004", "NEI001", testDataManager.fetchTokenPermissions("NEI004")),
                Arguments.of("XME004", "XME001", testDataManager.fetchTokenPermissions("XME004")),
                Arguments.of("XME004", "XME002", testDataManager.fetchTokenPermissions("XME004")),
                Arguments.of("XME004", "XME003", testDataManager.fetchTokenPermissions("XME004"))
        );
    }

    @ParameterizedTest
    @MethodSource("membershipRemovalFailureScenarios")
    void updateAcspMembershipForAcspAndIdWithUnprivilegedCallerReturnsBadRequestWhenAttemptingToRemoveMembership(final String requestingUserMembershipId, final String targetUserMembershipId, final String tokenPermissions) throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos(requestingUserMembershipId, targetUserMembershipId);
        final var requestUserId = acspMembersDaos.getFirst().getUserId();

        acspMembersRepository.insert(acspMembersDaos);
        Mockito.doReturn(testDataManager.fetchUserDtos(requestUserId).getFirst()).when(usersService).fetchUserDetails(requestUserId);

        final var targetAcspNumber = acspMembersDaos.getLast().getAcspNumber();
        Mockito.doReturn(testDataManager.fetchAcspProfiles(targetAcspNumber).getFirst()).when(acspProfileService).fetchAcspProfile(targetAcspNumber);

        mockMvc.perform(patch(String.format("/acsps/memberships/%s", targetUserMembershipId))
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", requestUserId)
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", tokenPermissions)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_status\":\"removed\"}"))
                .andExpect(status().isBadRequest());
    }

    private static Stream<Arguments> membershipUpdateRoleSuccessScenarios() {
        return Stream.of(
                Arguments.of("WIT004", "WIT001", "owner", testDataManager.fetchTokenPermissions("WIT004")),
                Arguments.of("WIT004", "WIT002", "owner", testDataManager.fetchTokenPermissions("WIT004")),
                Arguments.of("WIT004", "WIT003", "owner", testDataManager.fetchTokenPermissions("WIT004")),
                Arguments.of("WIT004", "WIT001", "admin", testDataManager.fetchTokenPermissions("WIT004")),
                Arguments.of("WIT004", "WIT001", "standard", testDataManager.fetchTokenPermissions("WIT004")),
                Arguments.of("WIT004", "WIT002", "admin", testDataManager.fetchTokenPermissions("WIT004")),
                Arguments.of("WIT004", "WIT002", "standard", testDataManager.fetchTokenPermissions("WIT004")),
                Arguments.of("WIT004", "WIT003", "admin", testDataManager.fetchTokenPermissions("WIT004")),
                Arguments.of("WIT004", "WIT003", "standard", testDataManager.fetchTokenPermissions("WIT004")),
                Arguments.of("NEI004", "NEI002", "admin", testDataManager.fetchTokenPermissions("NEI004")),
                Arguments.of("NEI004", "NEI002", "standard", testDataManager.fetchTokenPermissions("NEI004")),
                Arguments.of("NEI004", "NEI003", "admin", testDataManager.fetchTokenPermissions("NEI004")),
                Arguments.of("NEI004", "NEI003", "standard", testDataManager.fetchTokenPermissions("NEI004"))
        );
    }

    @ParameterizedTest
    @MethodSource("membershipUpdateRoleSuccessScenarios")
    void updateAcspMembershipForAcspAndIdWithPrivilegedCallerSuccessfullyUpdatesMembership(final String requestingUserMembershipId, final String targetUserMembershipId, final String userRole, final String tokenPermissions) throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos(requestingUserMembershipId, targetUserMembershipId);
        final var originalDao = acspMembersDaos.getLast();
        final var requestUserId = acspMembersDaos.getFirst().getUserId();
        final var requestingUser = testDataManager.fetchUserDtos(requestUserId).getFirst();
        final var targetUser = testDataManager.fetchUserDtos(originalDao.getUserId()).getFirst();
        final var acsp = testDataManager.fetchAcspProfiles(originalDao.getAcspNumber()).getFirst();

        acspMembersRepository.insert(acspMembersDaos);
        Mockito.doReturn(testDataManager.fetchUserDtos(requestUserId).getFirst()).when(usersService).fetchUserDetails(requestUserId);
        Mockito.doReturn(targetUser).when(usersService).fetchUserDetails(targetUser.getUserId());
        Mockito.doReturn(acsp).when(acspProfileService).fetchAcspProfile(acsp.getNumber());

        setEmailProducerCountDownLatch(1);

        mockMvc.perform(patch(String.format("/acsps/memberships/%s", targetUserMembershipId))
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", requestUserId)
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", tokenPermissions)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"user_role\":\"%s\"}", userRole)))
                .andExpect(status().isOk());

        final var updatedDao = acspMembersRepository.findById(targetUserMembershipId).get();
        Assertions.assertNotEquals(originalDao.getEtag(), updatedDao.getEtag());
        Assertions.assertEquals(userRole, updatedDao.getUserRole());
        Assertions.assertEquals(originalDao.getStatus(), updatedDao.getStatus());
        Assertions.assertEquals(originalDao.getRemovedAt(), updatedDao.getRemovedAt());
        Assertions.assertEquals(originalDao.getRemovedBy(), updatedDao.getRemovedBy());

        latch.await(10, TimeUnit.SECONDS);

        final var requestingUserDisplayName = Optional.ofNullable(requestingUser.getDisplayName()).orElse(requestingUser.getEmail());
        if (UserRoleEnum.OWNER.getValue().equals(userRole)) {
            Mockito.verify(emailProducer).sendEmail(new YourRoleAtAcspHasChangedToOwnerEmailData(targetUser.getEmail(), requestingUserDisplayName, acsp.getName(), signinUrl), YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE.getValue());
        } else if (UserRoleEnum.ADMIN.getValue().equals(userRole)) {
            Mockito.verify(emailProducer).sendEmail(new YourRoleAtAcspHasChangedToAdminEmailData(targetUser.getEmail(), requestingUserDisplayName, acsp.getName(), signinUrl), YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE.getValue());
        } else {
            Mockito.verify(emailProducer).sendEmail(new YourRoleAtAcspHasChangedToStandardEmailData(targetUser.getEmail(), requestingUserDisplayName, acsp.getName(), signinUrl), YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE.getValue());
        }
    }

    private static Stream<Arguments> membershipUpdateRoleFailureScenarios() {
        return Stream.of(
                Arguments.of("NEI004", "NEI001", "owner", testDataManager.fetchTokenPermissions("NEI004")),
                Arguments.of("NEI004", "NEI002", "owner", testDataManager.fetchTokenPermissions("NEI004")),
                Arguments.of("NEI004", "NEI003", "owner", testDataManager.fetchTokenPermissions("NEI004")),
                Arguments.of("NEI004", "NEI001", "admin", testDataManager.fetchTokenPermissions("NEI004")),
                Arguments.of("NEI004", "NEI001", "standard", testDataManager.fetchTokenPermissions("NEI004")),
                Arguments.of("XME004", "XME001", "owner", testDataManager.fetchTokenPermissions("XME004")),
                Arguments.of("XME004", "XME001", "admin", testDataManager.fetchTokenPermissions("XME004")),
                Arguments.of("XME004", "XME001", "standard", testDataManager.fetchTokenPermissions("XME004")),
                Arguments.of("XME004", "XME002", "owner", testDataManager.fetchTokenPermissions("XME004")),
                Arguments.of("XME004", "XME002", "admin", testDataManager.fetchTokenPermissions("XME004")),
                Arguments.of("XME004", "XME002", "standard", testDataManager.fetchTokenPermissions("XME004")),
                Arguments.of("XME004", "XME003", "owner", testDataManager.fetchTokenPermissions("XME004")),
                Arguments.of("XME004", "XME003", "admin", testDataManager.fetchTokenPermissions("XME004")),
                Arguments.of("XME004", "XME003", "standard", testDataManager.fetchTokenPermissions("XME004"))
        );
    }

    @ParameterizedTest
    @MethodSource("membershipUpdateRoleFailureScenarios")
    void updateAcspMembershipForAcspAndIdWithUnprivilegedCallerReturnsBadRequestWhenAttemptingToUpdateRole(final String requestingUserMembershipId, final String targetUserMembershipId, final String userRole, final String tokenPermissions) throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos(requestingUserMembershipId, targetUserMembershipId);
        final var requestUserId = acspMembersDaos.getFirst().getUserId();

        acspMembersRepository.insert(acspMembersDaos);
        Mockito.doReturn(testDataManager.fetchUserDtos(requestUserId).getFirst()).when(usersService).fetchUserDetails(requestUserId);

        final var targetAcspNumber = acspMembersDaos.getLast().getAcspNumber();
        Mockito.doReturn(testDataManager.fetchAcspProfiles(targetAcspNumber).getFirst()).when(acspProfileService).fetchAcspProfile(targetAcspNumber);


        mockMvc.perform(patch(String.format("/acsps/memberships/%s", targetUserMembershipId))
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", requestUserId)
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", tokenPermissions)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"user_role\":\"%s\"}", userRole)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAcspMembershipForAcspAndIdCanUpdateUserRoleAndUserStatusAtTheSameTime() throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos("WIT004", "WIT002");
        final var originalDao = acspMembersDaos.getLast();

        acspMembersRepository.insert(acspMembersDaos);
        Mockito.doReturn(testDataManager.fetchUserDtos("67ZeMsvAEgkBWs7tNKacdrPvOmQ").getFirst()).when(usersService).fetchUserDetails("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(testDataManager.fetchUserDtos("WITU002").getFirst()).when(usersService).fetchUserDetails("WITU002");
        Mockito.doReturn(testDataManager.fetchAcspProfiles("WITA001").getFirst()).when(acspProfileService).fetchAcspProfile("WITA001");

        mockMvc.perform(patch("/acsps/memberships/WIT002")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_role\":\"standard\",\"user_status\":\"removed\"}"))
                .andExpect(status().isOk());

        final var updatedDao = acspMembersRepository.findById("WIT002").get();
        Assertions.assertNotEquals(originalDao.getEtag(), updatedDao.getEtag());
        Assertions.assertEquals(UserRoleEnum.STANDARD.getValue(), updatedDao.getUserRole());
        Assertions.assertEquals(UserStatusEnum.REMOVED.getValue(), updatedDao.getStatus());
        Assertions.assertNotEquals(originalDao.getRemovedAt(), updatedDao.getRemovedAt());
        Assertions.assertEquals("67ZeMsvAEgkBWs7tNKacdrPvOmQ", updatedDao.getRemovedBy());
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithApiKeyByPassesOAuth2Checks() throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos("COM001", "COM004");

        acspMembersRepository.insert(acspMembersDaos);
        Mockito.doReturn(testDataManager.fetchUserDtos("COMU001").getFirst()).when(usersService).fetchUserDetails("COMU001");

        mockMvc.perform(patch("/acsps/memberships/COM004")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU001")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_role\":\"standard\",\"user_status\":\"removed\"}"))
                .andExpect(status().isOk());
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AcspMembersDao.class);
    }

}
