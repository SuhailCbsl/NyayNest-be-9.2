package org.dspace.app.rest.report.service.serviceimpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.dspace.app.rest.report.dto.DataTrendDTO;
import org.dspace.app.rest.report.service.DataTrendService;
import org.springframework.stereotype.Service;

@Service
public class DataTrendServiceImpl implements DataTrendService {
    private final DataSource dataSource;

    public DataTrendServiceImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<DataTrendDTO> findData() {
        List<DataTrendDTO> list = new ArrayList<>();
        String sql = "SELECT d.date_of_upload::date AS date," +
                " SUM(d.page_count) AS totalPageCount" +
                " FROM item_upload_info d GROUP BY d.date_of_upload::date " +
                "ORDER BY d.date_of_upload::date";
        try(Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();){
            while(rs.next()) {
                DataTrendDTO dataTrendDTO = new DataTrendDTO();

                dataTrendDTO.setPageCount(rs.getInt("totalPageCount"));
                dataTrendDTO.setDateOfUpload(rs.getDate("date"));
                list.add(dataTrendDTO);
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }

        return list;
    }
}
