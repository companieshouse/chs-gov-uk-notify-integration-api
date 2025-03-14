package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChEmailTemplate;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChLetterTemplate;

import java.util.Map;

@Component
public interface TemplatePersonalisationInterface {

    /**
     * @param template
     * @param personalisationDetails a list of key: value pairs used to replace placeholders in the template
     * @return
     */
    String personaliseEmailTemplate(ChEmailTemplate template, Map<String, String> personalisationDetails);

    /**
     * @param template
     * @param personalisationDetails
     * @return
     */
    String personaliseLetterTemplate(ChLetterTemplate template, Map<String, String> personalisationDetails);
}
