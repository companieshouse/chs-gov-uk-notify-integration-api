package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import org.apache.commons.lang.NotImplementedException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChEmailTemplate;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChLetterTemplate;

import java.util.Map;

public class TemplatePersonalisationImpl implements TemplatePersonalisationInterface {
    /**
     * 1. Validate the parameters | valid personalisation string
     * <p>
     * replaces placeholders in the template with the personalisation details | via mapping
     *
     * @param template
     * @param personalisationDetails a list of key: value pairs used to replace placeholders in the template
     * @return
     */
    @Override
    public String personaliseEmailTemplate(ChEmailTemplate template, Map<String, String> personalisationDetails) {

        throw new NotImplementedException();
    }

    /**
     * @param template
     * @param personalisationDetails
     * @return
     */
    @Override
    public String personaliseLetterTemplate(ChLetterTemplate template, Map<String, String> personalisationDetails) {

        throw new NotImplementedException();
    }
}
