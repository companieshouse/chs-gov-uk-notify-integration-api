package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import static org.apache.commons.lang.StringUtils.isEmpty;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChLetterTemplate;

@Component
public class TemplatePersonaliser {

    private final ITemplateEngine templateEngine;

    public TemplatePersonaliser(ITemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    /**
     * Populates the letter Thymeleaf template with the data for the letter.
     * @param template the {@link ChLetterTemplate} identifying the template to be used
     * @param  personalisationDetails the {@link Map} providing the data to be substituted into the
     *               letter template substitution variables
     * @return the HTML representation of the letter
     */
    public String personaliseLetterTemplate(ChLetterTemplate template,
                                            Map<String, String> personalisationDetails,
                                            Address address) {

        var context = new Context();

        // Use today's date for traceability.
        var format = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        context.setVariable("date", LocalDate.now().format(format));

        var upperCaseCompanyName = getUpperCasedCompanyName(personalisationDetails);
        populateAddress(context, address, upperCaseCompanyName);
        personaliseLetter(context, personalisationDetails, upperCaseCompanyName);

        return templateEngine.process(template.id(), context);
    }

    private String getUpperCasedCompanyName(Map<String, String> personalisationDetails) {
        // Company name must be provided and is always rendered in UPPER CASE in the letter.
        if (isEmpty(personalisationDetails.get("company_name"))) {
            throw new LetterValidationException(
                    "No company name found in the letter personalisation details.");
        }
        return personalisationDetails.get("company_name").toUpperCase();
    }

    private void populateAddress(Context context, Address address, String upperCaseCompanyName) {
        context.setVariable("address_line_1",
                uppercaseIfCompanyName(address.getAddressLine1(), upperCaseCompanyName));
        context.setVariable("address_line_2",
                uppercaseIfCompanyName(address.getAddressLine2(), upperCaseCompanyName));
        context.setVariable("address_line_3",
                uppercaseIfCompanyName(address.getAddressLine3(), upperCaseCompanyName));
        context.setVariable("address_line_4",
                uppercaseIfCompanyName(address.getAddressLine4(), upperCaseCompanyName));
        context.setVariable("address_line_5",
                uppercaseIfCompanyName(address.getAddressLine5(), upperCaseCompanyName));
        context.setVariable("address_line_6",
                uppercaseIfCompanyName(address.getAddressLine6(), upperCaseCompanyName));
        // TODO DEEP-287 postcode_or_country or just line 7?
        context.setVariable("postcode_or_country", address.getAddressLine7());
    }

    private void personaliseLetter(Context context,
                                   Map<String, String> personalisationDetails,
                                   String upperCaseCompanyName) {
        personalisationDetails.keySet().forEach(name ->
                context.setVariable(name,
                        uppercaseIfCompanyName(personalisationDetails.get(name),
                                upperCaseCompanyName)));
    }

    /**
     * If the address line is the company name, this serves to replace the company name with an
     * uppercased version of the same.
     *
     * @param addressLine          the address line to be checked
     * @param uppercaseCompanyName the uppercased company name
     * @return either the uppercased version of the company name if the address line is the
    company name, or otherwise the original address line, unchanged.
     */
    private String uppercaseIfCompanyName(String addressLine, String uppercaseCompanyName) {
        return addressLine != null && addressLine.equalsIgnoreCase(uppercaseCompanyName)
                ? uppercaseCompanyName : addressLine;
    }
}
