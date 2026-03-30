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
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.RequestService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * AuditTrailConsumer (no special MODIFY_METADATA buffering)
 *
 * - Inserts every event immediately
 * - Dynamic action: "<ENTITY_TYPE> <EVENT>"
 * - Dynamic user details from Context
 * - Produces clean "--changes ..." detail when event detail contains metadata tokens
 */
public class AuditTrailConsumer implements Consumer {
    private static final Logger log = LogManager.getLogger(AuditTrailConsumer.class);

    private final ObjectMapper mapper = new ObjectMapper();

    // small dedupe window to avoid immediate duplicates (keeps behavior you had)
    private static final long DEDUP_WINDOW_MS = 1500L;
    private final Map<String, Long> recentEvents = new ConcurrentHashMap<>();

    private RequestService requestService;
    private DataSource dataSource;

    public AuditTrailConsumer() {
        log.info("AuditTrailConsumer constructed");
    }

    @Override
    public void initialize() throws Exception {
        log.info("AuditTrailConsumer.initialize()");
        try {
            ServiceManager sm = DSpaceServicesFactory.getInstance().getServiceManager();
            if (sm != null) {
                try {
                    this.dataSource = sm.getServiceByName(DataSource.class.getName(), DataSource.class);
                } catch (Exception e) {
                    try {
                        this.dataSource = sm.getServiceByName("dataSource", DataSource.class);
                    } catch (Exception ex) {
                        this.dataSource = null;
                    }
                }
                try {
                    this.requestService = sm.getServiceByName(RequestService.class.getName(), RequestService.class);
                } catch (Exception e) {
                    this.requestService = null;
                }
            } else {
                log.warn("ServiceManager not available in initialize()");
            }
        } catch (Exception ex) {
            log.warn("initialize() error", ex);
        }
    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {
        if (event == null) {
            return;
        }

        try {
            // Resolve subject (best-effort)
            DSpaceObject subj = null;
            try {
                subj = event.getSubject(ctx);
            } catch (Throwable t) {
                subj = null;
            }

            String eventType = event.getEventTypeAsString();
            String rawDetail = event.getDetail();

            // dynamic user details
            EPerson currentUser = ctx != null ? ctx.getCurrentUser() : null;
            String userId = (currentUser != null) ? String.valueOf(currentUser.getID()) : "anonymous";
            String userName = (currentUser != null) ? currentUser.getEmail() : "anonymous";
            Date lastActive = Date.from(currentUser.getLastActive());
            log.info("Last Active: {}", lastActive);
            // entity type text
            String entityText = "UNKNOWN";
            String handle = null;
            if (subj != null) {
                try {
                    entityText = Constants.typeText[subj.getType()];
                } catch (Throwable t) {
                    entityText = "UNKNOWN";
                }
                try {
                    handle = subj.getHandle();
                } catch (Throwable ignore) {
                    handle = null;
                }
            }

            // Dedup (prevents many identical fast duplicates)
            // ----------------- improved dedupe -----------------
            // Build a more robust subject identifier:
            // prefer handle (stable across workspace->workflow->install),
            // else DB id if available, else fallback to event hash
            String subjectId;
            try {
                if (subj != null) {
                    // prefer stable handle when present
                    String h = null;
                    try {
                        h = subj.getHandle();
                    } catch (Throwable ignore) {
                        h = null;
                    }
                    if (h != null && !h.trim().isEmpty()) {
                        subjectId = h;
                    } else {
                        try {
                            subjectId = String.valueOf(subj.getID());
                        } catch (Throwable t) {
                            subjectId = "subj-" + System.identityHashCode(subj);
                        }
                    }
                } else {
                    subjectId = "evt-" + System.identityHashCode(event);
                }
            } catch (Throwable t) {
                subjectId = "evt-" + System.identityHashCode(event);
            }

            // include a short fingerprint of the raw detail so different metadata changes aren't deduped away
            String detailFingerprint = (rawDetail != null && !rawDetail.isEmpty())
                    ? Integer.toHexString(rawDetail.hashCode())
                    : "nodetail";

            // final dedupe key
            String dedupKey = entityText + ":" + subjectId + ":" + eventType + ":" + detailFingerprint;

            long now = System.currentTimeMillis();
            // prune recentEvents occasionally to avoid unbounded growth (small tidy-up)
            if (recentEvents.size() > 5000) {
                long cutoff = now - (5 * DEDUP_WINDOW_MS); // keep a bit more history
                recentEvents.entrySet().removeIf(e -> e.getValue() < cutoff);
            }

            Long last = recentEvents.get(dedupKey);
            if (last != null && (now - last) < DEDUP_WINDOW_MS) {
                log.debug("Skipping duplicate rapid event (dedup): {}", dedupKey);
                return;
            }
            recentEvents.put(dedupKey, now);


            // request info
            HttpServletRequest request = null;
            try {
                if (this.requestService != null && this.requestService.getCurrentRequest() != null) {
                    request = this.requestService.getCurrentRequest().getHttpServletRequest();
                }
            } catch (Throwable t) {
                request = null;
            }
            String ipAddress = getClientIp(request);
            String url = (request != null) ? request.getRequestURL().toString() : null;

            // build dynamic action
            String action = (entityText != null ? entityText : "OBJECT")
                    + " " +
                    (eventType != null ? eventType : "UNKNOWN");

            // Build a clean detail string:
            // - if the raw detail contains metadata tokens we try to extract and present them as --changes ...
            // - otherwise send a friendly detail (entity + name + rawDetail)
            String detail = buildCleanDetail(ctx, subj, rawDetail, entityText, eventType);

            // Insert immediately (no buffering)
            insertAuditRow(handle, action, userId, userName, ipAddress, url, detail, event.getTimeStamp());

        } catch (Throwable t) {
            log.error("AuditTrailConsumer.consume() error (non-fatal):", t);
        }
    }

    @Override
    public void end(Context ctx) throws Exception {
        // no-op (no buffering)
    }

    @Override
    public void finish(Context ctx) throws Exception {
        // no-op
    }

    // ----------------- helpers -----------------

    private String buildCleanDetail(Context ctx,
                                    DSpaceObject dso,
                                    String rawDetail,
                                    String entityText,
                                    String eventType) {
        StringBuilder sb = new StringBuilder();

        // start with action + entity
        sb.append(eventType != null ? eventType : "EVENT");
        if (dso != null) {
            try {
                sb.append(" ").append(entityText != null ? entityText : ("type#" + dso.getType()));
            } catch (Throwable ignored) {
                ignored.printStackTrace();
            }
            try {
                String h = dso.getHandle();
                if (h != null) {
                    sb.append(" (handle=").append(h).append(")");
                }
            } catch (Throwable ignored) {
                ignored.printStackTrace();
            }
        }

        // try name using simple reflection or service lookup
        try {
            String name = null;
            if (dso instanceof Item) {
                try {
                    name = ((Item) dso).getName();
                } catch (Throwable t) {
                    name = null;
                }
            } else if (dso instanceof Collection) {
                try {
                    name = ((Collection) dso).getName();
                } catch (Throwable t) {
                    name = null;
                }
            } else if (dso instanceof Community) {
                try {
                    name = ((Community) dso).getName();
                } catch (Throwable t) {
                    name = null;
                }
            }
            if (name != null) {
                sb.append(" name=\"").append(name.replace("\"", "\\\"")).append("\"");
            }
        } catch (Throwable ignore) {
            ignore.printStackTrace();
        }

        // if rawDetail looks like metadata, produce compressed --changes output
        if (rawDetail != null &&
                (rawDetail.toLowerCase().contains("dc.") ||
                        rawDetail.toLowerCase().contains("dc_") ||
                        rawDetail.toLowerCase().contains("metadata"))) {
            Map<String, String> fields = parseMetadataFieldsFromDetail(rawDetail);
            if (!fields.isEmpty()) {
                // fill missing values by querying DSpace object
                for (Map.Entry<String, String> entry : new LinkedHashMap<>(fields).entrySet()) {
                    if (entry.getValue() == null || entry.getValue().isEmpty()) {
                        String fetched = fetchMetadataFromDSpace(ctx, dso, entry.getKey());
                        if (fetched != null) {
                            fields.put(entry.getKey(), fetched);
                        }
                    }
                }

                String joined = fields.entrySet().stream()
                        .map(e -> {
                            String key = e.getKey().replace('_', '.');
                            String[] parts = key.split("\\.");
                            String outKey = key;
                            if (parts.length >= 2) {
                                String schema = parts[0];
                                String element = parts[1];
                                String qualifier = (parts.length > 2) ? parts[2] : null;
                                String bk = buildMetadataKey(schema, element, qualifier);
                                if (bk != null) {
                                    outKey = bk;
                                }
                            }
                            String v = e.getValue();
                            return outKey + "=\"" + (v == null ? "" : v.replace("\"", "\\\"")) + "\"";
                        }).collect(Collectors.joining(";"));
                sb.append(" --changes ").append(joined);

                return sb.toString();
            } else {
                // fallback: put raw detail after a marker
                sb.append(" --detail: ").append(rawDetail);
                return sb.toString();
            }
        }

        // otherwise if rawDetail exists, append it
        if (rawDetail != null && !rawDetail.isEmpty()) {
            sb.append(" --detail: ").append(rawDetail);
        }
        return sb.toString();
    }

    /**
     * Try JSON parse (or heuristics) to produce field->value map from event.getDetail()
     * This is permissive — adapt to the actual detail shape in your deployment if needed.
     */
    private Map<String, String> parseMetadataFieldsFromDetail(String raw) {
        Map<String, String> out = new LinkedHashMap<>();
        if (raw == null || raw.trim().isEmpty()) {
            return out;
        }

        // Try JSON first
        try {
            JsonNode root = mapper.readTree(raw);

            // JSON Patch: array of ops with "path" and "value" fields
            if (root.isArray()) {
                for (JsonNode op : root) {
                    JsonNode pathNode = op.get("path");
                    JsonNode valueNode = op.get("value");
                    if (pathNode != null && valueNode != null) {
                        String path = pathNode.asText();
                        String val = valueNode.isNull() ? "" : valueNode.asText();
                        if (path.contains("/metadata/") || path.toLowerCase().contains("metadata")) {
                            String[] parts = path.split("/");
                            for (String p : parts) {
                                if (p != null && p.contains(".")) {
                                    // p may look like "dc.title" or "dc-title"; normalize it
                                    String normalized = p.replace('-', '.');
                                    // split into schema.element[.qualifier]
                                    String[] tok = normalized.split("\\.");
                                    if (tok.length >= 2) {
                                        String schema = tok[0];
                                        String element = tok[1];
                                        String qualifier = (tok.length > 2) ? tok[2] : null;
                                        String key = buildMetadataKey(schema, element, qualifier);
                                        if (key != null) {
                                            out.put(key, val);
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!out.isEmpty()) {
                    return out;
                }
            }

            // snapshot style: look for "metadata" arrays under "new" or top-level
            JsonNode meta = null;
            if (root.has("new") && root.path("new").has("metadata")) {
                meta = root.path("new").path("metadata");
            } else if (root.has("metadata")) {
                meta = root.path("metadata");
            }
            if (meta != null && meta.isArray()) {
                for (JsonNode m : meta) {
                    String schema = safeText(m, "schema");
                    String element = safeText(m, "element");
                    String qualifier = safeText(m, "qualifier");
                    String value = safeText(m, "value");
                    String key = buildMetadataKey(schema, element, qualifier);
                    if (key != null) {
                        out.put(key, value);
                    }
                }
                if (!out.isEmpty()) {
                    return out;
                }
            }
        } catch (Exception e) {
            // parsing failed, fall back to heuristic below
            log.debug("parseMetadataFieldsFromDetail: JSON parse failed: {}", e.getMessage());
        }

        // Heuristic fallback: look for tokens like dc.title="..." or dc_title="..."
        String[] tokens = raw.split("[,;\\n]");
        for (String tok : tokens) {
            String t = tok.trim();
            if (t.isEmpty()) {
                continue;
            }
            int idx = t.indexOf("dc.");
            if (idx < 0) {
                idx = t.indexOf("dc_");
            }
            if (idx >= 0) {
                String sub = t.substring(idx).trim();
                // token may include punctuation; keep only allowed chars for token part
                String token = sub.split("\\s+")[0].replace('_', '.').replaceAll("[^a-zA-Z0-9\\._]", "");
                // split into parts
                String field = null;
                String[] parts = token.split("\\.");
                if (parts.length >= 2) {
                    String schema = parts[0];
                    String element = parts[1];
                    String qualifier = (parts.length > 2) ? parts[2] : null;
                    field = buildMetadataKey(schema, element, qualifier);
                } else {
                    // fallback: use raw token
                    field = token;
                }

                String val = null;
                int q1 = t.indexOf('"');
                if (q1 >= 0) {
                    int q2 = t.indexOf('"', q1 + 1);
                    if (q2 > q1) {
                        val = t.substring(q1 + 1, q2);
                    }
                } else if (t.contains("=")) {
                    String[] kv = t.split("=", 2);
                    if (kv.length == 2) {
                        val = kv[1].trim().replaceAll("^\"|\"$", "");
                    }
                }
                if (field != null) {
                    out.put(field, val);
                }
            }
        }

        return out;
    }


    private String safeText(JsonNode n, String f) {
        if (n == null || !n.has(f) || n.get(f).isNull()) {
            return null;
        }
        return n.get(f).asText();
    }

    private void insertAuditRow(String handle, String action, String userId, String userName,
                                String ipAddress, String url, String detail, long timeStamp) {
        if (this.dataSource == null) {
            try {
                ServiceManager sm = DSpaceServicesFactory.getInstance().getServiceManager();
                if (sm != null) {
                    DataSource ds = sm.getServiceByName(DataSource.class.getName(), DataSource.class);
                    if (ds == null) {
                        ds = sm.getServiceByName("dataSource", DataSource.class);
                    }
                    this.dataSource = ds;
                }
            } catch (Exception e) {
                log.warn("Could not re-resolve DataSource: {}", e.getMessage());
            }
        }

        if (this.dataSource == null) {
            log.warn("No DataSource — skipping audit insert. detail={}", detail);
            return;
        }

        final String sql = "INSERT INTO audittrail (" +
                "uuid," +
                " action," +
                " handle," +
                " user_id," +
                " user_name," +
                " ip_addresses," +
                " url," +
                " event_time," +
                " detail) " +
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
            ps.setTimestamp(8, Timestamp.from(Instant.ofEpochMilli(timeStamp)));
            ps.setString(9, detail);

            int affected = ps.executeUpdate();
            if (affected != 1) {
                log.warn("Audit insert affected {} rows (expected 1).", affected);
            } else {
                log.info("Audit inserted: action={}, handle={}, user={}", action, handle, userName);
            }
        } catch (Exception e) {
            log.error("insertAuditRow failed:", e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private String fetchMetadataFromDSpace(Context ctx, DSpaceObject dso, String fieldToken) {
        if (dso == null || fieldToken == null || fieldToken.trim().isEmpty()) {
            return null;
        }

        // Normalize token: accept 'dc.title', 'dc_title', 'dc.title.qualifier'
        String cleaned = fieldToken.trim().replace('_', '.');
        String[] parts = cleaned.split("\\.");
        if (parts.length < 2) {
            return null;
        }
        String schema = parts[0];
        String element = parts[1];
        String qualifier = (parts.length > 2) ? parts[2] : null;

        try {
            // ---------------- Items ----------------
            if (dso instanceof Item) {
                Item item = (Item) dso;
                ItemService itemService = ContentServiceFactory.getInstance().getItemService();
                if (itemService != null) {
                    try {
                        // Use ItemService.getMetadata(...) which returns a List<MetadataValue>
                        // Signature used here is the common one in DSpace 7+:
                        // List<MetadataValue>
                        // getMetadata(Item item, String schema, String element, String qualifier, String lang)
                        java.util.List<?> values = itemService.getMetadata(item, schema, element, qualifier, Item.ANY);
                        if (values != null && !values.isEmpty()) {
                            Object first = values.get(0);
                            // Try common MetadataValue#getValue() reflection-free:
                            try {
                                // MetadataValue is usually org.dspace.content.MetadataValue
                                // but to avoid explicit dependency issues, handle common cases:
                                if (first instanceof org.dspace.content.MetadataValue) {
                                    return ((org.dspace.content.MetadataValue) first).getValue();
                                } else {
                                    // fallback to toString of the first value
                                    return String.valueOf(first);
                                }
                            } catch (Throwable t) {
                                return String.valueOf(first);
                            }
                        }
                    } catch (Throwable e) {
                        // If for some reason the service method is not present or fails, fallback to item.getName()
                        log.debug("ItemService.getMetadata failed: {}", e.getMessage());
                    }
                }
                // fallback: try item.getName() if metadata couldn't be retrieved
                try {
                    return item.getName();
                } catch (Throwable ignore) {
                    ignore.printStackTrace();
                }
                return null;
            }

            // ---------------- Collections ----------------
            if (dso instanceof Collection) {
                Collection coll = (Collection) dso;
                CollectionService collService = ContentServiceFactory.getInstance().getCollectionService();
                if (collService != null) {
                    try {
                        // Use CollectionService.getMetadata(...) to fetch metadata values
                        List<MetadataValue> values = collService.getMetadata(coll, schema, element, qualifier, null);
                        if (values != null && !values.isEmpty()) {
                            // Return the first metadata value
                            return values.get(0).getValue();
                        }
                    } catch (Throwable e) {
                        log.debug("CollectionService.getMetadata failed: {}", e.getMessage());
                    }
                }
                // Fallback to collection.getName()
                try {
                    return coll.getName();
                } catch (Throwable ignore) {
                    ignore.printStackTrace();
                }
                return null;
            }

            // ---------------- Communities ----------------
            if (dso instanceof Community) {
                Community comm = (Community) dso;
                CommunityService commService = ContentServiceFactory.getInstance().getCommunityService();
                if (commService != null) {
                    try {
                        // Use CommunityService.getMetadata(...) to fetch metadata values
                        List<MetadataValue> values = commService.getMetadata(comm, schema, element, qualifier, null);
                        if (values != null && !values.isEmpty()) {
                            // Return the first metadata value
                            return values.get(0).getValue();
                        }
                    } catch (Throwable e) {
                        log.debug("CommunityService.getMetadata failed: {}", e.getMessage());
                    }
                }
                // Fallback to community.getName()
                try {
                    return comm.getName();
                } catch (Throwable ignore) {
                    ignore.printStackTrace();
                }
                return null;
            }

        } catch (Throwable e) {
            log.debug("fetchMetadataFromDSpace overall failure for {}: {}", fieldToken, e.getMessage());
        }

        return null;
    }



    /**
     * Build metadata key like "dc.title" or "dc.title.provenance".
     * Avoid producing ".null" when qualifier is null.
     */
    private String buildMetadataKey(String schema, String element, String qualifier) {
        if (schema == null || element == null) {
            return null;
        }
        if (qualifier == null || qualifier.isEmpty() || "null".equals(qualifier)) {
            return schema + "." + element;
        }
        return schema + "." + element + "." + qualifier;
    }


}