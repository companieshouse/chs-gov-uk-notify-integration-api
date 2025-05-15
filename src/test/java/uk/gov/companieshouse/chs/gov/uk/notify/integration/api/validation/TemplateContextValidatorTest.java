package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChLetterTemplate;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TWO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class TemplateContextValidatorTest {

    private static final String TOKEN_CONTEXT_VARIABLE_VALUE = "Not an empty string";
    private static final String NO_VALID_CONTEXT_FOUND_ERROR_MESSAGE =
            "Unable to find a valid context for ChLetterTemplate"
                    + "[appId=chips, id=directionLetter, version=2]";
    private static final String SOME_VARIABLES_ARE_MISSING_ERROR_MESSAGE =
            "Context variable(s) [company_name, deadline_date] missing for "
                    + "ChLetterTemplate[appId=chips, id=directionLetter, version=1].";
    private static final String ALL_VARIABLES_ARE_MISSING_ERROR_MESSAGE =
            "Context variable(s) [address_line_1, address_line_2, postcode_or_country, "
                    + "date, reference, company_name, psc_full_name, deadline_date, "
                    + "extension_date] missing for "
                    + "ChLetterTemplate[appId=chips, id=directionLetter, version=1].";

    @InjectMocks
    private TemplateContextValidator validator;

    @SuppressWarnings("squid:S2699") // at least one assertion`
    @Test
    @DisplayName("Does not raise an error where all required context variables are present")
    void noErrorWhereAllRequiredVariablesPresent() {

        // Given
        var letter = new ChLetterTemplate("chips", "directionLetter", ONE);
        var context = new Context();
        context.setVariable("address_line_1", TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable("address_line_2", TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable("postcode_or_country", TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable("date", TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable("reference", TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable("company_name", TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable("psc_full_name", TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable("deadline_date", TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable("extension_date", TOKEN_CONTEXT_VARIABLE_VALUE);

        // When
        validator.validateContextForTemplate(context, letter);
    }

    @Test
    @DisplayName("Raises an error where the letter (template) is unknown")
    void errorsWhereTemplateIsUnknown() {

        // Given
        var letter = new ChLetterTemplate("chips", "directionLetter", TWO);
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
        var letter = new ChLetterTemplate("chips", "directionLetter", ONE);
        var context = new Context();
        context.setVariable("address_line_1", TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable("address_line_2", TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable("postcode_or_country", TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable("date", TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable("reference", TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable("psc_full_name", TOKEN_CONTEXT_VARIABLE_VALUE);
        context.setVariable("extension_date", TOKEN_CONTEXT_VARIABLE_VALUE);

        // When and then
        var exception = assertThrows(LetterValidationException.class,
                () -> validator.validateContextForTemplate(context, letter));
        assertThat(exception.getMessage(),
                is(SOME_VARIABLES_ARE_MISSING_ERROR_MESSAGE));
    }

    @Test
    @DisplayName("Raises an error where all required context variables are missing")
    void errorsWhereAllRequiredVariablesMissing() {

        // Given
        var letter = new ChLetterTemplate("chips", "directionLetter", ONE);
        var context = new Context();

        // When and then
        var exception = assertThrows(LetterValidationException.class,
                () -> validator.validateContextForTemplate(context, letter));
        assertThat(exception.getMessage(),
                is(ALL_VARIABLES_ARE_MISSING_ERROR_MESSAGE));
    }

}