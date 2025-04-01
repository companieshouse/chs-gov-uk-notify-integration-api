package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.api.NotificationSenderInterface;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.emailgovuknotifypayload.EmailGovUkNotifyPayloadInterface;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.lettergovuknotifypayload.LetterGovUkNotifyPayloadInterface;

@Controller
@Validated
public class SenderRestApi implements NotificationSenderInterface {

    EmailGovUkNotifyPayloadInterface emailGovUkNotifyPayload;
    LetterGovUkNotifyPayloadInterface letterGovUkNotifyPayload;

    public SenderRestApi(EmailGovUkNotifyPayloadInterface emailGovUkNotifyPayload, LetterGovUkNotifyPayloadInterface letterGovUkNotifyPayload) {
        this.emailGovUkNotifyPayload = emailGovUkNotifyPayload;
        this.letterGovUkNotifyPayload = letterGovUkNotifyPayload;
    }

    @Override
    public ResponseEntity<Void> sendEmail(@Valid GovUkEmailDetailsRequest govUkEmailDetailsRequest,
                                          @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String xHeaderId) {

        validateEmailInputs(govUkEmailDetailsRequest);

        emailGovUkNotifyPayload.sendEmail(govUkEmailDetailsRequest);

        throw new NotImplementedException();
    }

    private void validateEmailInputs(GovUkEmailDetailsRequest govUkEmailDetailsRequest) {
        if (govUkEmailDetailsRequest.getSenderDetails().getEmailAddress().length() == 0
            || govUkEmailDetailsRequest.getRecipientDetails().getEmailAddress().length() == 0) {
            throw new IllegalArgumentException("Sender Email Address is empty");
        }
    }

    @Override
    public ResponseEntity<Void> sendLetter(@RequestBody @Valid GovUkLetterDetailsRequest govUkLetterDetailsRequest,
                                           @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String xHeaderId) {

        //FIXME :  call letterGovUkNotifyPayload

        throw new NotImplementedException();
    }
}
