package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants;

import java.time.format.DateTimeFormatter;

public class Constants {
    private Constants() {
    }

    public static final String DATE_FORMAT = "dd MMMM yyyy";
    public static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_FORMAT);
}
