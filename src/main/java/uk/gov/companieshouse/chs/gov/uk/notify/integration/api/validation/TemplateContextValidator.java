package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.validation;

import static java.math.BigDecimal.ONE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_2;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.COMPANY_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.DEADLINE_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.EXTENSION_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.POSTCODE_OR_COUNTRY;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.PSC_FULL_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.REFERENCE;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChLetterTemplate;

@Component
public class TemplateContextValidator {

    // ImmutableSet preserves ordering of set elements, java.util.Set does not.
    @SuppressWarnings("java:S4738")
    private static final Map<ChLetterTemplate, Set<String>> VALID_CONTEXTS =
            Map.ofEntries(
                    new AbstractMap.SimpleEntry<>(
                            new ChLetterTemplate("chips", "directionLetter", ONE),
                            ImmutableSet.of(
                                    ADDRESS_LINE_1, ADDRESS_LINE_2, POSTCODE_OR_COUNTRY,
                                    DATE,
                                    REFERENCE,
                                    COMPANY_NAME,
                                    PSC_FULL_NAME,
                                    DEADLINE_DATE,
                                    EXTENSION_DATE
                            )
                    )
            );

    /**
     * Validate that the context provided contains the variables expected for the letter
     * template identified.
     *
     * @param context the Thymeleaf context assumed to contain the variable values required for
     *                substitutions of field variables in the Thymeleaf template identified
     * @param template identifies the letter template to be personalised
     */
    public void validateContextForTemplate(Context context, ChLetterTemplate template) {
        var validContext = VALID_CONTEXTS.get(template);
        if (validContext == null) {
            throw new LetterValidationException(
                    "Unable to find a valid context for " + template);
        }
        var missingVariables = Sets.difference(validContext, context.getVariableNames());
        if (!missingVariables.isEmpty()) {
            throw new LetterValidationException("Context variable(s) "
                    + missingVariables + " missing for " + template + ".");
        }
    }

}
