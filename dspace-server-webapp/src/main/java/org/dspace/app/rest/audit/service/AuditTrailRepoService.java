package org.dspace.app.rest.audit.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.dspace.app.rest.audit.dto.AuditTrailDTO;
import org.springframework.stereotype.Service;

@Service
public class AuditTrailRepoService {

    private final DataSource dataSource;

    public AuditTrailRepoService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<AuditTrailDTO> findBetweenTimes() {
        List<AuditTrailDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM audittrail ORDER BY event_time DESC";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                AuditTrailDTO dto = new AuditTrailDTO();
                dto.setUuid(rs.getString("uuid"));
                dto.setAction(rs.getString("action"));
                dto.setHandle(rs.getString("handle"));
                dto.setUserId(rs.getString("user_id"));
                dto.setUserName(rs.getString("user_name"));
                dto.setIpAddress(rs.getString("ip_addresses"));
                dto.setUrl(rs.getString("url"));
                dto.setEventTime(rs.getString("event_time"));
                dto.setDetail(rs.getString("detail"));
                list.add(dto);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public byte[] generatePdfAuditReport(List<AuditTrailDTO> events) throws DocumentException, IOException {

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Document document = new Document();
        PdfWriter.getInstance(document, out);

        document.open();

        // Title
        Font titleFont = new Font(Font.HELVETICA, 18, Font.BOLD);
        Paragraph title = new Paragraph("Audit Report", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);
        document.add(new Paragraph(" ")); // Empty line

        // Table (3 columns: User, Action, Timestamp)
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10f);
        table.setSpacingAfter(10f);

        // Table header
        Font headFont = new Font(Font.HELVETICA, 12, Font.BOLD);

        PdfPCell hcell;
        hcell = new PdfPCell(new Paragraph("ACTION", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("HANDLE", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("USER ID", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("USER NAME", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("IP_ADDRESS", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("URL", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("EVENT TIME", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        hcell = new PdfPCell(new Paragraph("DETAIL", headFont));
        hcell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(hcell);

        // Table rows
        for (AuditTrailDTO event : events) {
            PdfPCell cell;

            cell = new PdfPCell(new Paragraph(event.getAction()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(event.getHandle()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(event.getUserId()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(event.getUserName()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(event.getIpAddress()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(event.getUrl()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(event.getEventTime()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

            cell = new PdfPCell(new Paragraph(event.getDetail()));
            cell.setPaddingLeft(5);
            table.addCell(cell);

        }

        document.add(table);
        document.close();

        return out.toByteArray();
    }

    public List<AuditTrailDTO> getDataBetweenTwoDates(String fromDate, String toDate) {
        String sql = "SELECT uuid, action, handle, user_id, user_name, ip_addresses, url, event_time, detail " +
                "FROM audittrail " +
                "WHERE event_time BETWEEN ? AND ? " +
                "ORDER BY event_time DESC";

        List<AuditTrailDTO> list = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Parse incoming yyyy-MM-dd into LocalDate
            LocalDate from = LocalDate.parse(fromDate);  // "2025-09-15"
            LocalDate to   = LocalDate.parse(toDate);    // "2025-09-17"

            // Convert to start of day and end of day
            Timestamp fromTs = Timestamp.valueOf(from.atStartOfDay());
            Timestamp toTs   = Timestamp.valueOf(to.atTime(LocalTime.MAX));

            ps.setTimestamp(1, fromTs);
            ps.setTimestamp(2, toTs);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditTrailDTO dto = new AuditTrailDTO();
                    dto.setUuid(rs.getString("uuid"));
                    dto.setAction(rs.getString("action"));
                    dto.setHandle(rs.getString("handle"));
                    dto.setUserId(rs.getString("user_id"));
                    dto.setUserName(rs.getString("user_name"));
                    dto.setIpAddress(rs.getString("ip_addresses"));
                    dto.setUrl(rs.getString("url"));

                    Timestamp ts = rs.getTimestamp("event_time");
                    if (ts != null) {
                        dto.setEventTime(ts.toLocalDateTime()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }

                    dto.setDetail(rs.getString("detail"));
                    list.add(dto);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error fetching audit trail between dates", e);
        }

        return list;
    }

}
