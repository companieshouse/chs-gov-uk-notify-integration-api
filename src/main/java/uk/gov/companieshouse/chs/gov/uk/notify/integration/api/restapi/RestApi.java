package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.api.NotificationSenderInterface;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;

@Controller
@Validated
public class RestApi implements NotificationSenderInterface {
    @Override
    public ResponseEntity<Void> sendEmail(@Valid GovUkEmailDetailsRequest govUkEmailDetailsRequest, String s) {
        return null;
    }

    @Override
    public ResponseEntity<Void> sendLetter(@Valid GovUkLetterDetailsRequest govUkLetterDetailsRequest, String s) {
        return null;
    }
}
