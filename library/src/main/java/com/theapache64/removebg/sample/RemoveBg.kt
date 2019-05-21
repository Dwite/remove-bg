package com.theapache64.removebg.sample

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.theapache64.twinkill.logger.info
import okhttp3.*
import java.io.File
import java.io.IOException

object RemoveBg {

    // Strings
    private const val API_ENDPOINT = "https://api.remove.bg/v1.0/removebg"

    private var apiKey: String? = null

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .build()
    }

    fun init(apiKey: String) {
        this.apiKey = apiKey
    }

    fun from(file: File, onRemoved: (bitmap: Bitmap) -> Unit) {

        require(apiKey != null) { "You must call RemoveBg.init before calling RemoveBg.from" }

        // file
        val filePart = MultipartBody.create(MediaType.parse("image/png"), file)
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("size", "auto")
            .addFormDataPart("image_file", "image_file", filePart)
            .build()


        // new request
        val request = Request.Builder()
            .url(API_ENDPOINT)
            .addHeader("X-Api-Key", apiKey!!)
            .post(body)
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {

                if (response.isSuccessful) {
                    response.body()!!.byteStream().let { bytesStream ->
                        val bmp = BitmapFactory.decodeStream(bytesStream)
                        onRemoved(bmp)
                    }
                }
            }
        })

    }

}