package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.api.NotificationSenderInterface;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.emailfacade.EmailFacadeInterface;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.lettergovuknotifypayload.LetterGovUkNotifyPayloadInterface;

import java.util.Map;

@Controller
@Validated
public class SenderRestApi implements NotificationSenderInterface {

    LetterGovUkNotifyPayloadInterface letterGovUkNotifyPayload;
    EmailFacadeInterface emailFacade;

    public SenderRestApi(LetterGovUkNotifyPayloadInterface letterGovUkNotifyPayload, EmailFacadeInterface emailFacade) {
        this.letterGovUkNotifyPayload = letterGovUkNotifyPayload;
        this.emailFacade = emailFacade;
    }

    @Override
    public ResponseEntity<Void> sendEmail(@Valid GovUkEmailDetailsRequest govUkEmailDetailsRequest , String xHeaderId) {
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
    public ResponseEntity<Void> sendLetter(@Valid GovUkLetterDetailsRequest govUkLetterDetailsRequest, String xHeaderId) {

        //FIXME :  call letterGovUkNotifyPayload

        throw new NotImplementedException();
    }
}
