package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import org.springframework.data.mongodb.core.mapping.Field;
import java.time.OffsetDateTime;
import java.util.Objects;

public class EmailRequestDao {
    @Field("senderDetails")
    private SenderDetailsDao senderDetails;

    @Field("recipientDetails")
    private EmailRecipientDetailsDao recipientDetails;

    @Field("emailDetails")
    private EmailDetailsDao emailDetails;

    @Field("createdAt")
    private OffsetDateTime createdAt;

    public SenderDetailsDao getSenderDetails() {
        return senderDetails;
    }

    public void setSenderDetails(SenderDetailsDao senderDetails) {
        this.senderDetails = senderDetails;
    }

    public EmailRecipientDetailsDao getRecipientDetails() {
        return recipientDetails;
    }

    public void setRecipientDetails(EmailRecipientDetailsDao recipientDetails) {
        this.recipientDetails = recipientDetails;
    }

    public EmailDetailsDao getEmailDetails() {
        return emailDetails;
    }

    public void setEmailDetails(EmailDetailsDao emailDetails) {
        this.emailDetails = emailDetails;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdAt, emailDetails, recipientDetails, senderDetails);
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
        EmailRequestDao other = (EmailRequestDao) obj;
        return Objects.equals(createdAt, other.createdAt)
                && Objects.equals(emailDetails, other.emailDetails)
                && Objects.equals(recipientDetails, other.recipientDetails)
                && Objects.equals(senderDetails, other.senderDetails);
    }

}
