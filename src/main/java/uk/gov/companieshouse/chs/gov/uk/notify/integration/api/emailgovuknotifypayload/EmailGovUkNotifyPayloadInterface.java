package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.emailgovuknotifypayload;

import jakarta.validation.Valid;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.govukconnection.GovUkConnectionInterface;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.TemplateLookupInterface;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation.TemplatePersonalisationInterface;

public interface EmailGovUkNotifyPayloadInterface {

    TemplateLookupInterface templateLookup = null;
    TemplatePersonalisationInterface templatePersonalisation = null;
    GovUkConnectionInterface govUkConnection = null;

    /**
     * Manages the process of sending an email
     *
     * @param govUkEmailDetailsRequest the details of the email to be sent
     */
    void sendEmail(@Valid GovUkEmailDetailsRequest govUkEmailDetailsRequest);
}
