package org.houxg.leanotelite.network.api;

import org.houxg.leanotelite.model.BaseResponse;
import org.houxg.leanotelite.model.SyncState;
import org.houxg.leanotelite.model.User;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface UserApi {

    @GET("user/info")
    Call<User> getInfo(@Query("userId") String userId);

    @GET("user/updateUsername")
    Call<BaseResponse> updateUsername(@Query("username") String username);

    @GET("user/updatePwd")
    Call<BaseResponse> updatePassword(@Query("oldPwd") String oldPwd, @Query("pwd") String pwd);

    @GET("user/getSyncState")
    Call<SyncState> getSyncState();
}
