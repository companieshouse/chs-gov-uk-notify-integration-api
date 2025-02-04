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
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.configuration.InterceptorConfig;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.configuration.WebSecurityConfig;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor.InterceptorHelper;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspMembersService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspProfileService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.EmailService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.UsersService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.StaticPropertyUtil;

import java.util.Arrays;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@Tag("unit-test")
@Import({InterceptorHelper.class, WebSecurityConfig.class})
@WebMvcTest(AcspMembershipController.class)
class ControllerAdviceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private AcspMembersService acspMemersService;

    @MockBean
    private InterceptorConfig interceptorConfig;

    @MockBean
    private UsersService usersService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private AcspProfileService acspProfileService;

    @MockBean
    private StaticPropertyUtil staticPropertyUtil;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        Mockito.doNothing().when(interceptorConfig).addInterceptors(any());
    }

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private void mockFetchUserDetailsFor(final String... userIds) {
        Arrays.stream(userIds).forEach(userId -> Mockito.doReturn(testDataManager.fetchUserDtos(userId).getFirst()).when(usersService).fetchUserDetails(userId));
    }

    @Test
    void testNotFoundRuntimeError() throws Exception {
        mockFetchUserDetailsFor("TSU001");
        Mockito.doThrow(new NotFoundRuntimeException("acsp-manage-users-api", "Couldn't find association")).when(acspMemersService).fetchMembership(any());

        mockMvc.perform(get("/acsps/memberships/TS001")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity", "TSU001"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testBadRequestRuntimeError() throws Exception {
        mockFetchUserDetailsFor("TSU001");
        Mockito.doThrow(new BadRequestRuntimeException("Request was less than ideal")).when(acspMemersService).fetchMembership(any());

        mockMvc.perform(get("/acsps/memberships/TS001")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity", "TSU001"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testConstraintViolationError() throws Exception {
        mockFetchUserDetailsFor("TSU001");
        mockMvc.perform(get("/acsps/memberships/$$$")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity", "TSU001"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testOnInternalServerError() throws Exception {
        mockFetchUserDetailsFor("TSU001");
        Mockito.doThrow(new NullPointerException("Something was null, which shouldn't have been.")).when(acspMemersService).fetchMembership(any());

        mockMvc.perform(get("/acsps/memberships/TS001")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity", "TSU001"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void testOnInternalServerErrorRuntimeException() throws Exception {
        mockFetchUserDetailsFor("TSU001");
        Mockito.doThrow(new InternalServerErrorRuntimeException("Problem")).when(acspMemersService).fetchMembership(any());

        mockMvc.perform(get("/acsps/memberships/TS001")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity", "TSU001"))
                .andExpect(status().isInternalServerError());
    }

}

