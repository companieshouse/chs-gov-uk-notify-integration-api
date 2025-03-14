package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.govukconnection;

import org.springframework.stereotype.Component;

@Component
public interface GovUkConnectionInterface {

    /**
     * 1. validate parameter
     * 2. connect to Gov.uk Notify
     * 3. call endpoint to send email
     * 4. return Gov.uk Notify response/reference
     *
     * @param govUkEmail
     * @return
     */
    String sendEmail(GovUkEmail govUkEmail);

    /**
     * @return
     */
    String sendLetter(GovUkLetter govUkLetter);

}
