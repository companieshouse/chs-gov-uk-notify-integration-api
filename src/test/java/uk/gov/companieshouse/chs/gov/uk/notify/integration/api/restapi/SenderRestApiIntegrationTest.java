package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.SharedMongoContainer;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration-test")
@SpringBootTest("gov.uk.notify.api.key=${GOV_UK_NOTIFY_API_KEY}")
@AutoConfigureMockMvc
@ExtendWith({SystemStubsExtension.class, OutputCaptureExtension.class})
class SenderRestApiIntegrationTest {

    private static final String CONTEXT_ID = "X9uND6rXQxfbZNcMVFA7JI4h2KOh";
    private static final String INVALID_CONTEXT_ID = "X9uND6rXQxfbZ:cMVFA7JI4h2KOh";
    private static final String INVALID_CONTEXT_ID_ERROR_MESSAGE_PREFIX =
            "Error in chs-gov-uk-notify-integration-api: context ID (X-Request-ID): must match ";
    private static final String CONTEXT_ID_PATTERN = "[0-9A-Za-z-_]{8,32}";
    private static final String INVALID_CONTEXT_ID_ERROR_MESSAGE =
            INVALID_CONTEXT_ID_ERROR_MESSAGE_PREFIX + "\"" + CONTEXT_ID_PATTERN + "\"";
    private static final String EXPECTED_NULL_FIELDS_ERRORS =
            "Error(s) in chs-gov-uk-notify-integration-api: "
            + "[govUkLetterDetailsRequest senderDetails.appId must not be null, "
            + "govUkLetterDetailsRequest senderDetails.reference must not be null]";

    static {
        SharedMongoContainer.getInstance();
    }

    @Autowired
    private MockMvc mockMvc;

    @SystemStub
    private static EnvironmentVariables variables;

    @BeforeAll
    public static void setup() {
        // Given
        variables.set("GOV_UK_NOTIFY_API_KEY", "Token value");
    }

    @Test
    @DisplayName("Send letter successfully")
    void sendLetterSuccessfully(CapturedOutput log) throws Exception {

        // When and then
        mockMvc.perform(post("/gov-uk-notify-integration/letter")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", CONTEXT_ID)
                .content(resourceToString("/fixtures/send-letter-request.json", UTF_8)))
                .andExpect(status().isCreated());

        assertThat(log.getAll().contains("\"context_id\":\"" + CONTEXT_ID+ "\""), is(true));
        assertThat(log.getAll().contains("emailAddress: vjackson1@companieshouse.gov.uk"), is(true));
    }

    @Test
    @DisplayName("Send letter with an invalid context ID")
    void sendLetterWithInvalidContextId(CapturedOutput log) throws Exception {

        // When and then
        mockMvc.perform(post("/gov-uk-notify-integration/letter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", INVALID_CONTEXT_ID)
                        .content(resourceToString("/fixtures/send-letter-request.json", UTF_8)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(INVALID_CONTEXT_ID_ERROR_MESSAGE));

        assertThat(log.getAll().contains(INVALID_CONTEXT_ID_ERROR_MESSAGE_PREFIX), is(true));
        assertThat(log.getAll().contains(CONTEXT_ID_PATTERN), is(true));
    }

    @Test
    @DisplayName("Send letter with an invalid request")
    void sendLetterWithInvalidRequest(CapturedOutput log) throws Exception {

        // When and then
        mockMvc.perform(post("/gov-uk-notify-integration/letter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", INVALID_CONTEXT_ID)
                        .content(resourceToString("/fixtures/send-letter-request-missing-sender-reference-and-app-id.json",
                                UTF_8)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(EXPECTED_NULL_FIELDS_ERRORS));

        assertThat(log.getAll().contains(EXPECTED_NULL_FIELDS_ERRORS), is(true));

    }

}
