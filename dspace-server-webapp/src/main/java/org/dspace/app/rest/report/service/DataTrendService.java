package org.dspace.app.rest.report.service;

import java.util.List;

import org.dspace.app.rest.report.dto.DataTrendDTO;

public interface DataTrendService {
    List<DataTrendDTO> findData();
}
