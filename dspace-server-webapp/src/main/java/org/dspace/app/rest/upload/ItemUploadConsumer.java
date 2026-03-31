package org.dspace.app.rest.upload;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.upload.service.ItemUploadInfoService;
import org.dspace.app.rest.upload.service.impl.ItemUploadInfoServiceImpl;
import org.dspace.core.Context;
import org.dspace.event.Consumer;
import org.dspace.event.Event;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.context.ApplicationContext;

public class ItemUploadConsumer implements Consumer {

    private static final Logger log = LogManager.getLogger(ItemUploadConsumer.class);

    private ItemUploadInfoService itemUploadInfoService;

    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Override
    public void initialize() throws Exception {
        itemUploadInfoService = DSpaceServicesFactory.getInstance().getServiceManager()
                .getServiceByName(ApplicationContext.class.getName(), ApplicationContext.class)
                .getBean(ItemUploadInfoServiceImpl.class);
    }

    @Override
    public void consume(Context ctx, Event event) throws Exception {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        int eventType = event.getEventType();
        switch (eventType) {

            case Event.INSTALL:
                log.info("consume() got item INSTALL event: {}", event);
                executor.submit(() -> itemUploadInfoService.storeItemUploadInfo(event.getDetail()));
                break;
            case Event.DELETE:
                log.info("consume() got item DELETE event: {}", event);
                executor.submit(() -> itemUploadInfoService.updateStatusForItemUploadInfo(event.getDetail(), Boolean.TRUE));
                break;
            default:
                log.info("consume() got unrecognised event: {}", event);

        }

    }

    @Override
    public void end(Context ctx) throws Exception {

    }

    @Override
    public void finish(Context ctx) throws Exception {

    }
}
