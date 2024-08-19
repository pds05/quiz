package ru.otus.java.basic.project.server.providers.db.jdbc.entity;

import java.util.Date;

public class UserActivity {
    private Integer id;
    private int userId;
    private Date lastConnectDate;
    private Date LastDisconnectDate;
    private Date kickDate;
    private boolean isOnline;

    public UserActivity(Integer id, int userId, Date lastConnectDate, Date lastDisconnectDate, Date kickDate, boolean isOnline) {
        this.id = id;
        this.userId = userId;
        this.lastConnectDate = lastConnectDate;
        LastDisconnectDate = lastDisconnectDate;
        this.kickDate = kickDate;
        this.isOnline = isOnline;
    }

    public UserActivity(int userId, Date lastConnectDate, Date lastDisconnectDate, Date kickDate, boolean isOnline) {
        this.userId = userId;
        this.lastConnectDate = lastConnectDate;
        LastDisconnectDate = lastDisconnectDate;
        this.kickDate = kickDate;
        this.isOnline = isOnline;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public Date getLastConnectDate() {
        return lastConnectDate;
    }

    public void setLastConnectDate(Date lastConnectDate) {
        this.lastConnectDate = lastConnectDate;
    }

    public Date getLastDisconnectDate() {
        return LastDisconnectDate;
    }

    public void setLastDisconnectDate(Date lastDisconnectDate) {
        LastDisconnectDate = lastDisconnectDate;
    }

    public Date getKickDate() {
        return kickDate;
    }

    public void setKickDate(Date kickDate) {
        this.kickDate = kickDate;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    @Override
    public String toString() {
        return "UserActivity{" +
                "id=" + id +
                ", userId=" + userId +
                ", lastConnectDate=" + lastConnectDate +
                ", LastDisconnectDate=" + LastDisconnectDate +
                ", kickDate=" + kickDate +
                ", isOnline=" + isOnline +
                '}';
    }
}
