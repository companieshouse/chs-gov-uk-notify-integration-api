package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import static java.util.AbstractMap.SimpleEntry;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.thymeleaf.context.Context;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;

/**
 * Responsible for making Welsh dates variables available for use in the Thymeleaf templates. This
 * assumes all date personalisation details have names (keys) ending with the suffix
 * <code>_date</code>. It publishes each corresponding Welsh date to the context under the
 * name <code>welsh_&lt;date variable name&gt;</code>.
 */
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

    private WelshDatesPublisher() {
        // private constructor for static class so that it cannot be instantiated
    }

    public static void publishWelshDates(final Context context) {
        var welshDateVariables = extractWelshDates(context.getVariableNames(), context::getVariable);
        Map<String, Object> welshDateVariablesObject = new HashMap<>(welshDateVariables);
        context.setVariables(welshDateVariablesObject);
    }

    public static void publishWelshDates(final Map<String, Object> personalisationDetails) {
        var welshDateVariables = extractWelshDates(personalisationDetails.keySet(), personalisationDetails::get);
        personalisationDetails.putAll(welshDateVariables);
    }

    private static Map<String, String> extractWelshDates(final Set<String> variableNames, Function<String, Object> dateGetter) {
        return variableNames.stream()
                .filter(variableName -> variableName.endsWith(DATE_VARIABLE_NAME_SUFFIX)) // Only replace date values
                .map(variableName -> Map.entry(variableName, dateGetter.apply(variableName))) // map to name => value
                .filter(e -> e.getValue() instanceof String) // Only replace string objects
                .collect(Collectors.toMap(
                        e -> WELSH_DATE_VARIABLE_NAME_PREFIX + e.getKey(),
                        e -> getWelshDate((String) e.getValue(), e.getKey()))
                );
    }

    private static String getWelshDate(final String value, final String dateVariableName) {
        try {
            return getWelshDate(value);
        } catch (LetterValidationException ex) {
            throw new LetterValidationException(ex.getMessage() + " for " + dateVariableName);
        }
    }

    public static String getWelshDate(final String englishDate) {
        var dayMonthYear = englishDate.split(" ");
        if (dayMonthYear.length != 3) {
            throw new LetterValidationException("Incorrect date format '" + englishDate + "'");
        }
        var month = dayMonthYear[1];
        var welshMonth = WELSH_MONTHS_DICTIONARY.get(month);
        if (welshMonth == null) {
            throw new LetterValidationException(
                    "Unknown month '" + month + "' in date '" + englishDate + "'");
        }
        return dayMonthYear[0] + " " + welshMonth + " " + dayMonthYear[2];
    }

}
