package com.otmanelabouze.abdallahguennoun

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.tasks.await
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit
import javax.net.ssl.HttpsURLConnection

internal object SchoolMediaCache {
    const val TTL=24L*60*60*1000
    private const val MAX_BYTES=12*1024*1024
    private const val DISK_LIMIT=96L*1024*1024
    private val slots=Semaphore(2)
    private val requestLocks=Array(32){Mutex()}
    private val fileLock=Any()
    private var generation=0L
    data class CachedImage(val bitmap:Bitmap,val expiresAt:Long)
    private fun directory(context:Context)=File(context.cacheDir,"school_images_v2").apply{mkdirs()}
    private fun scope(link:String):String {
        val uri=Uri.parse(link)
        require(uri.scheme=="https" && uri.userInfo==null)
        return if(uri.path.orEmpty().startsWith("/avatar/")) {
            val uid=AuthManager.auth.currentUser?.uid ?: error("signed_out")
            require(uri.pathSegments.getOrNull(1)==uid)
            "private:$uid:"
        } else "public:"
    }
    private fun target(context:Context,link:String):File {
        val digest=java.security.MessageDigest.getInstance("SHA-256").digest((scope(link)+link).toByteArray())
        return File(directory(context),digest.joinToString(""){"%02x".format(it)})
    }
    internal fun fresh(created:Long,now:Long)=created>0 && now>=created && now-created<TTL
    suspend fun bytes(context:Context,link:String):Pair<ByteArray,Long> = requestLocks[(link.hashCode() and Int.MAX_VALUE)%requestLocks.size].withLock { fetchBytes(context,link) }
    private suspend fun fetchBytes(context:Context,link:String):Pair<ByteArray,Long> = slots.withPermit {
        withContext(Dispatchers.IO) {
            val requestGeneration=synchronized(fileLock){generation}
            val file=target(context,link)
            val hit=synchronized(fileLock) {
                if(file.isFile && fresh(file.lastModified(),System.currentTimeMillis()))
                    runCatching {file.readBytes() to (file.lastModified()+TTL)}.getOrNull()
                else {file.delete();null}
            }
            if(hit!=null)return@withContext hit
            val privateImage=Uri.parse(link).path.orEmpty().startsWith("/avatar/")
            val uid=AuthManager.auth.currentUser?.uid
            val token=if(privateImage) {
                val base=ProfilePhotoStore.endpoint()
                require(link.startsWith("$base/avatar/"))
                AuthManager.auth.currentUser?.getIdToken(false)?.await()?.token ?: error("signed_out")
            } else null
            val connection=URL(link).openConnection() as HttpsURLConnection
            val data=try {
                connection.instanceFollowRedirects=false
                connection.connectTimeout=10000;connection.readTimeout=15000
                token?.let {connection.setRequestProperty("Authorization","Bearer $it")}
                require(connection.responseCode==200)
                connection.inputStream.use {input ->
                    val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                    while(true) {
                        currentCoroutineContext().ensureActive()
                        val n=input.read(buffer);if(n<0)break
                        require(out.size()+n<=MAX_BYTES)
                        out.write(buffer,0,n)
                    }
                    out.toByteArray()
                }
            } finally {connection.disconnect()}
            val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            BitmapFactory.decodeByteArray(data,0,data.size,bounds)
            require(bounds.outWidth>0 && bounds.outHeight>0)
            if(privateImage)check(AuthManager.auth.currentUser?.uid==uid)
            val created=System.currentTimeMillis()
            synchronized(fileLock) {
                check(requestGeneration==generation)
                if(privateImage)check(AuthManager.auth.currentUser?.uid==uid)
                val temp=File.createTempFile("image", ".tmp",directory(context))
                try {temp.writeBytes(data);check(temp.renameTo(file));file.setLastModified(created)}finally{temp.delete()}
                trimLocked(context,created)
            }
            data to (created+TTL)
        }
    }
    suspend fun load(context:Context,link:String,maxDimension:Int):CachedImage {
        require(maxDimension in 64..4096)
        val (data,expiry)=bytes(context,link)
        return withContext(Dispatchers.IO) {
            val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
            BitmapFactory.decodeByteArray(data,0,data.size,bounds)
            var sample=1
            while(bounds.outWidth/sample>maxDimension || bounds.outHeight/sample>maxDimension)sample*=2
            val bitmap=BitmapFactory.decodeByteArray(data,0,data.size,BitmapFactory.Options().apply{inSampleSize=sample}) ?: error("image")
            CachedImage(bitmap,expiry)
        }
    }
    private fun trimLocked(context:Context,now:Long) {
        val files=directory(context).listFiles().orEmpty().filter {it.isFile}
        files.filter {!fresh(it.lastModified(),now)}.forEach{it.delete()}
        var size=files.filter{it.exists()}.sumOf{it.length()}
        files.sortedBy{it.lastModified()}.forEach {if(size>DISK_LIMIT && it.exists()){val n=it.length();if(it.delete())size-=n}}
    }
    fun cleanup(context:Context)=synchronized(fileLock) {
        trimLocked(context,System.currentTimeMillis())
        // Retire the old cache which did not enforce a fixed TTL.
        File(context.cacheDir,"public_media").deleteRecursively()
    }
    suspend fun clear(context:Context)=withContext(Dispatchers.IO) {synchronized(fileLock){generation++;directory(context).deleteRecursively()};Unit}
    fun schedule(context:Context) {
        val manager=WorkManager.getInstance(context)
        manager.enqueueUniquePeriodicWork("school-image-expiry",ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<ImageCacheCleanupWorker>(15,TimeUnit.MINUTES).build())
        manager.enqueueUniqueWork("school-image-clean-now",ExistingWorkPolicy.KEEP,OneTimeWorkRequestBuilder<ImageCacheCleanupWorker>().build())
    }
}

class ImageCacheCleanupWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
    override suspend fun doWork():Result=withContext(Dispatchers.IO) {
        try {SchoolMediaCache.cleanup(applicationContext);Result.success()}catch(e:CancellationException){throw e}catch(_:Exception){Result.retry()}
    }
}
