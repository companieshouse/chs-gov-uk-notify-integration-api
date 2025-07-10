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

    private static final String CHIPS_APPLICATION_ID = "chips";

    public static final LetterTemplateKey CHIPS_DIRECTION_LETTER_1 =
            new LetterTemplateKey(CHIPS_APPLICATION_ID, "direction_letter", ONE);

    public static final LetterTemplateKey CHIPS_NEW_PSC_DIRECTION_LETTER_1 =
            new LetterTemplateKey(CHIPS_APPLICATION_ID, "new_psc_direction_letter", ONE);

    public static final LetterTemplateKey CHIPS_TRANSITIONAL_NON_DIRECTOR_PSC_INFORMATION_LETTER_1 =
            new LetterTemplateKey(
                    CHIPS_APPLICATION_ID,
                    "transitional_non_director_psc_information_letter",
                    ONE);

}
