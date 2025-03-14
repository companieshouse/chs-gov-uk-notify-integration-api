package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChEmailTemplate;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChLetterTemplate;

@Component
public interface TemplatePersonalisationInterface {

    /**
     * @param template
     * @param personalisationDetails
     * @return
     */
    String personaliseEmailTemplate(ChEmailTemplate template, String personalisationDetails);

    /**
     * @param template
     * @param personalisationDetails
     * @return
     */
    String personaliseLetterTemplate(ChLetterTemplate template, String personalisationDetails);
}
