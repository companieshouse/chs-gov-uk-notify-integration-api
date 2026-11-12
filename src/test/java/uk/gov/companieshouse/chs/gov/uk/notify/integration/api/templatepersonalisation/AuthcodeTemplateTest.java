package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.Constants.DATE_FORMATTER;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ORIGINAL_SENDING_DATE;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config.ThymeleafConfig;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model.AddressDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.TemplateLookup;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.validation.TemplateContextValidator;

@Tag("unit-test")
class AuthcodeTemplateTest {

    private static final Map<String, String> PERSONALISATION = Map.of(
            "recipient_name", "Ms Example Director",
            "company_name", "EXAMPLE & SONS LIMITED",
            "company_number", "01234567",
            "authcode", "AB12CD");

    private TemplatePersonaliser personaliser;
    private AddressDao address;

    @BeforeEach
    void setUp() {
        var config = new ThymeleafConfig();
        var resolver = config.getTemplateResolver();
        personaliser = new TemplatePersonaliser(config.getTemplateEngine(resolver),
                new TemplateLookup(), resolver, new TemplateContextValidator(), new PathsPublisher());
        address = new AddressDao();
        address.setAddressLine1("Ms Example Director");
        address.setAddressLine2("100 Example Street");
        address.setAddressLine3("London");
        address.setAddressLine7("SW1P 1JP");
    }

    @Test
    void rendersAuthcodeLetterWithCompanyDetailsAndAddress() {
        var html = personalise(PERSONALISATION);
        var letter = Jsoup.parse(html);
        var text = letter.text();

        assertThat(text, containsString("Company name: EXAMPLE & SONS LIMITED"));
        assertThat(text, containsString("Company number: 01234567"));
        assertThat(text, containsString("Authentication code: AB12CD"));
        assertThat(text, containsString("Companies House online filing services"));
        assertThat(letter.select("#address-table").text(),
                is("Ms Example Director 100 Example Street London SW1P 1JP"));
        assertThat(text, containsString("Ms Example Director"));
        assertThat(letter.select("#letter-date").text(), is(LocalDate.now().format(DATE_FORMATTER)));
        assertThat(html, containsString("EXAMPLE &amp; SONS LIMITED"));
    }

    @Test
    void rejectsPayloadWithoutCompanyName() {
        var personalisation = new HashMap<>(PERSONALISATION);
        personalisation.remove("company_name");

        var exception = assertThrows(LetterValidationException.class,
                () -> personalise(personalisation));

        assertThat(exception.getMessage(), containsString("[company_name] missing"));
    }

    @Test
    void rejectsOriginalPayloadWithoutCompanyNumber() {
        var personalisation = new HashMap<>(PERSONALISATION);
        personalisation.remove("company_number");

        var exception = assertThrows(LetterValidationException.class,
                () -> personalise(personalisation));

        assertThat(exception.getMessage(), containsString("[company_number] missing"));
    }

    @Test
    void preservesOriginalSendingDateWhenRegeneratingLetter() {
        var personalisation = new HashMap<>(PERSONALISATION);
        personalisation.put(ORIGINAL_SENDING_DATE, "12 November 2026");

        var letter = Jsoup.parse(personalise(personalisation));

        assertThat(letter.select("#letter-date").text(), is("12 November 2026"));
    }

    private String personalise(Map<String, String> personalisation) {
        return personaliser.personaliseLetterTemplate(LetterTemplateKey.AUTHCODE_TEMPLATE,
                "test-reference", personalisation, address);
    }
}
