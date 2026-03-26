/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.audit;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.LogoutSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuditLogoutListener implements ApplicationListener<LogoutSuccessEvent> {

    private final AuditTrailService auditService;

    public AuditLogoutListener(AuditTrailService auditService) {
        this.auditService = auditService;
    }

    @Override
    public void onApplicationEvent(LogoutSuccessEvent event) {
        String username = event.getAuthentication().getName();
        AuditLoginFilter.removeLoggedUser(username);
        String ip = "unknown";
        String url = null;
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isEmpty()) {
                ip = xff.split(",")[0].trim();
            } else {
                ip = req.getRemoteAddr();
            }
            url = req.getRequestURI();
        }
        auditService.log("LOGOUT", null, event.getAuthentication().getName(), username,
                ip, url, java.time.Instant.now(),
                "User logged out");
    }
}
