package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service;

import org.junit.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.MongoTestExtension;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailRequestRepository;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.TestUtils.createSampleEmailRequest;


//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
//@Tag("integration-test")
//@ExtendWith(MongoTestExtension.class)
@Testcontainers
@SpringBootTest(classes = ChsGovUkNotifyIntegrationService.class)
public class NotificationDatabaseServiceTest {

    @Autowired
    private NotificationDatabaseService notificationDatabaseService;

    // When_StatusSaved_Expect_CanBeFoundByResponseId
    @Test
    public void When_StoreEmail_ThenEmailStored() {
        GovUkEmailDetailsRequest emailRequest = createSampleEmailRequest("john.doe@example.com");
        NotificationEmailRequest savedRequest = notificationDatabaseService.storeEmail(emailRequest);

        assertNotNull(savedRequest);
        assertNotNull(savedRequest.id());
    }
    
    // todo(finish these tests/integrate with main or eoin's stuff) - do mondya and finish off story
//    
//    public Optional<NotificationEmailRequest> getEmail(String id) {
//        return notificationEmailRequestRepository.findById(id);
//    }
//
//    
//    public List<NotificationEmailRequest> findAllEmails() {
//        return notificationEmailRequestRepository.findAll();
//    }
//
//    public NotificationLetterRequest storeLetter(GovUkLetterDetailsRequest letterDetails) {
//        return notificationLetterRequestRepository.save(new NotificationLetterRequest(null, letterDetails));
//    }
//    
//    public Optional<NotificationLetterRequest> getLetter(String id) {
//        return notificationLetterRequestRepository.findById(id);
//    }
//    
//    public List<NotificationLetterRequest> findAllLetters() {
//        return notificationLetterRequestRepository.findAll();
//    }
//
//    public NotificationStatus updateStatus(NotificationStatus notificationStatus) {
//        return notificationStatusRepository.save(notificationStatus);
//    }

}
