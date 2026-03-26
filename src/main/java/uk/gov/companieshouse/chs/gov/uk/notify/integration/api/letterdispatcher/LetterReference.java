package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.letterdispatcher;

import org.apache.commons.lang3.StringUtils;

public record LetterReference(String appId, String letterId, String reference) {

    public String getFullReference() {
        if (StringUtils.isBlank(letterId)) {
            // Old letters do not have letter IDs and use just the reference
            return reference;
        } else {
            return String.join("-", appId, letterId, reference);
        }
    }
}
