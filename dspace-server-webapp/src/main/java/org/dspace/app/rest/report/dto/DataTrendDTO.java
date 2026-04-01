package org.dspace.app.rest.report.dto;

import java.util.Date;
import java.util.UUID;

public class DataTrendDTO {
    String handle;
    UUID itemId;
    String lotNo;
    String barcodeNo;
    Date dateOfUpload;
    int pageCount;
    int mediaFileSize;
    String typeOfFile;
    boolean deleteStatus;
    int pdfCount;
    int totalFileCount;
    boolean isPdfA;
    boolean hasDigitalSignatures;
    String signee;
    Date signedAt;
    String collectionName;
    String cnrNo;

    public DataTrendDTO() {}

    public DataTrendDTO(UUID itemId, String handle, String lotNo, String barcodeNo, Date dateOfUpload, int pageCount, int mediaFileSize, String typeOfFile, boolean deleteStatus, int pdfCount, int totalFileCount, boolean isPdfA, boolean hasDigitalSignatures, String signee, Date signedAt, String collectionName, String cnrNo) {
        this.itemId = itemId;
        this.handle = handle;
        this.lotNo = lotNo;
        this.barcodeNo = barcodeNo;
        this.dateOfUpload = dateOfUpload;
        this.pageCount = pageCount;
        this.mediaFileSize = mediaFileSize;
        this.typeOfFile = typeOfFile;
        this.deleteStatus = deleteStatus;
        this.pdfCount = pdfCount;
        this.totalFileCount = totalFileCount;
        this.isPdfA = isPdfA;
        this.hasDigitalSignatures = hasDigitalSignatures;
        this.signee = signee;
        this.signedAt = signedAt;
        this.collectionName = collectionName;
        this.cnrNo = cnrNo;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public UUID getItemId() {
        return itemId;
    }

    public void setItemId(UUID itemId) {
        this.itemId = itemId;
    }

    public String getLotNo() {
        return lotNo;
    }

    public void setLotNo(String lotNo) {
        this.lotNo = lotNo;
    }

    public String getBarcodeNo() {
        return barcodeNo;
    }

    public void setBarcodeNo(String barcodeNo) {
        this.barcodeNo = barcodeNo;
    }

    public Date getDateOfUpload() {
        return dateOfUpload;
    }

    public void setDateOfUpload(Date dateOfUpload) {
        this.dateOfUpload = dateOfUpload;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
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

    public boolean isPdfA() {
        return isPdfA;
    }

    public void setPdfA(boolean pdfA) {
        isPdfA = pdfA;
    }

    public boolean isHasDigitalSignatures() {
        return hasDigitalSignatures;
    }

    public void setHasDigitalSignatures(boolean hasDigitalSignatures) {
        this.hasDigitalSignatures = hasDigitalSignatures;
    }

    public String getSignee() {
        return signee;
    }

    public void setSignee(String signee) {
        this.signee = signee;
    }

    public Date getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(Date signedAt) {
        this.signedAt = signedAt;
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
