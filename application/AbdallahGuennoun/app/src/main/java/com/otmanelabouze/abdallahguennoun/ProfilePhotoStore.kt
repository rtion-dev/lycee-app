package com.otmanelabouze.abdallahguennoun

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.util.UUID
import javax.net.ssl.HttpsURLConnection

object ProfilePhotoStore {
    // Optional protected backend endpoint. Never put Cloudinary API secrets in the app.
    // POST image/jpeg + Authorization: Bearer <Firebase ID token>
    // Server must verify token, size/content, ownership; upload to storage; return {"url":"https://..."}.
    suspend fun endpoint(): String {
        val value = AuthManager.db.collection("schoolConfig").document("media")
            .get(com.google.firebase.firestore.Source.SERVER).await().getString("uploadBaseUrl")?.trimEnd('/')
            ?: throw ProfileIssue("photo_setup")
        val uri = Uri.parse(value)
        if (uri.scheme != "https" || uri.host.isNullOrBlank() || !uri.query.isNullOrEmpty() || !uri.fragment.isNullOrEmpty()) throw ProfileIssue("photo_setup")
        return value
    }

    suspend fun importImage(context: Context, uri: Uri, uid: String): String = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw ProfileIssue("photo_error")
        var sample = 1
        while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw ProfileIssue("photo_error")
        val exif = resolver.openInputStream(uri)?.use { ExifInterface(it) }
        val matrix = Matrix()
        if (exif != null) {
            matrix.postRotate(exif.rotationDegrees.toFloat())
            if (exif.isFlipped) matrix.postScale(-1f, 1f)
        }
        val corrected = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        val scale = minOf(1f, 512f / maxOf(corrected.width, corrected.height))
        val scaled = Bitmap.createScaledBitmap(corrected,
            (corrected.width * scale).toInt().coerceAtLeast(1),
            (corrected.height * scale).toInt().coerceAtLeast(1), true)
        val dir = File(context.filesDir, "profile_drafts/$uid").apply { mkdirs() }
        val file = File(dir, "${UUID.randomUUID()}.jpg")
        file.outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        if (scaled !== corrected) scaled.recycle()
        if (corrected !== bitmap) corrected.recycle()
        bitmap.recycle()
        file.absolutePath
    }

    suspend fun upload(path: String): String {
        val uploadEndpoint = endpoint() + "/upload?kind=avatar"
        val user = AuthManager.auth.currentUser ?: throw ProfileIssue("login_error")
        val token = user.getIdToken(false).await().token ?: throw ProfileIssue("login_error")
        return withContext(Dispatchers.IO) {
            val url = URL(uploadEndpoint)
            if (url.protocol != "https") throw ProfileIssue("photo_setup")
            val file = File(path)
            if (!file.isFile || file.length() > 2 * 1024 * 1024) throw ProfileIssue("photo_error")
            val connection = url.openConnection() as HttpsURLConnection
            try {
                connection.requestMethod = "POST"
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.doOutput = true
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "image/jpeg")
                connection.setFixedLengthStreamingMode(file.length())
                connection.outputStream.use { output -> file.inputStream().use { it.copyTo(output) } }
                if (connection.responseCode !in 200..299) throw ProfileIssue("upload_error")
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body).getString("url").also {
                    if (!it.startsWith("https://")) throw ProfileIssue("upload_error")
                }
            } finally { connection.disconnect() }
        }
    }
    suspend fun deleteRemote(link: String) {
        val base = endpoint()
        if (!link.startsWith("$base/avatar/")) return
        val parts = Uri.parse(link).pathSegments
        val user = AuthManager.auth.currentUser ?: return
        if (parts.size != 4 || parts[1] != user.uid) return
        val token = user.getIdToken(false).await().token ?: return
        withContext(Dispatchers.IO) {
            val body = JSONObject().put("publicId", "rtion/avatars/${user.uid}/${parts[2]}")
                .put("resourceType", "image").toString().toByteArray()
            val connection = URL("$base/delete").openConnection() as HttpsURLConnection
            try {
                connection.requestMethod = "POST"; connection.doOutput = true
                connection.connectTimeout = 10000; connection.readTimeout = 15000
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body) }
                if (connection.responseCode !in 200..299) throw ProfileIssue("upload_error")
            } finally { connection.disconnect() }
        }
    }

}
