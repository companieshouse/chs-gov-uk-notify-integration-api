package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import java.util.Objects;
import org.springframework.data.mongodb.core.mapping.Field;

public class SenderDetailsDao {
    @Field("app_id")
    private String appId;

    @Field("reference")
    private String reference;

    @Field("name")
    private String name;

    @Field("user_id")
    private String userId;

    @Field("email_address")
    private String emailAddress;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(appId, emailAddress, name, reference, userId);
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
        SenderDetailsDao other = (SenderDetailsDao) obj;
        return Objects.equals(appId, other.appId)
                && Objects.equals(emailAddress, other.emailAddress)
                && Objects.equals(name, other.name) && Objects.equals(reference, other.reference)
                && Objects.equals(userId, other.userId);
    }

}