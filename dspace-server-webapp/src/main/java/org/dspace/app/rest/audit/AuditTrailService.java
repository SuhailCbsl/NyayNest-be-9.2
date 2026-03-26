/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.audit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Simple reusable service to insert rows into audittrail table.
 * Uses constructor injection for DataSource so Spring provides it.
 */
@Service
public class AuditTrailService {
    private static final Logger log = LoggerFactory.getLogger(AuditTrailService.class);

    private final DataSource dataSource;

    /**
     * Constructor injection: let Spring provide the configured DataSource.
     * If Spring can't find a DataSource bean you'll see an app context startup error,
     * which is helpful (instead of silently skipping logs).
     */
    public AuditTrailService(DataSource dataSource) {
        this.dataSource = dataSource;
        if (this.dataSource == null) {
            log.warn("AuditTrailService constructed with null DataSource");
        } else {
            log.info("AuditTrailService constructed and DataSource injected");
        }
    }

    /**
     * Insert one audittrail row.
     *
     * @param action     Action name (e.g. "LOGIN", "ITEM MODIFY_METADATA")
     * @param handle     Handle of object (or null for login/logout/system)
     * @param userId     User identifier (username, email, or EPerson UUID)
     * @param userName   Human-readable user name/email
     * @param ipAddress  Client IP
     * @param url        Request URL if available
     * @param when       Timestamp of event; if null -> now
     * @param detail     Detail text (or JSON if column is jsonb)
     */
    public void log(String action,
                    String handle,
                    String userId,
                    String userName,
                    String ipAddress,
                    String url,
                    Instant when,
                    String detail) {

        if (this.dataSource == null) {
            log.warn("AuditTrailService: DataSource is null; " +
                    "cannot write audit row. action={}, user={}", action, userName);
            return;
        }

        final String sql = "INSERT INTO audittrail (" +
                "uuid, action, handle, user_id, user_name, ip_addresses, url, event_time, detail) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, action);
            ps.setString(3, handle);
            ps.setString(4, userId);
            ps.setString(5, userName);
            ps.setString(6, ipAddress);
            ps.setString(7, url);
            ps.setTimestamp(8,Timestamp.from(when));
            ps.setString(9, detail);

            int rows = ps.executeUpdate();
            if (rows != 1) {
                log.warn("AuditTrailService.log affected {} " +
                        "rows (expected 1) for action={}, user={}", rows, action, userName);
            } else {
                log.debug("Audit trail row inserted: action={}, user={}, handle={}", action, userName, handle);
            }
        } catch (Exception e) {
            log.error("Failed to insert audittrail row: action={}, user={}, " +
                    "error={}", action, userName, e.getMessage(), e);
        }
    }
}
