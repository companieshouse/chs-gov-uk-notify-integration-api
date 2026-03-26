package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationEmailResponse;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.NotificationLetterResponse;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailResponseRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterResponseRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;

@Service
public class NotificationDatabaseService {

    private final NotificationEmailRequestRepository notificationEmailRequestRepository;
    private final NotificationEmailResponseRepository notificationEmailResponseRepository;
    private final NotificationLetterRequestRepository notificationLetterRequestRepository;
    private final NotificationLetterResponseRepository notificationLetterResponseRepository;

    public NotificationDatabaseService(
            final NotificationEmailRequestRepository notificationEmailRequestRepository,
            final NotificationEmailResponseRepository notificationEmailResponseRepository,
            final NotificationLetterRequestRepository notificationLetterRequestRepository,
            final NotificationLetterResponseRepository notificationLetterResponseRepository
    ) {
        this.notificationEmailRequestRepository = notificationEmailRequestRepository;
        this.notificationLetterRequestRepository = notificationLetterRequestRepository;
        this.notificationEmailResponseRepository = notificationEmailResponseRepository;
        this.notificationLetterResponseRepository = notificationLetterResponseRepository;
    }

    @Transactional( readOnly = true )
    public Optional<NotificationEmailRequest> getEmail(final String id) {
        return notificationEmailRequestRepository.findById(id);
    }

    @Transactional( readOnly = true )
    public Optional<NotificationEmailRequest> getEmail(final String appId, final String reference) {
        return notificationEmailRequestRepository.findByUniqueReference(appId, reference);
    }

    @Transactional( readOnly = true )
    public List<NotificationEmailRequest> getEmailByReference(final String reference) {
        return notificationEmailRequestRepository.findByReference(reference);
    }

    @Transactional( readOnly = true )
    public List<NotificationEmailRequest> findAllEmails() {
        return notificationEmailRequestRepository.findAll();
    }

    @Transactional( readOnly = true )
    public Optional<NotificationLetterRequest> getLetter(final String letterId) {
        return notificationLetterRequestRepository.findById(letterId);
    }

    @Transactional( readOnly = true )
    public Optional<NotificationLetterRequest> getLetter(final String appId, final String reference) {
        return notificationLetterRequestRepository.findByUniqueReference(appId, reference);
    }

    @Transactional( readOnly = true )
    public List<NotificationLetterRequest> getLetterByReference(final String reference) {
        return notificationLetterRequestRepository.findByReference(reference);
    }

    @Transactional( readOnly = true )
    public List<NotificationLetterRequest> findAllLetters() {
        return notificationLetterRequestRepository.findAll();
    }

    @Transactional()
    public NotificationEmailResponse storeResponse(final GovUkNotifyService.EmailResp emailResp) {
        return notificationEmailResponseRepository.save(new NotificationEmailResponse(null, null, emailResp.response(), null));
    }

    @Transactional()
    public NotificationLetterResponse storeResponse(final GovUkNotifyService.LetterResp letterResp) {
        return notificationLetterResponseRepository.save(new NotificationLetterResponse(null, null, letterResp.response(), null));
    }

    @Transactional
    public NotificationEmailRequest saveEmail(final NotificationEmailRequest request) {
        return notificationEmailRequestRepository.save(request);
    }

    @Transactional
    public NotificationLetterRequest saveLetter(final NotificationLetterRequest request) {
        return notificationLetterRequestRepository.save(request);
    }
}
