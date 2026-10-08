package com.otmanelabouze.abdallahguennoun

import android.content.Intent
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Source
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.tasks.await
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

private val mediaWords=mapOf(
        "album" to listOf("فتح الألبوم والتكبير","Ouvrir l’album et zoomer","Open album and zoom"),
        "photo" to listOf("تغيير صورة البروفيل","Changer la photo","Change profile photo"),
        "saved" to listOf("تم الحفظ","Enregistré","Saved"),
        "failed" to listOf("تعذر إتمام العملية. أعد المحاولة.","Échec. Réessayez.","Unable to complete. Please retry."),
        "pdf" to listOf("فتح PDF","Ouvrir le PDF","Open PDF"),
        "reset" to listOf("إعادة التكبير","Réinitialiser le zoom","Reset zoom"),
        "empty" to listOf("لم يُنشر استعمال زمن لقسمك بعد.","Aucun emploi du temps publié pour votre classe.","No timetable published for your class yet."),
        "imageError" to listOf("تعذر تحميل الصورة — اضغط للمحاولة","Image indisponible — toucher pour réessayer","Image unavailable — tap to retry")
    )
internal fun mediaLabel(lang:String,key:String):String {
    return mediaWords.getValue(key)[when(lang){"fr"->1;"en"->2;else->0}]
}
internal fun PortalArticle.images():List<String> =
    ((fields["images"] as? List<*>)?.mapNotNull { (it as? Map<*,*>)?.get("url") as? String }
        ?: listOfNotNull((fields["imageUrl"] as? String)?.takeIf {it.isNotBlank()})).take(20)

internal fun PortalArticle.cover():String =
    ((fields["images"] as? List<*>)?.firstOrNull() as? Map<*,*>)?.get("thumbnailUrl") as? String
        ?: images().firstOrNull().orEmpty()

@Composable
internal fun MediaImage(url:String,modifier:Modifier=Modifier,fit:Boolean=false,lang:String="ar",maxDimension:Int=1200) {
    val context=LocalContext.current
    var retry by remember(url){mutableIntStateOf(0)}
    var busy by remember(url){mutableStateOf(true)}
    val image by produceState<Bitmap?>(null,url,retry,maxDimension) {
        busy=true
        value=null
        val cached=try{SchoolMediaCache.load(context,url,maxDimension)}catch(e:CancellationException){throw e}catch(_:Exception){null}
        value=cached?.bitmap
        busy=false
        if(cached!=null) {delay((cached.expiresAt-System.currentTimeMillis()).coerceAtLeast(0));value=null}
    }
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant),contentAlignment=Alignment.Center) {
        val bitmap=image
        if(bitmap!=null)Image(bitmap.asImageBitmap(),null,Modifier.fillMaxSize(),contentScale=if(fit)ContentScale.Fit else ContentScale.Crop)
        else if(busy&&url.isNotBlank())CircularProgressIndicator(Modifier.size(24.dp))
        else if(url.isNotBlank())TextButton(onClick={retry++}){Text(mediaLabel(lang,"imageError"))}
    }
}

@Composable
internal fun AlbumViewer(images:List<String>,lang:String,close:()->Unit) {
    if(images.isEmpty())return
    val pager=rememberPagerState(pageCount={images.size})
    Dialog(onDismissRequest=close,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
            Column(Modifier.systemBarsPadding()) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
                    TextButton(onClick=close){Text(label(lang,"close"))}
                    Text("${pager.currentPage+1} / ${images.size}",Modifier.padding(16.dp))
                }
                HorizontalPager(pager,Modifier.weight(1f),beyondViewportPageCount=0) { index ->
                    var scale by remember {mutableFloatStateOf(1f)}
                    var x by remember {mutableFloatStateOf(0f)};var y by remember {mutableFloatStateOf(0f)}
                    LaunchedEffect(pager.currentPage){scale=1f;x=0f;y=0f}
                    Column(Modifier.fillMaxSize()) {
                        val transform=rememberTransformableState { zoom,pan,_ ->scale=(scale*zoom).coerceIn(1f,5f);x=if(scale>1f)(x+pan.x).coerceIn(-1500f,1500f)else 0f;y=if(scale>1f)(y+pan.y).coerceIn(-1500f,1500f)else 0f}
                        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().transformable(transform,canPan={scale>1f})) {
                            MediaImage(images[index],Modifier.fillMaxSize().graphicsLayer{scaleX=scale;scaleY=scale;translationX=x;translationY=y},true,lang,maxDimension=2400)
                        }
                        TextButton(onClick={scale=1f;x=0f;y=0f}){Text(mediaLabel(lang,"reset"))}
                        GallerySaveButton(images[index],lang)
                    }
                }
            }
        }
    }
}

@Composable
internal fun TimetableFeed(lang:String,header:(@Composable ()->Unit)?=null) {
    var documents by remember{mutableStateOf<List<PortalArticle>>(emptyList())}
    var loading by remember{mutableStateOf(true)};var failed by remember{mutableStateOf(false)};var refresh by remember{mutableIntStateOf(0)}
    var album by remember{mutableStateOf<List<String>?>(null)}
    var documentUrl by remember{mutableStateOf<String?>(null)}
    var listener by remember{mutableStateOf<com.google.firebase.firestore.ListenerRegistration?>(null)}
    DisposableEffect(Unit){onDispose{listener?.remove()}}
    val context=LocalContext.current
    LaunchedEffect(refresh) {
        loading=true;failed=false
        try {
            val uid=AuthManager.auth.currentUser?.uid?:error("signed_out")
            val user=AuthManager.db.collection("users").document(uid).get(Source.SERVER).await()
            listener?.remove()
            listener=AuthManager.db.collection("timetables").whereEqualTo("status","published")
                .whereEqualTo("classId",user.getString("classId")).whereEqualTo("academicYear",user.getString("academicYear"))
                .addSnapshotListener { result,error ->
                    failed=error!=null
                    if(result!=null) documents=result.documents.sortedByDescending{it.getTimestamp("publishedAt")}.mapNotNull{d->d.data?.let{PortalArticle(d.id,it)}}
                    loading=false
                }
        }catch(e:CancellationException){throw e}catch(_:Exception){failed=true;loading=false}
    }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        if(header!=null)item{header()}
        item {TextButton(onClick={refresh++},enabled=!loading){Text(label(lang,"refresh"))}}
        if(loading) item {CircularProgressIndicator()}
        else if(failed) item {Text(label(lang,"error"),color=MaterialTheme.colorScheme.error)}
        else if(documents.isEmpty()) item {Text(mediaLabel(lang,"empty"))}
        items(documents,key={it.id}) { article ->
            Card(Modifier.fillMaxWidth(),shape=androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    Text(article.text("title",lang),style=MaterialTheme.typography.titleMedium)
                    val images=article.images()
                    if(images.isNotEmpty()) {
                        MediaImage(images.first(),Modifier.fillMaxWidth().height(220.dp).clickable{album=images},true,lang)
                        TextButton(onClick={album=images}){Text(mediaLabel(lang,"album"))}
                    }
                    val pdf=(article.fields["pdf"] as? Map<*,*>)?.get("url") as? String
                    if(pdf?.startsWith("https://")==true)TextButton(onClick={
                        documentUrl=pdf
                    }){Text(mediaLabel(lang,"pdf"))}
                }
            }
        }
    }
    album?.let{AlbumViewer(it,lang){album=null}}
    documentUrl?.let {url->Dialog(onDismissRequest={documentUrl=null},properties=DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
        Surface(Modifier.fillMaxSize()) {Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            SchoolWebScreen(SchoolWebsite.TELMIDTICE,lang,isSystemInDarkTheme(),initialUrl=url){documentUrl=null}
        }}
    }}
}

@Composable
internal fun AccountPhoto(lang:String, initials:String="") {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var photo by remember{mutableStateOf("")};var busy by remember{mutableStateOf(false)};var feedback by remember{mutableStateOf("")}
    LaunchedEffect(Unit){try{val uid=AuthManager.auth.currentUser?.uid?:return@LaunchedEffect;photo=AuthManager.db.collection("users").document(uid).get(Source.DEFAULT).await().getString("photoUrl").orEmpty()}catch(e:CancellationException){throw e}catch(_:Exception){feedback=mediaLabel(lang,"failed")}}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri!=null&&!busy)scope.launch {
            busy=true;feedback=""
            try {
                val uid=AuthManager.auth.currentUser?.uid?:error("signed_out")
                val path=ProfilePhotoStore.importImage(context,uri,uid)
                val oldPhoto=photo
                val url=ProfilePhotoStore.upload(path)
                AuthManager.db.collection("users").document(uid).update(mapOf("photoUrl" to url,"avatarType" to "photo","updatedAt" to FieldValue.serverTimestamp())).await()
                photo=url;feedback=mediaLabel(lang,"saved");File(path).delete()
                // Delete only after Firestore confirms the new reference.
                if(oldPhoto.isNotBlank() && oldPhoto!=url) try { ProfilePhotoStore.deleteRemote(oldPhoto) } catch(e:CancellationException){throw e} catch(_:Exception) {}
            }catch(e:CancellationException){throw e}catch(_:Exception){feedback=mediaLabel(lang,"failed")}
            finally{busy=false}
        }
    }
    if(photo.isNotBlank())MediaImage(photo,Modifier.size(110.dp).then(Modifier.clip(androidx.compose.foundation.shape.CircleShape)),true,lang,maxDimension=512)
    else Surface(Modifier.size(110.dp),shape=androidx.compose.foundation.shape.CircleShape,color=MaterialTheme.colorScheme.primaryContainer) {
        Box(contentAlignment=Alignment.Center) { Text(initials.ifBlank{"?"},fontSize=32.sp,color=MaterialTheme.colorScheme.onPrimaryContainer) }
    }
    Button(onClick={picker.launch("image/*")},enabled=!busy){Text(mediaLabel(lang,"photo"))}
    if(photo.isNotBlank())GallerySaveButton(photo,lang)
    if(busy)CircularProgressIndicator()
    if(feedback.isNotBlank())Text(feedback)
}
