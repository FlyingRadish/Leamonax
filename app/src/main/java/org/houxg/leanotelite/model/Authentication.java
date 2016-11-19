package org.houxg.leanotelite.model;


import com.google.gson.annotations.SerializedName;
import com.raizlabs.android.dbflow.annotation.Column;

public class Authentication extends BaseResponse {

    @SerializedName("UserId")
    String userId = "";
    @SerializedName("Username")
    String userName = "";
    @Column(name = "email")
    @SerializedName("Email")
    String email = "";
    @SerializedName("Token")
    String accessToken = "";

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getEmail() {
        return email;
    }

    public String getAccessToken() {
        return accessToken;
    }
}
