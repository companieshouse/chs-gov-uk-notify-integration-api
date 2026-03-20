package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailResponse;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterResponse;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailResponseRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationLetterResponseRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;

@Service
public class NotificationDatabaseService {

    private static final String UTC_TIMEZONE_SUFFIX = "T00:00:00.000Z";

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
    public Page<NotificationLetterRequest> getLetterByReference(final String reference,
                                                                final int letterNumber) {
        return notificationLetterRequestRepository.findByReference(reference,
                PageRequest.of(letterNumber - 1, 1));
    }

    @Transactional( readOnly = true )
    public List<NotificationLetterRequest> getLettersByPscNameOrLetterAndCompanyTemplateDate(
            final String pscName,
            final String companyNumber,
            final String letterId,
            final String templateId,
            final LocalDate letterSendingDate) {
        var nextDay = letterSendingDate.plusDays(1);
        return notificationLetterRequestRepository.findByPscNameOrLetterAndCompanyTemplateDate(
                pscName,
                companyNumber,
                // prevents null letter ID matching on all non-compliance letters
                letterId == null ? "" : letterId,
                templateId,
                letterSendingDate + UTC_TIMEZONE_SUFFIX,
                nextDay + UTC_TIMEZONE_SUFFIX);
    }

    @Transactional( readOnly = true )
    public Page<NotificationLetterRequest> getLettersByPscNameOrLetterAndCompanyTemplateDate(
            final String pscName,
            final String companyNumber,
            final String letterId,
            final String templateId,
            final LocalDate letterSendingDate,
            final int letterNumber) {
        var nextDay = letterSendingDate.plusDays(1);
        return notificationLetterRequestRepository.findByPscNameOrLetterAndCompanyTemplateDate(
                pscName,
                companyNumber,
                // prevents null letter ID matching on all non-compliance letters
                letterId == null ? "" : letterId,
                templateId,
                letterSendingDate + UTC_TIMEZONE_SUFFIX,
                nextDay + UTC_TIMEZONE_SUFFIX,
                PageRequest.of(letterNumber - 1, 1));
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

}
