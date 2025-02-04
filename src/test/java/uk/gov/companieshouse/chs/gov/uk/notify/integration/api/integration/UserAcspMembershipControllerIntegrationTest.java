package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.TestDataManager;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.AcspMembersDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.repositories.AcspMembersRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspProfileService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.UsersService;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.ParsingUtils.parseResponseTo;

@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
@SpringBootTest
class UserAcspMembershipControllerIntegrationTest {

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

    @MockBean
    private EmailProducer emailProducer;

    @MockBean
    private KafkaProducerFactory kafkaProducerFactory;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private void mockFetchUserDetailsFor(final String... userIds) {
        Arrays.stream(userIds).forEach(userId -> Mockito.doReturn(testDataManager.fetchUserDtos(userId).getFirst()).when(usersService).fetchUserDetails(userId));
    }

    private void mockFetchAcspProfilesFor(final String... acspNumbers) {
        Arrays.stream(acspNumbers).forEach(acspNumber -> Mockito.doReturn(testDataManager.fetchAcspProfiles(acspNumber).getFirst()).when(
                acspProfileService).fetchAcspProfile(acspNumber));
    }

    @Test
    void getAcspMembershipsForUserIdWithoutXRequestIdReturnsBadRequest() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

        mockFetchUserDetailsFor("COMU002");

        mockMvc.perform(get("/user/acsps/memberships")
                        .header("Eric-identity", "COMU002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAcspMembershipsForUserIdWithWrongIncludeRemovedParameterInBodyReturnsBadRequest() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("COM002"));

        mockFetchUserDetailsFor("COMU002");

        mockMvc.perform(get("/user/acsps/memberships?include_removed=null")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAcspMembershipsForUserIdWhoHasNoAcspMembershipsReturnsOk() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS001", "TS002", "COM001", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016"));

        mockFetchUserDetailsFor("COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008", "COMU009", "COMU010", "COMU011", "COMU012", "COMU013", "COMU014", "COMU015", "COMU016");
        mockFetchAcspProfilesFor("COMA001");

        mockMvc.perform(get("/user/acsps/memberships")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU001")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM001")))
                .andExpect(status().isOk());
    }

    @Test
    void getAcspMembershipsForUserIdWhoHasAcspMembershipsReturnsNonEmptyAcspMembershipsList() throws Exception {
        acspMembersRepository.insert(testDataManager.fetchAcspMembersDaos("TS001", "TS002", "COM001", "COM002", "COM003", "COM004", "COM005", "COM006", "COM007", "COM008", "COM009", "COM010", "COM011", "COM012", "COM013", "COM014", "COM015", "COM016"));

        mockFetchUserDetailsFor("COMU001", "COMU002", "COMU003", "COMU004", "COMU005", "COMU006", "COMU007", "COMU008", "COMU009", "COMU010", "COMU011", "COMU012", "COMU013", "COMU014", "COMU015", "COMU016");
        mockFetchAcspProfilesFor("COMA001");

        final var response =
                mockMvc.perform(get("/user/acsps/memberships")
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "COMU002")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*")
                                .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002")))
                        .andExpect(status().isOk());

        final var acspMemberships = parseResponseTo(response, AcspMembershipsList.class).getItems();

        assertEquals(1, acspMemberships.size());
        assertEquals("COM002", acspMemberships.getFirst().getId());
        assertEquals("COMU002", acspMemberships.getFirst().getUserId());
        assertEquals("COMA001", acspMemberships.getFirst().getAcspNumber());
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AcspMembersDao.class);
    }

}
