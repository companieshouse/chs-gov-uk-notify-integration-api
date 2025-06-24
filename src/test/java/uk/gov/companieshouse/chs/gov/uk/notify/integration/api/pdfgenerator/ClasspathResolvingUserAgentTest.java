package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xhtmlrenderer.pdf.ITextOutputDevice;

import java.io.File;

/**
 * Unit tests those parts of the {@link ClasspathResolvingUserAgent} that cannot easily be
 * covered by integrations tests.
 */
@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class ClasspathResolvingUserAgentTest {

    private static final String NON_EXISTENT_CSS_URI_OUTSIDE_OF_APP_JAR =
            "/app/resources/assets/templates/letters/common/notify_letter_layout.css";
    private static final String CSS_URI_OUTSIDE_OF_APP_JAR =
            "src/test/resources/mock_assets/notify_letter_layout.css";
    private static final String RESOLVED_NON_EXISTENT_CSS_URI_OUTSIDE_OF_APP_JAR =
            "file:" + NON_EXISTENT_CSS_URI_OUTSIDE_OF_APP_JAR;
    private static final String CSS_URI_INSIDE_OF_APP_JAR =
            "nested:/opt/api/target/api.jar/!BOOT-INF/classes/!"
                    + "/assets/templates/letters/common/notify_letter_layout.css";

    @InjectMocks
    private ClasspathResolvingUserAgent agentUnderTest;

    @Mock
    private ITextOutputDevice iTextOutputDevice;

    @Test
    @DisplayName("Resolves URI for CSS file outside of application jar")
    void resolveUriForCssOutsideOfJar() {
        assertThat(agentUnderTest.resolveURI(NON_EXISTENT_CSS_URI_OUTSIDE_OF_APP_JAR),
                is(RESOLVED_NON_EXISTENT_CSS_URI_OUTSIDE_OF_APP_JAR));
    }

    @Test
    @DisplayName("Resolves URI for CSS file inside application jar")
    void resolveUriForCssInsideJar() {
        assertThat(agentUnderTest.resolveURI(CSS_URI_INSIDE_OF_APP_JAR),
                is(CSS_URI_INSIDE_OF_APP_JAR));
    }

    @Test
    @DisplayName("Does not open a stream for a non-existent CSS file outside of application jar")
    void resolveAndOpenStreamForNonExistentCssOutsideOfJar() {
        assertThat(agentUnderTest.resolveAndOpenStream(NON_EXISTENT_CSS_URI_OUTSIDE_OF_APP_JAR),
                is(nullValue()));
    }

    @Test
    @DisplayName("Opens a stream for a CSS file outside of application jar")
    void resolveAndOpenStreamForCssOutsideOfJar() {
        var css = new File(CSS_URI_OUTSIDE_OF_APP_JAR);
        assertThat(agentUnderTest.resolveAndOpenStream(css.getAbsolutePath()),
                is(notNullValue()));
    }

    @Test
    @DisplayName("Opens a stream for a CSS file inside application jar")
    void resolveAndOpenStreamForCssInsideJar() {
        assertThat(agentUnderTest.resolveAndOpenStream(CSS_URI_INSIDE_OF_APP_JAR),
                is(notNullValue()));
    }

}