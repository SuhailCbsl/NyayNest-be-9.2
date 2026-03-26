/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.audit;

import java.io.IOException;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Listens for Spring Security AuthenticationSuccessEvent and writes a LOGIN row to audittrail.
 */
@Component
public class AuditLoginFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AuditLoginFilter.class);

    private final AuditTrailService auditService;

    // Track session login state
    private static final String LOGIN_TRACK_KEY = "AUDIT_LOGIN_DONE";

    private static final Set<String> loggedUsers = ConcurrentHashMap.newKeySet();

    public AuditLoginFilter(AuditTrailService auditService) {
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        filterChain.doFilter(request, response);

        if (isStatusRequest(request)) {

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (auth != null && auth.isAuthenticated()
                    && !"anonymousUser".equals(auth.getPrincipal())) {

                String username = auth.getName();

                // Only log once per login session
                if (!loggedUsers.contains(username)) {

                    loggedUsers.add(username);

                    String ip = extractIp(request);

                    auditService.log(
                            "LOGIN",
                            null,
                            username,
                            username,
                            ip,
                            request.getRequestURI(),
                            Instant.now(),
                            "User logged in"
                    );
                }
            }
        }
    }

    private boolean isStatusRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri != null && uri.contains("/authn/status");
    }

    private String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public static void removeLoggedUser(String username){
        loggedUsers.remove(username);
    }

}
