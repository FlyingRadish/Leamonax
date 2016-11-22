package org.houxg.leamonax.model;

import com.google.gson.annotations.SerializedName;
import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import org.houxg.leamonax.database.AppDataBase;

@Table(name = "Account", database = AppDataBase.class)
public class Account extends BaseModel {

    public static final int EDITOR_RICH_TEXT = 0;
    public static final int EDITOR_MARKDOWN = 1;

    @SerializedName("Ok")
    boolean isOk;
    @SerializedName("Msg")
    String msg;

    @Column(name = "id")
    @PrimaryKey(autoincrement = true)
    @SerializedName("LocalUserId")
    long localUserId;
    @Column(name = "userId")
    @SerializedName("UserId")
    String userId = "";
    @Column(name = "userName")
    @SerializedName("Username")
    String userName = "";
    @Column(name = "email")
    @SerializedName("Email")
    String email = "";
    @Column(name = "verified")
    @SerializedName("Verified")
    boolean verified;
    @Column(name = "avatar")
    @SerializedName("Avatar")
    String avatar = "";
    @Column(name = "token")
    @SerializedName("Token")
    String accessToken = "";
    @Column(name = "defaultEditor")
    int defaultEditor = EDITOR_MARKDOWN;
    @Column(name = "lastUsn")
    @SerializedName("LastSyncUsn")
    int lastSyncUsn;
    @Column(name = "host")
    @SerializedName("Host")
    String host = "";

    public Account() {
    }

    public boolean isOk() {
        return isOk;
    }

    public long getLocalUserId() {
        return localUserId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isVerified() {
        return verified;
    }

    public String getAvatar() {
        return avatar;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public int getLastSyncUsn() {
        return lastSyncUsn;
    }

    public int getDefaultEditor() {
        return defaultEditor;
    }

    public String getHost() {
        return host;
    }

    public String getMsg() {
        return msg;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setLastUsn(int lastUsn) {
        this.lastSyncUsn = lastUsn;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public void setDefaultEditor(int defaultEditor) {
        this.defaultEditor = defaultEditor;
    }

    @Override
    public String toString() {
        return "Account{" +
                "isOk=" + isOk +
                ", msg='" + msg + '\'' +
                ", localUserId=" + localUserId +
                ", userId='" + userId + '\'' +
                ", userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", verified=" + verified +
                ", avatar='" + avatar + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", defaultEditor=" + defaultEditor +
                ", lastSyncUsn=" + lastSyncUsn +
                ", host='" + host + '\'' +
                '}';
    }
}
