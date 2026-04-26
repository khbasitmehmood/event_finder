package com.eventfinder.app.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import com.eventfinder.app.domain.repository.StorageRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : StorageRepository {

    private val cloudName = "de3o2qgki"
    private val uploadPreset = "EventApp"
    private val apiKey = "581196453461348"
    private val client = OkHttpClient()

    override suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // 1. Create a temporary file from the URI to use with OkHttp
                val tempFile = getFileFromUri(imageUri)
                    ?: throw Exception("Could not create file from URI")

                // Determine mime type
                val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"

                // 2. Build the Multipart request using OkHttp (more reliable than HttpURLConnection)
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("upload_preset", uploadPreset)
                    .addFormDataPart("api_key", apiKey)
                    .addFormDataPart("public_id", "profile_$userId")
                    .addFormDataPart(
                        "file",
                        tempFile.name,
                        tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
                    )
                    .build()

                val request = Request.Builder()
                    .url("https://api.cloudinary.com/v1_1/$cloudName/image/upload")
                    .post(requestBody)
                    .build()

                // 3. Execute the request
                val response = client.newCall(request).execute()

                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonObject = JSONObject(responseBody)
                    val secureUrl = jsonObject.getString("secure_url")
                    
                    // Clean up temp file
                    tempFile.delete()
                    
                    Result.success(secureUrl)
                } else {
                    // Clean up temp file
                    tempFile.delete()
                    Result.failure(Exception("Upload failed with code ${response.code}: $responseBody"))
                }

            } catch (e: Exception) {
                Result.failure(Exception("Failed to upload image: ${e.message}", e))
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(context.cacheDir, "upload_temp_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(tempFile)
            
            inputStream.copyTo(outputStream)
            
            inputStream.close()
            outputStream.close()
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}