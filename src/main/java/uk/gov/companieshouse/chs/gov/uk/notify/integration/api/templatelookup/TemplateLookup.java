package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

import org.springframework.stereotype.Component;

/**
 * Responsible for inferring the template asset location from its key.
 */
@Component
public class TemplateLookup {

    private static final String LETTER_TEMPLATES_ROOT_DIRECTORY = "assets/templates/letters/";

    /**
     * Derives a specification of the template location from the template look up key
     * provided.
     * @param templateLookupKey key or specification consisting of the client application ID
     *                          (aka service name),
     * @return the inferred location of the template
     */
    public LetterTemplateLocatorSpec lookupTemplate(LetterTemplateKey templateLookupKey) {
        var filename = templateLookupKey.id();
        return new LetterTemplateLocatorSpec(
                getLetterTemplatesRootDirectory() + templateLookupKey.appId() + "/", filename);
    }

    public String getLetterTemplatesRootDirectory() {
        return LETTER_TEMPLATES_ROOT_DIRECTORY;
    }

}
