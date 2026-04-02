package org.dspace.app.rest.report.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.dspace.app.rest.report.dto.DataTrendDTO;
import org.dspace.app.rest.report.service.DataUploadService;
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
@RequestMapping("/api/dataupload")
public class DataUploadController {
    private DataUploadService dataUploadService;

    public DataUploadController(DataUploadService dataUploadService) {
        this.dataUploadService = dataUploadService;
    }
    @GetMapping
    public List<DataTrendDTO> getAll() {
        return dataUploadService.findAll();
    }

    // return between two dates
    @GetMapping(value = "/{from:\\d{4}-\\d{2}-\\d{2}}/to/{to:\\d{4}-\\d{2}-\\d{2}}")
    public List<DataTrendDTO> findBetween(@PathVariable("from") String from,
                                          @PathVariable("to") String to) {
        return dataUploadService.findBetweenTwoDates(from, to);
    }

    // download entire pdf
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadEntirePdf() {
        List<DataTrendDTO> get = dataUploadService.findAll();
        byte[] pdfReport = dataUploadService.getPdfReport(get);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data-upload.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfReport);
    }

    // download entire CSV file
    @GetMapping("/csv")
    public ResponseEntity<Resource> downloadEntireCsv() {
        List<DataTrendDTO> get = dataUploadService.findAll();
        ByteArrayResource csvReport = dataUploadService.getCsvReport(get);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=upload-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvReport);
    }

    // download pdf range wise
    @GetMapping("/{from}/to/{to}/pdf")
    public ResponseEntity<byte[]> downloadRangePdf(@PathVariable("from") String from,
                                                   @PathVariable("to") String to) {
        List<DataTrendDTO> list = dataUploadService.findBetweenTwoDates(from, to);
        byte[] pdfReport = dataUploadService.getPdfReport(list);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=data-upload.pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfReport);
    }

    // download csv range wise
    @GetMapping("/{from}/to/{to}/csv")
    public ResponseEntity<Resource> downloadRangeCsv(@PathVariable("from") String from,
                                                     @PathVariable("to") String to) {
        List<DataTrendDTO> get = dataUploadService.findBetweenTwoDates(from, to);
        ByteArrayResource csvReport = dataUploadService.getCsvReport(get);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=upload-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvReport);
    }

    @GetMapping("/saveitembulkupload")
    public boolean saveItemBulkUpload() {
        try {
            System.out.println("Scheduler status update started...");
            boolean result = dataUploadService.saveSchedulerStatus();
            return result; // true or false to UI
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error occurred while updating scheduler status: " + e.getMessage());
            return false; // return false if exception occurs
        }
    }
    @GetMapping("/total-page-count")
    public ResponseEntity<?> getTotalPageCount() {
        try {
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> totalPageCountData = dataUploadService.getTotalPageCount();
            response.put("status", "Success");
            response.put("totalPageCountData", totalPageCountData);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

}
