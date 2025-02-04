package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import java.util.Objects;

public abstract class YourRoleAtAcspHasChangedEmailData extends BaseEmailData<YourRoleAtAcspHasChangedEmailData> {

    private String editedBy;

    private String acspName;

    private String signinUrl;

    protected YourRoleAtAcspHasChangedEmailData() {
    }

    protected YourRoleAtAcspHasChangedEmailData(final String to, final String editedBy, final String acspName, final String signinUrl) {
        setTo(to);
        this.editedBy = editedBy;
        this.acspName = acspName;
        this.signinUrl = signinUrl;
        setSubject();
    }

    public void setEditedBy(final String editedBy) {
        this.editedBy = editedBy;
    }

    public YourRoleAtAcspHasChangedEmailData editedBy(final String editedBy) {
        setEditedBy(editedBy);
        return this;
    }

    public String getEditedBy() {
        return editedBy;
    }

    public void setAcspName(final String acspName) {
        this.acspName = acspName;
    }

    public YourRoleAtAcspHasChangedEmailData acspName(final String acspName) {
        setAcspName(acspName);
        return this;
    }

    public String getAcspName() {
        return acspName;
    }

    public void setSigninUrl(final String signinUrl) {
        this.signinUrl = signinUrl;
    }

    public YourRoleAtAcspHasChangedEmailData signinUrl(final String signinUrl) {
        setSigninUrl(signinUrl);
        return this;
    }

    public String getSigninUrl() {
        return signinUrl;
    }

    @Override
    protected YourRoleAtAcspHasChangedEmailData self() {
        return this;
    }

    @Override
    public void setSubject() {
        if (Objects.isNull(acspName)) {
            throw new NullPointerException("acspName cannot be null");
        }
        setSubject(String.format("Your role for %s has changed", acspName));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof YourRoleAtAcspHasChangedEmailData that)) {
            return false;
        }

        return new EqualsBuilder()
                .append(getEditedBy(), that.getEditedBy())
                .append(getAcspName(), that.getAcspName())
                .append(getTo(), that.getTo())
                .append(getSubject(), that.getSubject())
                .append(getSigninUrl(), that.getSigninUrl())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getEditedBy())
                .append(getAcspName())
                .append(getTo())
                .append(getSubject())
                .append(getSigninUrl())
                .toHashCode();
    }

    @Override
    public String toString() {
        return "YourRoleAtAcspHasChangedEmailData{" +
                "editedBy='" + editedBy + '\'' +
                ", acspName='" + acspName + '\'' +
                ", to='" + getTo() + '\'' +
                ", subject='" + getSubject() + '\'' +
                ", signinUrl='" + getSigninUrl() + '\'' +
                '}';
    }

}
