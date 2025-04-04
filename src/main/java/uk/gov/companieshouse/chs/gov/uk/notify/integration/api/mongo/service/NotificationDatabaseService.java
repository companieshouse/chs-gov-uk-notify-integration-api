package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service;


import java.util.List;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationResponseRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationStatusRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationStatus;
import java.util.Optional;


@Service
public class NotificationDatabaseService implements MongoDataStoreInterface {

    private final NotificationEmailRequestRepository notificationEmailRequestRepository;
    private final NotificationLetterRequestRepository notificationLetterRequestRepository;
    private final NotificationResponseRepository notificationResponseRepository; // not used for now?
    private final NotificationStatusRepository notificationStatusRepository; // not in mvp, will be done later

    public NotificationDatabaseService(
            NotificationEmailRequestRepository notificationEmailRequestRepository,
            NotificationLetterRequestRepository notificationLetterRequestRepository,
            NotificationResponseRepository notificationResponseRepository,
            NotificationStatusRepository notificationStatusRepository
    ) {
        this.notificationEmailRequestRepository = notificationEmailRequestRepository;
        this.notificationLetterRequestRepository = notificationLetterRequestRepository;
        this.notificationResponseRepository = notificationResponseRepository;
        this.notificationStatusRepository = notificationStatusRepository;
    }

    @Override
    public NotificationEmailRequest storeEmail(GovUkEmailDetailsRequest emailDetailsRequest) {
        return notificationEmailRequestRepository.save(new NotificationEmailRequest(null, emailDetailsRequest));
    }

    @Override
    public Optional<NotificationEmailRequest> getEmail(String id) {
        return notificationEmailRequestRepository.findById(id);
    }
    

    @Override
    public List<NotificationEmailRequest> findAllEmails() {
        return notificationEmailRequestRepository.findAll();
    }

    @Override
    public NotificationLetterRequest storeLetter(GovUkLetterDetailsRequest letterDetails) {
        return notificationLetterRequestRepository.save(new NotificationLetterRequest(null, letterDetails));
    }

    @Override
    public Optional<NotificationLetterRequest> getLetter(String id) {
        return notificationLetterRequestRepository.findById(id);
    }
    
    @Override
    public List<NotificationLetterRequest> findAllLetters() {
        return notificationLetterRequestRepository.findAll();
    }

    @Override
    public NotificationStatus updateStatus(NotificationStatus notificationStatus) {
        return notificationStatusRepository.save(notificationStatus);
    }
}
