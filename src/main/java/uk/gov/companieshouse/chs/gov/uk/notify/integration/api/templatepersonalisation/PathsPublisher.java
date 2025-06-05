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
    private static final String COMMON_RESOURCE_PATH = ROOT_RESOURCE_PATH + "common/";

    public void publishPathsViaContext(final Context context,
                                       final LetterTemplateKey templateLookupKey) {

        context.setVariable(ROOT_RESOURCE_PATH_VARIABLE, ROOT_RESOURCE_PATH);
        context.setVariable(LETTER_RESOURCE_PATH_VARIABLE,
                ROOT_RESOURCE_PATH + templateLookupKey.appId() + "/");
        context.setVariable(COMMON_RESOURCE_PATH_VARIABLE, COMMON_RESOURCE_PATH);

    }

}
