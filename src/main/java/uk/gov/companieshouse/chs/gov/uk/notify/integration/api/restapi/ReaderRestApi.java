package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.validation.constraints.Pattern;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.api.NotificationRetrievalInterface;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

@Controller
public class ReaderRestApi implements NotificationRetrievalInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);

    private final NotificationDatabaseService notificationDatabaseService;

    public ReaderRestApi(
            final NotificationDatabaseService notificationDatabaseService
    ) {
        this.notificationDatabaseService = notificationDatabaseService;
    }

    @Override
    public ResponseEntity<List<GovUkEmailDetailsRequest>> getAllEmails(
            @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") final String xRequestId
    ) {
        Map<String, Object> logMap = createLogMap(xRequestId, "get_all_emails");
        LOGGER.info("Retrieving all email notifications", logMap);

        List<NotificationEmailRequest> emails = notificationDatabaseService.findAllEmails();

        logMap.put("email_count", emails.size());
        LOGGER.info("Retrieved " + emails.size() + " email notifications", logMap);

        return new ResponseEntity<>(
                emails.stream()
                        .map(NotificationEmailRequest::request)
                        .toList(),
                HttpStatus.OK
        );
    }

    @Override
    public ResponseEntity<List<GovUkLetterDetailsRequest>> getAllLetters() {
        Map<String, Object> logMap = createLogMap("system", "get_all_letters");
        LOGGER.info("Retrieving all letter notifications", logMap);

        List<NotificationLetterRequest> letters = notificationDatabaseService.findAllLetters();

        logMap.put("letter_count", letters.size());
        LOGGER.info("Retrieved " + letters.size() + " letter notifications", logMap);

        return new ResponseEntity<>(
                letters.stream()
                        .map(NotificationLetterRequest::request)
                        .toList(),
                HttpStatus.OK
        );
    }

    @Override
    public ResponseEntity<GovUkEmailDetailsRequest> getEmailDetailsById(
            final String emailId,
            @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") final String xRequestId
    ) {
        Map<String, Object> logMap = createLogMap(xRequestId, "get_email_by_id");
        logMap.put("email_id", emailId);
        LOGGER.info("Retrieving email notification by ID: " + emailId, logMap);

        Optional<NotificationEmailRequest> emailRequest = notificationDatabaseService.getEmail(emailId);

        if (emailRequest.isPresent()) {
            LOGGER.info("Email notification found with ID: " + emailId, logMap);
            return new ResponseEntity<>(emailRequest.get().request(), HttpStatus.OK);
        } else {
            LOGGER.info("Email notification not found with ID: " + emailId, logMap);
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    // the method from the interface doesn't have reference in the path, so we can't access it properly
    @Override
    public ResponseEntity<List<GovUkEmailDetailsRequest>> getEmailDetailsByReference(
            final String reference,
            @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") final String xRequestId
    ) {
        Map<String, Object> logMap = createLogMap(xRequestId, "get_email_by_reference");
        logMap.put("reference", reference);
        LOGGER.info("Retrieving email notifications by reference: " + reference, logMap);

        List<NotificationEmailRequest> emails = notificationDatabaseService.getEmailByReference(reference);

        logMap.put("email_count", emails.size());
        LOGGER.info("Retrieved " + emails.size() + " email notifications with reference: " + reference, logMap);

        return new ResponseEntity<>(
                emails.stream()
                        .map(NotificationEmailRequest::request)
                        .toList(),
                HttpStatus.OK
        );
    }

    @Override
    public ResponseEntity<GovUkLetterDetailsRequest> getLetterDetails(
            final String xRequestId
    ) {
        Map<String, Object> logMap = createLogMap(xRequestId, "get_letter_details");
        LOGGER.info("Attempting to retrieve letter details without ID or reference", logMap);

        // Note: is something missing on the schema here? we don't have an id or anything, what letter details are we getting?
        LOGGER.debug("Letter details endpoint called without ID parameter - Schema may be incomplete", logMap);

        throw new NotImplementedException();
    }

    // the method from the interface doesn't have reference in the path, so we can't access it properly
    @Override
    public ResponseEntity<List<GovUkLetterDetailsRequest>> getLetterDetailsByReference(
            final String reference,
            @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") final String xRequestId
    ) {
        Map<String, Object> logMap = createLogMap(xRequestId, "get_letter_by_reference");
        logMap.put("reference", reference);
        LOGGER.info("Retrieving letter notifications by reference: " + reference, logMap);

        List<NotificationLetterRequest> letters = notificationDatabaseService.getLetterByReference(reference);

        logMap.put("letter_count", letters.size());
        LOGGER.info("Retrieved " + letters.size() + " letter notifications with reference: " + reference, logMap);

        return new ResponseEntity<>(
                letters.stream()
                        .map(NotificationLetterRequest::request)
                        .toList(),
                HttpStatus.OK
        );
    }

    private Map<String, Object> createLogMap(final String contextId, final String action) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("contextId", contextId);
        logMap.put("action", action);
        return logMap;
    }
}
