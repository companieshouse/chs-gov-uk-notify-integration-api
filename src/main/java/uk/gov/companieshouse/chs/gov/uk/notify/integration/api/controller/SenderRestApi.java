package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.controller;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.api.NotificationSenderInterface;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.util.GovUKNotifyEmailHelper;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.NotificationDatabaseService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.logging.util.DataMap;

import static org.springframework.http.HttpStatus.CREATED;

@Controller
@Validated
public class SenderRestApi implements NotificationSenderInterface {

    private final Logger logger = LoggerFactory.getLogger(SenderRestApi.class.getName());
    
    private final GovUKNotifyEmailHelper govUKNotifyEmailHelper;
    private final NotificationDatabaseService notificationDatabaseService;

    public SenderRestApi(GovUKNotifyEmailHelper govUKNotifyEmailHelper,
                         NotificationDatabaseService notificationDatabaseService) {
        this.govUKNotifyEmailHelper = govUKNotifyEmailHelper;
        this.notificationDatabaseService = notificationDatabaseService;
    }

    @Override
    public ResponseEntity<Void> sendEmail(@Valid GovUkEmailDetailsRequest govUkEmailDetailsRequest ,
                                          @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String xHeaderId) {
        
        // should personalisation detailas be coming in as a map, not a json string? will need to change
        // the other modules to account for this.
        Map<String, Object> personilisationDetails = null;
        try {
            personilisationDetails = new ObjectMapper().readValue(govUkEmailDetailsRequest.getEmailDetails().getPersonalisationDetails(), Map.class);
        } catch (JsonProcessingException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        notificationDatabaseService.storeEmail(govUkEmailDetailsRequest);
        
        // instead of a boolean, get the response back so we can save it
        boolean successful = govUKNotifyEmailHelper.sendEmail(govUkEmailDetailsRequest.getRecipientDetails().getEmailAddress(),
                govUkEmailDetailsRequest.getEmailDetails().getTemplateId(),
                personilisationDetails);
        
        return new ResponseEntity<>(successful ? HttpStatus.CREATED : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<Void> sendLetter(@Valid GovUkLetterDetailsRequest govUkLetterDetailsRequest,
                                           @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String xHeaderId) {

        logger.info("sendLetter(" + govUkLetterDetailsRequest + ", " + xHeaderId + ")", getLogMap(xHeaderId));

        // todo, other letter stuff?
        notificationDatabaseService.storeLetter(govUkLetterDetailsRequest);

        return ResponseEntity.status(CREATED).build();
    }

    private Map<String, Object> getLogMap(final String contextId) {
        return new DataMap.Builder()
                .contextId(contextId)
                .build()
                .getLogMap();
    }
}
