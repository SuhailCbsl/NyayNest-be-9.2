package org.dspace.app.rest.audit.dto;

public class AuditTrailDTO {

    private String uuid;
    private String action;
    private String handle;
    private String userId;
    private String userName;
    private String ipAddress;
    private String url;
    private String eventTime;
    private String detail;

    public AuditTrailDTO() {
    }

    public AuditTrailDTO(String uuid,
                         String entityType,
                         String handle,
                         String action,
                         String userId,
                         String userName,
                         String ipAddress,
                         String url,
                         String eventTime,
                         String detail) {
        this.uuid = uuid;
        this.handle = handle;
        this.action = action;
        this.userId = userId;
        this.userName = userName;
        this.ipAddress = ipAddress;
        this.url = url;
        this.eventTime = eventTime;
        this.detail = detail;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getEventTime() {
        return eventTime;
    }

    public void setEventTime(String eventTime) {
        this.eventTime = eventTime;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    @Override
    public String toString() {
        return "AuditTrailDTO{" +
                "uuid='" + uuid + '\'' +
                ", handle='" + handle + '\'' +
                ", action='" + action + '\'' +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", url='" + url + '\'' +
                ", eventTime='" + eventTime + '\'' +
                ", detail='" + detail + '\'' +
                '}';
    }
}
