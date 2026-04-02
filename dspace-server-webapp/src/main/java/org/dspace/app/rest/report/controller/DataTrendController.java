package org.dspace.app.rest.report.controller;

import java.util.List;

import org.dspace.app.rest.report.dto.DataTrendDTO;
import org.dspace.app.rest.report.service.DataTrendService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DataTrendController {
    @Autowired
    DataTrendService service;

    @GetMapping(value = "/datatrend")
    public ResponseEntity<List<DataTrendDTO>> getData(){
        List<DataTrendDTO> res = service.findData();
        return new ResponseEntity<>(res, HttpStatus.OK);
    }
}
