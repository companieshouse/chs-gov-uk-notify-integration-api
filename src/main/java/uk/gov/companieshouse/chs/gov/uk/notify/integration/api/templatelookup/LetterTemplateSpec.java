package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

/**
 * Specification of where a letter template can be found.
 * @param prefix the template resolver prefix identifying the assets directory in which the
 *               template resides
 * @param filename the name of the template file
 */
public record LetterTemplateSpec(String prefix, String filename) {
}
