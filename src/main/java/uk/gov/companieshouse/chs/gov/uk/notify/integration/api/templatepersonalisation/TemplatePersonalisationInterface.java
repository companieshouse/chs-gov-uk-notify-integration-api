package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChTemplate;

@Component
public interface TemplatePersonalisationInterface {

    String personaliseTemplate(ChTemplate template, String personalisationDetails);
}
