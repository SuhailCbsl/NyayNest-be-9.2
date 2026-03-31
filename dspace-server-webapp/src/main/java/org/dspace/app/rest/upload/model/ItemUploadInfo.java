package org.dspace.app.rest.upload.model;

import java.time.LocalDateTime;

public class ItemUploadInfo {

    private String handle;
    private String itemUUID;
    private String  lotNumber;
    private String barcodeNumber;
    private LocalDateTime dateOfUpload;
    private int pageCount;
    private boolean isPDFA;
    private boolean hasDigitalSignature;
    private String signee;
    private LocalDateTime signedAt;
    private int pdfCount;
    private int totalFileCount;
    private int mediaFileSize;
    private String typeOfFile;
    private boolean deleteStatus;
    private String collectionName;
    private String cnrNo;

    public ItemUploadInfo() {}

    public ItemUploadInfo(String handle, String itemUUID, String lotNumber, String barcodeNumber, LocalDateTime dateOfUpload, int pageCount, boolean isPDFA, boolean hasDigitalSignature, String signee, LocalDateTime signedAt, int pdfCount, int totalFileCount, int mediaFileSize, String typeOfFile, boolean deleteStatus, String collectionName, String cnrNo) {
        this.handle = handle;
        this.itemUUID = itemUUID;
        this.lotNumber = lotNumber;
        this.barcodeNumber = barcodeNumber;
        this.dateOfUpload = dateOfUpload;
        this.pageCount = pageCount;
        this.isPDFA = isPDFA;
        this.hasDigitalSignature = hasDigitalSignature;
        this.signee = signee;
        this.signedAt = signedAt;
        this.pdfCount = pdfCount;
        this.totalFileCount = totalFileCount;
        this.mediaFileSize = mediaFileSize;
        this.typeOfFile = typeOfFile;
        this.deleteStatus = deleteStatus;
        this.collectionName = collectionName;
        this.cnrNo = cnrNo;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getItemUUID() {
        return itemUUID;
    }

    public void setItemUUID(String itemUUID) {
        this.itemUUID = itemUUID;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public String getBarcodeNumber() {
        return barcodeNumber;
    }

    public void setBarcodeNumber(String barcodeNumber) {
        this.barcodeNumber = barcodeNumber;
    }

    public LocalDateTime getDateOfUpload() {
        return dateOfUpload;
    }

    public void setDateOfUpload(LocalDateTime dateOfUpload) {
        this.dateOfUpload = dateOfUpload;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public boolean isPDFA() {
        return isPDFA;
    }

    public void setPDFA(boolean PDFA) {
        isPDFA = PDFA;
    }

    public boolean isHasDigitalSignature() {
        return hasDigitalSignature;
    }

    public void setHasDigitalSignature(boolean hasDigitalSignature) {
        this.hasDigitalSignature = hasDigitalSignature;
    }

    public String getSignee() {
        return signee;
    }

    public void setSignee(String signee) {
        this.signee = signee;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(LocalDateTime signedAt) {
        this.signedAt = signedAt;
    }

    public int getPdfCount() {
        return pdfCount;
    }

    public void setPdfCount(int pdfCount) {
        this.pdfCount = pdfCount;
    }

    public int getTotalFileCount() {
        return totalFileCount;
    }

    public void setTotalFileCount(int totalFileCount) {
        this.totalFileCount = totalFileCount;
    }

    public int getMediaFileSize() {
        return mediaFileSize;
    }

    public void setMediaFileSize(int mediaFileSize) {
        this.mediaFileSize = mediaFileSize;
    }

    public String getTypeOfFile() {
        return typeOfFile;
    }

    public void setTypeOfFile(String typeOfFile) {
        this.typeOfFile = typeOfFile;
    }

    public boolean isDeleteStatus() {
        return deleteStatus;
    }

    public void setDeleteStatus(boolean deleteStatus) {
        this.deleteStatus = deleteStatus;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public String getCnrNo() {
        return cnrNo;
    }

    public void setCnrNo(String cnrNo) {
        this.cnrNo = cnrNo;
    }
}
