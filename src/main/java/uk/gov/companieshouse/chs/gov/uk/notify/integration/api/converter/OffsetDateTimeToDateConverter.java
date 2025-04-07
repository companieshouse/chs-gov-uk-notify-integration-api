package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.converter;

import java.time.OffsetDateTime;
import java.util.Date;

import org.springframework.core.convert.converter.Converter;

public class OffsetDateTimeToDateConverter implements Converter<OffsetDateTime, Date> {
    @Override
    public Date convert(OffsetDateTime source) {
        return Date.from(source.toInstant());
    }
}
