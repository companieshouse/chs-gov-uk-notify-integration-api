package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter;

import java.time.OffsetDateTime;
import java.util.Date;

import javax.validation.constraints.NotNull;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

public class OffsetDateTimeToDateConverter implements Converter<OffsetDateTime, Date> {
    @Override
    public Date convert(OffsetDateTime source) {
        return Date.from(source.toInstant());
    }
}
