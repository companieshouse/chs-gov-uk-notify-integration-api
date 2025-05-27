package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

import org.apache.commons.lang.NotImplementedException;

public class TemplateLookupImpl implements TemplateLookupInterface {
    /**
     * 1. Validate templateId - is it null check? | does the template exist/ in correct folder
     * | will need additional validation
     * 2. Retrieve email template from /resources within repo
     * 3. Ensure ChEmailTemplate is populated - (details to come later)
     *
     * @param templateId
     * @return
     */
    @Override
    public ChEmailTemplate retrieveEmailTemplate(String templateId) {
        throw new NotImplementedException();
    }

    /**
     * @param templateId
     * @return
     */
    @Override
    public LetterTemplateKey retrieveLetterTemplate(String templateId) {

        throw new NotImplementedException();
    }
}
