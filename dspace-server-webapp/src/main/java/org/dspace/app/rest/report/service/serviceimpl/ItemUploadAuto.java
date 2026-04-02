package org.dspace.app.rest.report.service.serviceimpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;

import org.dspace.content.service.BitstreamService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.dspace.upload.service.ItemUploadInfoService;
import org.dspace.upload.service.impl.ItemUploadInfoServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ItemUploadAuto {

    @Value("${onoffAutoItemUpload}")
    private String onoffAutoItemUpload;

    private static final Logger log = LoggerFactory.getLogger(org.dspace.upload.service.impl.ItemUploadInfoServiceImpl.class);

    private DataSource dataSource = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName("dataSource", DataSource.class);

    @Autowired
    private BitstreamService bitstreamService;

    @Autowired
    private BitstreamStorageService bitstreamStorageService;

    private ItemUploadInfoService itemUploadInfoService;

    public ItemUploadAuto() {
        itemUploadInfoService = DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName(ApplicationContext.class.getName(), ApplicationContext.class)
                .getBean(ItemUploadInfoServiceImpl.class);
    }

    @Scheduled(cron = "${updateBulkItemsCron}", zone = "Asia/Kolkata")
    public void storeItemUploadInfo() {
        if (onoffAutoItemUpload.equals("true")) {

            if (Objects.nonNull(dataSource)) {

                List<String> handles = new ArrayList<>();
                try (Connection conn = dataSource.getConnection();) {

                    Thread.sleep(1000);
                    String sql1 = "SELECT h.handle FROM handle h JOIN item i ON h.resource_id = i.uuid " +
                            "LEFT JOIN item_upload_info u ON i.uuid = u.item_id WHERE u.item_id IS NULL; ";
                    PreparedStatement ps1 = conn.prepareStatement(sql1);
                    ResultSet resultSet1 = ps1.executeQuery();
                    while (resultSet1.next()) {
                        handles.add(resultSet1.getString("handle"));
                    }
                } catch (Exception e) {
                    log.error("error occurred while trying to store item upload info using CRON");
                }
                if (!handles.isEmpty()) {
                    for (String handle: handles) {
                        itemUploadInfoService.storeItemUploadInfo(handle);
                    }
                }

            } else {
                log.warn("no dataSource available in storeItemUploadInfo()");
            }
        } else {
            log.info("Auto-upload of items is turned off!");
        }
    }
}
