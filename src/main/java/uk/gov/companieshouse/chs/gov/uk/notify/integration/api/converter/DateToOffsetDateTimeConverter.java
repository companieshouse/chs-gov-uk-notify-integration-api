package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.converter;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;

public class DateToOffsetDateTimeConverter implements Converter<Date, OffsetDateTime> {
    @Override
    public OffsetDateTime convert(Date source) {
        return source.toInstant().atOffset(ZoneOffset.UTC);
    }
}
