package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common;

import org.bson.Document;
import org.mockito.ArgumentMatcher;
import org.springframework.data.mongodb.core.query.Update;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.Preprocessors.Preprocessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.ParsingUtils.toMap;

public class ComparisonUtils {

    private final LocalDateTime now = LocalDateTime.now();

    private boolean mapsMatchOnAll(final Map<String, Object> actualMap, final Map<String, Object> referenceMap, final List<String> matchingFields) {
        for (final String key : matchingFields) {
            final var referenceValue = referenceMap.getOrDefault(key, null);
            final var actualValue = actualMap.getOrDefault(key, null);

            final var referenceIsNullButActualIsNotNull = Objects.isNull(referenceValue) && !Objects.isNull(actualValue);
            final var referenceDoesNotMatchActual = !Objects.isNull(referenceValue) && !referenceValue.equals(actualValue);

            if (referenceIsNullButActualIsNotNull || referenceDoesNotMatchActual) {
                return false;
            }
        }
        return true;
    }

    private boolean mapsDoNotMatchOnAll(final Map<String, Object> actualMap, final Map<String, Object> referenceMap, final List<String> nonmatchingFields) {
        for (final String key : nonmatchingFields) {
            final var referenceValue = referenceMap.getOrDefault(key, null);
            final var actualValue = actualMap.getOrDefault(key, null);

            final var referenceIsNullAndActualIsNull = Objects.isNull(referenceValue) && Objects.isNull(actualValue);
            final var referenceMatchesActual = !Objects.isNull(referenceValue) && referenceValue.equals(actualValue);

            if (referenceIsNullAndActualIsNull || referenceMatchesActual) {
                return false;
            }

        }
        return true;
    }

    private void applyPreprocessorsToMap(final Map<String, Object> map, final Map<String, Preprocessor> preprocessors) {
        for (final Entry<String, Preprocessor> entry : preprocessors.entrySet()) {
            final var key = entry.getKey();
            final var preprocessor = entry.getValue();
            final var value = map.getOrDefault(key, null);
            final var preprocesedValue = preprocessor.preprocess(value);
            map.put(key, preprocesedValue);
        }
    }

    public <T> ArgumentMatcher<T> compare(final T referenceObject, final List<String> matchingFields, final List<String> nonmatchingFields, final Map<String, Preprocessor> preprocessors) {
        final var referenceMap = toMap(referenceObject);
        applyPreprocessorsToMap(referenceMap, preprocessors);
        return actualObject -> {
            final var actualMap = toMap(actualObject);
            applyPreprocessorsToMap(actualMap, preprocessors);
            return mapsMatchOnAll(referenceMap, actualMap, matchingFields) && mapsDoNotMatchOnAll(referenceMap, actualMap, nonmatchingFields);
        };
    }

    public static ArgumentMatcher<Update> updateMatches(final Map<String, Object> expectedKeyValuePairs) {
        return update -> {
            final var document = update.getUpdateObject().get("$set", Document.class);
            return expectedKeyValuePairs.entrySet()
                    .stream()
                    .map(entry -> {
                        final var value = document.get(entry.getKey());
                        final var expectedValue = entry.getValue();
                        return value.equals(expectedValue);
                    })
                    .reduce((firstIsCorrect, secondIsCorrect) -> firstIsCorrect && secondIsCorrect)
                    .get();
        };
    }

}
