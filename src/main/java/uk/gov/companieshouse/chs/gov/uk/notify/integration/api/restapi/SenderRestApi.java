package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.createLogMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.companieshouse.api.chs.notification.integration.api.NotifyIntegrationSenderControllerInterface;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.letterdispatcher.LetterDispatcher;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.Postage;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation.WelshDatesPublisher;
import uk.gov.companieshouse.logging.Logger;

@Controller
public class SenderRestApi implements NotifyIntegrationSenderControllerInterface {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Set of letters that should be sent using second class postage
     */
    private static final Set<LetterTemplateKey> SECOND_CLASS_LETTERS = Set.of(
            LetterTemplateKey.CHIPS_DIRECTION_LETTER_1,
            LetterTemplateKey.CHIPS_NEW_PSC_DIRECTION_LETTER_1,
            LetterTemplateKey.CHIPS_TRANSITIONAL_NON_DIRECTOR_PSC_INFORMATION_LETTER_1,
            LetterTemplateKey.CHIPS_EXTENSION_ACCEPTANCE_LETTER_1,
            LetterTemplateKey.CHIPS_SECOND_EXTENSION_ACCEPTANCE_LETTER_1);

    private final GovUkNotifyService govUkNotifyService;
    private final NotificationDatabaseService notificationDatabaseService;
    private final LetterDispatcher letterDispatcher;
    private final Logger logger;

    public SenderRestApi(
            final GovUkNotifyService govUkNotifyService,
            final NotificationDatabaseService notificationDatabaseService,
            final LetterDispatcher letterDispatcher,
            final Logger logger
    ) {
        this.govUkNotifyService = govUkNotifyService;
        this.notificationDatabaseService = notificationDatabaseService;
        this.letterDispatcher = letterDispatcher;
        this.logger = logger;
    }

    @Override
    public ResponseEntity<Void> sendEmail(
            @Valid final GovUkEmailDetailsRequest govUkEmailDetailsRequest,
            @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") final String xHeaderId
    ) {
        Map<String, Object> logMap = createLogMap("", "letter_send");
        logMap.put("govUkEmailDetailsRequest", govUkEmailDetailsRequest.toString());

        logger.infoContext(xHeaderId, "Starting sendEmail process", createLogMap(xHeaderId, "email_send_start"));

        Map<String, String> personalisationDetails;
        try {
            logger.debugContext( xHeaderId,"Parsing personalisation details", createLogMap(xHeaderId, "parse_details"));
            personalisationDetails = OBJECT_MAPPER.readValue(
                    govUkEmailDetailsRequest.getEmailDetails().getPersonalisationDetails(),
                    new TypeReference<Map<String, String>>() {
                    }
            );
        } catch (JsonProcessingException e) {
            logger.errorContext(xHeaderId, new Exception( "Failed to parse personalisation details: " + e.getMessage() ), createLogMap(xHeaderId, "parse_error"));
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        WelshDatesPublisher.publishWelshDates(personalisationDetails);

        logger.debugContext(xHeaderId,"Storing email request in database", createLogMap(xHeaderId, "store_email"));
        notificationDatabaseService.storeEmail(govUkEmailDetailsRequest);

        logger.infoContext(xHeaderId, "Sending email to " + govUkEmailDetailsRequest.getRecipientDetails().getEmailAddress(),
                createLogMap(xHeaderId, "send_email"));

        var emailResp = govUkNotifyService.sendEmail(
                govUkEmailDetailsRequest.getRecipientDetails().getEmailAddress(),
                govUkEmailDetailsRequest.getEmailDetails().getTemplateId(),
                govUkEmailDetailsRequest.getSenderDetails().getReference(),
                personalisationDetails
        );

        logger.debugContext(xHeaderId, "Storing email response in database", createLogMap(xHeaderId, "store_response"));
        notificationDatabaseService.storeResponse(emailResp);

        if (emailResp.success()) {
            logger.infoContext(xHeaderId, "Email sent successfully", createLogMap(xHeaderId, "email_success"));
            return new ResponseEntity<>(HttpStatus.CREATED);
        } else {
            logger.errorContext(xHeaderId, new Exception( "Failed to send email" ), createLogMap(xHeaderId, "email_failure"));
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

        logger.infoContext( contextId,"Starting sendLetter process", logMap );

        logger.debugContext( contextId, "Storing letter request in database", createLogMap(contextId, "store_letter"));
        notificationDatabaseService.storeLetter(govUkLetterDetailsRequest);

        logger.infoContext( contextId, "Processing letter for "
                        + govUkLetterDetailsRequest.getRecipientDetails().getName(),
                createLogMap(contextId, "process_letter"));

        var senderDetails = govUkLetterDetailsRequest.getSenderDetails();
        var reference = senderDetails.getReference();
        var appId = senderDetails.getAppId();
        var letterDetails = govUkLetterDetailsRequest.getLetterDetails();
        var letterId = letterDetails.getLetterId();
        var templateId = letterDetails.getTemplateId();
        var postage = determinePostage(appId, letterId, templateId);
        var address = govUkLetterDetailsRequest.getRecipientDetails().getPhysicalAddress();
        var personalisationDetails = letterDetails.getPersonalisationDetails();

        try {
            var response = letterDispatcher.sendLetter(
                    postage,
                    reference,
                    appId,
                    letterId,
                    templateId,
                    address,
                    personalisationDetails,
                    contextId
                    );
            if (response.success()) {
                logger.infoContext(contextId, "Letter processed successfully",
                        createLogMap(contextId, "letter_success"));
                return new ResponseEntity<>(HttpStatus.CREATED);
            } else {
                logger.errorContext( contextId, new Exception("Failed to process letter"), createLogMap(contextId, "letter_failure"));
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (IOException ioe) {
            logger.errorContext( contextId, new Exception( "Failed to load precompiled letter PDF. Caught IOException: "
                    + ioe.getMessage()), createLogMap(contextId, "load_pdf_error"));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private Postage determinePostage(final String appId, final String letterId, final String templateId) {
        var letterTemplateKey = new LetterTemplateKey(appId, letterId, templateId);
        if (SECOND_CLASS_LETTERS.contains(letterTemplateKey)) {
            return Postage.SECOND_CLASS;
        } else {
            // Default to economy mail
            return Postage.ECONOMY;
        }
    }
}
