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

    public SenderRestApi(LetterGovUkNotifyPayloadInterface letterGovUkNotifyPayload,
                         Logger logger) {
        this.letterGovUkNotifyPayload = letterGovUkNotifyPayload;
        this.logger = logger;
    }

    @Override
    public ResponseEntity<Void> sendEmail(@Valid GovUkEmailDetailsRequest govUkEmailDetailsRequest,
                                          @Pattern(regexp = "[0-9A-Za-z-_]{8,32}") String xHeaderId) {

        // notifyEmailFacade.sendEmail ...

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
