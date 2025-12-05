package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class WelshDatesPublisherTest {

    private final WelshDatesPublisher publisher = new WelshDatesPublisher();

    @Test
    void publishWelshDates_addsWelshDateToMap() {
        Map<String, String> personalisation = new HashMap<>();
        personalisation.put("start_date", "01 January 2024");

        publisher.publishWelshDates(personalisation);

        assertEquals("01 Ionawr 2024", personalisation.get("welsh_start_date"));
        assertEquals("01 January 2024", personalisation.get("start_date"));

    }

    @Test
    void getWelshDate_throwsOnInvalidFormat() {
        assertThrows(LetterValidationException.class,
                () -> WelshDatesPublisher.getWelshDate("01-Jan-2024", "start_date"));
    }

    @Test
    void getWelshDate_throwsOnUnknownMonth() {
        assertThrows(LetterValidationException.class,
                () -> WelshDatesPublisher.getWelshDate("01 Foo 2024", "start_date"));
    }

    @Test
    void publishWelshDates_contextSetsVariables(@Mock Context context) {
        when(context.getVariableNames()).thenReturn(Set.of("dob_date", "name"));
        when(context.getVariable("dob_date")).thenReturn("02 February 2020");
        // name is not a _date so won't be used; no need to stub context.getVariable("name")

        publisher.publishWelshDates(context);

        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(context).setVariables(captor.capture());

        Map<String, Object> setVars = captor.getValue();
        assertEquals("02 Chwefror 2020", setVars.get("welsh_dob_date"));
        // ensure other variables in the context were not added or changed
        assertNull(setVars.get("name"));
        verify(context, org.mockito.Mockito.never()).getVariable("name");

    }
}
