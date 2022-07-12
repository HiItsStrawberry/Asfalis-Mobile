package com.example.asfalis_mobile.services

import com.example.asfalis_mobile.models.JwtDTO
import com.example.asfalis_mobile.models.LoginPersonalDTO
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface UserService {

    // Api call services

    // Call api to login as mobile user
    @POST("login/personal")
    fun login(@Body user: LoginPersonalDTO) : Call<ResponseBody>

    // Call api to validate the login jwt token
    @POST("login/token/validation")
    fun validateToken(@Body token: JwtDTO) : Call<ResponseBody>

    // Call api to decrypt the QR Code
    @GET("login/qrcode/{userId}/{code}")
    fun getCodeFromQR(
        @Path("userId") userId: Int,
        @Path("code") code: String,
        @Header("Authorization") token: String) : Call<ResponseBody>
}