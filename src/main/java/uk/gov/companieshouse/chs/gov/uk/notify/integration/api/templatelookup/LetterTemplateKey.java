package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

import static java.math.BigDecimal.ONE;

import java.math.BigDecimal;

/**
 * Key identifying a letter template.
 * @param appId the ID of the client app (aka service) requesting the sending of the letter
 * @param id the template ID (aka name) corresponding to the type of letter to be sent
 * @param version the version of the template
 */
public record LetterTemplateKey(String appId, String id, BigDecimal version) {

    public static final LetterTemplateKey CHIPS_DIRECTION_LETTER_1 =
            new LetterTemplateKey("chips", "direction_letter", ONE);

    public static final LetterTemplateKey CHIPS_NEW_PSC_DIRECTION_LETTER_1 =
            new LetterTemplateKey("chips", "new_psc_direction_letter", ONE);

}
