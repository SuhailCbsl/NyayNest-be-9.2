package org.dspace.upload;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.storage.bitstore.factory.StorageServiceFactory;
import org.dspace.storage.bitstore.service.BitstreamStorageService;
import org.dspace.upload.service.ItemUploadInfoService;
import org.dspace.upload.service.impl.ItemUploadInfoServiceImpl;

public class ItemUploadConsumer implements Consumer {

    private static final Logger log = LogManager.getLogger(ItemUploadConsumer.class);

    private ItemUploadInfoService itemUploadInfoService;

    private ItemService itemService;
    private BitstreamService bitstreamService;
    private BitstreamStorageService bitstreamStorageService;

    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    @Override
    public void initialize() {
        itemService = ContentServiceFactory.getInstance().getItemService();
        bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
        bitstreamStorageService = StorageServiceFactory.getInstance().getBitstreamStorageService();

        log.info("✅ Consumer initialized");
    }

    @Override
    public void consume(Context ctx, Event event) {

        try {
            UUID itemId = event.getSubjectID();

            if (itemId == null) {
                log.warn("⚠️ Item ID is null");
                return;
            }

            // ✅ Use already initialized service (from initialize())
            Item item = itemService.find(ctx, itemId);

            if (item == null) {
                log.warn("⚠️ Item not found for UUID: {}", itemId);
                return;
            }

            String handle = item.getHandle();

            if (handle == null) {
                log.warn("⚠️ Handle is null for item: {}", itemId);
                return;
            }

            int eventType = event.getEventType();

            switch (eventType) {

                case Event.INSTALL:
                    log.info("📦 INSTALL event for handle: {}", handle);

                    executor.submit(() -> {
                        try {
                            ItemUploadInfoServiceImpl service = new ItemUploadInfoServiceImpl();

                            // ✅ Inject already initialized services
                            service.setItemService(itemService);
                            service.setBitstreamService(bitstreamService);
                            service.setBitstreamStorageService(bitstreamStorageService);

                            service.storeItemUploadInfo(handle);

                        } catch (Exception e) {
                            log.error("❌ Error processing upload info", e);
                        }
                    });
                    break;

                case Event.DELETE:
                    log.info("🗑 DELETE event for handle: {}", handle);

                    executor.submit(() -> {
                        try {
                            ItemUploadInfoServiceImpl service = new ItemUploadInfoServiceImpl();

                            service.setItemService(itemService);
                            service.setBitstreamService(bitstreamService);
                            service.setBitstreamStorageService(bitstreamStorageService);

                            service.updateStatusForItemUploadInfo(handle, true);

                        } catch (Exception e) {
                            log.error("❌ Error updating delete status", e);
                        }
                    });
                    break;

                default:
                    log.debug("Ignoring event type: {}", eventType);
            }

        } catch (Exception e) {
            log.error("❌ Error in consume()", e);
        }
    }

    @Override
    public void end(Context ctx) throws Exception {

    }

    @Override
    public void finish(Context ctx) throws Exception {

    }
}
