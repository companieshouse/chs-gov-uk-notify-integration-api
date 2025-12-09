package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.COMMON_RESOURCE_PATH_VARIABLE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.LETTER_RESOURCE_PATH_VARIABLE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ROOT_RESOURCE_PATH_VARIABLE;

import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey;

/**
 * Responsible for making path variables available for use in the Thymeleaf templates.
 */
@Component
public class PathsPublisher {

    private static final String ROOT_RESOURCE_PATH = "assets/templates/letters/";
    private static final String OLD_ROOT_RESOURCE_PATH = "assets/templates/old_letters/";

    public void publishPathsViaContext(final Context context,
                                       final LetterTemplateKey templateLookupKey) {
        var letterId = templateLookupKey.letterId();
        String rootPath = ROOT_RESOURCE_PATH;
        if (letterId == null || letterId.isBlank()) {
            // old letters
            rootPath = OLD_ROOT_RESOURCE_PATH;
            context.setVariable(LETTER_RESOURCE_PATH_VARIABLE,
                    rootPath + templateLookupKey.appId() + "/");
        } else {
            context.setVariable(LETTER_RESOURCE_PATH_VARIABLE,
                    rootPath + templateLookupKey.appId() + "/" + templateLookupKey.letterId() + "/" + templateLookupKey.templateId() + "/");
        }
        context.setVariable(ROOT_RESOURCE_PATH_VARIABLE, rootPath);
        context.setVariable(COMMON_RESOURCE_PATH_VARIABLE, rootPath + "common/");
    }

}
