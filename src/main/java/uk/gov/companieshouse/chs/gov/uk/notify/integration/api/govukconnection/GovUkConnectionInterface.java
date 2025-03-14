package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.govukconnection;

import org.springframework.stereotype.Component;

@Component
public interface GovUkConnectionInterface {

    String sendEmail(GovUkEmail govUkEmail);

    String sendLetter();

}
