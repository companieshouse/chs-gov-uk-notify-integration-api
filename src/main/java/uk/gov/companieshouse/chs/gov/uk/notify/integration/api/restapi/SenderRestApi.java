package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.companieshouse.api.chs.notification.integration.api.NotifyIntegrationSenderControllerInterface;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;
import uk.gov.companieshouse.logging.Logger;

@Controller
public class SenderRestApi implements NotifyIntegrationSenderControllerInterface {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final GovUkNotifyService govUkNotifyService;
    private final NotificationDatabaseService notificationDatabaseService;
    private final Logger logger;

    public SenderRestApi(
            final GovUkNotifyService govUkNotifyService,
            final NotificationDatabaseService notificationDatabaseService,
            final Logger logger
    ) {
        this.govUkNotifyService = govUkNotifyService;
        this.notificationDatabaseService = notificationDatabaseService;
        this.logger = logger;
    }

    @Override
    public ResponseEntity<Void> sendEmail(
            @Valid final GovUkEmailDetailsRequest govUkEmailDetailsRequest,
            @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") final String xHeaderId
    ) {
        Map<String, Object> logMap = createLogMap("", "letter_send");
        logMap.put("govUkEmailDetailsRequest", govUkEmailDetailsRequest.toString());

        logger.info("Starting sendEmail process", createLogMap(xHeaderId, "email_send_start"));

        Map<String, ?> personalisationDetails;
        try {
            logger.debug("Parsing personalisation details", createLogMap(xHeaderId, "parse_details"));
            personalisationDetails = OBJECT_MAPPER.readValue(
                    govUkEmailDetailsRequest.getEmailDetails().getPersonalisationDetails(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse personalisation details: " + e.getMessage(), createLogMap(xHeaderId, "parse_error"));
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        logger.debug("Storing email request in database", createLogMap(xHeaderId, "store_email"));
        notificationDatabaseService.storeEmail(govUkEmailDetailsRequest);

        logger.info("Sending email to " + govUkEmailDetailsRequest.getRecipientDetails().getEmailAddress(),
                createLogMap(xHeaderId, "send_email"));

        var emailResp = govUkNotifyService.sendEmail(
                govUkEmailDetailsRequest.getRecipientDetails().getEmailAddress(),
                govUkEmailDetailsRequest.getEmailDetails().getTemplateId(),
                personalisationDetails
        );

        logger.debug("Storing email response in database", createLogMap(xHeaderId, "store_response"));
        notificationDatabaseService.storeResponse(emailResp);

        if (emailResp.success()) {
            logger.info("Email sent successfully", createLogMap(xHeaderId, "email_success"));
            return new ResponseEntity<>(HttpStatus.CREATED);
        } else {
            logger.error("Failed to send email", createLogMap(xHeaderId, "email_failure"));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<Void> sendLetter(
            @Valid final GovUkLetterDetailsRequest govUkLetterDetailsRequest,
            @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") final String contextId
    ) {
        Map<String, Object> logMap = createLogMap(contextId, "letter_send");
        logMap.put("govUkLetterDetailsRequest", govUkLetterDetailsRequest.toString());

        logger.info("Starting sendLetter process", logMap);

        logger.debug("Storing letter request in database", createLogMap(contextId, "store_letter"));
        notificationDatabaseService.storeLetter(govUkLetterDetailsRequest);

        logger.info("Processing letter for "
                        + govUkLetterDetailsRequest.getRecipientDetails().getName(),
                createLogMap(contextId, "process_letter"));

        try (var precompiledPdf = getPrecompiledPdf()) {

            var letterResp =
                    govUkNotifyService.sendLetter(
                            govUkLetterDetailsRequest.getSenderDetails().getReference(),
                            precompiledPdf);

            logger.debug("Storing letter response in database",
                    createLogMap(contextId, "store_letter_response"));
            notificationDatabaseService.storeResponse(letterResp);

            if (letterResp.success()) {
                logger.info("Letter processed successfully",
                        createLogMap(contextId, "letter_success"));
                return new ResponseEntity<>(HttpStatus.CREATED);
            } else {
                logger.error("Failed to process letter", createLogMap(contextId, "letter_failure"));
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (IOException ioe) {
            logger.error("Failed to load precompiled letter PDF. Caught IOException: "
                            + ioe.getMessage(), createLogMap(contextId, "load_pdf_error"));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @SuppressWarnings("java:S1135") // TODO left in place intentionally for now.
    InputStream getPrecompiledPdf() {
        // TODO DEEP-288 Replace temporary test code and remove Demonstrate connectivity.pdf.
        return getClass().getClassLoader().getResourceAsStream("Demonstrate connectivity.pdf");
    }

    private Map<String, Object> createLogMap(final String contextId, final String action) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("contextId", contextId);
        logMap.put("action", action);
        return logMap;
    }

}
