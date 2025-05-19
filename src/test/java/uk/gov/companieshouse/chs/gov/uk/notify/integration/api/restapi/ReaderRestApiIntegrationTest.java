package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.AbstractMongoDBTest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.TestUtils;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReaderRestApiIntegrationTest extends AbstractMongoDBTest {

    private static final String CONTEXT_ID = "X9uND6rXQxfbZNcMVFA7JI4h2KOh";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_ADDRESS_LINE = "123 Test Street";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationDatabaseService notificationDatabaseService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationEmailRequestRepository notificationEmailRequestRepository;

    @Autowired
    private NotificationLetterRequestRepository notificationLetterRequestRepository;

    @Test
    void When_RequestingAllEmails_Expect_SuccessfulResponseWithEmailList() throws Exception {
        notificationEmailRequestRepository.deleteAll();

        GovUkEmailDetailsRequest emailRequest = TestUtils.createSampleEmailRequest(TEST_EMAIL);
        notificationDatabaseService.storeEmail(emailRequest);

        MvcResult result = mockMvc.perform(get("/gov-uk-notify-integration/emails")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", CONTEXT_ID))
                .andExpect(status().isOk())
                .andReturn();

        List<GovUkEmailDetailsRequest> emailResponses = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );

        assertNotNull(emailResponses);
        assertEquals(1, emailResponses.size());
        assertEquals(TEST_EMAIL, emailResponses.get(0).getRecipientDetails().getEmailAddress());
    }

    @Test
    void When_RequestingAllLetters_Expect_SuccessfulResponseWithLetterList() throws Exception {
        notificationLetterRequestRepository.deleteAll();
        
        GovUkLetterDetailsRequest letterRequest = TestUtils.createSampleLetterRequest(TEST_ADDRESS_LINE);
        notificationDatabaseService.storeLetter(letterRequest);

        MvcResult result = mockMvc.perform(get("/gov-uk-notify-integration/letters")
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", CONTEXT_ID))
                .andExpect(status().isOk())
                .andReturn();

        List<GovUkLetterDetailsRequest> letterResponses = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );

        assertNotNull(letterResponses);
        assertEquals(1, letterResponses.size());
        assertEquals(TEST_ADDRESS_LINE, letterResponses.get(0).getRecipientDetails().getPhysicalAddress().getAddressLine1());
    }

    @Test
    void When_RequestingEmailById_Expect_SuccessfulResponseWithMatchingEmail() throws Exception {
        GovUkEmailDetailsRequest emailRequest = TestUtils.createSampleEmailRequest(TEST_EMAIL);
        NotificationEmailRequest savedEmail = notificationDatabaseService.storeEmail(emailRequest);
        String emailId = savedEmail.getId();

        MvcResult result = mockMvc.perform(get("/gov-uk-notify-integration/email/" + emailId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", CONTEXT_ID))
                .andExpect(status().isOk())
                .andReturn();

        GovUkEmailDetailsRequest emailResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                GovUkEmailDetailsRequest.class);

        assertNotNull(emailResponse);
        assertEquals(TEST_EMAIL, emailResponse.getRecipientDetails().getEmailAddress());
    }

    @Test
    void When_RequestingNonExistentEmailById_Expect_NotFoundResponse() throws Exception {
        String nonExistentId = "nonexistent123456789";

        mockMvc.perform(get("/gov-uk-notify-integration/email/" + nonExistentId)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Request-ID", CONTEXT_ID))
                .andExpect(status().isNotFound());
    }

}
