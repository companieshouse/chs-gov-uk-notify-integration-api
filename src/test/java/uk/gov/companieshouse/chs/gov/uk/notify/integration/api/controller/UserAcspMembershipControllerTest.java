package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.TestDataManager;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.configuration.WebSecurityConfig;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor.InterceptorHelper;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspMembersService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.UsersService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.StaticPropertyUtil;

import java.util.Arrays;
import java.util.Optional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserAcspMembershipController.class)
@Import({InterceptorHelper.class, WebSecurityConfig.class})
@Tag("unit-test")
class UserAcspMembershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private StaticPropertyUtil staticPropertyUtil;

    @MockBean
    private UsersService usersService;

    @MockBean
    private AcspMembersService acspMembersService;

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
    void getAcspMembershipsForUserIdWithoutXRequestIdReturnsBadRequest() throws Exception {
        final var requestingUserDao = testDataManager.fetchAcspMembersDaos("COM002").getFirst();
        mockFetchUserDetailsFor("COMU002");
        Mockito.doReturn(Optional.of(requestingUserDao)).when(acspMembersService).fetchActiveAcspMembership("COMU002", "COMA001");

        mockMvc.perform(get("/user/acsps/memberships")
                        .header("Eric-identity", "COMU002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAcspMembershipsForUserIdWithWrongIncludeRemovedParameterInBodyReturnsBadRequest() throws Exception {
        final var requestingUserDao = testDataManager.fetchAcspMembersDaos("COM002").getFirst();
        mockFetchUserDetailsFor("COMU002");
        Mockito.doReturn(Optional.of(requestingUserDao)).when(acspMembersService).fetchActiveAcspMembership("COMU002", "COMA001");

        mockMvc.perform(get("/user/acsps/memberships?include_removed=null")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "COMU002")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .header("Eric-Authorised-Token-Permissions", testDataManager.fetchTokenPermissions("COM002")))
                .andExpect(status().isBadRequest());
    }

}
