package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import java.util.Objects;
import org.springframework.data.mongodb.core.mapping.Field;

public class EmailRecipientDetailsDao {
    @Field("name")
    private String name;

    @Field("email_address")
    private String emailAddress;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, emailAddress);
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
        EmailRecipientDetailsDao other = (EmailRecipientDetailsDao) obj;
        return Objects.equals(name, other.name)
                && Objects.equals(emailAddress, other.emailAddress);
    }

}