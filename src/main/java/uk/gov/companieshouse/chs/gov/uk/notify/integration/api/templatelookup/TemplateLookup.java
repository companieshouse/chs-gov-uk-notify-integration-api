package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

import org.springframework.stereotype.Component;

/**
 * Responsible for inferring the template asset location from its key.
 */
@Component
public class TemplateLookup {

    private static final String ROOT_DIRECTORY = "assets/templates/letters/";
    private static final String OLD_ROOT_DIRECTORY = "assets/templates/old_letters/";

    /**
     * Derives a specification of the template location from the template look up key
     * provided.
     * @param templateLookupKey key or specification consisting of the client application ID
     *                          (aka service name),
     * @return the inferred location of the template
     */
    public LetterTemplateLocatorSpec lookupTemplate(LetterTemplateKey templateLookupKey) {
        var letterId = templateLookupKey.letterId();
        var templateId = templateLookupKey.templateId();

        if (letterId == null || letterId.isBlank()) {
            // old letters
            return new LetterTemplateLocatorSpec(
                    OLD_ROOT_DIRECTORY + templateLookupKey.appId() + "/", templateId);
        } else {
            return new LetterTemplateLocatorSpec(getLetterTemplatesRootDirectory()
                    + templateLookupKey.appId() + "/" + letterId + "/" + templateId + "/",
                    "template.html");
        }
    }

    public String getLetterTemplatesRootDirectory() {
        return ROOT_DIRECTORY;
    }

}
