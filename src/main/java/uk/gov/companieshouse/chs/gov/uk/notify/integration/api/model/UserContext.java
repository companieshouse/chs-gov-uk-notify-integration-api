package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model;

import uk.gov.companieshouse.api.accounts.user.model.User;

public final class UserContext {

    private static final ThreadLocal<User> userContextThreadLocal = new ThreadLocal<>();

    public static User getLoggedUser() {
        return userContextThreadLocal.get();
    }

    public static void setLoggedUser(final User user) {

        userContextThreadLocal.set(user);
    }

    public static void clear() {
        userContextThreadLocal.remove();
    }
}
