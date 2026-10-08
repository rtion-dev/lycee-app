package com.otmanelabouze.abdallahguennoun

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID

internal suspend fun saveImageToGallery(context:Context,url:String)=withContext(Dispatchers.IO) {
    val bytes=SchoolMediaCache.bytes(context,url).first
    val options=BitmapFactory.Options().apply{inJustDecodeBounds=true}
    BitmapFactory.decodeByteArray(bytes,0,bytes.size,options)
    val mime=options.outMimeType ?: error("image")
    val extension=when(mime){"image/png"->"png";"image/webp"->"webp";"image/gif"->"gif";"image/jpeg"->"jpg";else->error("format")}
    val name="Rtion_${UUID.randomUUID()}.$extension"
    if(Build.VERSION.SDK_INT>=29) {
        val resolver=context.contentResolver
        val values=ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME,name);put(MediaStore.Images.Media.MIME_TYPE,mime)
            put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/Rtion")
            put(MediaStore.Images.Media.IS_PENDING,1)
        }
        val uri=resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values) ?: error("gallery")
        try {
            checkNotNull(resolver.openOutputStream(uri)).use {it.write(bytes)}
            currentCoroutineContext().ensureActive()
            values.clear();values.put(MediaStore.Images.Media.IS_PENDING,0)
            check(resolver.update(uri,values,null,null)>0)
        }catch(e:Exception){resolver.delete(uri,null,null);throw e}
    } else {
        @Suppress("DEPRECATION")
        val folder=File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"Rtion").apply{mkdirs()}
        val file=File(folder,name)
        try {file.writeBytes(bytes);currentCoroutineContext().ensureActive()}catch(e:Exception){file.delete();throw e}
        MediaScannerConnection.scanFile(context,arrayOf(file.absolutePath),arrayOf(mime),null)
    }
}

@Composable
internal fun GallerySaveButton(url:String,lang:String) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var busy by remember(url){mutableStateOf(false)}
    var saved by remember(url){mutableStateOf(false)}
    var error by remember(url){mutableStateOf(false)}
    fun save() {if(!busy)scope.launch {
        busy=true;error=false
        try {saveImageToGallery(context,url);saved=true}catch(e:CancellationException){throw e}catch(_:Exception){error=true}finally{busy=false}
    }}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){allowed->if(allowed)save()else error=true}
    TextButton(enabled=!busy&&!saved,onClick={
        if(Build.VERSION.SDK_INT<=28 && ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED)
            permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        else save()
    }) {
        Text(if(saved)appText(lang,"تم الحفظ في المعرض","Enregistré dans la galerie","Saved to gallery")
            else if(busy)appText(lang,"جارٍ الحفظ…","Enregistrement…","Saving…")
            else appText(lang,"حفظ في المعرض ↓","Enregistrer dans la galerie ↓","Save to gallery ↓"))
    }
    if(error)Text(appText(lang,"تعذر الحفظ. تحقق من الاتصال والمساحة والإذن ثم حاول مجدداً.","Échec : vérifiez connexion, espace et autorisation.","Could not save. Check connection, storage and permission, then retry."),color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
}
