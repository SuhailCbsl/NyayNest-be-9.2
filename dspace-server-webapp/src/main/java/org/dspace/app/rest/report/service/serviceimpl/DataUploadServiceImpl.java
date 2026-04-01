package org.dspace.app.rest.report.service.serviceimpl;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.dspace.app.rest.report.DataUploadService;
import org.dspace.app.rest.report.dto.DataTrendDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;

@Service
public class DataUploadServiceImpl implements DataUploadService {
    private static final Logger log = LoggerFactory.getLogger(DataUploadServiceImpl.class);
    private final DataSource dataSource;

    @Value("${highCourtName}")
    private String highCourtName;

    public DataUploadServiceImpl(DataSource dataSource) {
        log.info("Inside DataUploadServiceImpl");
        this.dataSource = dataSource;
    }

    @Override
    public List<DataTrendDTO> findAll() {
        String sql = "select * from item_upload_info";
        List<DataTrendDTO> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DataTrendDTO dataTrend = new DataTrendDTO();
                dataTrend.setHandle(rs.getString("handle"));
                dataTrend.setItemId(UUID.fromString(rs.getString("item_id")));
                dataTrend.setLotNo(rs.getString("lot_number"));
                dataTrend.setBarcodeNo(rs.getString("barcode_number"));
                dataTrend.setDateOfUpload(rs.getTimestamp("date_of_upload"));
                dataTrend.setPageCount(rs.getInt("page_count"));
                dataTrend.setMediaFileSize(rs.getInt("media_file_size"));
                dataTrend.setTypeOfFile(rs.getString("type_of_file")==null?"":rs.getString("type_of_file"));
                dataTrend.setDeleteStatus(rs.getBoolean("delete_status"));
                dataTrend.setPdfCount(rs.getInt("pdf_count"));
                dataTrend.setTotalFileCount(rs.getInt("total_file_count"));
                dataTrend.setPdfA(rs.getBoolean("is_pdfa"));
                dataTrend.setHasDigitalSignatures(rs.getBoolean("has_digital_signatures"));
                dataTrend.setSignee(rs.getString("signee")==null?"":rs.getString("signee"));
                dataTrend.setSignedAt(rs.getTimestamp("signed_at"));
                dataTrend.setCollectionName(rs.getString("collection_name")==null?"":rs.getString("collection_name"));
                dataTrend.setCnrNo(rs.getString("cnr_no")==null?"":rs.getString("cnr_no"));
                list.add(dataTrend);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public List<DataTrendDTO> findBetweenTwoDates(String from, String to) {
        String sql = "SELECT * FROM item_upload_info WHERE date_of_upload BETWEEN ? AND ? order by date_of_upload desc";
        List<DataTrendDTO> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);) {
            // Parse incoming yyyy-MM-dd into LocalDate
            LocalDate fromLocal = LocalDate.parse(from);  // "2025-09-15"
            LocalDate toLocal = LocalDate.parse(to);    // "2025-09-17"

            // Convert to start of day and end of day
            Timestamp fromTs = Timestamp.valueOf(fromLocal.atStartOfDay());
            Timestamp toTs = Timestamp.valueOf(toLocal.atTime(LocalTime.MAX));

            ps.setTimestamp(1, fromTs);
            ps.setTimestamp(2, toTs);
//            ps.setTimestamp(3, fromTs);
//            ps.setTimestamp(4, toTs);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                DataTrendDTO dataTrend = new DataTrendDTO();
                dataTrend.setHandle(rs.getString("handle"));
                dataTrend.setItemId(UUID.fromString(rs.getString("item_id")));
                dataTrend.setLotNo(rs.getString("lot_number"));
                dataTrend.setBarcodeNo(rs.getString("barcode_number"));

                dataTrend.setDateOfUpload(rs.getTimestamp("date_of_upload"));
//                Date ts = rs.getTimestamp("date_of_upload");
//                if (ts != null) {
//                    dataTrend.setDateOfUpload(((Timestamp) ts).toLocalDateTime()
//                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
//                }
                dataTrend.setPageCount(rs.getInt("page_count"));
                dataTrend.setMediaFileSize(rs.getInt("media_file_size"));
                dataTrend.setTypeOfFile(rs.getString("type_of_file"));
                dataTrend.setDeleteStatus(rs.getBoolean("delete_status"));
                dataTrend.setPdfCount(rs.getInt("pdf_count"));
                dataTrend.setTotalFileCount(rs.getInt("total_file_count"));
                dataTrend.setPdfA(rs.getBoolean("is_pdfa"));
                dataTrend.setHasDigitalSignatures(rs.getBoolean("has_digital_signatures"));
                dataTrend.setSignee(rs.getString("signee"));
                dataTrend.setSignedAt(rs.getTimestamp("signed_at"));
                dataTrend.setCollectionName(rs.getString("collection_name"));
                dataTrend.setCnrNo(rs.getString("cnr_no"));
                list.add(dataTrend);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public byte[] getPdfReport(List<DataTrendDTO> events) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document(PageSize.A4.rotate(),20f,20f,20f,20f);
        PdfWriter.getInstance(document, out);

        document.open();

        // Title
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("Data Upload", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" ")); // Empty line
        int count =0;
        if(highCourtName.equalsIgnoreCase("Delhi")){
            count =17;
        }else{
            count = 16;
        }
        // Table (3 columns: User, Action, Timestamp)
        PdfPTable table = new PdfPTable(count);
        table.setWidthPercentage(100);
        table.setSpacingBefore(5f);
        table.setSpacingAfter(5f);

        // Table header
        Font headFont = new Font(Font.HELVETICA, 10, Font.BOLD);

        PdfPCell hcell;
        if(highCourtName.equalsIgnoreCase("Delhi")){
            //        cnr no 17th column
            hcell = new PdfPCell(new Paragraph("CNR NO", headFont));
            hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(hcell);
        }

        hcell = new PdfPCell(new Paragraph("HANDLE", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("ITEM ID", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("LOT NO", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("BARCODE NO", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("UPLOAD DATE", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("PAGE COUNT", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("FILE SIZE", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("FILE TYPE", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("DELETED", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("PDF COUNT", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("Total File COUNT", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("IS PDFA", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("HAS DIGITAL SIGNATURES", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("SIGNEE", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("SIGNED AT", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("COLLECTION NAME", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        // Table rows
        for (DataTrendDTO event : events) {
            PdfPCell cell;

            if(highCourtName.equalsIgnoreCase("Delhi")){
                //        cnr no 17th column
                cell = new PdfPCell(new Paragraph(event.getCnrNo()));
                cell.setPaddingLeft(5);
                table.addCell(cell);
            }

            cell = new PdfPCell(new Paragraph(event.getHandle()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(String.valueOf(event.getItemId())));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(event.getLotNo()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(event.getBarcodeNo()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(String.valueOf(event.getDateOfUpload())));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(String.valueOf(event.getPageCount())));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(String.valueOf(event.getMediaFileSize())));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(event.getTypeOfFile()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(String.valueOf(event.isDeleteStatus())));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(event.getPdfCount()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(event.getTotalFileCount()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(String.valueOf(event.isPdfA())));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(String.valueOf(event.isHasDigitalSignatures())));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(event.getSignee()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(String.valueOf(event.getSignedAt())));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(event.getCollectionName()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

        }

        document.add(table);
        document.close();

        return out.toByteArray();
    }

    @Override
    public ByteArrayResource getCsvReport(List<DataTrendDTO> events) {
        StringBuilder csv = new StringBuilder();
        if(highCourtName.equalsIgnoreCase("Delhi")){
            csv = new StringBuilder(
                    "Handle," +
                            "Item id," +
                            "Lot Number," +
                            "Barcode Number," +
                            "Date of Upload," +
                            "Page Count," +
                            "Pdf Count," +
                            "Total File Count," +
                            "Media File Size," +
                            "Type of File," +
                            "Delete Status," +
                            "Is PdfA," +
                            "Has Digital Signature," +
                            "Signee," +
                            "Signed At," +
                            "Collection Name," +
                            "CNR No\n");
            for (DataTrendDTO e : events) {
                csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        e.getHandle(),
                        e.getItemId(),
                        e.getLotNo(),
                        e.getBarcodeNo(),
                        e.getDateOfUpload(),
                        e.getPageCount(),
                        e.getPdfCount(),
                        e.getTotalFileCount(),
                        e.getMediaFileSize(),//
                        e.getTypeOfFile(),
                        e.isDeleteStatus(),
                        e.isPdfA(),
                        e.isHasDigitalSignatures(),
                        e.getSignee(),
                        e.getSignedAt(),
                        e.getCollectionName(),
                        e.getCnrNo()
                ));
            }
        }else {
            csv = new StringBuilder(
                    "Handle," +
                            "Item id," +
                            "Lot Number," +
                            "Barcode Number," +
                            "Date of Upload," +
                            "Page Count," +
                            "Pdf Count," +
                            "Total File Count," +
                            "Media File Size," +
                            "Type of File," +
                            "Delete Status," +
                            "Is PdfA," +
                            "Has Digital Signature," +
                            "Signee," +
                            "Signed At," +
                            "Collection Name\n");

            for (DataTrendDTO e : events) {
                csv.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                        e.getHandle(),
                        e.getItemId(),
                        e.getLotNo(),
                        e.getBarcodeNo(),
                        e.getDateOfUpload(),
                        e.getPageCount(),
                        e.getPdfCount(),
                        e.getTotalFileCount(),
                        e.getMediaFileSize(),//
                        e.getTypeOfFile(),
                        e.isDeleteStatus(),
                        e.isPdfA(),
                        e.isHasDigitalSignatures(),
                        e.getSignee(),
                        e.getSignedAt(),
                        e.getCollectionName()
                ));
            }
        }
        ByteArrayResource resource = new ByteArrayResource(csv.toString().getBytes());
        return resource;
    }
}
