package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.govukconnection;

import org.springframework.stereotype.Component;

@Component
public interface GovUkConnectionInterface {

    /**
     *
     * @param govUkEmail
     * @return
     */
    String sendEmail(GovUkEmail govUkEmail);

    /**
     *
     * @return
     */
    String sendLetter(GovUkLetter govUkLetter);

}
