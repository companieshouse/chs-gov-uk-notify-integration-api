package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import jakarta.validation.Valid;
import org.apache.commons.lang.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.api.NotificationSenderInterface;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.lettergovuknotifypayload.LetterGovUkNotifyPayloadInterface;
import org.apache.logging.log4j.Logger;

@Controller
@Validated
public class SenderRestApi implements NotificationSenderInterface {

    private static final Logger logger = LogManager.getLogger(SenderRestApi.class);
    EmailGovUkNotifyPayloadInterface emailGovUkNotifyPayload;
    LetterGovUkNotifyPayloadInterface letterGovUkNotifyPayload;

    public SenderRestApi(LetterGovUkNotifyPayloadInterface letterGovUkNotifyPayload) {
        this.letterGovUkNotifyPayload = letterGovUkNotifyPayload;
    }

    @Override
    public ResponseEntity<Void> sendEmail(@Valid GovUkEmailDetailsRequest govUkEmailDetailsRequest, String xHeaderId) {

        validateEmailRequest(govUkEmailDetailsRequest);
        validateEmailInputs(govUkEmailDetailsRequest);

        emailGovUkNotifyPayload.sendEmail(govUkEmailDetailsRequest);
        logger.info("Received request to send an email");
        return new ResponseEntity<>(HttpStatus.CREATED);
        // notifyEmailFacade.sendEmail ...

        throw new NotImplementedException();
    }

    private void validateEmailInputs(GovUkEmailDetailsRequest govUkEmailDetailsRequest) {
        if (govUkEmailDetailsRequest.getSenderDetails().getEmailAddress().isEmpty()
            || govUkEmailDetailsRequest.getRecipientDetails().getEmailAddress().isEmpty()) {
            logger.error("Request missing email address");
            throw new IllegalArgumentException("Sender Email Address is empty");
        }
        new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
    private void validateEmailRequest(GovUkEmailDetailsRequest govUkEmailDetailsRequest) {
        if (govUkEmailDetailsRequest.getSenderDetails() == null || govUkEmailDetailsRequest.getEmailDetails() == null
                || govUkEmailDetailsRequest.getRecipientDetails() == null) {
            logger.error("Request has null field/s");
            throw new NullPointerException("Sender request has null fields");
        }
        new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    @Override
    public ResponseEntity<Void> sendLetter(@Valid GovUkLetterDetailsRequest govUkLetterDetailsRequest, String xHeaderId) {

        //FIXME :  call letterGovUkNotifyPayload

        throw new NotImplementedException();
    }
}
