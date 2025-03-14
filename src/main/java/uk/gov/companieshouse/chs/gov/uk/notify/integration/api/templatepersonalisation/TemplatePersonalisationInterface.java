package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChEmailTemplate;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChLetterTemplate;

@Component
public interface TemplatePersonalisationInterface {

    String personaliseEmailTemplate(ChEmailTemplate template, String personalisationDetails);

    String personaliseLetterTemplate(ChLetterTemplate template, String personalisationDetails);
}
