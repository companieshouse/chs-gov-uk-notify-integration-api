package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPatch.UserStatusEnum;
import uk.gov.companieshouse.api.acspprofile.Status;
import uk.gov.companieshouse.api.sdk.ApiClientService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.TestDataManager;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.configuration.WebSecurityConfig;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor.InterceptorHelper;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspMembersService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspProfileService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.EmailService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.UsersService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.StaticPropertyUtil;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AcspMembershipController.class)
@Import({InterceptorHelper.class, WebSecurityConfig.class})
@Tag("unit-test")
class AcspMembershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private ApiClientService apiClientService;

    @MockBean
    private InternalApiClient internalApiClient;

    @MockBean
    private UsersService usersService;

    @MockBean
    private AcspProfileService acspProfileService;

    @MockBean
    private StaticPropertyUtil staticPropertyUtil;

    @MockBean
    private AcspMembersService acspMembersService;

    @MockBean
    private EmailService emailService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private void mockFetchUserDetailsFor(final String... userIds) {
        Arrays.stream(userIds).forEach(userId -> Mockito.doReturn(testDataManager.fetchUserDtos(userId).getFirst()).when(usersService).fetchUserDetails(userId));
    }

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void getAcspMembershipForAcspAndIdWithoutXRequestIdReturnsBadRequest() throws Exception {
        final var requestingUserDao = testDataManager.fetchAcspMembersDaos("WIT004").getFirst();
        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(Optional.of(requestingUserDao)).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001");

        mockMvc.perform(get("/acsps/memberships/TS001")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAcspMembershipForAcspAndIdWithMalformedMembershipIdReturnsBadRequest() throws Exception {
        final var requestingUserDao = testDataManager.fetchAcspMembersDaos("WIT004").getFirst();
        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(Optional.of(requestingUserDao)).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001");

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
        final var requestingUserDao = testDataManager.fetchAcspMembersDaos("WIT004").getFirst();
        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(Optional.of(requestingUserDao)).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001");

        mockMvc.perform(get("/acsps/memberships/TS001")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004")))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAcspMembershipForAcspAndIdWithOAuth2Succeeds() throws Exception {
        final var requestingUserDao = testDataManager.fetchAcspMembersDaos("WIT004").getFirst();

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(Optional.of(requestingUserDao)).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001");
        Mockito.doReturn(Optional.of(new AcspMembership().id("TS001"))).when(acspMembersService).fetchMembership("TS001");

        mockMvc.perform(get("/acsps/memberships/TS001")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004")))
                .andExpect(status().isOk());
    }

    @Test
    void getAcspMembershipForAcspAndIdWithApiKeySucceeds() throws Exception {
        final var requestingUserDao = testDataManager.fetchAcspMembersDaos("WIT004").getFirst();

        Mockito.doReturn(Optional.of(requestingUserDao)).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001");
        Mockito.doReturn(Optional.of(new AcspMembership().id("TS001"))).when(acspMembersService).fetchMembership("TS001");

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
        final var requestingUserDao = testDataManager.fetchAcspMembersDaos("WIT004").getFirst();

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(Optional.of(requestingUserDao)).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001");

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
        final var requestingUserDao = testDataManager.fetchAcspMembersDaos("WIT004").getFirst();

        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(Optional.of(requestingUserDao)).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001");

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
        final var requestingUserDao = testDataManager.fetchAcspMembersDaos("WIT004").getFirst();

        Mockito.doReturn(Optional.of(requestingUserDao)).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001");
        Mockito.doReturn(testDataManager.fetchUserDtos("67ZeMsvAEgkBWs7tNKacdrPvOmQ").getFirst()).when(usersService).fetchUserDetails("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(Optional.empty()).when(acspMembersService).fetchMembershipDao("67ZeMsvAEgkBWs7tNKacdrPvOmQ");

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
        final var requestingUserDao = testDataManager.fetchAcspMembersDaos("WIT004").getFirst();
        mockFetchUserDetailsFor("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(Optional.of(requestingUserDao)).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001");

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
        final var requestingUserDao = testDataManager.fetchAcspMembersDaos("WIT004").getFirst();
        Mockito.doReturn(Optional.of(requestingUserDao)).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001");

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
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos("WIT004").getFirst();

        Mockito.doReturn(Optional.of(acspMemberDaos)).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001");
        Mockito.doReturn(testDataManager.fetchUserDtos("67ZeMsvAEgkBWs7tNKacdrPvOmQ").getFirst()).when(usersService).fetchUserDetails("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(Optional.of(acspMemberDaos)).when(acspMembersService).fetchMembershipDao("WIT004");
        Mockito.doReturn(1).when(acspMembersService).fetchNumberOfActiveOwners("WITA004");
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
        Mockito.doReturn(testDataManager.fetchUserDtos("67ZeMsvAEgkBWs7tNKacdrPvOmQ").getFirst()).when(usersService).fetchUserDetails("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(Optional.of(testDataManager.fetchAcspMembersDaos("WIT004").getFirst())).when(acspMembersService).fetchMembershipDao("WIT004");
        Mockito.doReturn(testDataManager.fetchAcspProfiles("WITA001").getFirst()).when(acspProfileService).fetchAcspProfile("WITA001");
        Mockito.doReturn(1).when(acspMembersService).fetchNumberOfActiveOwners("WITA001");

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

        Mockito.doReturn(testDataManager.fetchUserDtos("67ZeMsvAEgkBWs7tNKacdrPvOmQ").getFirst()).when(usersService).fetchUserDetails("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(Optional.of(testDataManager.fetchAcspMembersDaos("WIT004").getFirst())).when(acspMembersService).fetchMembershipDao("WIT004");
        Mockito.doReturn(acspProfile).when(acspProfileService).fetchAcspProfile("WITA001");
        Mockito.doReturn(1).when(acspMembersService).fetchNumberOfActiveOwners("WITA001");

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

    @Test
    void updateAcspMembershipForAcspAndIdWithAdminCallerAndUserRoleSetToOwnerInRequestBodyReturnsBadRequest() throws Exception {
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos("WIT002", "WIT003");

        Mockito.doReturn(Optional.of(acspMemberDaos.getFirst())).when(acspMembersService).fetchActiveAcspMembership("WITU002", "WITA001");
        Mockito.doReturn(testDataManager.fetchUserDtos("WITU002").getFirst()).when(usersService).fetchUserDetails("WITU002");
        Mockito.doReturn(Optional.of(acspMemberDaos.getLast())).when(acspMembersService).fetchMembershipDao("WIT003");
        Mockito.doReturn(Optional.of(acspMemberDaos.getFirst())).when(acspMembersService).fetchActiveAcspMembership("WITU002", "WITA001");

        mockMvc.perform(patch("/acsps/memberships/WIT003")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT002"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_role\":\"owner\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithStandardCallerReturnsBadRequest() throws Exception {
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos("XME004", "XME002");

        Mockito.doReturn(testDataManager.fetchUserDtos("67ZeMsvAEgkBWs7tNKacdrPvOmQ").getFirst()).when(usersService).fetchUserDetails("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(Optional.of(acspMemberDaos.getLast())).when(acspMembersService).fetchMembershipDao("XME002");
        Mockito.doReturn(Optional.of(acspMemberDaos.getFirst())).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "XMEA001");

        mockMvc.perform(patch("/acsps/memberships/XME002")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("XME004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_role\":\"standard\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithAdminCallerAndOwnerTargetReturnsBadRequest() throws Exception {
        final var acspMemberDaos = testDataManager.fetchAcspMembersDaos("COM004", "COM002");

        Mockito.doReturn(testDataManager.fetchUserDtos("67ZeMsvAEgkBWs7tNKacdrPvOmQ").getFirst()).when(usersService).fetchUserDetails("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(Optional.of(acspMemberDaos.getLast())).when(acspMembersService).fetchMembershipDao("COM002");
        Mockito.doReturn(2).when(acspMembersService).fetchNumberOfActiveOwners("COMA001");
        Mockito.doReturn(Optional.of(acspMemberDaos.getFirst())).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "COMA001");

        mockMvc.perform(patch("/acsps/memberships/COM002")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_role\":\"standard\"}"))
                .andExpect(status().isBadRequest());
    }


    @Test
    void updateAcspMembershipForAcspAndIdWithOAuth2RequestAndUserRoleAndWithoutDisplayNameSendsNotification() throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos("WIT004", "WIT002");

        Mockito.doReturn(testDataManager.fetchUserDtos("67ZeMsvAEgkBWs7tNKacdrPvOmQ").getFirst()).when(usersService).fetchUserDetails("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(Optional.of(acspMembersDaos.getLast())).when(acspMembersService).fetchMembershipDao("WIT002");
        Mockito.doReturn(2).when(acspMembersService).fetchNumberOfActiveOwners("WITA001");
        Mockito.doReturn(Optional.of(acspMembersDaos.getFirst())).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001");
        Mockito.doReturn(testDataManager.fetchUserDtos("WITU002").getFirst()).when(usersService).fetchUserDetails("WITU002");
        Mockito.doReturn(testDataManager.fetchAcspProfiles("WITA001").getFirst()).when(acspProfileService).fetchAcspProfile("WITA001");

        mockMvc.perform(patch("/acsps/memberships/WIT002")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT004"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_role\":\"owner\"}"))
                .andExpect(status().isOk());

        Mockito.verify(acspMembersService).updateMembership("WIT002", null, UserRoleEnum.OWNER, "67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.verify(emailService).sendYourRoleAtAcspHasChangedEmail("theId123", "yennefer@witcher.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER);
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithOAuth2RequestAndUserRoleAndWithDisplayNameSendsNotification() throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos("WIT001", "WIT002");

        Mockito.doReturn(testDataManager.fetchUserDtos("WITU001").getFirst()).when(usersService).fetchUserDetails("WITU001");
        Mockito.doReturn(Optional.of(acspMembersDaos.getLast())).when(acspMembersService).fetchMembershipDao("WIT002");
        Mockito.doReturn(2).when(acspMembersService).fetchNumberOfActiveOwners("WITA001");
        Mockito.doReturn(Optional.of(acspMembersDaos.getFirst())).when(acspMembersService).fetchActiveAcspMembership("WITU001", "WITA001");
        Mockito.doReturn(testDataManager.fetchUserDtos("WITU002").getFirst()).when(usersService).fetchUserDetails("WITU002");
        Mockito.doReturn(testDataManager.fetchAcspProfiles("WITA001").getFirst()).when(acspProfileService).fetchAcspProfile("WITA001");

        mockMvc.perform(patch("/acsps/memberships/WIT002")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU001")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT001"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_role\":\"standard\"}"))
                .andExpect(status().isOk());

        Mockito.verify(acspMembersService).updateMembership("WIT002", null, UserRoleEnum.STANDARD, "WITU001");
        Mockito.verify(emailService).sendYourRoleAtAcspHasChangedEmail("theId123", "yennefer@witcher.com", "Geralt of Rivia", "Witcher", UserRoleEnum.STANDARD);
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithOAuth2RequestAndUserRoleWhereUserCannotBeFoundReturnsNotFound() throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos("WIT001", "WIT002");

        Mockito.doReturn(testDataManager.fetchUserDtos("WITU001").getFirst()).when(usersService).fetchUserDetails("WITU001");
        Mockito.doReturn(Optional.of(acspMembersDaos.getLast())).when(acspMembersService).fetchMembershipDao("WIT002");
        Mockito.doReturn(2).when(acspMembersService).fetchNumberOfActiveOwners("WITA001");
        Mockito.doReturn(Optional.of(acspMembersDaos.getFirst())).when(acspMembersService).fetchActiveAcspMembership("WITU001", "WITA001");
        Mockito.doThrow(new NotFoundRuntimeException("acsp-manage-users-api", "Not found")).when(usersService).fetchUserDetails("WITU002");

        mockMvc.perform(patch("/acsps/memberships/WIT002")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU001")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT001"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_role\":\"standard\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithOAuth2RequestAndUserRoleWhereAcspCannotBeFoundReturnsNotFound() throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos("WIT001", "WIT002");

        Mockito.doReturn(testDataManager.fetchUserDtos("WITU001").getFirst()).when(usersService).fetchUserDetails("WITU001");
        Mockito.doReturn(Optional.of(acspMembersDaos.getLast())).when(acspMembersService).fetchMembershipDao("WIT002");
        Mockito.doReturn(2).when(acspMembersService).fetchNumberOfActiveOwners("WITA001");
        Mockito.doReturn(Optional.of(acspMembersDaos.getFirst())).when(acspMembersService).fetchActiveAcspMembership("WITU001", "WITA001");
        Mockito.doThrow(new NotFoundRuntimeException("acsp-manage-users-api", "Not found")).when(acspProfileService).fetchAcspProfile("WITA001");

        mockMvc.perform(patch("/acsps/memberships/WIT002")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "WITU001")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT001"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_role\":\"standard\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAcspMembershipForAcspAndIdCanUpdateUserRoleAndUserStatusAtTheSameTime() throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos("WIT004", "WIT002");

        Mockito.doReturn(testDataManager.fetchUserDtos("67ZeMsvAEgkBWs7tNKacdrPvOmQ").getFirst()).when(usersService).fetchUserDetails("67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.doReturn(Optional.of(acspMembersDaos.getLast())).when(acspMembersService).fetchMembershipDao("WIT002");
        Mockito.doReturn(2).when(acspMembersService).fetchNumberOfActiveOwners("WITA001");
        Mockito.doReturn(Optional.of(acspMembersDaos.getFirst())).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "WITA001");
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

        Mockito.verify(acspMembersService).updateMembership("WIT002", UserStatusEnum.REMOVED, UserRoleEnum.STANDARD, "67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        Mockito.verify(emailService).sendYourRoleAtAcspHasChangedEmail("theId123", "yennefer@witcher.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.STANDARD);
    }

    @Test
    void updateAcspMembershipForAcspAndIdWithApiKeyByPassesOAuth2Checks() throws Exception {
        final var acspMembersDaos = testDataManager.fetchAcspMembersDaos("COM001", "COM004");

        Mockito.doReturn(testDataManager.fetchUserDtos("COMU001").getFirst()).when(usersService).fetchUserDetails("COMU001");
        Mockito.doReturn(Optional.of(acspMembersDaos.getLast())).when(acspMembersService).fetchMembershipDao("COM004");
        Mockito.doReturn(2).when(acspMembersService).fetchNumberOfActiveOwners("COMA001");
        Mockito.doReturn(Optional.of(acspMembersDaos.getFirst())).when(acspMembersService).fetchActiveAcspMembership("67ZeMsvAEgkBWs7tNKacdrPvOmQ", "COMA001");

        mockMvc.perform(patch("/acsps/memberships/COM004")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU001")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_role\":\"standard\",\"user_status\":\"removed\"}"))
                .andExpect(status().isOk());
    }

}
