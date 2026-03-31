package org.dspace.app.rest.upload.service;

public interface ItemUploadInfoService {

    void storeItemUploadInfo(String handle);

    void updateStatusForItemUploadInfo(String handle, Boolean isDeleted);

}
