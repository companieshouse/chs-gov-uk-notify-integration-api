package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;


import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.NotificationStatus;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.dao.NotificationEmailRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.dao.NotificationLetterRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.dao.NotificationResponseRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.dao.NotificationStatusRepository;


@Service
public class NotificationDatabaseService {

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

    public NotificationEmailRequest storeEmail(GovUkEmailDetailsRequest emailDetailsRequest) {
        return notificationEmailRequestRepository.save(new NotificationEmailRequest(null, emailDetailsRequest));
    }

    public Optional<NotificationEmailRequest> getEmail(String id) {
        return notificationEmailRequestRepository.findById(id);
    }

    public List<NotificationEmailRequest> findAllEmails() {
        return notificationEmailRequestRepository.findAll();
    }

    public NotificationLetterRequest storeLetter(GovUkLetterDetailsRequest letterDetails) {
        return notificationLetterRequestRepository.save(new NotificationLetterRequest(null, letterDetails));
    }

    public Optional<NotificationLetterRequest> getLetter(String id) {
        return notificationLetterRequestRepository.findById(id);
    }
    
    public List<NotificationLetterRequest> findAllLetters() {
        return notificationLetterRequestRepository.findAll();
    }

    public NotificationStatus updateStatus(NotificationStatus notificationStatus) {
        return notificationStatusRepository.save(notificationStatus);
    }
}
