package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

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
    @DisplayName("Template look up behaves as expected")
    void behavesAsExpected() {
        LetterTemplateKey lookupKey =
                new LetterTemplateKey("the_client_app", "the_letter", "the_template");
        var locator = templateLookup.lookupTemplate(lookupKey);
        var expectedPrefix =
                templateLookup.getLetterTemplatesRootDirectory()
                        + lookupKey.appId() + "/" + lookupKey.letterId() + "/";
        assertThat(locator.prefix(), is(expectedPrefix));
        assertThat(locator.filename(), is(lookupKey.templateId()));
    }

}