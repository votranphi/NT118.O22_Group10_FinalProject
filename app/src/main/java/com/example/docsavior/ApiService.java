package com.example.docsavior;

import java.sql.Blob;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {
    @POST("/user/login")
    Call<Detail> postLoginInfo(@Query("username") String username, @Query("password") String password);

    @POST("/user/logout")
    Call<Detail> postLogout(@Query("username") String username);

    @POST("/user/add")
    Call<Detail> postSignUpInfo(@Query("username") String username, @Query("email") String email, @Query("phoneNumber") String phoneNumber, @Query("password") String password, @Query("fullName") String fullName, @Query("birthDay") String birthDay, @Query("gender") boolean gender);

    @POST("/user/password_recovery")
    Call<Detail> postRecoverPasswordInfo(@Query("username") String username, @Query("email") String email, @Query("phoneNumber") String phoneNumber);

    @POST("/user/password_change")
    Call<Detail> postChangePassword(@Query("username") String username, @Query("oldPassword") String oldPassword, @Query("newPassword") String newPassword);

    @POST("/otp/create_or_refresh")
    Call<Detail> postCreateOrRefreshOTP(@Query("username") String username, @Query("email") String email);

    @POST("/otp/check")
    Call<Detail> postCheckOTP(@Query("username") String username, @Query("otp") String otp);

    @GET("/newsfeed/all")
    Call<List<NewsFeed>> getAllPosts();

    @POST("/newsfeed/add")
    Call<Detail> postNewsfeed(@Query("username") String username, @Query("postDescription") String postDescription, @Query("postContent") String postContent, @Query("fileData") String fileData, @Query("fileName") String fileName, @Query("fileExtension") String fileExtension);

    @POST("/friend/add")
    Call<Detail> postNewFriend(@Query("username") String username, @Query("usernameFriend") String usernameFriend);

    @GET("/friend_request/all")
    Call<Requester> getAllFriendRequests(@Query("username") String username);

    @DELETE("/friend_request/delete")
    Call<Detail> deleteFriendRequest(@Query("username") String username, @Query("requester") String requester);
}