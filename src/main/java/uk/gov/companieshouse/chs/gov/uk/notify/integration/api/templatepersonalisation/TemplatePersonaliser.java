package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ACTION_DUE_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.Constants.DATE_FORMATTER;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_2;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_3;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_4;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_5;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_6;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ADDRESS_LINE_7;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.COMPANY_NAME;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.EXTENSION_REQUEST_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.IDV_START_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ORIGINAL_SENDING_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.REFERENCE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.TODAYS_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.TRIGGERING_EVENT_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CHIPS_DIRECTION_LETTER_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CHIPS_EXTENSION_ACCEPTANCE_LETTER_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CHIPS_NEW_PSC_DIRECTION_LETTER_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CHIPS_TRANSITIONAL_NON_DIRECTOR_PSC_INFORMATION_LETTER_1;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey.CSIDVDEFLET;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.TemplateLookup;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.validation.TemplateContextValidator;

@Component
public class TemplatePersonaliser {

    /**
     * Those letter types for which the letter date is today's date.
     */
    private static final List<LetterTemplateKey> LETTERS_WITH_TODAYS_DATE =
            List.of(CHIPS_DIRECTION_LETTER_1,
                    CHIPS_TRANSITIONAL_NON_DIRECTOR_PSC_INFORMATION_LETTER_1,
                    CSIDVDEFLET);

    private final ITemplateEngine templateEngine;
    private final TemplateLookup templateLookup;
    private final AbstractConfigurableTemplateResolver templateResolver;
    private final TemplateContextValidator validator;
    private final PathsPublisher pathsPublisher;
    private final WelshDatesPublisher welshDatesPublisher;

    public TemplatePersonaliser(ITemplateEngine templateEngine,
                                TemplateLookup templateLookup,
                                AbstractConfigurableTemplateResolver templateResolver,
                                TemplateContextValidator validator,
                                PathsPublisher pathsPublisher,
                                WelshDatesPublisher welshDatesPublisher) {
        this.templateEngine = templateEngine;
        this.templateLookup = templateLookup;
        this.templateResolver = templateResolver;
        this.validator = validator;
        this.pathsPublisher = pathsPublisher;
        this.welshDatesPublisher = welshDatesPublisher;
    }

    /**
     * Populates the letter Thymeleaf template with the data for the letter.
     * @param templateLookupKey the {@link LetterTemplateKey} identifying the template to be used
     * @param reference the letter reference
     * @param personalisationDetails the {@link Map} providing the data to be substituted into the
     *                               letter template substitution variables
     * @param address the {@link Address} providing the data to be substituted into the
     *                letter address block template substitution variables
     * @return the HTML representation of the letter
     */
    public String personaliseLetterTemplate(LetterTemplateKey templateLookupKey,
                                            String reference,
                                            Map<String, String> personalisationDetails,
                                            Address address) {

        validatePersonalisationDetails(personalisationDetails);

        var context = new Context();
        populateLetterWithDynamicDates(context, personalisationDetails, templateLookupKey);
        populateLetterWithTriggeringEventDate(context, personalisationDetails, templateLookupKey);
        context.setVariable(REFERENCE, reference);
        var upperCaseCompanyName = getUpperCasedCompanyName(personalisationDetails);
        populateAddress(context, address, upperCaseCompanyName);
        personaliseLetter(context, personalisationDetails, upperCaseCompanyName);
        pathsPublisher.publishPathsViaContext(context, templateLookupKey);
        welshDatesPublisher.publishWelshDatesViaContext(context);

        validator.validateContextForTemplate(context, templateLookupKey);

        var templateSpec = templateLookup.lookupTemplate(templateLookupKey);
        templateResolver.setPrefix(templateSpec.prefix());
        return templateEngine.process(templateSpec.filename(), context);
    }

    private void validatePersonalisationDetails(Map<String, String> personalisationDetails) {
        // To avoid confusion and the possibility of personalisation details
        // overwriting values provided in other details, we prevent certain fields from
        // appearing in the personalisation details.
        if (!isBlank(personalisationDetails.get(REFERENCE))) {
            throw new LetterValidationException(
                    "The key field reference must not appear in the personalisation details.");
        }
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

    private void populateAddress(Context context, Address address, String upperCaseCompanyName) {
        var addressLines = Map.of(
                ADDRESS_LINE_1, blankIfNull(address.getAddressLine1()),
                ADDRESS_LINE_2, blankIfNull(address.getAddressLine2()),
                ADDRESS_LINE_3, blankIfNull(address.getAddressLine3()),
                ADDRESS_LINE_4, blankIfNull(address.getAddressLine4()),
                ADDRESS_LINE_5, blankIfNull(address.getAddressLine5()),
                ADDRESS_LINE_6, blankIfNull(address.getAddressLine6()),
                ADDRESS_LINE_7, blankIfNull(address.getAddressLine7())
        );

        addressLines.forEach((key, value) -> {
            if (!isBlank(value)) {
                context.setVariable(key, uppercaseIfCompanyName(value, upperCaseCompanyName));
            }
        });
    }

    private String blankIfNull(final String value) {
        return value == null ? "" : value;
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

    /**
     * Provides the current or original sending date as required for some letter types as "today's
     * date".
     * Also adds other date variables as required by some letter types.
     *
     * @param context the Thymeleaf context holding variables for template population
     * @param personalisationDetails the {@link Map} providing the data to be substituted into the
     *                               letter template substitution variables, from which the value
     *                               to be used for the <code>triggering_event_date</code> may be
     *                               obtained
     * @param templateLookupKey the key used to determine which letter type we are dealing with
     */
    private void populateLetterWithDynamicDates(Context context,
                                              Map<String, String> personalisationDetails,
                                              LetterTemplateKey templateLookupKey) {
        if (LETTERS_WITH_TODAYS_DATE.contains(templateLookupKey)) {
            String date;
            if (personalisationDetails.containsKey(ORIGINAL_SENDING_DATE)) {
                // Then we are regenerating a previously sent letter. Use its sending date.
                date = personalisationDetails.get(ORIGINAL_SENDING_DATE);
            } else {
                // Else we are sending the letter now. Use today's date.
                date = LocalDate.now().format(DATE_FORMATTER);
            }
            context.setVariable(TODAYS_DATE, date);
        }
        if(CSIDVDEFLET.equals(templateLookupKey)) {
            context.setVariable(ACTION_DUE_DATE, LocalDate.now().plusDays(28).format(DATE_FORMATTER));
        }
    }

    /**
     * Populates the <code>triggering_event_date</code> context variable with the value of the
     * corresponding date context variable where appropriate.
     * @param context the Thymeleaf context holding variables for template population
     * @param personalisationDetails the {@link Map} providing the data to be substituted into the
     *                               letter template substitution variables, from which the value
     *                               to be used for the <code>triggering_event_date</code> may be
     *                               obtained
     * @param templateLookupKey the key used to determine which letter type we are dealing with
     */
    private void populateLetterWithTriggeringEventDate(Context context,
                                                       Map<String, String> personalisationDetails,
                                                       LetterTemplateKey templateLookupKey) {
        if (templateLookupKey.equals(CHIPS_NEW_PSC_DIRECTION_LETTER_1)) {
            context.setVariable(TRIGGERING_EVENT_DATE, personalisationDetails.get(IDV_START_DATE));
        } else if (templateLookupKey.equals(CHIPS_EXTENSION_ACCEPTANCE_LETTER_1)) {
            context.setVariable(TRIGGERING_EVENT_DATE,
                    personalisationDetails.get(EXTENSION_REQUEST_DATE));
        }
    }
}
