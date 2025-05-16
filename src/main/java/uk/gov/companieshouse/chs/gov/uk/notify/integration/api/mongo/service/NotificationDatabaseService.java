package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailResponse;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterResponse;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationStatus;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailResponseRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterResponseRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationStatusRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;


@Service
public class NotificationDatabaseService {

    private final NotificationEmailRequestRepository notificationEmailRequestRepository;
    private final NotificationEmailResponseRepository notificationEmailResponseRepository;
    private final NotificationLetterRequestRepository notificationLetterRequestRepository;
    private final NotificationLetterResponseRepository notificationLetterResponseRepository;
    private final NotificationStatusRepository notificationStatusRepository; // not in mvp, will be done later

    public NotificationDatabaseService(
            final NotificationEmailRequestRepository notificationEmailRequestRepository,
            final NotificationEmailResponseRepository notificationEmailResponseRepository,
            final NotificationLetterRequestRepository notificationLetterRequestRepository,
            final NotificationLetterResponseRepository notificationLetterResponseRepository,
            final NotificationStatusRepository notificationStatusRepository
    ) {
        this.notificationEmailRequestRepository = notificationEmailRequestRepository;
        this.notificationLetterRequestRepository = notificationLetterRequestRepository;
        this.notificationEmailResponseRepository = notificationEmailResponseRepository;
        this.notificationLetterResponseRepository = notificationLetterResponseRepository;
        this.notificationStatusRepository = notificationStatusRepository;
    }

    public NotificationEmailRequest storeEmail(final GovUkEmailDetailsRequest emailDetailsRequest) {
        return notificationEmailRequestRepository.save(new NotificationEmailRequest(LocalDateTime.now(), LocalDateTime.now().plusHours(1), emailDetailsRequest, null));
    }

    public Optional<NotificationEmailRequest> getEmail(final String id) {
        return notificationEmailRequestRepository.findById(id);
    }

    public List<NotificationEmailRequest> getEmailByReference(final String reference) {
        return notificationEmailRequestRepository.findByReference(reference);
    }

    public List<NotificationEmailRequest> findAllEmails() {
        return notificationEmailRequestRepository.findAll();
    }

    public NotificationLetterRequest storeLetter(final GovUkLetterDetailsRequest letterDetails) {
        return notificationLetterRequestRepository.save(new NotificationLetterRequest(LocalDateTime.now(), LocalDateTime.now().plusHours(1), letterDetails, null));
    }

    public Optional<NotificationLetterRequest> getLetter(final String letterId) {
        return notificationLetterRequestRepository.findById(letterId);
    }

    public List<NotificationLetterRequest> getLetterByReference(final String reference) {
        return notificationLetterRequestRepository.findByReference(reference);
    }
    
    public List<NotificationLetterRequest> findAllLetters() {
        return notificationLetterRequestRepository.findAll();
    }

    public NotificationEmailResponse storeResponse(final GovUkNotifyService.EmailResp emailResp) {
        return notificationEmailResponseRepository.save(new NotificationEmailResponse(LocalDateTime.now(), LocalDateTime.now().plusHours(1), emailResp.response(), null));
    }

    public NotificationLetterResponse storeResponse(final GovUkNotifyService.LetterResp letterResp) {
        return notificationLetterResponseRepository.save(new NotificationLetterResponse(LocalDateTime.now(), LocalDateTime.now().plusHours(1), letterResp.response(), null));
    }

    public NotificationStatus updateStatus(final NotificationStatus notificationStatus) {
        return notificationStatusRepository.save(notificationStatus);
    }

}
