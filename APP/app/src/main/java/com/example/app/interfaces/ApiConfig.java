package com.example.app.interfaces;

import com.example.app.models.chord_model;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiConfig {
    @Multipart
    @POST("/upload")
    Call<chord_model> uploadFile(@Part MultipartBody.Part file, @Part("file") RequestBody name);
}
