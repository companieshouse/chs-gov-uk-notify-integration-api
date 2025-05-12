package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import java.util.Map;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.util.HtmlUtils;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChLetterTemplate;

@SpringBootTest
@ExtendWith(OutputCaptureExtension.class) // TODO DEEP-287 Am I using this?
class TemplatePersonalisationImplIntegrationTest {

    // Note comparison with an equivalent text block seems to fail incorrectly.
    private static final String LETTER_TITLE = "Verify your identity\n" +
            "—Person with\n" +
            "significant control";

    @Autowired
    private TemplatePersonalisationImpl templatePersonalisation;

    @Test
    @DisplayName("Generate letter HTML successfully")
    void generateLetterHtmlSuccessfully(CapturedOutput log) {

        // Given and when
        var letter = templatePersonalisation.personaliseLetterTemplate(
                new ChLetterTemplate("directionLetter"),
                Map.of("psc_full_name", "Vaughan Jackson",
                        "company_name", "Tŷ'r Cwmnïau",
                        "reference", "reference",
                        "deadline_date", "18 August 2025",
                        "extension_date", "1 September 2025"));

        // Then
        assertThat(letter, containsEscapedString(LETTER_TITLE));
        assertThat(letter, containsEscapedString("Vaughan Jackson"));
        assertThat(letter, containsEscapedString("Tŷ'r Cwmnïau"));
        assertThat(letter, containsEscapedString("reference"));
        assertThat(letter, containsEscapedString("18 August 2025"));
        assertThat(letter, containsEscapedString("1 September 2025"));
    }

    private static Matcher<String> containsEscapedString(String substring) {
        return containsString(HtmlUtils.htmlEscape(substring, UTF_8.toString()));
    }


}
