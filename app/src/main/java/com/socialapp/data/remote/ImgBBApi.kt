package com.socialapp.data.remote

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

object ImgBBApi {
    private const val BASE_URL = "https://api.imgbb.com/1/upload"
    private const val API_KEY = "068773bc8e4cc1631e6da11d8f1d8882" // Replace with your key

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun uploadImage(base64Image: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", API_KEY)
                .addFormDataPart("image", base64Image)
                .build()

            val request = Request.Builder()
                .url(BASE_URL)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: throw Exception("Empty response")
            val result = Gson().fromJson(json, ImgBBResponse::class.java)

            if (result.success) {
                result.data.url
            } else {
                throw Exception("Upload failed")
            }
        }
    }
}

data class ImgBBResponse(val success: Boolean, val data: ImgBBData)
data class ImgBBData(val url: String)
