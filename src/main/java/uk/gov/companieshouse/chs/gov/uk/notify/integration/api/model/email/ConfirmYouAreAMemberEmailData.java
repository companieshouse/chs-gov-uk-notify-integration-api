package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public abstract class ConfirmYouAreAMemberEmailData extends BaseEmailData<ConfirmYouAreAMemberEmailData> {

    private String addedBy;

    private String acspName;

    private String signinUrl;

    protected ConfirmYouAreAMemberEmailData() {
    }

    protected ConfirmYouAreAMemberEmailData(final String to, final String addedBy, final String acspName, final String signinUrl) {
        setTo(to);
        this.addedBy = addedBy;
        this.acspName = acspName;
        this.signinUrl = signinUrl;
        setSubject();
    }

    public void setAddedBy(final String addedBy) {
        this.addedBy = addedBy;
    }

    public ConfirmYouAreAMemberEmailData addedBy(final String addedBy) {
        setAddedBy(addedBy);
        return this;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAcspName(final String acspName) {
        this.acspName = acspName;
    }

    public ConfirmYouAreAMemberEmailData acspName(final String acspName) {
        setAcspName(acspName);
        return this;
    }

    public String getAcspName() {
        return acspName;
    }

    public void setSigninUrl(final String signinUrl) {
        this.signinUrl = signinUrl;
    }

    public ConfirmYouAreAMemberEmailData signinUrl(final String signinUrl) {
        setSigninUrl(signinUrl);
        return this;
    }

    public String getSigninUrl() {
        return signinUrl;
    }

    @Override
    protected ConfirmYouAreAMemberEmailData self() {
        return this;
    }

    @Override
    public void setSubject() {
        setSubject("You have been added to a Companies House authorised agent");
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ConfirmYouAreAMemberEmailData that)) {
            return false;
        }

        return new EqualsBuilder()
                .append(getAddedBy(), that.getAddedBy())
                .append(getAcspName(), that.getAcspName())
                .append(getTo(), that.getTo())
                .append(getSubject(), that.getSubject())
                .append(getSigninUrl(), that.getSigninUrl())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getAddedBy())
                .append(getAcspName())
                .append(getTo())
                .append(getSubject())
                .append(getSigninUrl())
                .toHashCode();
    }

    @Override
    public String toString() {
        return "ConfirmYouAreAMemberEmailData{" +
                "addedBy='" + addedBy + '\'' +
                ", acspName='" + acspName + '\'' +
                ", to='" + getTo() + '\'' +
                ", subject='" + getSubject() + '\'' +
                ", signinUrl='" + getSigninUrl() + '\'' +
                '}';
    }

}
