package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

import org.springframework.stereotype.Component;

@Component
public interface TemplateLookupInterface {

    ChTemplate retrieveTemplate(String templateId);
}
