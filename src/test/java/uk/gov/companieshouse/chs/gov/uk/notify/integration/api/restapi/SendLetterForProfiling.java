package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.API_KEY_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.SecurityConstants.INTERNAL_USER_ROLE;

// Set gov.uk.notify.api.key to a valid value, DO NOT COMMIT IT.
@SpringBootTest(properties = {"gov.uk.notify.api.key = provide_key_name_here"})
@AutoConfigureMockMvc
@ExtendWith({SystemStubsExtension.class, OutputCaptureExtension.class})
@Tag("manual")
@SuppressWarnings("squid:S3577") // This is NOT to be run as part of an automated test suite.
class SendLetterForProfiling extends AbstractMongoDBTest {

    private static final String CONTEXT_ID = "X9uND6rXQxfbZNcMVFA7JI4h2KOh";
    private static final String X_REQUEST_ID = "X-Request-ID";
    private static final String ERIC_IDENTITY = "ERIC-Identity";
    private static final String ERIC_IDENTITY_VALUE = "65e73495c8e2";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Send letter repeatedly")
    void sendLetterRepeatedly() throws Exception {
        for (var time = 0; time < 10; time++) {
            postSendLetterRequest(getValidSendLetterRequestBody(),
                    status().isCreated());
        }
    }

    private ResultActions postSendLetterRequest(String requestBody,
                                                ResultMatcher expectedResponseStatus)
            throws Exception {
        return mockMvc.perform(post("/gov-uk-notify-integration/letter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(X_REQUEST_ID, CONTEXT_ID)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, API_KEY_IDENTITY_TYPE)
                        .header(ERIC_AUTHORISED_KEY_ROLES, INTERNAL_USER_ROLE)
                        .content(requestBody))
                .andExpect(expectedResponseStatus);
    }

    private static String getValidSendLetterRequestBody() throws IOException {
        return resourceToString("/fixtures/send-letter-request.json", UTF_8);
    }

}
