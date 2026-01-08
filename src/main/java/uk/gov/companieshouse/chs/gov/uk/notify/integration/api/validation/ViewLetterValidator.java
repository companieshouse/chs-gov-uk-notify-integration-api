package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.validation;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.ViewLetterValidationException;

@Component
public class ViewLetterValidator {

    /**
     * Validates the inputs provided to ensure that at least one of them is not blank.
     * @param pscName the PSC name on the letter(s) to be viewed
     * @param letterId the letter ID on the letter(s) to be viewed
     */
    public void validateViewLetterInputs(final String pscName, final String letterId) {
        if (isBlank(pscName) && isBlank(letterId)) {
            throw new ViewLetterValidationException("PSC name [" + pscName
                    + "] and/or letter ID [" + letterId + "] cannot be null or blank.");
        }
    }

}
