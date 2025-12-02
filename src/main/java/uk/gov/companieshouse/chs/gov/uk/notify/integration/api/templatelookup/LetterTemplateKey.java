package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

import java.util.Set;

/**
 * Key identifying a letter template.
 * @param appId the ID of the client app (aka service) requesting the sending of the letter
 * @param id the template ID (aka name) corresponding to the type of letter to be sent
 */
public record LetterTemplateKey(String appId, String id) {

    public static final String CHIPS_APPLICATION_ID = "chips";
    public static final String DIRECTION_LETTER = "direction_letter_v1";
    public static final String NEW_PSC_DIRECTION_LETTER = "new_psc_direction_letter_v1";
    public static final String TRANSITIONAL_NON_DIRECTOR_PSC_INFORMATION_LETTER =
            "transitional_non_director_psc_information_letter_v1";

    public static final LetterTemplateKey CHIPS_DIRECTION_LETTER_1 =
            new LetterTemplateKey(CHIPS_APPLICATION_ID, DIRECTION_LETTER);

    public static final LetterTemplateKey CHIPS_NEW_PSC_DIRECTION_LETTER_1 =
            new LetterTemplateKey(CHIPS_APPLICATION_ID, NEW_PSC_DIRECTION_LETTER);

    public static final LetterTemplateKey CHIPS_TRANSITIONAL_NON_DIRECTOR_PSC_INFORMATION_LETTER_1 =
            new LetterTemplateKey(
                    CHIPS_APPLICATION_ID,
                    TRANSITIONAL_NON_DIRECTOR_PSC_INFORMATION_LETTER);

    public static final LetterTemplateKey CHIPS_EXTENSION_ACCEPTANCE_LETTER_1 =
            new LetterTemplateKey(CHIPS_APPLICATION_ID, "extension_acceptance_letter_v1");

    public static final LetterTemplateKey CHIPS_SECOND_EXTENSION_ACCEPTANCE_LETTER_1 =
            new LetterTemplateKey(
                    CHIPS_APPLICATION_ID,
                    "second_extension_acceptance_letter_v1");

    public static Set<LetterTemplateKey> CSIDVDEFLET_TEMPLATES = Set.of(
            new LetterTemplateKey(CHIPS_APPLICATION_ID, "CSIDVDEFLET_v1"),
            new LetterTemplateKey(CHIPS_APPLICATION_ID, "CSIDVDEFLET_v1.1")
    );

    public static Set<LetterTemplateKey> IDVPSCDEFAULT_TEMPLATES = Set.of(
            new LetterTemplateKey(CHIPS_APPLICATION_ID, "IDVPSCDEFAULT_v1"),
            new LetterTemplateKey(CHIPS_APPLICATION_ID, "IDVPSCDEFAULT_v1.1"),
            new LetterTemplateKey(CHIPS_APPLICATION_ID, "IDVPSCDEFAULT_WELSH_v1")
    );
}
