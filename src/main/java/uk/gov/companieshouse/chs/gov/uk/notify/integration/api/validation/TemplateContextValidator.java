package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.validation;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ACTION_DUE_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_2;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_3;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.COMPANY_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.COMPANY_NUMBER;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.CS_REVIEW_PERIOD_END_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.CS_REVIEW_PERIOD_START_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.DEADLINE_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.EXTENSION_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.EXTENSION_REQUEST_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.IDV_START_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.IDV_VERIFICATION_DUE_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.PSC_APPOINTMENT_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.PSC_FULL_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.PSC_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.REFERENCE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.TODAYS_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CHIPS_DIRECTION_LETTER_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CHIPS_EXTENSION_ACCEPTANCE_LETTER_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CHIPS_NEW_PSC_DIRECTION_LETTER_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CHIPS_TRANSITIONAL_NON_DIRECTOR_PSC_INFORMATION_LETTER_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CSIDVDEFLET;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.IDVPSCDEFAULT;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey;

@Component
public class TemplateContextValidator {

    // ImmutableSet preserves ordering of set elements, java.util.Set does not.
    @SuppressWarnings("java:S4738")
    private static final Map<LetterTemplateKey, Set<String>> VALID_CONTEXTS =
            Map.ofEntries(
                    new AbstractMap.SimpleEntry<>(
                            CHIPS_DIRECTION_LETTER_1,
                            ImmutableSet.of(
                                    ADDRESS_LINE_1, ADDRESS_LINE_2, ADDRESS_LINE_3,
                                    TODAYS_DATE,
                                    REFERENCE,
                                    COMPANY_NAME,
                                    PSC_FULL_NAME,
                                    DEADLINE_DATE,
                                    EXTENSION_DATE
                            )
                    ),
                    new AbstractMap.SimpleEntry<>(
                            CHIPS_NEW_PSC_DIRECTION_LETTER_1,
                            ImmutableSet.of(
                                    ADDRESS_LINE_1, ADDRESS_LINE_2, ADDRESS_LINE_3,
                                    IDV_START_DATE,
                                    PSC_APPOINTMENT_DATE,
                                    IDV_VERIFICATION_DUE_DATE,
                                    REFERENCE,
                                    COMPANY_NAME,
                                    COMPANY_NUMBER,
                                    PSC_NAME
                            )
                    ),
                    new AbstractMap.SimpleEntry<>(
                            CHIPS_TRANSITIONAL_NON_DIRECTOR_PSC_INFORMATION_LETTER_1,
                            ImmutableSet.of(
                                    ADDRESS_LINE_1, ADDRESS_LINE_2, ADDRESS_LINE_3,
                                    TODAYS_DATE,
                                    IDV_START_DATE,
                                    IDV_VERIFICATION_DUE_DATE,
                                    REFERENCE,
                                    COMPANY_NAME,
                                    COMPANY_NUMBER,
                                    PSC_NAME
                            )
                    ),
                    new AbstractMap.SimpleEntry<>(
                            CHIPS_EXTENSION_ACCEPTANCE_LETTER_1,
                            ImmutableSet.of(
                                    ADDRESS_LINE_1, ADDRESS_LINE_2, ADDRESS_LINE_3,
                                    EXTENSION_REQUEST_DATE,
                                    IDV_VERIFICATION_DUE_DATE,
                                    REFERENCE,
                                    COMPANY_NAME,
                                    COMPANY_NUMBER,
                                    PSC_NAME
                            )
                    ),
                    new AbstractMap.SimpleEntry<>(
                            CSIDVDEFLET,
                            ImmutableSet.of(
                                    ADDRESS_LINE_1, ADDRESS_LINE_2, ADDRESS_LINE_3,
                                    REFERENCE,
                                    COMPANY_NAME,
                                    COMPANY_NUMBER,
                                    CS_REVIEW_PERIOD_START_DATE,
                                    CS_REVIEW_PERIOD_END_DATE,
                                    TODAYS_DATE,
                                    ACTION_DUE_DATE
                            )
                    ),
                    new AbstractMap.SimpleEntry<>(
                            IDVPSCDEFAULT,
                            ImmutableSet.of(
                                    ADDRESS_LINE_1, ADDRESS_LINE_2, ADDRESS_LINE_3,
                                    REFERENCE,
                                    COMPANY_NAME,
                                    COMPANY_NUMBER,
                                    PSC_NAME,
                                    TODAYS_DATE,
                                    ACTION_DUE_DATE
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
    public void validateContextForTemplate(Context context, LetterTemplateKey template) {
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
