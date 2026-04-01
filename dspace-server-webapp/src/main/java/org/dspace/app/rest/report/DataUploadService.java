package org.dspace.app.rest.report;

import java.util.List;

import org.dspace.app.rest.report.dto.DataTrendDTO;
import org.springframework.core.io.ByteArrayResource;

public interface DataUploadService {
    List<DataTrendDTO> findAll();
    List<DataTrendDTO> findBetweenTwoDates(String from, String to);

    byte[] getPdfReport(List<DataTrendDTO> pdf);
    ByteArrayResource getCsvReport(List<DataTrendDTO> csv);
}
