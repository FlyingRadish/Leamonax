package org.houxg.leamonax.network.api;

import org.houxg.leamonax.model.Authentication;
import org.houxg.leamonax.model.BaseResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface AuthApi {

    @GET("auth/login")
    Call<Authentication> login(@Query("email") String email, @Query("pwd") String password);

    @GET("auth/logout")
    Call<BaseResponse> logout(@Query("token") String token);

    @GET("auth/register")
    Call<BaseResponse> register(@Query("email") String email, @Query("pwd") String password);
}
