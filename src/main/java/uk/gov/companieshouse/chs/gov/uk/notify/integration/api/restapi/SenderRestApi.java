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
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationEmailRequestRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.repository.NotificationResponseRepository;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;

@Controller
@Validated
public class SenderRestApi implements NotificationSenderInterface {

    private final NotificationDatabaseService notificationDatabaseService;
    
    public SenderRestApi(NotificationDatabaseService notificationDatabaseService) {
        this.notificationDatabaseService = notificationDatabaseService;
    }

    @Override
    public ResponseEntity<Void> sendEmail(@Valid GovUkEmailDetailsRequest govUkEmailDetailsRequest, String xHeaderId) {
        notificationDatabaseService.storeEmail(govUkEmailDetailsRequest);

        // make request to govuk notify
        throw new NotImplementedException();
    }

    @Override
    public ResponseEntity<Void> sendLetter(@Valid GovUkLetterDetailsRequest govUkLetterDetailsRequest, String xHeaderId) {
        notificationDatabaseService.storeLetter(govUkLetterDetailsRequest);

        // make request to govuk notify
        throw new NotImplementedException();
    }
}
