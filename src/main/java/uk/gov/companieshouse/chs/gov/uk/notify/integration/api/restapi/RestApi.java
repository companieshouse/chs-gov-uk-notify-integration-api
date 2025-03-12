package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import jakarta.validation.Valid;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.api.NotificationSenderInterface;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.emailgovuknotifypayload.EmailGovUkNotifyPayloadInterface;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.lettergovuknotifypayload.LetterGovUkNotifyPayloadInterface;

@Controller
@Validated
public class RestApi implements NotificationSenderInterface {

    EmailGovUkNotifyPayloadInterface emailGovUkNotifyPayload;
    LetterGovUkNotifyPayloadInterface letterGovUkNotifyPayload;

    public RestApi(EmailGovUkNotifyPayloadInterface emailGovUkNotifyPayload, LetterGovUkNotifyPayloadInterface letterGovUkNotifyPayload) {
        this.emailGovUkNotifyPayload = emailGovUkNotifyPayload;
        this.letterGovUkNotifyPayload = letterGovUkNotifyPayload;
    }

    @Override
    public ResponseEntity<Void> sendEmail(@Valid GovUkEmailDetailsRequest govUkEmailDetailsRequest, String xHeaderId) {

        //FIXME :  call emailGovUkNotifyPayload

        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Void> sendLetter(@Valid GovUkLetterDetailsRequest govUkLetterDetailsRequest, String xHeaderId) {

        //FIXME :  call letterGovUkNotifyPayload

        throw new NotImplementedException();
    }
}
