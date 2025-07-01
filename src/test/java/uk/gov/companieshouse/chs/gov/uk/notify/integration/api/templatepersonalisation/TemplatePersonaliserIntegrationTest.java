package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TWO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.COMPANY_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.COMPANY_NUMBER;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.DEADLINE_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.EXTENSION_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.IDV_START_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.IDV_VERIFICATION_DUE_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.IS_WELSH;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.PSC_APPOINTMENT_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.PSC_FULL_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.PSC_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CHIPS_DIRECTION_LETTER_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CHIPS_NEW_PSC_DIRECTION_LETTER_1;

import java.util.Map;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
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

    private static final String TOKEN_VALUE = "Token value";

    private static final String VALID_PSC_APPOINTMENT_DATE = "24 June 2025";
    private static final String VALID_IDV_START_DATE = "30 June 2025";
    private static final String VALID_IDV_VERIFICATION_DUE_DATE = "14 July 2025";

    private static final String EXPECTED_WELSH_PSC_APPOINTMENT_DATE = "24 Mehefin 2025";
    private static final String EXPECTED_WELSH_IDV_START_DATE = "30 Mehefin 2025";
    private static final String EXPECTED_WELSH_IDV_VERIFICATION_DUE_DATE = "14 Gorffennaf 2025";

    private static final String EXPECTED_ENGLISH_P1_LOGO_NAME = "logo.svg";
    private static final String EXPECTED_WELSH_P1_LOGO_NAME = "welsh_logo.svg";
    private static final String EXPECTED_ENGLISH_P2_LOGO_NAME = "pages_2_onwards_logo.svg";
    private static final String EXPECTED_WELSH_P2_LOGO_NAME = "welsh_pages_2_onwards_logo.svg";

    private static final String EXPECTED_ENGLISH_P1_FOOTER_NAME = "page_1_footer_artwork.svg";
    private static final String EXPECTED_WELSH_P1_FOOTER_NAME = "welsh_page_1_footer_artwork.svg";
    private static final String EXPECTED_ENGLISH_P2_FOOTER_NAME =
            "pages_2_onwards_footer_artwork.svg";
    private static final String EXPECTED_WELSH_P2_FOOTER_NAME =
            "welsh_pages_2_onwards_footer_artwork.svg";

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
                Map.of(PSC_FULL_NAME, "Joe Bloggs",
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
                Map.of(PSC_FULL_NAME, "Joe Bloggs",
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

    @Test
    @DisplayName("Generate English New PSC Direction Letter HTML successfully")
    void generateEnglishNewPscLetterHtmlSuccessfully() {

        // Given and when
        var letter = parse(templatePersonalisation.personaliseLetterTemplate(
                CHIPS_NEW_PSC_DIRECTION_LETTER_1,
                "English New PSC Direction Letter",
                Map.of(PSC_APPOINTMENT_DATE, VALID_PSC_APPOINTMENT_DATE,
                        IDV_VERIFICATION_DUE_DATE, VALID_IDV_VERIFICATION_DUE_DATE,
                        IDV_START_DATE, VALID_IDV_START_DATE,
                        COMPANY_NUMBER, TOKEN_VALUE,
                        COMPANY_NAME, TOKEN_VALUE,
                        PSC_NAME, TOKEN_VALUE),
                ADDRESS));

        // Then
        verifyLetterIsEnglishOnly(letter);
    }

    @Test
    @DisplayName("Generate Welsh New PSC Direction Letter HTML successfully")
    void generateWelshNewPscLetterHtmlSuccessfully() {

        // Given and when
        var letter = parse(templatePersonalisation.personaliseLetterTemplate(
                CHIPS_NEW_PSC_DIRECTION_LETTER_1,
                "Welsh New PSC Direction Letter",
                Map.of(PSC_APPOINTMENT_DATE, VALID_PSC_APPOINTMENT_DATE,
                        IDV_VERIFICATION_DUE_DATE, VALID_IDV_VERIFICATION_DUE_DATE,
                        IDV_START_DATE, VALID_IDV_START_DATE,
                        COMPANY_NUMBER, TOKEN_VALUE,
                        COMPANY_NAME, TOKEN_VALUE,
                        PSC_NAME, TOKEN_VALUE,
                        IS_WELSH, "true"),
                ADDRESS));

        // Then
        verifyLetterIsBilingualEnglishAndWelsh(letter);
        verifyWelshDatesInLetter(letter);
        verifyEnglishDatesInLetter(letter);
        verifyWelshImagesInLetter(letter);
        verifyEnglishImagesInLetter(letter);
    }

    private static void verifyWelshImagesInLetter(final Document letter) {
        assertThat(getAttribute(letter, ".logo-img", "src"),
                endsWith(EXPECTED_WELSH_P1_LOGO_NAME));
        assertThat(getAttribute(letter, ".pages-2-onwards-logo-img", "src"),
                endsWith(EXPECTED_WELSH_P2_LOGO_NAME));
        assertThat(getAttribute(letter, ".page-1-footer-artwork-img", "src"),
                endsWith(EXPECTED_WELSH_P1_FOOTER_NAME));
        assertThat(getAttribute(letter, ".pages-2-onwards-footer-artwork-img", "src"),
                endsWith(EXPECTED_WELSH_P2_FOOTER_NAME));
    }

    private static void verifyEnglishImagesInLetter(final Document letter) {
        assertThat(getLastElementAttribute(letter, ".logo-img", "src"),
                endsWith(EXPECTED_ENGLISH_P1_LOGO_NAME));
        assertThat(getLastElementAttribute(letter, ".pages-2-onwards-logo-img", "src"),
                endsWith(EXPECTED_ENGLISH_P2_LOGO_NAME));
        assertThat(getLastElementAttribute(letter, ".page-1-footer-artwork-img", "src"),
                endsWith(EXPECTED_ENGLISH_P1_FOOTER_NAME));
        assertThat(getLastElementAttribute(letter, ".pages-2-onwards-footer-artwork-img", "src"),
                endsWith(EXPECTED_ENGLISH_P2_FOOTER_NAME));
    }

    private static void verifyWelshDatesInLetter(final Document letter) {
        assertThat(getText(letter, "#welsh-idv-start-date"), is(EXPECTED_WELSH_IDV_START_DATE));
        assertThat(getText(letter, "#welsh-psc-appointment-date"),
                is(EXPECTED_WELSH_PSC_APPOINTMENT_DATE));
        assertThat(getText(letter, "#welsh-idv-verification-due-date"),
                is(EXPECTED_WELSH_IDV_VERIFICATION_DUE_DATE));
        assertThat(getText(letter, "#welsh-idv-verification-due-date-2"),
                is(EXPECTED_WELSH_IDV_VERIFICATION_DUE_DATE));
    }

    private static void verifyEnglishDatesInLetter(final Document letter) {
        assertThat(getText(letter, "#idv-start-date"), is(VALID_IDV_START_DATE));
        assertThat(getText(letter, "#psc-appointment-date"), is(VALID_PSC_APPOINTMENT_DATE));
        assertThat(getText(letter, "#idv-verification-due-date"),
                is(VALID_IDV_VERIFICATION_DUE_DATE));
        assertThat(getText(letter, "#idv-verification-due-date-2"),
                is(VALID_IDV_VERIFICATION_DUE_DATE));

    }

    private static void verifyLetterPersonalised(final Document letter) {
        assertThat(getText(letter, ".direction-letter-title"), is(LETTER_TITLE));
        assertThat(getText(letter, ".close-packed-top .emphasis"), is("Joe Bloggs"));
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

    private static void verifyLetterIsEnglishOnly(final Document letter) {
        assertThat(getElement(letter, "#english_letter"), is(notNullValue()));
        assertThat(getElement(letter, "#welsh_letter"), is(nullValue()));
    }

    private static void verifyLetterIsBilingualEnglishAndWelsh(final Document letter) {
        assertThat(getElement(letter, "#english_letter"), is(notNullValue()));
        assertThat(getElement(letter, "#welsh_letter"), is(notNullValue()));
    }

    private static String getAddressLine(final Document document, final int lineNumber) {
        var selector = "#address-table tbody tr:nth-child({lineNumber})"
                .replace("{lineNumber}", String.valueOf(lineNumber));
        return getText(document, selector);
    }

    private static Element getElement(final Document document, final String selector) {
        return document.selectFirst(selector);
    }

    private static String getText(final Document document, final String selector) {
        var element = document.selectFirst(selector);
        return element != null ? element.text() : "";
    }

    private static String getAttribute(final Document document,
                                       final String selector,
                                       final String attribute) {
        var element = document.selectFirst(selector);
        return element != null ? element.attr(attribute) : null;
    }

    private static String getLastElementAttribute(final Document document,
                                                  final String selector,
                                                  final String attribute) {
        var element = document.select(selector).getLast();
        return element != null ? element.attr(attribute) : null;
    }

    private static Document parse(final String letterText) {
        return Parser.htmlParser().parseInput(letterText, "");
    }

}
