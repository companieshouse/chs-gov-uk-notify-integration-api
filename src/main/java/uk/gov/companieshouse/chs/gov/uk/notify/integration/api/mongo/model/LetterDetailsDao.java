package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import java.util.Objects;
import org.springframework.data.mongodb.core.mapping.Field;

public class LetterDetailsDao {
    @Field("template_id")
    private String templateId;

    @Field("personalisation_details")
    private String personalisationDetails;

    @Field("letter_id")
    private String letterId;

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getPersonalisationDetails() {
        return personalisationDetails;
    }

    public void setPersonalisationDetails(String personalisationDetails) {
        this.personalisationDetails = personalisationDetails;
    }

    public String getLetterId() {
        return letterId;
    }

    public void setLetterId(String letterId) {
        this.letterId = letterId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(letterId, personalisationDetails, templateId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LetterDetailsDao other = (LetterDetailsDao) obj;
        return Objects.equals(letterId, other.letterId)
                && Objects.equals(personalisationDetails, other.personalisationDetails)
                && Objects.equals(templateId, other.templateId);
    }

}
