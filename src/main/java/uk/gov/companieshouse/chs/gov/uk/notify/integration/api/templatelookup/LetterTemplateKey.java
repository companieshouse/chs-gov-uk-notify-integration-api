package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup;

import java.util.Set;

/**
 * Key identifying a letter template.
 * @param appId the ID of the client app (aka service) requesting the sending of the letter
 * @param id the template ID (aka name) corresponding to the type of letter to be sent
 */
public record LetterTemplateKey(String appId, String letterId, String templateId) {

    public static final String CHIPS_APPLICATION_ID = "chips";
    public static final String DIRECTION_LETTER = "direction_letter_v1";
    public static final String NEW_PSC_DIRECTION_LETTER = "new_psc_direction_letter_v1";
    public static final String TRANSITIONAL_NON_DIRECTOR_PSC_INFORMATION_LETTER =
            "transitional_non_director_psc_information_letter_v1";

    public static final LetterTemplateKey CHIPS_DIRECTION_LETTER_1 =
            new LetterTemplateKey(CHIPS_APPLICATION_ID, null, DIRECTION_LETTER);

    public static final LetterTemplateKey CHIPS_NEW_PSC_DIRECTION_LETTER_1 =
            new LetterTemplateKey(CHIPS_APPLICATION_ID, null, NEW_PSC_DIRECTION_LETTER);

    public static final LetterTemplateKey CHIPS_TRANSITIONAL_NON_DIRECTOR_PSC_INFORMATION_LETTER_1 =
            new LetterTemplateKey(
                    CHIPS_APPLICATION_ID, null,
                    TRANSITIONAL_NON_DIRECTOR_PSC_INFORMATION_LETTER);

    public static Set<LetterTemplateKey> CSIDVDEFLET_TEMPLATES = Set.of(
            new LetterTemplateKey(CHIPS_APPLICATION_ID, null, "CSIDVDEFLET_v1"),
            new LetterTemplateKey(CHIPS_APPLICATION_ID, null, "CSIDVDEFLET_v1.1"),
            new LetterTemplateKey(CHIPS_APPLICATION_ID, "CSIDVDEFLET", "v1.0")
    );

    public static Set<LetterTemplateKey> IDVPSCDEFAULT_TEMPLATES = Set.of(
            new LetterTemplateKey(CHIPS_APPLICATION_ID, null, "IDVPSCDEFAULT_v1"),
            new LetterTemplateKey(CHIPS_APPLICATION_ID, null, "IDVPSCDEFAULT_v1.1"),
            new LetterTemplateKey(CHIPS_APPLICATION_ID, "IDVPSCDEFAULT", "v1.0")
    );

    public static Set<LetterTemplateKey> IDVPSCEXT_TEMPLATES = Set.of(
            new LetterTemplateKey(CHIPS_APPLICATION_ID, null, "extension_acceptance_letter_v1"),
            new LetterTemplateKey(CHIPS_APPLICATION_ID, null, "second_extension_acceptance_letter_v1"),
            new LetterTemplateKey(CHIPS_APPLICATION_ID, "IDVPSCEXT1", "v1.0"),
            new LetterTemplateKey(CHIPS_APPLICATION_ID, "IDVPSCEXT2", "v1.0")
    );
}
