package com.example.docsavior;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @POST("/user/login")
    Call<Detail> postLoginInfo(@Query("username") String username, @Query("password") String password);

    @POST("/user/add")
    Call<Detail> postSignUpInfo(@Query("username") String username, @Query("email") String email, @Query("phoneNumber") String phoneNumber, @Query("password") String password);

    @POST("/user/password_recovery")
    Call<Detail> postRecoverPasswordInfo(@Query("username") String username, @Query("email") String email, @Query("phoneNumber") String phoneNumber);

    @GET("/newfeed/all")
    Call<List<Newfeed>> getAllPosts();
}