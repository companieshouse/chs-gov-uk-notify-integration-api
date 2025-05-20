package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class TemplateLookupTest {

    private static final BigDecimal THE_VERSION = BigDecimal.TEN;
    private static final ChLetterTemplate LOOKUP_KEY =
            new ChLetterTemplate("the_client_app", "the_letter", THE_VERSION);

    @Test
    @DisplayName("Template look up behaves as expected")
    void behavesAsExpected() {
        var templateLookup = new TemplateLookup();
        var locator = templateLookup.lookupTemplate(LOOKUP_KEY);
        assertThat(locator.prefix(),
                is(templateLookup.getLetterTemplatesRootDirectory() + LOOKUP_KEY.appId() + "/"));
        assertThat(locator.filename(), is(LOOKUP_KEY.id() + "_v" + LOOKUP_KEY.version()));
    }

}