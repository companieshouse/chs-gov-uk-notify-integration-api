package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith({SystemStubsExtension.class})
class SenderRestApiIntegrationTest {

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
    void sendLetterSuccessfully() throws Exception {
        // When and then
        mockMvc.perform(post("/gov-uk-notify-integration/letter")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(resourceToString("/fixtures/send-letter-request.json", UTF_8)))
                .andExpect(status().isCreated());
    }

}
