package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TWO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.COMPANY_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.DEADLINE_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.EXTENSION_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.PSC_FULL_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CHIPS_DIRECTION_LETTER_1;

import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thymeleaf.context.Context;
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.TemplateLookup;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.validation.TemplateContextValidator;

@SpringBootTest(properties = {"spring.data.mongodb.uri=mongodb://token_value"})
class TemplatePersonaliserIntegrationTest {

    private static final String LETTER_TITLE =
            "Verify your identity —Person with significant control";

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

    private static final Address SHORTER_ADDRESS = new Address()
            .addressLine1("Line 1")
            .addressLine2("Line 2")
            .addressLine3("Line 3")
            .addressLine4("Line 4");

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
        var letter = parse(templatePersonalisation.personaliseLetterTemplate(
                CHIPS_DIRECTION_LETTER_1,
                "the reference",
                Map.of(PSC_FULL_NAME, "Vaughan Jackson",
                        COMPANY_NAME, "Tŷ'r Cwmnïau",
                        DEADLINE_DATE, "18 August 2025",
                        EXTENSION_DATE, "1 September 2025"),
                ADDRESS));

        // Then
        verifyLetterPersonalised(letter);
        verifyLetterAddressed(letter);
    }

    @Test
    @DisplayName("Generate letter HTML successfully with a shorter address")
    void generateLetterHtmlSuccessfullyWithShorterAddress() {
        // Given and when
        var letter = parse(templatePersonalisation.personaliseLetterTemplate(
                CHIPS_DIRECTION_LETTER_1,
                "the reference",
                Map.of(PSC_FULL_NAME, "Vaughan Jackson",
                        COMPANY_NAME, "Tŷ'r Cwmnïau",
                        DEADLINE_DATE, "18 August 2025",
                        EXTENSION_DATE, "1 September 2025"),
                SHORTER_ADDRESS));

        // Then
        verifyLetterPersonalised(letter);
        verifyLetterAddressedWithShorterAddress(letter);
    }

    @Test
    @DisplayName("Personalise templates for different client apps")
    void personaliseTemplatesForDifferentClientApps() {

        // Given
        when(templateLookup.getLetterTemplatesRootDirectory()).thenReturn("mock_assets/");

        var templateSpec1 = new LetterTemplateKey("app1", "letter1", ONE);
        var templateSpec2 = new LetterTemplateKey("app2", "letter1", ONE);
        doNothing().when(templateContextValidator).validateContextForTemplate(
                any(Context.class), any(LetterTemplateKey.class));

        // When
        var letter1 = templatePersonalisation.personaliseLetterTemplate(
                templateSpec1,
                "the reference",
                PERSONALISATION_DETAILS,
                ADDRESS);
        var letter2 = templatePersonalisation.personaliseLetterTemplate(
                templateSpec2,
                "the reference",
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

        var templateSpec = new LetterTemplateKey("app1", "letter1", TWO);
        doNothing().when(templateContextValidator).validateContextForTemplate(
                any(Context.class), any(LetterTemplateKey.class));

        // When
        var letter = templatePersonalisation.personaliseLetterTemplate(
                templateSpec,
                "the reference",
                PERSONALISATION_DETAILS,
                ADDRESS);

        assertThat(letter, is("This is letter1_v2.html in app1."));
    }

    @Test
    @DisplayName("Personalise template for a different template ID")
    void personaliseTemplateForDifferentTemplateId() {

        // Given
        when(templateLookup.getLetterTemplatesRootDirectory()).thenReturn("mock_assets/");

        var templateSpec1 = new LetterTemplateKey("app1", "letter2", ONE);
        doNothing().when(templateContextValidator).validateContextForTemplate(
                any(Context.class), any(LetterTemplateKey.class));

        // When
        var letter1 = templatePersonalisation.personaliseLetterTemplate(
                templateSpec1,
                "the reference",
                PERSONALISATION_DETAILS,
                ADDRESS);

        assertThat(letter1, is("This is letter2_v1.html in app1."));
    }

    private static void verifyLetterPersonalised(final Document letter) {
        assertThat(getText(letter, ".direction-letter-title"), is(LETTER_TITLE));
        assertThat(getText(letter, ".close-packed-top .emphasis"), is("Vaughan Jackson"));
        assertThat(getText(letter, "p .subject-line"), is("Tŷ'r Cwmnïau".toUpperCase()));
        assertThat(getText(letter, ".date-and-ref tr:nth-child(5)"), is("the reference"));
        assertThat(getText(letter, "#deadline-date"), is("18 August 2025"));
        assertThat(getText(letter, "#extension-date"), is("1 September 2025"));
    }

    private static void verifyLetterAddressed(final Document letter) {
        assertThat(getAddressLine(letter, 1), is("Line 1"));
        assertThat(getAddressLine(letter, 2), is("Line 2"));
        assertThat(getAddressLine(letter, 3), is("Line 3"));
        assertThat(getAddressLine(letter, 4), is("Line 4"));
        assertThat(getAddressLine(letter, 5), is("Line 5"));
        assertThat(getAddressLine(letter, 6), is("Line 6"));
        assertThat(getAddressLine(letter, 7), is("Line 7"));
    }

    private static void verifyLetterAddressedWithShorterAddress(final Document letter) {
        assertThat(getAddressLine(letter, 1), is("Line 1"));
        assertThat(getAddressLine(letter, 2), is("Line 2"));
        assertThat(getAddressLine(letter, 3), is("Line 3"));
        assertThat(getAddressLine(letter, 4), is("Line 4"));
    }

    private static String getAddressLine(final Document document, final int lineNumber) {
        var selector = "#address-table tbody tr:nth-child({lineNumber})"
                .replace("{lineNumber}", String.valueOf(lineNumber));
        return getText(document, selector);
    }

    private static String getText(final Document document, final String selector) {
        var element = document.select(selector).first();
        return element != null ? element.text() : "";
    }

    private static Document parse(final String letterText) {
        return Parser.htmlParser().parseInput(letterText, "");
    }

}
