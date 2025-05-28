package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.companieshouse.api.chs.notification.integration.api.NotifyIntegrationRetrieverControllerInterface;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Controller
public class ReaderRestApi implements NotifyIntegrationRetrieverControllerInterface {

    private static final String RETRIEVED = "Retrieved ";
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private final NotificationDatabaseService notificationDatabaseService;

    public ReaderRestApi(final NotificationDatabaseService notificationDatabaseService) {
        this.notificationDatabaseService = notificationDatabaseService;
    }

    @Override
    public ResponseEntity<List<GovUkEmailDetailsRequest>> getAllEmails(
            final String xRequestId
    ) {
        Map<String, Object> logMap = createLogMap(xRequestId, "get_all_emails");
        LOGGER.info("Retrieving all email notifications", logMap);

        List<NotificationEmailRequest> emails = notificationDatabaseService.findAllEmails();

        logMap.put("email_count", emails.size());
        LOGGER.info(RETRIEVED + emails.size() + " email notifications", logMap);

        return new ResponseEntity<>(
                emails.stream()
                        .map(NotificationEmailRequest::getRequest)
                        .toList(),
                HttpStatus.OK
        );
    }

    @Override
    public ResponseEntity<GovUkEmailDetailsRequest> getEmailDetailsById(
            final String id,
            final String xRequestId
    ) {
        Map<String, Object> logMap = createLogMap(xRequestId, "get_email_by_id");
        logMap.put("email_id", id);
        LOGGER.info("Retrieving email notification by ID: " + id, logMap);

        Optional<NotificationEmailRequest> emailRequest = notificationDatabaseService.getEmail(id);

        if (emailRequest.isPresent()) {
            LOGGER.info("Email notification found with ID: " + id, logMap);
            return new ResponseEntity<>(emailRequest.get().getRequest(), HttpStatus.OK);
        } else {
            LOGGER.info("Email notification not found with ID: " + id, logMap);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<List<GovUkEmailDetailsRequest>> getEmailDetailsByReference(
            final String reference,
            final String xRequestId
    ) {
        Map<String, Object> logMap = createLogMap(xRequestId, "get_email_by_reference");
        logMap.put("reference", reference);
        LOGGER.info("Retrieving email notifications by reference: " + reference, logMap);

        List<NotificationEmailRequest> emails = notificationDatabaseService.getEmailByReference(reference);

        logMap.put("email_count", emails.size());
        LOGGER.info(RETRIEVED + emails.size() + " email notifications with reference: " + reference, logMap);

        return new ResponseEntity<>(
                emails.stream()
                        .map(NotificationEmailRequest::getRequest)
                        .toList(),
                HttpStatus.OK
        );
    }

    @Override
    public ResponseEntity<List<GovUkLetterDetailsRequest>> getAllLetters(
            final String xRequestId
    ) {
        Map<String, Object> logMap = createLogMap(xRequestId, "get_all_letters");
        LOGGER.info("Retrieving all letter notifications", logMap);

        List<NotificationLetterRequest> letters = notificationDatabaseService.findAllLetters();

        logMap.put("letter_count", letters.size());
        LOGGER.info(RETRIEVED + letters.size() + " letter notifications", logMap);

        return new ResponseEntity<>(
                letters.stream()
                        .map(NotificationLetterRequest::getRequest)
                        .toList(),
                HttpStatus.OK
        );
    }

    @Override
    public ResponseEntity<GovUkLetterDetailsRequest> getLetterDetailsById(
            final String id,
            final String xRequestId
    ) {

        Map<String, Object> logMap = createLogMap(xRequestId, "get_letter_by_id");
        logMap.put("letter_id", id);
        LOGGER.info("Retrieving letter notification by ID: " + id, logMap);

        Optional<NotificationLetterRequest> letterRequest = notificationDatabaseService.getLetter(id);

        if (letterRequest.isPresent()) {
            LOGGER.info("Letter notification found with ID: " + id, logMap);
            return new ResponseEntity<>(letterRequest.get().getRequest(), HttpStatus.OK);
        } else {
            LOGGER.info("Letter notification not found with ID: " + id, logMap);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @Override
    public ResponseEntity<List<GovUkLetterDetailsRequest>> getLetterDetailsByReference(
            final String reference,
            final String xRequestId
    ) {
        Map<String, Object> logMap = createLogMap(xRequestId, "get_letter_by_reference");
        logMap.put("reference", reference);
        LOGGER.info("Retrieving letter notifications by reference: " + reference, logMap);

        List<NotificationLetterRequest> letters = notificationDatabaseService.getLetterByReference(reference);

        logMap.put("letter_count", letters.size());
        LOGGER.info(RETRIEVED + letters.size() + " letter notifications with reference: " + reference, logMap);

        return new ResponseEntity<>(
                letters.stream()
                        .map(NotificationLetterRequest::getRequest)
                        .toList(),
                HttpStatus.OK
        );
    }

    private Map<String, Object> createLogMap(final String xRequestId, final String action) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("xRequestId", xRequestId);
        logMap.put("action", action);
        return logMap;
    }
}
