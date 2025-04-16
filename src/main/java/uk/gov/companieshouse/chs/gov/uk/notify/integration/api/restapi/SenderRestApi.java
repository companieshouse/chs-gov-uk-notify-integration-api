package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.companieshouse.api.chs.notification.integration.api.NotifyIntegrationSenderControllerInterface;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

@Controller
public class SenderRestApi implements NotifyIntegrationSenderControllerInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);

    private final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final GovUkNotifyService govUkNotifyService;
    private final NotificationDatabaseService notificationDatabaseService;

    public SenderRestApi(
            final GovUkNotifyService govUkNotifyService,
            final NotificationDatabaseService notificationDatabaseService
    ) {
        this.govUkNotifyService = govUkNotifyService;
        this.notificationDatabaseService = notificationDatabaseService;
    }

    @Override
    public ResponseEntity<Void> sendEmail(
            @Valid final GovUkEmailDetailsRequest govUkEmailDetailsRequest,
            @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") final String xHeaderId
    ) {
        Map<String, Object> logMap = createLogMap("", "letter_send");
        logMap.put("govUkEmailDetailsRequest", govUkEmailDetailsRequest.toString());

        LOGGER.info("Starting sendEmail process", createLogMap(xHeaderId, "email_send_start"));

        Map<String, ?> personalisationDetails;
        try {
            LOGGER.debug("Parsing personalisation details", createLogMap(xHeaderId, "parse_details"));
            personalisationDetails = OBJECT_MAPPER.readValue(
                    govUkEmailDetailsRequest.getEmailDetails().getPersonalisationDetails(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to parse personalisation details: " + e.getMessage(), createLogMap(xHeaderId, "parse_error"));
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        LOGGER.debug("Storing email request in database", createLogMap(xHeaderId, "store_email"));
        notificationDatabaseService.storeEmail(govUkEmailDetailsRequest);

        LOGGER.info("Sending email to " + govUkEmailDetailsRequest.getRecipientDetails().getEmailAddress(),
                createLogMap(xHeaderId, "send_email"));

        var emailResp = govUkNotifyService.sendEmail(
                govUkEmailDetailsRequest.getRecipientDetails().getEmailAddress(),
                govUkEmailDetailsRequest.getEmailDetails().getTemplateId(),
                personalisationDetails
        );

        LOGGER.debug("Storing email response in database", createLogMap(xHeaderId, "store_response"));
        notificationDatabaseService.storeResponse(emailResp);

        if (emailResp.success()) {
            LOGGER.info("Email sent successfully", createLogMap(xHeaderId, "email_success"));
            return new ResponseEntity<>(HttpStatus.CREATED);
        } else {
            LOGGER.error("Failed to send email", createLogMap(xHeaderId, "email_failure"));
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

        LOGGER.info("Starting sendLetter process", logMap);

        LOGGER.debug("Storing letter request in database", createLogMap(contextId, "store_letter"));
        notificationDatabaseService.storeLetter(govUkLetterDetailsRequest);

        LOGGER.info("Processing letter for " + govUkLetterDetailsRequest.getRecipientDetails().getName(),
                createLogMap(contextId, "process_letter"));

        var letterResp = new GovUkNotifyService.LetterResp(true, null);
        // hardcoded for now, may eventually use result of below (depending on implementation)
        // GovUkNotifyService.EmailResp emailResponse = govUkNotifyService.sendLetter(
        //     govUkLetterDetailsRequest.getRecipientDetails().getName(),
        //     new File(),
        // );

        LOGGER.debug("Storing letter response in database", createLogMap(contextId, "store_letter_response"));
        notificationDatabaseService.storeResponse(letterResp);

        if (letterResp.success()) {
            LOGGER.info("Letter processed successfully", createLogMap(contextId, "letter_success"));
            return new ResponseEntity<>(HttpStatus.CREATED);
        } else {
            LOGGER.error("Failed to process letter", createLogMap(contextId, "letter_failure"));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, Object> createLogMap(final String contextId, final String action) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("contextId", contextId);
        logMap.put("action", action);
        return logMap;
    }
}
