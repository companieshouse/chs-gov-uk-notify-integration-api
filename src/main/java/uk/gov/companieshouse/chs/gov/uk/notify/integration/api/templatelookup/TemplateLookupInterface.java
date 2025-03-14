package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public interface TemplateLookupInterface {

    ChEmailTemplate retrieveEmailTemplate(String templateId);

    ChLetterTemplate retrieveLetterTemplate(String templateId);
}
