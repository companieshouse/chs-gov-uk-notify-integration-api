package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.integration;

import org.junit.jupiter.api.*;
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
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.AcspMembersDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email.ConfirmYouAreAStandardMemberEmailData;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email.ConfirmYouAreAnAdminMemberEmailData;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email.ConfirmYouAreAnOwnerMemberEmailData;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.repositories.AcspMembersRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspProfileService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.MessageType.*;

@AutoConfigureMockMvc
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class AcspMembershipsControllerIntegrationTest {

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

    private void mockFetchUserDetailsFor(String... userIds) {
        Arrays.stream(userIds).forEach(userId -> Mockito.doReturn(testDataManager.fetchUserDtos(userId).getFirst()).when(usersService).fetchUserDetails(userId));
    }

    private void mockFetchAcspProfilesFor(String... acspNumbers) {
        Arrays.stream(acspNumbers).forEach(acspNumber -> Mockito.doReturn(testDataManager.fetchAcspProfiles(acspNumber).getFirst()).when(
                acspProfileService).fetchAcspProfile(acspNumber));
    }

    private void setEmailProducerCountDownLatch(int countdown) {
        latch = new CountDownLatch(countdown);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(emailProducer).sendEmail(any(), any());
    }

    @Nested
    class GetMembersForAcsp {

        @Test
        void getMembersForAcspWithoutXRequestIdReturnsBadRequest() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");

            mockMvc.perform(get("/acsps/COMA001/memberships")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void getMembersForAcspWithMalformedAcspNumberReturnsBadRequest() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");

            mockMvc.perform(get("/acsps/££££££/memberships")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void getMembersForAcspWithNonExistentAcspNumberReturnsNotFound() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");
            Mockito.doThrow(new NotFoundRuntimeException("acsp-manage-users-api", "Was not found")).when(
                    acspProfileService).fetchAcspProfile("919191");

            mockMvc.perform(get("/acsps/919191/memberships")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002")))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getMembersForAcspAppliesAcspNumberAndIncludeRemovedAndRoleFilterCorrectly() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016"));
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS001", "TS002"));

            mockFetchUserDetailsFor("COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008", "COMU009", "COMU010", "COMU011", "COMU012", "COMU013", "COMU014", "COMU015", "COMU016");
            mockFetchAcspProfilesFor("COMA001");

            final var response =
                    mockMvc.perform(get("/acsps/COMA001/memberships?include_removed=false&role=owner")
                                    .header("X-Request-Id", "theId123")
                                    .header("Eric-identity", "COMU002")
                                    .header("ERIC-Identity-Type", "oauth2")
                                    .header("ERIC-Authorised-Key-Roles", "*")
                                    .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002")))
                            .andExpect(status().isOk());

            final var acspMembers = parseResponseTo(response, AcspMembershipsList.class);
            final var links = acspMembers.getLinks();

            final var acspIds = acspMembers.getItems().stream().map(AcspMembership::getId).collect(Collectors.toSet());

            Assertions.assertEquals(0, acspMembers.getPageNumber());
            Assertions.assertEquals(15, acspMembers.getItemsPerPage());
            Assertions.assertEquals(2, acspMembers.getTotalResults());
            Assertions.assertEquals(1, acspMembers.getTotalPages());
            Assertions.assertEquals("/acsps/COMA001/memberships?page_index=0&items_per_page=15", links.getSelf());
            Assertions.assertEquals("", links.getNext());
            assertTrue(acspIds.containsAll(Set.of("COM002", "COM010")));
        }

        @Test
        void getMembersForAcspAppliesIncludeRemovedFilterCorrectly() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016"));

            mockFetchUserDetailsFor("COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008", "COMU009", "COMU010", "COMU011", "COMU012", "COMU013", "COMU014", "COMU015", "COMU016");
            mockFetchAcspProfilesFor("COMA001");

            final var response =
                    mockMvc.perform(get("/acsps/COMA001/memberships?include_removed=true&items_per_page=20")
                                    .header("X-Request-Id", "theId123")
                                    .header("Eric-identity", "COMU002")
                                    .header("ERIC-Identity-Type", "oauth2")
                                    .header("ERIC-Authorised-Key-Roles", "*")
                                    .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002")))
                            .andExpect(status().isOk());

            final var acspMembers = parseResponseTo(response, AcspMembershipsList.class);
            final var links = acspMembers.getLinks();

            final var acspIds = acspMembers.getItems().stream().map(AcspMembership::getId).collect(Collectors.toSet());

            Assertions.assertEquals(0, acspMembers.getPageNumber());
            Assertions.assertEquals(20, acspMembers.getItemsPerPage());
            Assertions.assertEquals(16, acspMembers.getTotalResults());
            Assertions.assertEquals(1, acspMembers.getTotalPages());
            Assertions.assertEquals("/acsps/COMA001/memberships?page_index=0&items_per_page=20", links.getSelf());
            Assertions.assertEquals("", links.getNext());
            assertTrue(acspIds.containsAll(Set.of("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016")));
        }

        private static Stream<Arguments> provideRoleAndIncludeRemovedTestData() {
            return Stream.of(
                    Arguments.of("standard", false, 2, List.of("COMU007", "COMU008")),
                    Arguments.of("standard", true, 3, List.of("COMU006", "COMU007", "COMU008")),
                    Arguments.of("admin", false, 2, List.of("COMU004", "COMU005")),
                    Arguments.of("admin", true, 3, List.of("COMU003", "COMU004", "COMU005")),
                    Arguments.of("owner", false, 1, List.of("COMU002")),
                    Arguments.of("owner", true, 3, List.of("COMU001", "COMU002", "COMU009")));
        }

        @ParameterizedTest
        @MethodSource("provideRoleAndIncludeRemovedTestData")
        void getMembersForAcspWithRoleAndIncludeRemovedFilterAppliesCorrectly(final String role, final boolean includeRemoved, final int expectedCount, final List<String> expectedUserIds) throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009"));

            mockFetchUserDetailsFor("COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008", "COMU009");
            mockFetchAcspProfilesFor("COMA001");

            final var response =
                    mockMvc.perform(get(String.format("/acsps/COMA001/memberships?role=%s&include_removed=%s", role, includeRemoved))
                                    .header("X-Request-Id", "theId123")
                                    .header("Eric-identity", "COMU002")
                                    .header("ERIC-Identity-Type", "oauth2")
                                    .header("ERIC-Authorised-Key-Roles", "*")
                                    .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002")))
                            .andExpect(status().isOk());

            final var acspMembershipsList = parseResponseTo(response, AcspMembershipsList.class);
            final var userIds = acspMembershipsList.getItems().stream().map(AcspMembership::getUserId).collect(Collectors.toList());
            final var roles = acspMembershipsList.getItems().stream().map(AcspMembership::getUserRole).map(UserRoleEnum::getValue).collect(Collectors.toSet());
            ;

            Assertions.assertEquals(expectedCount, acspMembershipsList.getTotalResults());
            Assertions.assertTrue(userIds.containsAll(expectedUserIds));
            Assertions.assertEquals(1, roles.size());
            Assertions.assertTrue(roles.contains(role));
        }

        @Test
        void getMembersForAcspWithAdminUserPrivilegeReturnsData() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016"));

            mockFetchUserDetailsFor("COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008", "COMU009", "COMU010", "COMU011", "COMU012", "COMU013", "COMU014", "COMU015", "COMU016");
            mockFetchAcspProfilesFor("COMA001");

            final var response =
                    mockMvc.perform(get("/acsps/COMA001/memberships?include_removed=true&items_per_page=20")
                                    .header("X-Request-Id", "theId123")
                                    .header("Eric-identity", "COMU002")
                                    .header("ERIC-Identity-Type", "oauth2")
                                    .header("ERIC-Authorised-Key-Roles", "*")
                                    .header("ERIC-Authorised-Roles", "/admin/acsp/search"))
                            .andExpect(status().isOk());

            final var acspMembers = parseResponseTo(response, AcspMembershipsList.class);
            final var links = acspMembers.getLinks();

            final var acspIds = acspMembers.getItems().stream().map(AcspMembership::getId).collect(Collectors.toSet());

            Assertions.assertEquals(0, acspMembers.getPageNumber());
            Assertions.assertEquals(20, acspMembers.getItemsPerPage());
            Assertions.assertEquals(16, acspMembers.getTotalResults());
            Assertions.assertEquals(1, acspMembers.getTotalPages());
            Assertions.assertEquals("/acsps/COMA001/memberships?page_index=0&items_per_page=20", links.getSelf());
            Assertions.assertEquals("", links.getNext());
            assertTrue(acspIds.containsAll(Set.of("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016")));
        }
    }

    @Nested
    class FindMembershipsForUserAndAcsp {
        @Test
        void findMembershipsForUserAndAcspWithoutXRequestIdReturnsBadRequest() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");

            mockMvc.perform(post("/acsps/COMA001/memberships/lookup")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_email\":\"shaun.lock@comedy.com\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void findMembershipsForUserAndAcspWithMalformedAcspNumberReturnsBadRequest() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");

            mockMvc.perform(post("/acsps/££££££/memberships/lookup")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_email\":\"shaun.lock@comedy.com\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void findMembershipsForUserAndAcspWithNonExistentAcspNumberReturnsNotFound() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");
            Mockito.doThrow(new NotFoundRuntimeException("acsp-manage-users-api", "Was not found")).when(
                    acspProfileService).fetchAcspProfile("NONEXISTENT");

            mockMvc.perform(post("/acsps/NONEXISTENT/memberships/lookup")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_email\":\"shaun.lock@comedy.com\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void findMembershipsForUserAndAcspWithNullUserEmailReturnsBadRequest() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");

            mockMvc.perform(post("/acsps/COMA001/memberships/lookup")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void findMembershipsForUserAndAcspWithNonExistentUserReturnsNotFound() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");
            Mockito.doReturn(new UsersList()).when(usersService).searchUserDetails(List.of("shaun.lock@comedy.com"));
            mockMvc.perform(post("/acsps/COMA001/memberships/lookup")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_email\":\"shaun.lock@comedy.com\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void findMembershipsForUserAndAcspReturnsCorrectData() throws Exception {
            final var userDto = testDataManager.fetchUserDtos("COMU002").getFirst();
            final var acspProfile = testDataManager.fetchAcspProfiles("COMA001").getFirst();
            final var acspMemberDaos = testDataManager.fetchAcspMembersDaos("COM002");
            final var usersList = new UsersList();
            usersList.add(userDto);

            acspMembersRepository.insert(acspMemberDaos);

            mockFetchUserDetailsFor("COMU002");
            Mockito.doReturn(usersList).when(usersService).searchUserDetails(List.of(userDto.getEmail()));
            Mockito.doReturn(acspProfile).when(acspProfileService).fetchAcspProfile("COMA001");

            final var response =
                    mockMvc.perform(post("/acsps/COMA001/memberships/lookup")
                                    .header("X-Request-Id", "theId123")
                                    .header("Eric-identity", "COMU002")
                                    .header("ERIC-Identity-Type", "oauth2")
                                    .header("ERIC-Authorised-Key-Roles", "*")
                                    .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"user_email\":\"shaun.lock@comedy.com\"}"))
                            .andExpect(status().isOk());

            final var acspMembershipsList = parseResponseTo(response, AcspMembershipsList.class);

            assertEquals(1, acspMembershipsList.getItems().size());
            assertEquals("COMU002", acspMembershipsList.getItems().get(0).getUserId());
            assertEquals(AcspMembership.UserRoleEnum.OWNER, acspMembershipsList.getItems().get(0).getUserRole());
        }

        @Test
        void findMembershipsForActiveUser() throws Exception {
            final var activeUserDto = testDataManager.fetchUserDtos("COMU002").getFirst();
            final var acspProfile = testDataManager.fetchAcspProfiles("COMA001").getFirst();
            final var activeMembers = testDataManager.fetchAcspMembersDaos("COM002", "COM004", "COM005");
            final var removedMembers = testDataManager.fetchAcspMembersDaos("COM001", "COM003");

            acspMembersRepository.insert(activeMembers);
            acspMembersRepository.insert(removedMembers);

            mockFetchUserDetailsFor("COMU002");

            final var usersList = new UsersList();
            usersList.add(activeUserDto);
            Mockito.doReturn(usersList).when(usersService).searchUserDetails(List.of(activeUserDto.getEmail()));
            Mockito.doReturn(acspProfile).when(acspProfileService).fetchAcspProfile("COMA001");

            final var response =
                    mockMvc.perform(post("/acsps/COMA001/memberships/lookup?include_removed=true")
                                    .header("X-Request-Id", "theId123")
                                    .header("Eric-identity", "COMU002")
                                    .header("ERIC-Identity-Type", "oauth2")
                                    .header("ERIC-Authorised-Key-Roles", "*")
                                    .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"user_email\":\"shaun.lock@comedy.com\"}"))
                            .andExpect(status().isOk());

            final var acspMembershipsList = parseResponseTo(response, AcspMembershipsList.class);

            assertEquals(1, acspMembershipsList.getItems().size());
            assertEquals("COM002", acspMembershipsList.getItems().get(0).getId());
        }

        @Test
        void findMembershipsForUserAndAcspWithTrueIncludeRemovedIncludesRemovedMemberships() throws Exception {
            final var removedUserDto = testDataManager.fetchUserDtos("COMU001").getFirst();
            final var acspProfile = testDataManager.fetchAcspProfiles("COMA001").getFirst();
            final var members = testDataManager.fetchAcspMembersDaos("COM002", "COM004", "COM005", "COM001", "COM003");

            acspMembersRepository.insert(members);

            final var usersList = new UsersList();
            usersList.add(removedUserDto);
            Mockito.doReturn(usersList).when(usersService).searchUserDetails(List.of(removedUserDto.getEmail()));
            Mockito.doReturn(acspProfile).when(acspProfileService).fetchAcspProfile("COMA001");
            mockFetchUserDetailsFor("COMU002");

            final var response =
                    mockMvc.perform(post("/acsps/COMA001/memberships/lookup?include_removed=true")
                                    .header("X-Request-Id", "theId123")
                                    .header("Eric-identity", "COMU002")
                                    .header("ERIC-Identity-Type", "oauth2")
                                    .header("ERIC-Authorised-Key-Roles", "*")
                                    .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"user_email\":\"jimmy.carr@comedy.com\"}"))
                            .andExpect(status().isOk());

            final var acspMembershipsList = parseResponseTo(response, AcspMembershipsList.class);

            assertEquals(1, acspMembershipsList.getItems().size());
            assertEquals("COM001", acspMembershipsList.getItems().get(0).getId());
        }

        @Test
        void findMembershipsForUserAndAcspWithFalseIncludeRemovedDoesNotIncludeRemovedMemberships() throws Exception {
            final var removedUserDto = testDataManager.fetchUserDtos("COMU001").getFirst();
            final var acspProfile = testDataManager.fetchAcspProfiles("COMA001").getFirst();
            final var members = testDataManager.fetchAcspMembersDaos("COM002", "COM004", "COM005", "COM001", "COM003");

            acspMembersRepository.insert(members);

            final var usersList = new UsersList();
            usersList.add(removedUserDto);
            Mockito.doReturn(usersList).when(usersService).searchUserDetails(List.of(removedUserDto.getEmail()));
            Mockito.doReturn(acspProfile).when(acspProfileService).fetchAcspProfile("COMA001");
            mockFetchUserDetailsFor("COMU002");

            final var response =
                    mockMvc.perform(post("/acsps/COMA001/memberships/lookup?include_removed=false")
                                    .header("X-Request-Id", "theId123")
                                    .header("Eric-identity", "COMU002")
                                    .header("ERIC-Identity-Type", "oauth2")
                                    .header("ERIC-Authorised-Key-Roles", "*")
                                    .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"user_email\":\"jimmy.carr@comedy.com\"}"))
                            .andExpect(status().isOk());

            final var acspMembershipsList = parseResponseTo(response, AcspMembershipsList.class);

            assertEquals(0, acspMembershipsList.getItems().size());
        }
    }

    @Nested
    class AddMemberForAcsp {

        @Test
        void addMemberForAcspWithoutXRequestIdReturnsBadRequest() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");
            mockMvc.perform(post("/acsps/TSA001/memberships")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void addMemberForAcspWithMalformedUserIdReturnsBadRequest() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");

            mockMvc.perform(post("/acsps/TSA001/memberships")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_id\":\"abc-111-&\",\"user_role\":\"standard\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void addMemberForAcspWithMalformedAcspNumberReturnsBadRequest() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");

            mockMvc.perform(post("/acsps/TSA001-&/memberships")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void addMemberForAcspWithoutUserIdInBodyReturnsBadRequest() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");

            mockMvc.perform(post("/acsps/TSA001/memberships")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_role\":\"standard\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void addMemberForAcspWithoutUserRoleReturnsBadRequest() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");

            mockMvc.perform(post("/acsps/TSA001/memberships")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_id\":\"COMU001\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void addMemberForAcspWithNonexistentUserRoleReturnsBadRequest() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");

            mockMvc.perform(post("/acsps/TSA001/memberships")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_id\":\"COMU001\",\"user_role\":\"superuser\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void addMemberForAcspWithNonexistentAcspNumberReturnsBadRequest() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");
            Mockito.doThrow(new NotFoundRuntimeException("", "")).when(acspProfileService).fetchAcspProfile("TSA001");

            mockMvc.perform(post("/acsps/TSA001/memberships")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void addMemberForAcspWithNonexistentUserIdReturnsBadRequest() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

            mockFetchUserDetailsFor("COMU002");
            Mockito.doThrow(new NotFoundRuntimeException("", "")).when(usersService).fetchUserDetails("COMU001");

            mockMvc.perform(post("/acsps/TSA001/memberships")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "COMU002")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}"))
                    .andExpect(status().isBadRequest());
        }

        static Stream<Arguments> addMemberForAcspWithUserIdWithIncorrectPermissionsTestData() {
            return Stream.of(
                    Arguments.of("COMU002", "{\"user_id\":\"COMU002\",\"user_role\":\"standard\"}", testDataManager.fetchTokenPermissions("COM002")),
                    Arguments.of("COMU007", "{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}", testDataManager.fetchTokenPermissions("COM007")),
                    Arguments.of("COMU005", "{\"user_id\":\"COMU001\",\"user_role\":\"owner\"}", testDataManager.fetchTokenPermissions("COM005"))
            );
        }

        @ParameterizedTest
        @MethodSource("addMemberForAcspWithUserIdWithIncorrectPermissionsTestData")
        void addMemberForAcspWithUserIdThatAlreadyHasActiveMembershipReturnsBadRequest(final String ericIdentity, final String requestBody, final String tokenPermissions) throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "NEI003"));

            mockFetchUserDetailsFor("COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008", "COMU009", "NEIU003");
            mockFetchAcspProfilesFor("COMA001");

            mockMvc.perform(post("/acsps/COMA001/memberships")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", ericIdentity)
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", tokenPermissions)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void addMemberForAcspWithCorrectDataReturnsAddedAcspMembership() throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "TS001"));

            mockFetchUserDetailsFor("COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008", "COMU009", "TSU001");
            mockFetchAcspProfilesFor("TSA001");

            final var response =
                    mockMvc.perform(post("/acsps/TSA001/memberships")
                                    .header("X-Request-Id", "theId123")
                                    .header("Eric-identity", "TSU001")
                                    .header("ERIC-Identity-Type", "oauth2")
                                    .header("ERIC-Authorised-Key-Roles", "*")
                                    .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("TS001"))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"user_id\":\"COMU001\",\"user_role\":\"standard\"}"))
                            .andExpect(status().isCreated());

            final var acspMembership = parseResponseTo(response, AcspMembership.class);

            Assertions.assertEquals("TSA001", acspMembership.getAcspNumber());
            Assertions.assertEquals("COMU001", acspMembership.getUserId());
            Assertions.assertEquals("TSU001", acspMembership.getAddedBy());
        }

        static Stream<Arguments> rolesStream() {
            return Stream.of(
                    Arguments.of(UserRoleEnum.OWNER),
                    Arguments.of(UserRoleEnum.ADMIN),
                    Arguments.of(UserRoleEnum.STANDARD)
            );
        }

        @ParameterizedTest
        @MethodSource("rolesStream")
        void addMemberForAcspSendsConfirmYouAreAMemberNotificationsWithoutDisplayName(final UserRoleEnum role) throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS001"));
            mockFetchUserDetailsFor("TSU001", "COMU001");
            mockFetchAcspProfilesFor("TSA001");

            setEmailProducerCountDownLatch(1);

            mockMvc.perform(post("/acsps/TSA001/memberships")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "TSU001")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("TS001"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("{\"user_id\":\"COMU001\",\"user_role\":\"%s\"}", role.getValue())))
                    .andExpect(status().isCreated());

            latch.await(10, TimeUnit.SECONDS);

            if (UserRoleEnum.OWNER.equals(role)) {
                Mockito.verify(emailProducer).sendEmail(new ConfirmYouAreAnOwnerMemberEmailData("jimmy.carr@comedy.com", "buzz.lightyear@toystory.com", "Toy Story", signinUrl), CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE.getValue());
            } else if (UserRoleEnum.ADMIN.equals(role)) {
                Mockito.verify(emailProducer).sendEmail(new ConfirmYouAreAnAdminMemberEmailData("jimmy.carr@comedy.com", "buzz.lightyear@toystory.com", "Toy Story", signinUrl), CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE.getValue());
            } else if (UserRoleEnum.STANDARD.equals(role)) {
                Mockito.verify(emailProducer).sendEmail(new ConfirmYouAreAStandardMemberEmailData("jimmy.carr@comedy.com", "buzz.lightyear@toystory.com", "Toy Story", signinUrl), CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE.getValue());
            }
        }

        @ParameterizedTest
        @MethodSource("rolesStream")
        void addMemberForAcspSendsConfirmYouAreAMemberNotificationsWithDisplayName(final UserRoleEnum role) throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("WIT001"));
            mockFetchUserDetailsFor("WITU001", "COMU001");
            mockFetchAcspProfilesFor("WITA001");

            setEmailProducerCountDownLatch(1);

            mockMvc.perform(post("/acsps/WITA001/memberships")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "WITU001")
                            .header("ERIC-Identity-Type", "oauth2")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT001"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("{\"user_id\":\"COMU001\",\"user_role\":\"%s\"}", role.getValue())))
                    .andExpect(status().isCreated());

            latch.await(10, TimeUnit.SECONDS);

            if (UserRoleEnum.OWNER.equals(role)) {
                Mockito.verify(emailProducer).sendEmail(new ConfirmYouAreAnOwnerMemberEmailData("jimmy.carr@comedy.com", "Geralt of Rivia", "Witcher", signinUrl), CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE.getValue());
            } else if (UserRoleEnum.ADMIN.equals(role)) {
                Mockito.verify(emailProducer).sendEmail(new ConfirmYouAreAnAdminMemberEmailData("jimmy.carr@comedy.com", "Geralt of Rivia", "Witcher", signinUrl), CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE.getValue());
            } else if (UserRoleEnum.STANDARD.equals(role)) {
                Mockito.verify(emailProducer).sendEmail(new ConfirmYouAreAStandardMemberEmailData("jimmy.carr@comedy.com", "Geralt of Rivia", "Witcher", signinUrl), CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE.getValue());
            }

        }

        @ParameterizedTest
        @MethodSource("rolesStream")
        void addMemberForAcspDoesNotSendConfirmYouAreAMemberNotificationsWhenCalledInternally(final UserRoleEnum role) throws Exception {
            acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("WIT001"));
            mockFetchUserDetailsFor("WITU001", "COMU001");
            mockFetchAcspProfilesFor("WITA001");

            mockMvc.perform(post("/acsps/WITA001/memberships")
                            .header("X-Request-Id", "theId123")
                            .header("Eric-identity", "WITU001")
                            .header("ERIC-Identity-Type", "key")
                            .header("ERIC-Authorised-Key-Roles", "*")
                            .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("WIT001"))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("{\"user_id\":\"COMU001\",\"user_role\":\"%s\"}", role.getValue())))
                    .andExpect(status().isCreated());

            if (UserRoleEnum.OWNER.equals(role)) {
                Mockito.verify(emailProducer, times(0)).sendEmail(new ConfirmYouAreAnOwnerMemberEmailData("jimmy.carr@comedy.com", "Geralt of Rivia", "Witcher", signinUrl), CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE.getValue());
            } else if (UserRoleEnum.ADMIN.equals(role)) {
                Mockito.verify(emailProducer, times(0)).sendEmail(new ConfirmYouAreAnAdminMemberEmailData("jimmy.carr@comedy.com", "Geralt of Rivia", "Witcher", signinUrl), CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE.getValue());
            } else if (UserRoleEnum.STANDARD.equals(role)) {
                Mockito.verify(emailProducer, times(0)).sendEmail(new ConfirmYouAreAStandardMemberEmailData("jimmy.carr@comedy.com", "Geralt of Rivia", "Witcher", signinUrl), CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE.getValue());
            }

        }

    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AcspMembersDao.class);
    }

}
