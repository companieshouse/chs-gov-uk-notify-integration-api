package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import java.util.Objects;
import org.springframework.data.mongodb.core.mapping.Field;

public class EmailDetailsDao {
    @Field("template_id")
    private String templateId;

    @Field("personalisation_details")
    private String personalisationDetails;

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

    @Override
    public int hashCode() {
        return Objects.hash(personalisationDetails, templateId);
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
        EmailDetailsDao other = (EmailDetailsDao) obj;
        return Objects.equals(personalisationDetails, other.personalisationDetails)
                && Objects.equals(templateId, other.templateId);
    }

}