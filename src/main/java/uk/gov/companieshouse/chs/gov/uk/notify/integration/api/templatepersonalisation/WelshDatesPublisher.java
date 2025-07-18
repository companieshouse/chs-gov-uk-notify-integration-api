package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import static java.util.AbstractMap.SimpleEntry;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;

/**
 * Responsible for making Welsh dates variables available for use in the Thymeleaf templates. This
 * assumes all date personalisation details have names (keys) ending with the suffix
 * <code>_date</code>. It publishes each corresponding Welsh date to the context under the
 * name <code>welsh_&lt;date variable name&gt;</code>.
 */
@Component
public class WelshDatesPublisher {

    private static final String DATE_VARIABLE_NAME_SUFFIX = "_date";
    private static final String WELSH_DATE_VARIABLE_NAME_PREFIX = "welsh_";

    private static final Map<String,String> WELSH_MONTHS_DICTIONARY =
             Map.ofEntries(
                     new SimpleEntry<>("January",   "Ionawr"),
                     new SimpleEntry<>("February",  "Chwefror"),
                     new SimpleEntry<>("March",     "Mawrth"),
                     new SimpleEntry<>("April",     "Ebrill"),
                     new SimpleEntry<>("May",       "Mai"),
                     new SimpleEntry<>("June",      "Mehefin"),
                     new SimpleEntry<>("July",      "Gorffennaf"),
                     new SimpleEntry<>("August",    "Awst"),
                     new SimpleEntry<>("September", "Medi"),
                     new SimpleEntry<>("October",   "Hydref"),
                     new SimpleEntry<>("November",  "Tachwedd"),
                     new SimpleEntry<>("December",  "Rhagfyr"));

    public void publishWelshDatesViaContext(final Context context) {
        var welshDateVariables = context.getVariableNames().stream()
                .filter(variableName -> variableName.endsWith(DATE_VARIABLE_NAME_SUFFIX))
                .collect(Collectors.toMap(
                        variableName -> WELSH_DATE_VARIABLE_NAME_PREFIX + variableName,
                        variableName -> (Object) getWelshDate(
                                (String) context.getVariable(variableName), variableName)));
        context.setVariables(welshDateVariables);
    }

    public static String getWelshDate(final String englishDate, final String dateVariableName) {
        var dayMonthYear = englishDate.split(" ");
        if (dayMonthYear.length != 3) {
            throw new LetterValidationException("Format of date '" + dateVariableName
                    + "' '" + englishDate + "' is incorrect.");
        }
        var month = dayMonthYear[1];
        var welshMonth = WELSH_MONTHS_DICTIONARY.get(month);
        if (welshMonth == null) {
            throw new LetterValidationException("Unknown month '" + month
                    + "' found in '"  + dateVariableName + "' date '" + englishDate + "'.");
        }
        return dayMonthYear[0] + " " + welshMonth + " " + dayMonthYear[2];
    }

}
