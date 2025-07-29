package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.createLogMap;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.IOUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.chs.notification.integration.api.NotifyIntegrationRetrieverControllerInterface;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
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

    @GetMapping(
            value = {"/gov-uk-notify-integration/letters/view/{reference}"},
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> viewLetterPdfByReference(
            final @PathVariable("reference") String reference) throws IOException {

        // TODO DEEP-428 Replace this PDF with one regenerated from retrieved letter data.
        var in = getClass().getResourceAsStream("/letter.pdf");

        return ResponseEntity
                .ok()
                .headers(suggestFilename(reference))
                .body(IOUtils.toByteArray(in));
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
