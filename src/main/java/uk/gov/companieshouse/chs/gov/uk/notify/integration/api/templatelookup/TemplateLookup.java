package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

import org.springframework.stereotype.Component;

@Component
public class TemplateLookup {

    private static final String LETTER_TEMPLATE_ASSETS_FILEPATH = "assets/templates/letters/";

    /**
     * Derives a specification of the template location from the template look up key
     * provided.
     * @param templateLookupKey key or specification consisting of the client application ID
     *                          (aka service name),
     * @return the inferred location of the template
     */
    public LetterTemplateSpec lookupTemplate(ChLetterTemplate templateLookupKey) {
        return new LetterTemplateSpec(
                LETTER_TEMPLATE_ASSETS_FILEPATH + templateLookupKey.appId() + "/",
                templateLookupKey.id());
    }

}
