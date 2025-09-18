package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class TemplateLookupTest {
    private TemplateLookup templateLookup;

    @BeforeEach
    void setUp() {
        templateLookup = new TemplateLookup();
    }

    @Test
    @DisplayName("Template look up behaves as expected when a version is provided")
    @Deprecated(forRemoval = true, since = "Version is deprecated in LetterTemplateKey. This test should be removed in future")
    void behavesAsExpectedWithVersion() {
        LetterTemplateKey lookupKey =
                new LetterTemplateKey("the_client_app", "the_letter", BigDecimal.TEN);
        var locator = templateLookup.lookupTemplate(lookupKey);
        assertThat(locator.prefix(),
                is(templateLookup.getLetterTemplatesRootDirectory() + lookupKey.appId() + "/"));
        assertThat(locator.filename(), is(lookupKey.id() + "_v" + lookupKey.version()));
    }

    @Test
    @DisplayName("Template look up behaves as expected")
    void behavesAsExpected() {
        LetterTemplateKey lookupKey =
                new LetterTemplateKey("the_client_app", "the_letter", null);
        var locator = templateLookup.lookupTemplate(lookupKey);
        assertThat(locator.prefix(),
                is(templateLookup.getLetterTemplatesRootDirectory() + lookupKey.appId() + "/"));
        assertThat(locator.filename(), is(lookupKey.id()));
    }

}