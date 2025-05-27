package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

import org.springframework.stereotype.Component;

/**
 *
 */
@Component
public interface TemplateLookupInterface {

    /**
     * 1. Validate templateId - is it null check? | does the template exist/ in correct folder
     * | will need additional validation
     * 2. Retrieve email template from /resources within repo
     * 3. Ensure ChEmailTemplate is populated - (details to come later)
     *
     * @param templateId
     * @return
     */
    ChEmailTemplate retrieveEmailTemplate(String templateId);

    /**
     * @param templateId
     * @return
     */
    LetterTemplateKey retrieveLetterTemplate(String templateId);
}
