package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey;

import static java.math.BigDecimal.TWO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_2;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_3;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.COMPANY_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.DEADLINE_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.EXTENSION_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.TODAYS_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.PSC_FULL_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.REFERENCE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CHIPS_APPLICATION_ID;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CHIPS_DIRECTION_LETTER_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.DIRECTION_LETTER;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class TemplateContextValidatorTest {

    private static final String TOKEN_CONTEXT_VARIABLE_VALUE = "Not an empty string";
    private static final String NO_VALID_CONTEXT_FOUND_ERROR_MESSAGE =
            "Unable to find a valid context for LetterTemplateKey"
                    + "[appId=chips, id=direction_letter, version=2]";
    private static final String SOME_VARIABLES_ARE_MISSING_ERROR_MESSAGE =
            "Context variable(s) [company_name, deadline_date] missing for "
                    + "LetterTemplateKey[appId=chips, id=direction_letter, version=1].";
    private static final String ALL_VARIABLES_ARE_MISSING_ERROR_MESSAGE =
            "Context variable(s) [address_line_1, address_line_2, address_line_3, "
                    + "todays_date, reference, company_name, psc_full_name, deadline_date, "
                    + "extension_date] missing for "
                    + "LetterTemplateKey[appId=chips, id=direction_letter, version=1].";

    @InjectMocks
    private TemplateContextValidator validator;

    @SuppressWarnings("squid:S2699") // at least one assertion`
    @Test
    @DisplayName("Does not raise an error where all required context variables are present")
    void noErrorWhereAllRequiredVariablesPresent() {

        // Given
        var context = new Context();
        context.setVariable(ADDRESS_LINE_1, TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable(ADDRESS_LINE_2, TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable(ADDRESS_LINE_3, TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable(TODAYS_DATE, TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable(REFERENCE, TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable(COMPANY_NAME, TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable(PSC_FULL_NAME, TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable(DEADLINE_DATE, TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable(EXTENSION_DATE, TOKEN_CONTEXT_VARIABLE_VALUE);

        // When
        validator.validateContextForTemplate(context, CHIPS_DIRECTION_LETTER_1);
    }

    @Test
    @DisplayName("Raises an error where the letter (template) is unknown")
    void errorsWhereTemplateIsUnknown() {

        // Given
        var letter = new LetterTemplateKey(CHIPS_APPLICATION_ID, DIRECTION_LETTER, TWO);
        var context = new Context();

        // When and then
        var exception = assertThrows(LetterValidationException.class,
                () -> validator.validateContextForTemplate(context, letter));
        assertThat(exception.getMessage(),
                is(NO_VALID_CONTEXT_FOUND_ERROR_MESSAGE));
    }

    @Test
    @DisplayName("Raises an error where some required context variables are missing")
    void errorsWhereSomeRequiredVariablesMissing() {

        // Given
        var context = new Context();
        context.setVariable(ADDRESS_LINE_1, TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable(ADDRESS_LINE_2, TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable(ADDRESS_LINE_3, TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable(TODAYS_DATE, TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable(REFERENCE, TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable(PSC_FULL_NAME, TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable(EXTENSION_DATE, TOKEN_CONTEXT_VARIABLE_VALUE);

        // When and then
        var exception = assertThrows(LetterValidationException.class,
                () -> validator.validateContextForTemplate(context, CHIPS_DIRECTION_LETTER_1));
        assertThat(exception.getMessage(),
                is(SOME_VARIABLES_ARE_MISSING_ERROR_MESSAGE));
    }

    @Test
    @DisplayName("Raises an error where all required context variables are missing")
    void errorsWhereAllRequiredVariablesMissing() {

        // Given
        var context = new Context();

        // When and then
        var exception = assertThrows(LetterValidationException.class,
                () -> validator.validateContextForTemplate(context, CHIPS_DIRECTION_LETTER_1));
        assertThat(exception.getMessage(),
                is(ALL_VARIABLES_ARE_MISSING_ERROR_MESSAGE));
    }

}