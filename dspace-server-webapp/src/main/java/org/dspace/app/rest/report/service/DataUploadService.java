package org.dspace.app.rest.report.service;

import java.util.List;
import java.util.Map;

import org.dspace.app.rest.report.dto.DataTrendDTO;
import org.springframework.core.io.ByteArrayResource;

public interface DataUploadService {
    List<DataTrendDTO> findAll();
    List<DataTrendDTO> findBetweenTwoDates(String from, String to);

    byte[] getPdfReport(List<DataTrendDTO> pdf);
    ByteArrayResource getCsvReport(List<DataTrendDTO> csv);

    boolean saveSchedulerStatus();
    Map<String, Object> getTotalPageCount();
}
