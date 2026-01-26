package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.validation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_2;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_3;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.COMPANY_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.COMPANY_NUMBER;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.IDV_START_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.IDV_VERIFICATION_DUE_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.PSC_APPOINTMENT_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.PSC_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.REFERENCE;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class TemplateContextValidatorTest {

    private static final String TOKEN_CONTEXT_VARIABLE_VALUE = "Not an empty string";
    private static final String NO_VALID_CONTEXT_FOUND_ERROR_MESSAGE =
            "Unable to find a valid context for LetterTemplateKey"
                    + "[appId=chips, letterId=DUMMY, templateId=v1.0]";
    private static final String SOME_VARIABLES_ARE_MISSING_ERROR_MESSAGE =
            "Context variable(s) [company_name, idv_start_date] missing for %s.";
    private static final String ALL_VARIABLES_ARE_MISSING_ERROR_MESSAGE =
            "Context variable(s) [address_line_1, address_line_2, address_line_3, "
                    + "company_name, company_number, idv_start_date, idv_verification_due_date, "
                    + "psc_appointment_date, psc_name, reference] missing for %s.";



    @InjectMocks
    private TemplateContextValidator validator;

    @Test
    @DisplayName("Does not raise an error where all required context variables are present")
    void noErrorWhereAllRequiredVariablesPresent() {

        for (LetterTemplateKey key : LetterTemplateKey.NEW_PSC_DIRECTION_TEMPLATES) {
            // Given
            var context = new Context();
            context.setVariable(ADDRESS_LINE_1, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(ADDRESS_LINE_2, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(ADDRESS_LINE_3, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(IDV_START_DATE, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(PSC_APPOINTMENT_DATE, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(IDV_VERIFICATION_DUE_DATE, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(REFERENCE, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(COMPANY_NAME, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(COMPANY_NUMBER, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(PSC_NAME, TOKEN_CONTEXT_VARIABLE_VALUE);

            // When
            validator.validateContextForTemplate(context, key);

            assertTrue(true); // no exception means success
        }
    }

    @Test
    @DisplayName("Raises an error where the letter (template) is unknown")
    void errorsWhereTemplateIsUnknown() {

        // Given
        var letter = new LetterTemplateKey("chips", "DUMMY", "v1.0");
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

        for (LetterTemplateKey key : LetterTemplateKey.NEW_PSC_DIRECTION_TEMPLATES) {
            // Given
            var context = new Context();
            context.setVariable(ADDRESS_LINE_1, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(ADDRESS_LINE_2, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(ADDRESS_LINE_3, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(PSC_APPOINTMENT_DATE, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(IDV_VERIFICATION_DUE_DATE, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(REFERENCE, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(COMPANY_NUMBER, TOKEN_CONTEXT_VARIABLE_VALUE);
            context.setVariable(PSC_NAME, TOKEN_CONTEXT_VARIABLE_VALUE);

            // When and then
            var exception = assertThrows(LetterValidationException.class,
                    () -> validator.validateContextForTemplate(context, key));
            assertThat(exception.getMessage(),
                    is(String.format(SOME_VARIABLES_ARE_MISSING_ERROR_MESSAGE, key.toString())));
        }
    }

    @Test
    @DisplayName("Raises an error where all required context variables are missing")
    void errorsWhereAllRequiredVariablesMissing() {

        for (LetterTemplateKey key : LetterTemplateKey.NEW_PSC_DIRECTION_TEMPLATES) {
            // Given
            var context = new Context();

            // When and then
            var exception = assertThrows(LetterValidationException.class,
                    () -> validator.validateContextForTemplate(context, key));
            assertThat(exception.getMessage(),
                    is(String.format(ALL_VARIABLES_ARE_MISSING_ERROR_MESSAGE, key.toString())));
        }
    }

    @Test
    @DisplayName("requiresTodaysDate returns true for letter templates that need today's date")
    void requiresTodaysDateIsTrue() {
        Set<LetterTemplateKey> templatesRequiringTodaysDate = new HashSet<>();
        templatesRequiringTodaysDate.addAll(LetterTemplateKey.CSIDVDEFLET_TEMPLATES);
        templatesRequiringTodaysDate.addAll(LetterTemplateKey.IDVPSCDEFAULT_TEMPLATES);
        templatesRequiringTodaysDate.addAll(LetterTemplateKey.TRANSITIONAL_PSC_DIRECTION_TEMPLATES);
        for (var letterTemplateKey : templatesRequiringTodaysDate) {
            assertThat(validator.requiresTodaysDate(letterTemplateKey), is(true));
        }
    }

    @Test
    @DisplayName("requiresTodaysDate returns false for letter templates that do not need today's date")
    void requiresTodaysDateIsFalse() {
        Set<LetterTemplateKey> templatesNotRequiringTodaysDate = new HashSet<>();
        templatesNotRequiringTodaysDate.addAll(LetterTemplateKey.IDVPSCEXT_TEMPLATES);
        templatesNotRequiringTodaysDate.addAll(LetterTemplateKey.NEW_PSC_DIRECTION_TEMPLATES);
        for (var letterTemplateKey : templatesNotRequiringTodaysDate) {
            assertThat(validator.requiresTodaysDate(letterTemplateKey), is(false));
        }
    }

    @Test
    @DisplayName("requiresTodaysDate returns false for non-configured letter templates")
    void requiresTodaysDateIsFalseForUnknownLetter() {
        assertThat(validator.requiresTodaysDate(new LetterTemplateKey("chips", "UNKNOWN", "v7")), is(false));
    }
}