package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.createLogMap;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.io.IOUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.chs.notification.integration.api.NotifyIntegrationRetrieverControllerInterface;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.sentletterfetcher.SentLetterFetcher;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
public class ReaderRestApi implements NotifyIntegrationRetrieverControllerInterface {

    private static final String RETRIEVED = "Retrieved ";
    private static final String REFERENCE = "reference";
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private final NotificationDatabaseService notificationDatabaseService;
    private final SentLetterFetcher fetcher;

    public ReaderRestApi(final NotificationDatabaseService notificationDatabaseService,
                         final SentLetterFetcher fetcher) {
        this.notificationDatabaseService = notificationDatabaseService;
        this.fetcher = fetcher;
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
        logMap.put(REFERENCE, reference);
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
            final String contextId
    ) {
        Map<String, Object> logMap = createLogMap(contextId, "get_letter_by_reference");
        logMap.put(REFERENCE, reference);
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

    @Override
    public ResponseEntity<Object> viewLetterPdfByReference(
            final String reference,
            final String contextId) {

        var logMap = createLogMap(contextId, "view_letter_pdf");
        logMap.put(REFERENCE, reference);
        LOGGER.info("Starting viewLetterPdfByReference process", logMap);

        try {
            return ResponseEntity
                    .ok()
                    .headers(suggestFilename(reference))
                    .body(IOUtils.toByteArray(fetcher.fetchLetter(reference, contextId)));
        } catch (IOException ioe) {
            LOGGER.error("Failed to load precompiled letter PDF. Caught IOException: "
                    + ioe.getMessage(), createLogMap(contextId, "load_pdf_error"));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<Object> viewLetterPdf(
            final String pscName,
            final String companyNumber,
            final String templateId,
            final LocalDate letterSendingDate,
            final String contextId) {

        var logMap = createLogMap(contextId, "view_letter_pdf");
        logMap.put("psc_name", pscName);
        logMap.put("company_number", companyNumber);
        logMap.put("template_id", templateId);
        logMap.put("letter_sending_date", letterSendingDate.format(ISO_DATE));
        LOGGER.info("Starting viewLetterPdf process", logMap);

        try {
            return ResponseEntity
                .ok()
                .headers(suggestFilename(pscName + ":" + templateId + ":" + letterSendingDate))
                .body(IOUtils.toByteArray(
                        fetcher.fetchLetter(
                                pscName,
                                companyNumber,
                                templateId,
                                letterSendingDate,
                                contextId)));
        } catch (IOException ioe) {
            LOGGER.error("Failed to load precompiled letter PDF. Caught IOException: "
                    + ioe.getMessage(), logMap);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Suggests that the downloaded file be saved to the filename provided with '.pdf' as
     * the filename suffix.
     * @param filename the suggested filename
     * @return headers to be set in the response
     */
    private HttpHeaders suggestFilename(final String filename) {
        var contentDisposition = ContentDisposition.inline().filename(filename + ".pdf").build();
        var headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);
        return headers;
    }

}
