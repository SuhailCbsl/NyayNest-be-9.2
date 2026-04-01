package org.dspace.upload.service.impl;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.sql.DataSource;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.dspace.upload.model.ItemUploadInfo;
import org.dspace.upload.service.ItemUploadInfoService;
import org.dspace.util.NyayNestConstants;
import org.dspace.util.PDFAUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemUploadInfoServiceImpl implements ItemUploadInfoService {

    private static final Logger log = LoggerFactory.getLogger(ItemUploadInfoServiceImpl.class);

    private DataSource dataSource = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("dataSource", DataSource.class);

    private BitstreamService bitstreamService;
    private BitstreamStorageService bitstreamStorageService;
    private ItemService itemService;

    public void setBitstreamService(BitstreamService bitstreamService) {
        this.bitstreamService = bitstreamService;
    }

    public void setBitstreamStorageService(BitstreamStorageService bitstreamStorageService) {
        this.bitstreamStorageService = bitstreamStorageService;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    @Override
    public void storeItemUploadInfo(String handle) {

        if (Objects.nonNull(dataSource)) {

            try (Connection conn = dataSource.getConnection();) {

                Thread.sleep(1000);
                String sql = "SELECT resource_id FROM handle WHERE handle = ?";
                String uuid = null;
                try (PreparedStatement ps = conn.prepareStatement(sql);){
                    ps.setString(1, handle);
                    try (ResultSet resultSet = ps.executeQuery();){
                        if (Objects.nonNull(resultSet) && resultSet.next()) {
                            uuid = resultSet.getString("resource_id");
                        }
                    }
                }

                if (Objects.nonNull(uuid) && !uuid.isEmpty()) {

                    log.info("item with handle: {} has uuid: {}", handle, uuid);
                    // fetch item based on UUID
                    // save upload details in DB
                    sql = "SELECT element, metadata_field_id FROM metadatafieldregistry " +
                            "WHERE element IN ('LotNo', 'FileBarcode', 'title') AND " +
                            "qualifier IS NULL AND metadata_schema_id = (SELECT metadata_schema_id FROM metadataschemaregistry WHERE short_id = 'dc');";
                    int lotNoId = -1;
                    int fileBarcodeID = -1;
                    int titleID = -1;
                    try (PreparedStatement ps = conn.prepareStatement(sql);) {
                        try (ResultSet resultSet = ps.executeQuery();){
                            if (resultSet.next()) {
                                Map<String, Integer> metadataAndID = new HashMap<>();
                                do {
                                    metadataAndID.put(resultSet.getString("element"), resultSet.getInt("metadata_field_id"));
                                } while (resultSet.next());
                                lotNoId = metadataAndID.get("LotNo");
                                fileBarcodeID = metadataAndID.get("FileBarcode");
                                titleID = metadataAndID.get("title");
                            }
                        }
                    }

                    if (!Integer.valueOf(-1).equals(lotNoId) && !Integer.valueOf(-1).equals(fileBarcodeID) && !Integer.valueOf(-1).equals(titleID)) {

                        sql = "SELECT mv.text_value AS collection_name FROM metadatavalue AS mv " +
                                "INNER JOIN item AS i " +
                                "ON mv.dspace_object_id = i.owning_collection " +
                                "WHERE i.uuid = ?::uuid AND " +
                                "mv.metadata_field_id = ?;";
                        String collectionName = "";
                        try (PreparedStatement ps = conn.prepareStatement(sql);){
                            ps.setString(1, uuid);
                            ps.setInt(2, titleID);
                            try (ResultSet resultSet = ps.executeQuery();){
                                if (resultSet.next()) {
                                    collectionName = resultSet.getString("collection_name");
                                    log.info("collection name found for item with handle: {}", handle);
                                    log.info("UUID: {}, Collection Name: {}", uuid, collectionName);
                                }
                            }
                        }

                        sql = "SELECT " +
                                "mv.dspace_object_id, " +
                                "MAX(CASE WHEN mv.metadata_field_id = ? THEN mv.text_value END) AS LotNo, " +
                                "MAX(CASE WHEN mv.metadata_field_id = ? THEN mv.text_value END) AS FileBarcode " +
                                "FROM " +
                                "metadatavalue AS mv " +
                                "WHERE " +
                                "mv.dspace_object_id = ?::uuid " +
                                "AND mv.metadata_field_id IN (?, ?) " +
                                "GROUP BY " +
                                "mv.dspace_object_id;";
                        String lotNumber = "";
                        String fileBarcode = "";
                        try (PreparedStatement ps = conn.prepareStatement(sql);){
                            ps.setInt(1, lotNoId);
                            ps.setInt(2, fileBarcodeID);
                            ps.setString(3, uuid);
                            ps.setInt(4, lotNoId);
                            ps.setInt(5, fileBarcodeID);
                            try (ResultSet resultSet = ps.executeQuery();){
                                if (resultSet.next()) {
                                    lotNumber = resultSet.getString("LotNo");
                                    fileBarcode = resultSet.getString("FileBarcode");
                                    log.info("metadata values found for item with handle: {}", handle);
                                    log.info("UUID: {}, LotNo: {}, FileBarcode: {}", resultSet.getString("dspace_object_id"), resultSet.getString("LotNo"), resultSet.getArray("FileBarcode"));
                                }
                            }
                        }
                        sql = "SELECT COUNT(bitstream_id) FROM bundle2bitstream WHERE bundle_id IN ( " +
                                "SELECT ib.bundle_id FROM item2bundle AS ib " +
                                "INNER JOIN metadatavalue AS mv " +
                                "ON ib.bundle_id = mv.dspace_object_id " +
                                "WHERE ib.item_id = ?::uuid " +
                                "AND mv.text_value = 'ORIGINAL');";
                        int totalFileCount = 0;
                        try (PreparedStatement ps = conn.prepareStatement(sql);) {
                            ps.setString(1, uuid);
                            try (ResultSet resultSet = ps.executeQuery()){
                                if (Objects.nonNull(resultSet) && resultSet.next()) {
                                    totalFileCount = resultSet.getInt(1);
                                    log.info("item with uuid: {} has totalFileCount: {}", uuid, totalFileCount);
                                } else {
                                    log.info("no totalFileCount found for item with uuid; {}", uuid);
                                }
                            }
                        }
                        sql = "SELECT COUNT (b.uuid) FROM bitstream AS b INNER JOIN (SELECT btb.bitstream_id FROM bundle2bitstream AS btb WHERE bundle_id IN ( " +
                                "SELECT ib.bundle_id FROM item2bundle AS ib " +
                                "INNER JOIN metadatavalue AS mv " +
                                "ON ib.bundle_id = mv.dspace_object_id " +
                                "WHERE ib.item_id = ?::uuid " +
                                "AND mv.text_value = 'ORIGINAL')) as btsm " +
                                "ON b.uuid = btsm.bitstream_id " +
                                "WHERE b.bitstream_format_id IN ( " +
                                "SELECT bitstream_format_id FROM bitstreamformatregistry " +
                                "WHERE short_description ILIKE '%pdf%' " +
                                ");";
                        int pdfCount = 0;
                        try (PreparedStatement ps = conn.prepareStatement(sql);) {
                            ps.setString(1, uuid);
                            try (ResultSet resultSet = ps.executeQuery()){
                                if (Objects.nonNull(resultSet) && resultSet.next()) {
                                    pdfCount = resultSet.getInt(1);
                                    log.info("item with uuid: {} has pdfCount: {}", uuid, pdfCount);
                                } else {
                                    log.info("no pdfCount found for item with uuid; {}", uuid);
                                }
                            }
                        }
                        Map<String, Object> pdfDetails = getPdfDetails(uuid);
                        String cnrNo = getCNRNo(UUID.fromString(uuid));
                        ItemUploadInfo uploadInfo = new ItemUploadInfo();
                        uploadInfo.setHandle(handle);
                        uploadInfo.setItemUUID(uuid);
                        uploadInfo.setLotNumber(Objects.nonNull(lotNumber) ? lotNumber : "");
                        uploadInfo.setBarcodeNumber(Objects.nonNull(fileBarcode) ? fileBarcode : "");
                        uploadInfo.setDateOfUpload(LocalDateTime.now());
                        uploadInfo.setPageCount((int) pdfDetails.get(NyayNestConstants.PAGE_COUNT));
                        uploadInfo.setPDFA((boolean) pdfDetails.get(NyayNestConstants.IS_PDFA));
                        uploadInfo.setHasDigitalSignature((boolean) pdfDetails.get(NyayNestConstants.HAS_DIGITAL_SIGNATURES));
                        uploadInfo.setSignee((String) pdfDetails.get(NyayNestConstants.SIGNEE));
                        uploadInfo.setSignedAt(Objects.nonNull(pdfDetails.get(org.dspace.util.NyayNestConstants.SIGNED_AT)) ? (LocalDateTime) pdfDetails.get(NyayNestConstants.SIGNED_AT) : null);
                        uploadInfo.setTypeOfFile("");
                        uploadInfo.setTotalFileCount(totalFileCount);
                        uploadInfo.setPdfCount(pdfCount);
                        uploadInfo.setMediaFileSize(getMediaFileSizeForItem(uuid));
                        uploadInfo.setDeleteStatus(Boolean.FALSE);
                        uploadInfo.setCollectionName(collectionName);
                        uploadInfo.setCnrNo(cnrNo);
                        sql = "INSERT INTO item_upload_info(handle, item_id, lot_number, barcode_number, date_of_upload, page_count, is_pdfa, has_digital_signatures, signee, signed_at, media_file_size, type_of_file, delete_status, total_file_count, pdf_count, collection_name, cnr_no) " +
                                "VALUES (?, ?::uuid, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,?)";
                        try (PreparedStatement ps = conn.prepareStatement(sql);){
                            ps.setString(1, uploadInfo.getHandle());
                            ps.setString(2, uploadInfo.getItemUUID());
                            ps.setString(3, uploadInfo.getLotNumber());
                            ps.setString(4, uploadInfo.getBarcodeNumber());
                            ps.setObject(5, uploadInfo.getDateOfUpload());
                            ps.setInt(6, uploadInfo.getPageCount());
                            ps.setBoolean(7, uploadInfo.isPDFA());
                            ps.setBoolean(8, uploadInfo.isHasDigitalSignature());
                            ps.setString(9, uploadInfo.getSignee());
                            ps.setObject(10, uploadInfo.getSignedAt());
                            ps.setInt(11, uploadInfo.getMediaFileSize());
                            ps.setString(12, uploadInfo.getTypeOfFile());
                            ps.setBoolean(13, uploadInfo.isDeleteStatus());
                            ps.setInt(14, uploadInfo.getTotalFileCount());
                            ps.setInt(15, uploadInfo.getPdfCount());
                            ps.setString(16, uploadInfo.getCollectionName());
                            ps.setString(17, uploadInfo.getCnrNo());
                            int saveResponse = ps.executeUpdate();
                            boolean isSavedSuccessfully = Integer.valueOf(saveResponse).equals(1);
                            if (isSavedSuccessfully) {
                                log.info("upload info for item with handle: {} is saved in DB", handle);
                            } else {
                                log.info("no upload info saved for item with handle: {}", handle);
                            }
                        }
                    } else {
                        log.info("Required metadata fields not found in database");
                    }
                } else {
                    log.info("no item with handle: {} is found", handle);
                }

            } catch (SQLException | InterruptedException e) {
                e.printStackTrace();
            }

        } else {
            log.warn("no dataSource available in storeItemUploadInfo()");
        }

    }

    public Map<String, Object> getPdfDetails(String uuid) {

        Map<String, Object> pdfDetails = new HashMap<>();
        pdfDetails.put(NyayNestConstants.PAGE_COUNT, 0);
        pdfDetails.put(NyayNestConstants.IS_PDFA, false);
        pdfDetails.put(NyayNestConstants.HAS_DIGITAL_SIGNATURES, false);
        pdfDetails.put(NyayNestConstants.SIGNEE, "");
        pdfDetails.put(NyayNestConstants.SIGNED_AT, null);
        try (Connection conn = dataSource.getConnection();) {

            String sql = "SELECT i2b.bundle_id FROM item2bundle AS i2b " +
                    "INNER JOIN metadatavalue AS mv " +
                    "ON i2b.bundle_id = mv.dspace_object_id " +
                    "WHERE i2b.item_id = ?::uuid " +
                    "AND mv.text_value = 'ORIGINAL';";
            String bundleUUID = "";
            try (PreparedStatement ps = conn.prepareStatement(sql);) {
                ps.setString(1, uuid);
                try(ResultSet resultSet = ps.executeQuery();) {
                    if (resultSet.next()) {
                        bundleUUID = resultSet.getString("bundle_id");
                    }
                } catch (SQLException e) {
                    e.printStackTrace();;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (bundleUUID.isEmpty()) {
                log.info("no ORIGINAL bundle is found for item with uuid: {}", uuid);
            }

            sql = "SELECT DISTINCT b2b.bitstream_id AS bitstream_id FROM bundle2bitstream AS b2b " +
                    "INNER JOIN metadatavalue AS mv " +
                    "ON b2b.bitstream_id = mv.dspace_object_id " +
                    "WHERE b2b.bundle_id = ?::uuid " +
                    "AND mv.text_value ILIKE '%master%';";
            try (PreparedStatement ps = conn.prepareStatement(sql);) {
                ps.setString(1, bundleUUID);
                try (ResultSet resultSet = ps.executeQuery();
                     Context context = new Context();) {
                    if (resultSet.next()) {
                        log.info("MASTER file found for item with UUID: {}", uuid);
                        log.info("bitstream with UUID: {} found for MASTER file for item with UUID: {}", resultSet.getString("bitstream_id"), uuid);
                        String masterBitstreamUUID = resultSet.getString("bitstream_id");
                        int pageCount = getPagesOfBitstreamForUUID(masterBitstreamUUID);
                        pdfDetails.put(NyayNestConstants.PAGE_COUNT, pageCount);
                        Bitstream bitstream = bitstreamService.find(context, UUID.fromString(masterBitstreamUUID));
                        if (Objects.nonNull(bitstream)) {
                            InputStream bitstreamInputStream = bitstreamStorageService.retrieve(context, bitstream);
                            if (Objects.nonNull(bitstreamInputStream)) {
                                Map<String, Object> pdfAndSignatureDetails = PDFAUtility.getPDFAndSignatureDetails(bitstreamInputStream);
                                pdfDetails.put(NyayNestConstants.IS_PDFA, pdfAndSignatureDetails.get(NyayNestConstants.IS_PDFA));
                                pdfDetails.put(NyayNestConstants.HAS_DIGITAL_SIGNATURES, pdfAndSignatureDetails.get(NyayNestConstants.HAS_DIGITAL_SIGNATURES));
                                pdfDetails.put(NyayNestConstants.SIGNEE, pdfAndSignatureDetails.get(NyayNestConstants.SIGNEE));
                                pdfDetails.put(NyayNestConstants.SIGNED_AT, pdfAndSignatureDetails.get(NyayNestConstants.SIGNED_AT));
                            }
                        }
                    } else {
                        log.info("no MASTER file present for item with UUID: {}", uuid);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return pdfDetails;

    }

    private boolean isPDFA(String bitstreamUUID) {

        Map<String, Object> pdfAndSignatureDetails = new HashMap<>();
        boolean isPDFA = false;
        boolean hasDigitalSignatures = false;
        try(Context context = new Context();) {
            UUID uuid = UUID.fromString(bitstreamUUID);
            Bitstream bitstream = bitstreamService.find(context, uuid);
            if (Objects.nonNull(bitstream)) {
                InputStream bitstreamInputStream = bitstreamStorageService.retrieve(context, bitstream);
                if (Objects.nonNull(bitstreamInputStream)) {
                    isPDFA = PDFAUtility.isPdfa(bitstreamInputStream);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return isPDFA;

    }

    private int getPagesOfBitstreamForUUID(String bitstreamUUID) {

        int pageCount = 0;

        try (Context context = new Context()) {

            UUID uuid = UUID.fromString(bitstreamUUID);
            Bitstream bitstream = bitstreamService.find(context, uuid);

            if (bitstream == null) {
                log.info("No bitstream found for UUID: {}", bitstreamUUID);
                return 0;
            }

            String mimeType = bitstream.getFormat(context).getMIMEType();
            if (!"application/pdf".equalsIgnoreCase(mimeType)) {
                log.warn("Bitstream is not a PDF for UUID: {}", bitstreamUUID);
                return 0;
            }

            try (
                    InputStream inputStream = bitstreamStorageService.retrieve(context, bitstream);
                    RandomAccessReadBuffer buffer = new RandomAccessReadBuffer(inputStream);
                    PDDocument document = Loader.loadPDF(buffer)
            ) {
                pageCount = document.getNumberOfPages();

                log.info("PDF has {} pages for UUID: {}", pageCount, bitstreamUUID);

            } catch (Exception e) {
                log.error("Failed to read PDF for UUID: {}", bitstreamUUID, e);
            }

        } catch (Exception e) {
            log.error("Error fetching bitstream for UUID: {}", bitstreamUUID, e);
        }

        return pageCount;
    }

    private int getMediaFileSizeForItem(String uuid) {

        int sizeInBytes = 0;
        try (Connection conn = dataSource.getConnection();) {

            String sql = "SELECT i2b.bundle_id FROM item2bundle AS i2b " +
                    "INNER JOIN metadatavalue AS mv " +
                    "ON i2b.bundle_id = mv.dspace_object_id " +
                    "WHERE i2b.item_id = ?::uuid " +
                    "AND mv.text_value = 'ORIGINAL';";
            String bundleUUID = "";
            try (PreparedStatement ps = conn.prepareStatement(sql);) {
                ps.setString(1, uuid);
                try(ResultSet resultSet = ps.executeQuery();) {
                    if (resultSet.next()) {
                        bundleUUID = resultSet.getString("bundle_id");
                    } else {
                        return sizeInBytes;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            if (bundleUUID.isEmpty()) {
                log.info("no ORIGINAL bundle is found for item with uuid: {}", uuid);
                return sizeInBytes;
            }

            sql = "select sum(bs.size_bytes) from bundle2bitstream as b2b " +
                    "inner join bitstream as bs " +
                    "on b2b.bitstream_id = bs.uuid " +
                    "where b2b.bundle_id = ?::uuid;";
            try(PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, bundleUUID);
                try (ResultSet resultSet = ps.executeQuery();) {
                    if (resultSet.next()) {
                        log.info("item with uuid: {} has {} bytes of ORIGINAL bundle", uuid, resultSet.getInt(1));
                        sizeInBytes = resultSet.getInt(1);
                    } else {
                        log.info("no bitstream byte size is found for item with uuid: {}", uuid);
                        return sizeInBytes;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sizeInBytes;

    }

    @Override
    public void updateStatusForItemUploadInfo(String handle, Boolean isDeleted) {

        if (Objects.nonNull(dataSource)) {
            try (Connection conn = dataSource.getConnection();) {

                boolean infoExists = false;
                String sql = "SELECT EXISTS (SELECT 1 FROM item_upload_info WHERE handle = ?)";
                try(PreparedStatement ps = conn.prepareStatement(sql);) {
                    ps.setString(1, handle);
                    try {
                        ResultSet resultSet = ps.executeQuery();
                        if (resultSet.next()) {
                            infoExists = resultSet.getBoolean(1);
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                if (infoExists) {
                    log.info("upload info found for item with handle: {}", handle);

                } else {
                    log.info("upload info for item with handle: {} does not exist", handle);
                }

                sql = "UPDATE item_upload_info SET delete_status = TRUE WHERE handle = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql);) {
                    ps.setString(1, handle);
                    int rowCount = ps.executeUpdate();
                    if (!Integer.valueOf(0).equals(rowCount)) {
                        log.info("delete status set to TRUE for item with handle: {} in table item_upload_info", handle);
                    } else {
                        log.info("not able to set delete status to TRUE for item with handle: {}", handle);
                    }
                } catch (SQLException e) {
                    e.printStackTrace();;
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            log.warn("no dataSource available in updateStatusForItemUploadInfo()");
        }

    }
    private String getCNRNo(UUID uuid) throws SQLException {
        String cnrNo = null;
        try (Context context = new Context()) {
            context.turnOffAuthorisationSystem();
            Item item = itemService.find(context, uuid);
            if (item != null) {
                List<String> cnrNos = itemService
                        .getMetadata(item, "dc", "CNRNo", null, Item.ANY)
                        .stream()
                        .map(MetadataValue::getValue)
                        .collect(Collectors.toList());
                if (!cnrNos.isEmpty()) {
                    try {
                        cnrNo = cnrNos.get(0);
                        log.info("CNR No: {}", cnrNo);
                    } catch (Exception e) {
                        log.warn("CNR No is not numeric: {}", cnrNos.get(0));
                    }
                }
            }
            context.complete();
        }
        return cnrNo;
    }
}
