package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.validation;

import static java.math.BigDecimal.ONE;

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

    private static final Map<ChLetterTemplate, Set<String>> VALID_CONTEXTS =
            Map.ofEntries(
                    new AbstractMap.SimpleEntry<ChLetterTemplate, Set<String>>(
                            new ChLetterTemplate("directionLetter", ONE),
                            ImmutableSet.of(
                                    "address_line_1", "address_line_2", "postcode_or_country",
                                    "date",
                                    "reference",
                                    "company_name",
                                    "psc_full_name",
                                    "deadline_date",
                                    "extension_date"
                            )
                    )
            );

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
