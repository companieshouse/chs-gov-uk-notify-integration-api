package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public interface TemplateLookupInterface {

    /**
     * @param templateId
     * @return
     */
    ChEmailTemplate retrieveEmailTemplate(String templateId);

    /**
     * @param templateId
     * @return
     */
    ChLetterTemplate retrieveLetterTemplate(String templateId);
}
