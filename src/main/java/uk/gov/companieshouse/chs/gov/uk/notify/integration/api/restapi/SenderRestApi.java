package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService.ECONOMY_POSTAGE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService.SECOND_CLASS_POSTAGE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CSIDVDEFLET;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CSIDVDEFLET_v1_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.IDVPSCDEFAULT;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.IDVPSCDEFAULT_v1_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.createLogMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.companieshouse.api.chs.notification.integration.api.NotifyIntegrationSenderControllerInterface;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.letterdispatcher.LetterDispatcher;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey;
import uk.gov.companieshouse.logging.Logger;

@Controller
public class SenderRestApi implements NotifyIntegrationSenderControllerInterface {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
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

        Map<String, ?> personalisationDetails;
        try {
            logger.debugContext( xHeaderId,"Parsing personalisation details", createLogMap(xHeaderId, "parse_details"));
            personalisationDetails = OBJECT_MAPPER.readValue(
                    govUkEmailDetailsRequest.getEmailDetails().getPersonalisationDetails(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );
        } catch (JsonProcessingException e) {
            logger.errorContext(xHeaderId, new Exception( "Failed to parse personalisation details: " + e.getMessage() ), createLogMap(xHeaderId, "parse_error"));
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

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
        var templateId = letterDetails.getTemplateId();
        /* This is a temporary block to send new CSIDV letters via economy before
         * IDV go live. In the future this block should be removed and the postage
         * should be read from the request object.
         */
        var postage = SECOND_CLASS_POSTAGE;
        if (
            List.of(CSIDVDEFLET, IDVPSCDEFAULT, CSIDVDEFLET_v1_1, IDVPSCDEFAULT_v1_1)
                    .contains(new LetterTemplateKey(appId, templateId))
        ) {
            postage = ECONOMY_POSTAGE;
        }
        var address = govUkLetterDetailsRequest.getRecipientDetails().getPhysicalAddress();
        var personalisationDetails = letterDetails.getPersonalisationDetails();

        try {
            var response = letterDispatcher.sendLetter(
                    postage,
                    reference,
                    appId,
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

}
