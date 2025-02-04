package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.Preprocessors;

import java.util.Objects;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.ParsingUtils.reduceTimestampResolution;

public class ReduceTimeStampResolutionPreprocessor extends Preprocessor {

    @Override
    public Object preprocess(final Object object) {
        return Objects.isNull(object) ? null : reduceTimestampResolution((String) object);
    }

}
