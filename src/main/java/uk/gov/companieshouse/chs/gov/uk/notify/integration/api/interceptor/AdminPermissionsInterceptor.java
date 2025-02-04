package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.RequestContextUtil.isOAuth2Request;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AdminPermissionsInterceptor implements HandlerInterceptor {

    private static final Map<Pattern, String> httpRequestLineToPermissionMapper = new HashMap<>(Map.of(Pattern.compile("^GET /acsps/[0-9A-Za-z-_]{0,32}/memberships$"), "/admin/acsp/search"));

    private static final String NONE = "none";

    private static final String HAS_ADMIN_PRIVILEGE = "has_admin_privilege";


    private String fetchPermissionForHttpRequestLine(final String httpRequestLine) {
        for (Entry<Pattern, String> mapper : httpRequestLineToPermissionMapper.entrySet()) {
            final var httpRequestLinePattern = mapper.getKey();
            final var permission = mapper.getValue();
            if (httpRequestLinePattern.matcher(httpRequestLine).matches()) {
                return permission;
            }
        }
        return NONE;
    }


    private boolean requestingUserHasPermission(final HttpServletRequest request, final String permission) {
        final var requestingUsersPermissions =
                Optional.ofNullable(request)
                        .map(req -> req.getHeader("ERIC-Authorised-Roles"))
                        .map(roles -> roles.split(" "))
                        .map(Arrays::asList)
                        .orElse(List.of());
        return requestingUsersPermissions.contains(permission);
    }


    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
        final var httpRequestLine = String.format("%s %s", request.getMethod(), request.getRequestURI());
        final var adminPermission = fetchPermissionForHttpRequestLine(httpRequestLine);
        request.setAttribute(HAS_ADMIN_PRIVILEGE, !NONE.equals(adminPermission) && isOAuth2Request() && requestingUserHasPermission(request, adminPermission));
        return true;
    }

}
