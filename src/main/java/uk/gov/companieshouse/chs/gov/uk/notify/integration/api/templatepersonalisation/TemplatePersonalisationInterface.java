package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChTemplate;

public interface TemplatePersonalisationInterface {

    String personaliseTemplate(ChTemplate template, String personalisationDetails);
}
