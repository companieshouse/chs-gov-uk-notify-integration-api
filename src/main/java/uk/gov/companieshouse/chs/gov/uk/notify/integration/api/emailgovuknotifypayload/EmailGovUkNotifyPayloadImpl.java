package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.emailgovuknotifypayload;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;

@Component
class EmailGovUkNotifyPayloadImpl implements EmailGovUkNotifyPayloadInterface {
    /**
     * Manages the process of sending an email
     *
     * @param govUkEmailDetailsRequest the details of the email to be sent
     */
    @Override
    public void sendEmail(GovUkEmailDetailsRequest govUkEmailDetailsRequest) {

//        templateLookup.retrieveEmailTemplate();
//
//        templatePersonalisation.personaliseEmailTemplate();
//
//        govUkConnection.sendEmail();

        throw new NotImplementedException();
    }
}
