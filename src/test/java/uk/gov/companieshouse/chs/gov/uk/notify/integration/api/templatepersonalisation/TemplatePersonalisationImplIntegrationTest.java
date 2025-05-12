package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChLetterTemplate;

import java.util.Map;

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
        var letter = templatePersonalisation.personaliseLetterTemplate(
                new ChLetterTemplate("directionLetter"),
                Map.of("",""));
        assertThat(letter, containsString(LETTER_TITLE));
    }

}
