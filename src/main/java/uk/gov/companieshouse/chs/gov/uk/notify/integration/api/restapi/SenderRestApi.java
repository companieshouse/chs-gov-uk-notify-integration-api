package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

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
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.emailfacade.EmailFacadeInterface;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.lettergovuknotifypayload.LetterGovUkNotifyPayloadInterface;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.util.DataMap;

import java.util.Map;

import static org.springframework.http.HttpStatus.CREATED;

@Controller
@Validated
public class SenderRestApi implements NotificationSenderInterface {

    private final LetterGovUkNotifyPayloadInterface letterGovUkNotifyPayload;
    private final Logger logger;
    EmailFacadeInterface emailFacade;

    public SenderRestApi(LetterGovUkNotifyPayloadInterface letterGovUkNotifyPayload,
                         Logger logger, EmailFacadeInterface emailFacade) {
        this.letterGovUkNotifyPayload = letterGovUkNotifyPayload;
        this.logger = logger;
        this.emailFacade = emailFacade;
    }

    @Override
    public ResponseEntity<Void> sendEmail(@Valid GovUkEmailDetailsRequest govUkEmailDetailsRequest ,
                                          @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String xHeaderId) {
        Map<String, Object> personilisationDetails = null;
        try {
            personilisationDetails = new ObjectMapper().readValue(govUkEmailDetailsRequest.getEmailDetails().getPersonalisationDetails(), Map.class);
        } catch (JsonProcessingException e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        boolean successful = emailFacade.sendEmail(govUkEmailDetailsRequest.getRecipientDetails().getEmailAddress(),
                govUkEmailDetailsRequest.getEmailDetails().getTemplateId(),
                personilisationDetails);

        return new ResponseEntity<>(successful ? HttpStatus.CREATED : HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Override
    public ResponseEntity<Void> sendLetter(@Valid GovUkLetterDetailsRequest govUkLetterDetailsRequest,
                                           @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String xHeaderId) {

        logger.info("sendLetter(" + govUkLetterDetailsRequest + ", " + xHeaderId + ")", getLogMap(xHeaderId));

        //FIXME :  call letterGovUkNotifyPayload

        return ResponseEntity.status(CREATED).build();
    }

    private Map<String, Object> getLogMap(final String contextId) {
        return new DataMap.Builder()
                .contextId(contextId)
                .build()
                .getLogMap();
    }
}
