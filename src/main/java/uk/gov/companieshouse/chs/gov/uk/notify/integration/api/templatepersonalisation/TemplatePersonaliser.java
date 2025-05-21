package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import static org.apache.commons.lang.StringUtils.isBlank;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_2;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_3;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_4;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_5;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_6;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.COMPANY_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.POSTCODE_OR_COUNTRY;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.REFERENCE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChLetterTemplate;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.TemplateLookup;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.validation.TemplateContextValidator;

@Component
public class TemplatePersonaliser {

    private final ITemplateEngine templateEngine;
    private final TemplateLookup templateLookup;
    private final AbstractConfigurableTemplateResolver templateResolver;
    private final TemplateContextValidator validator;

    public TemplatePersonaliser(ITemplateEngine templateEngine,
                                TemplateLookup templateLookup,
                                AbstractConfigurableTemplateResolver templateResolver,
                                TemplateContextValidator validator) {
        this.templateEngine = templateEngine;
        this.templateLookup = templateLookup;
        this.templateResolver = templateResolver;
        this.validator = validator;
    }

    /**
     * Populates the letter Thymeleaf templateLookupKey with the data for the letter.
     * @param templateLookupKey the {@link ChLetterTemplate} identifying the template to be used
     * @param reference the letter reference
     * @param  personalisationDetails the {@link Map} providing the data to be substituted into the
     *               letter templateLookupKey substitution variables
     * @return the HTML representation of the letter
     */
    public String personaliseLetterTemplate(ChLetterTemplate templateLookupKey,
                                            String reference,
                                            Map<String, String> personalisationDetails,
                                            Address address) {

        var context = new Context();

        // Use today's date for traceability.
        var format = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        context.setVariable(DATE, LocalDate.now().format(format));

        context.setVariable(REFERENCE, reference);

        var upperCaseCompanyName = getUpperCasedCompanyName(personalisationDetails);
        populateAddress(context, address, upperCaseCompanyName);
        personaliseLetter(context, personalisationDetails, upperCaseCompanyName);

        validator.validateContextForTemplate(context, templateLookupKey);

        var templateSpec = templateLookup.lookupTemplate(templateLookupKey);
        templateResolver.setPrefix(templateSpec.prefix());
        return templateEngine.process(templateSpec.filename(), context);
    }

    private String getUpperCasedCompanyName(Map<String, String> personalisationDetails) {
        // Company name must be provided and is always rendered in UPPER CASE in the letter.
        var companyName = personalisationDetails.get(COMPANY_NAME);
        if (isBlank(companyName)) {
            throw new LetterValidationException(
                    "No company name found in the letter personalisation details.");
        }
        return companyName.toUpperCase();
    }

    @SuppressWarnings("java:S1135") // TODO left in place intentionally for now.
    private void populateAddress(Context context, Address address, String upperCaseCompanyName) {
        var addressLines = Map.of(
                ADDRESS_LINE_1, address.getAddressLine1(),
                ADDRESS_LINE_2, address.getAddressLine2(),
                ADDRESS_LINE_3, address.getAddressLine3(),
                ADDRESS_LINE_4, address.getAddressLine4(),
                ADDRESS_LINE_5, address.getAddressLine5(),
                ADDRESS_LINE_6, address.getAddressLine6(),

                // TODO DEEP-287 postcode_or_country or just line 7?
                // Consider populating this field with the last populated address line...
                POSTCODE_OR_COUNTRY, address.getAddressLine7()
        );

        addressLines.forEach((key, value) ->
                context.setVariable(key, uppercaseIfCompanyName(value, upperCaseCompanyName)));
    }

    private void personaliseLetter(Context context,
                                   Map<String, String> personalisationDetails,
                                   String upperCaseCompanyName) {
        personalisationDetails.forEach((key, value) ->
                context.setVariable(key, uppercaseIfCompanyName(value, upperCaseCompanyName)));
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
