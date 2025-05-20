package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TWO;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.COMPANY_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.DEADLINE_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.EXTENSION_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.PSC_FULL_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.REFERENCE;

import java.util.Map;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.util.HtmlUtils;
import org.thymeleaf.context.Context;
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChLetterTemplate;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.TemplateLookup;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.validation.TemplateContextValidator;

@SpringBootTest
class TemplatePersonaliserIntegrationTest {

    @SuppressWarnings("java:S6126") // Comparison with an equivalent text block fails incorrectly.
    private static final String LETTER_TITLE = "Verify your identity\n" +
            "—Person with\n" +
            "significant control";

    private static final Map<String, String> PERSONALISATION_DETAILS =
            Map.of("company_name", "Amazon");

    private static final Address ADDRESS = new Address()
                        .addressLine1("Line 1")
                        .addressLine2("Line 2")
                        .addressLine3("Line 3")
                        .addressLine4("Line 4")
                        .addressLine5("Line 5")
                        .addressLine6("Line 6")
                        .addressLine7("Line 7");

    @Autowired
    private TemplatePersonaliser templatePersonalisation;

    @MockitoSpyBean
    private TemplateLookup templateLookup;

    @MockitoSpyBean
    private TemplateContextValidator templateContextValidator;

    @Test
    @DisplayName("Generate letter HTML successfully")
    void generateLetterHtmlSuccessfully() {

        // Given and when
        var letter = templatePersonalisation.personaliseLetterTemplate(
                new ChLetterTemplate("chips", "directionLetter", ONE),
                Map.of(PSC_FULL_NAME, "Vaughan Jackson",
                        COMPANY_NAME, "Tŷ'r Cwmnïau",
                        REFERENCE, "reference",
                        DEADLINE_DATE, "18 August 2025",
                        EXTENSION_DATE, "1 September 2025"),
                ADDRESS);

        // Then
        verifyLetterPersonalised(letter);
        verifyLetterAddressed(letter);
    }

    @Test
    @DisplayName("Personalise templates for different client apps")
    void personaliseTemplatesForDifferentClientApps() {

        // Given
        when(templateLookup.getLetterTemplatesRootDirectory()).thenReturn("mock_assets/");

        var templateSpec1 = new ChLetterTemplate("app1", "letter1", ONE);
        var templateSpec2 = new ChLetterTemplate("app2", "letter1", ONE);
        doNothing().when(templateContextValidator).validateContextForTemplate(
                any(Context.class), any(ChLetterTemplate.class));

        // When
        var letter1 = templatePersonalisation.personaliseLetterTemplate(
                templateSpec1,
                PERSONALISATION_DETAILS,
                ADDRESS);
        var letter2 = templatePersonalisation.personaliseLetterTemplate(
                templateSpec2,
                PERSONALISATION_DETAILS,
                ADDRESS);

        assertThat(letter1, is("This is letter1_v1.html in app1."));
        assertThat(letter2, is("This is letter1_v1.html in app2."));
    }

    @Test
    @DisplayName("Personalise template for a different template version")
    void personaliseTemplateForDifferentTemplateVersion() {

        // Given
        when(templateLookup.getLetterTemplatesRootDirectory()).thenReturn("mock_assets/");

        var templateSpec = new ChLetterTemplate("app1", "letter1", TWO);
        doNothing().when(templateContextValidator).validateContextForTemplate(
                any(Context.class), any(ChLetterTemplate.class));

        // When
        var letter = templatePersonalisation.personaliseLetterTemplate(
                templateSpec,
                PERSONALISATION_DETAILS,
                ADDRESS);

        assertThat(letter, is("This is letter1_v2.html in app1."));
    }

    @Test
    @DisplayName("Personalise template for a different template ID")
    void personaliseTemplateForDifferentTemplateId() {

        // Given
        when(templateLookup.getLetterTemplatesRootDirectory()).thenReturn("mock_assets/");

        var templateSpec1 = new ChLetterTemplate("app1", "letter2", ONE);
        doNothing().when(templateContextValidator).validateContextForTemplate(
                any(Context.class), any(ChLetterTemplate.class));

        // When
        var letter1 = templatePersonalisation.personaliseLetterTemplate(
                templateSpec1,
                PERSONALISATION_DETAILS,
                ADDRESS);

        assertThat(letter1, is("This is letter2_v1.html in app1."));
    }

    private static void verifyLetterPersonalised(String letter) {
        assertThat(letter, containsEscapedString(LETTER_TITLE));
        assertThat(letter, containsEscapedString("Vaughan Jackson"));
        assertThat(letter, containsEscapedString("Tŷ'r Cwmnïau".toUpperCase()));
        assertThat(letter, containsEscapedString("reference"));
        assertThat(letter, containsEscapedString("18 August 2025"));
        assertThat(letter, containsEscapedString("1 September 2025"));
    }

    private static void verifyLetterAddressed(String letter) {
        assertThat(letter, containsEscapedString("Line 1"));
        assertThat(letter, containsEscapedString("Line 2"));
        assertThat(letter, containsEscapedString("Line 3"));
        assertThat(letter, containsEscapedString("Line 4"));
        assertThat(letter, containsEscapedString("Line 5"));
        assertThat(letter, containsEscapedString("Line 6"));
        assertThat(letter, containsEscapedString("Line 7"));
    }

    private static Matcher<String> containsEscapedString(String substring) {
        return containsString(HtmlUtils.htmlEscape(substring, UTF_8.toString()));
    }

}
