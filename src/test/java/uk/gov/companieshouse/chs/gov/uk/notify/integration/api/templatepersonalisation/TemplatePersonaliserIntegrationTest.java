package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import static java.math.BigDecimal.ONE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
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
import org.springframework.web.util.HtmlUtils;
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChLetterTemplate;

@SpringBootTest
class TemplatePersonaliserIntegrationTest {

    @SuppressWarnings("java:S6126") // Comparison with an equivalent text block fails incorrectly.
    private static final String LETTER_TITLE = "Verify your identity\n" +
            "—Person with\n" +
            "significant control";

    @Autowired
    private TemplatePersonaliser templatePersonalisation;

    @Test
    @DisplayName("Generate letter HTML successfully")
    void generateLetterHtmlSuccessfully() {

        // Given and when
        var letter = templatePersonalisation.personaliseLetterTemplate(
                new ChLetterTemplate("directionLetter", ONE),
                Map.of(PSC_FULL_NAME, "Vaughan Jackson",
                        COMPANY_NAME, "Tŷ'r Cwmnïau",
                        REFERENCE, "reference",
                        DEADLINE_DATE, "18 August 2025",
                        EXTENSION_DATE, "1 September 2025"),
                new Address()
                        .addressLine1("Line 1")
                        .addressLine2("Line 2")
                        .addressLine3("Line 3")
                        .addressLine4("Line 4")
                        .addressLine5("Line 5")
                        .addressLine6("Line 6")
                        .addressLine7("Line 7")
                        );

        // Then
        verifyLetterPersonalised(letter);
        verifyLetterAddressed(letter);
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
