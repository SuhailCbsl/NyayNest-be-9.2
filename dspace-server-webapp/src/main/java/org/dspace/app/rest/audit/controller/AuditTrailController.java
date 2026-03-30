package org.dspace.app.rest.audit.controller;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.sql.DataSource;

import org.dspace.app.rest.audit.dto.AuditTrailDTO;
import org.dspace.app.rest.audit.service.AuditTrailRepoService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/audittrail")
public class AuditTrailController {

    private final AuditTrailRepoService auditTrailService;
    private final DataSource dataSource;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    public AuditTrailController(AuditTrailRepoService repo, DataSource dataSource) {
        this.auditTrailService = repo;
        this.dataSource = dataSource;
    }

    @GetMapping
    public List<AuditTrailDTO> getAllAuditTrails() {
        return auditTrailService.findBetweenTimes();
    }

    // PDF Export
    @GetMapping("export/pdf/{fromDate}/to/{toDate}")
    public ResponseEntity<byte[]> exportAuditReport(@PathVariable(value = "fromDate") String fromDate,
                                                    @PathVariable(value = "toDate") String toDate) throws Exception {
        List<AuditTrailDTO> events = auditTrailService.getDataBetweenTwoDates(fromDate, toDate);

        byte[] pdfBytes = auditTrailService.generatePdfAuditReport(events);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit-report.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/export/csv/{fromDate}/to/{toDate}")
    public ResponseEntity<Resource> exportCsv(@PathVariable(value = "fromDate") String fromDate,
                                              @PathVariable(value = "toDate") String toDate) {
        StringBuilder csv = new StringBuilder("uuid," +
                "action," +
                "handle," +
                "user_id," +
                "user_name," +
                "ip_addresses," +
                "url," +
                "event_time," +
                "detail\n");
        List<AuditTrailDTO> events = auditTrailService.getDataBetweenTwoDates(fromDate, toDate);
        for (AuditTrailDTO e : events) {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%n",
                    e.getUuid(),
                    e.getAction(),
                    e.getHandle(),
                    e.getUserId(),
                    e.getUserName(),
                    e.getIpAddress(),
                    e.getUrl(),
                    e.getEventTime() != null ? e.getEventTime() : "",
                    e.getDetail() != null ? e.getDetail().replace(",", " ") : ""
            ));
        }
        ByteArrayResource resource = new ByteArrayResource(csv.toString().getBytes());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=audit_report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    @GetMapping(value = "/{from}/to/{to}")
    public List<AuditTrailDTO> findDataBetweenTwoDates(@PathVariable("from") String from,
                                                       @PathVariable("to") String to) {
        return auditTrailService.getDataBetweenTwoDates(from, to);
    }
}
