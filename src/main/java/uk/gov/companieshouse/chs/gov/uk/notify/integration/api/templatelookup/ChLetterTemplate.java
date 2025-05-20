package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

import java.math.BigDecimal;

/**
 * Key identifying a letter template.
 * @param appId the ID of the client app (aka service) requesting the sending of the letter
 * @param id the template ID (aka name) corresponding to the type of letter to be sent
 * @param version the version of the template
 */
public record ChLetterTemplate(String appId, String id, BigDecimal version) {
}
