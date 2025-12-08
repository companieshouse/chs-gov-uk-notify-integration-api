package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.context.Context;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class WelshDatesPublisherTest {

    @Test
    void publishWelshDates_addsWelshDateToMap() {
        Map<String, String> personalisation = new HashMap<>();
        personalisation.put("start_date", "01 January 2024");
        personalisation.put("name", "Hello");

        WelshDatesPublisher.publishWelshDates(personalisation);

        assertEquals("01 Ionawr 2024", personalisation.get("welsh_start_date"));
        assertEquals("01 January 2024", personalisation.get("start_date"));
        assertEquals("Hello", personalisation.get("name"));

    }

    @Test
    void publishWelshDates_contextSetsVariables() {

        Context context = new Context();
        context.setVariable("dob_date", "02 February 2020");
        context.setVariable("name", "Hello");

        WelshDatesPublisher.publishWelshDates(context);

        assertEquals("02 Chwefror 2020", context.getVariable("welsh_dob_date"));
        assertEquals("02 February 2020", context.getVariable("dob_date"));
        assertEquals("Hello", context.getVariable("name"));

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
}
