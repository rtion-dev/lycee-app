package com.otmanelabouze.abdallahguennoun

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Environment
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal enum class SchoolWebsite(val home:String) {
    MASSAR("https://massarservice.men.gov.ma/moutamadris/Account"),
    TELMIDTICE("https://telmidtice.men.gov.ma/")
}

private data class SchoolDownload(val url:String,val agent:String,val disposition:String,val mime:String)

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun SchoolWebScreen(site:SchoolWebsite,lang:String,dark:Boolean,initialUrl:String=site.home,onClose:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val close by rememberUpdatedState(onClose)
    val windows=remember {mutableStateListOf<WebView>()}
    var loading by remember {mutableStateOf(true)}
    var failed by remember {mutableStateOf(false)}
    var back by remember {mutableStateOf(false)}
    var forward by remember {mutableStateOf(false)}
    var tools by remember {mutableStateOf(true)}
    var menu by remember {mutableStateOf(false)}
    var host by remember {mutableStateOf(secureWebHost(initialUrl).orEmpty())}
    var pending by remember {mutableStateOf<Pair<WebView,String>?>(null)}
    var clear by remember {mutableStateOf(false)}
    var clearing by remember {mutableStateOf(false)}
    var download by remember {mutableStateOf<SchoolDownload?>(null)}
    var notice by remember {mutableStateOf("")}
    var fullscreen by remember {mutableStateOf<View?>(null)}
    var fullscreenCallback by remember {mutableStateOf<WebChromeClient.CustomViewCallback?>(null)}
    var chooser by remember {mutableStateOf<ValueCallback<Array<Uri>>?>(null)}
    var alive by remember {mutableStateOf(true)}
    val accepted=remember {mutableSetOf(initialUrl)}
    fun tr(ar:String,fr:String,en:String)=appText(lang,ar,fr,en)
    val filePicker=rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {result->
        chooser?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result.resultCode,result.data));chooser=null
    }
    fun detach(view:View) {(view.parent as? ViewGroup)?.removeView(view)}
    fun updateNavigation(web:WebView) {
        if(alive && windows.lastOrNull()===web) {back=web.canGoBack();forward=web.canGoForward();host=secureWebHost(web.url.orEmpty()).orEmpty()}
    }
    fun destroy(web:WebView) {
        detach(web);web.stopLoading();web.setDownloadListener(null);web.webChromeClient=null;web.webViewClient=WebViewClient();web.destroy()
    }
    fun hideFullscreen() {fullscreen?.let {detach(it)};fullscreen=null;fullscreenCallback?.onCustomViewHidden();fullscreenCallback=null}
    fun closeWindow(web:WebView) {
        if(windows.size>1){windows.remove(web);destroy(web);windows.lastOrNull()?.let {it.onResume();updateNavigation(it)}}else close()
    }
    fun newWindow():WebView {
        val web=WebView(context)
        web.setBackgroundColor(if(dark)android.graphics.Color.rgb(9,23,36)else android.graphics.Color.WHITE)
        web.settings.apply {
            javaScriptEnabled=true;domStorageEnabled=true
            allowFileAccess=false;allowContentAccess=false
            mixedContentMode=WebSettings.MIXED_CONTENT_NEVER_ALLOW
            safeBrowsingEnabled=true
            setSupportMultipleWindows(true);javaScriptCanOpenWindowsAutomatically=false
            builtInZoomControls=true;displayZoomControls=false
            useWideViewPort=true;loadWithOverviewMode=true
            mediaPlaybackRequiresUserGesture=true
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web,false)
        web.webViewClient=object:WebViewClient() {
            override fun shouldOverrideUrlLoading(view:WebView,request:WebResourceRequest):Boolean {
                val url=request.url.toString()
                if(url=="about:blank")return false
                if(secureWebHost(url)==null)return true
                // HTTPS lesson players may be embedded from another origin; only top-level
                // navigation changes the platform shown to the student.
                if(!request.isForMainFrame)return false
                if(isSchoolWebUrl(url)||url in accepted)return false
                if(request.isForMainFrame && alive)pending=view to url
                return true
            }
            override fun onPageStarted(view:WebView,url:String,icon:Bitmap?) {
                if(alive && windows.lastOrNull()===view){loading=true;failed=false;tools=true;host=secureWebHost(url).orEmpty()}
            }
            override fun onPageFinished(view:WebView,url:String) {if(alive && windows.lastOrNull()===view){loading=false;updateNavigation(view);CookieManager.getInstance().flush()}}
            override fun doUpdateVisitedHistory(view:WebView,url:String?,isReload:Boolean){updateNavigation(view)}
            override fun onReceivedError(view:WebView,request:WebResourceRequest,error:WebResourceError) {if(alive && request.isForMainFrame && windows.lastOrNull()===view){failed=true;loading=false;tools=true}}
            override fun onReceivedHttpError(view:WebView,request:WebResourceRequest,response:WebResourceResponse) {if(alive && request.isForMainFrame && windows.lastOrNull()===view){failed=true;loading=false;tools=true}}
            override fun onReceivedSslError(view:WebView,handler:SslErrorHandler,error:SslError) {handler.cancel();if(alive){failed=true;loading=false;tools=true}}
            override fun onRenderProcessGone(view:WebView,detail:RenderProcessGoneDetail):Boolean {
                if(pending?.first===view)pending=null
                windows.remove(view);detach(view);view.destroy()
                if(alive){hideFullscreen();failed=true;loading=false;tools=true;back=false;forward=false}
                return true
            }
        }
        web.webChromeClient=object:WebChromeClient() {
            override fun onCreateWindow(view:WebView,isDialog:Boolean,isUserGesture:Boolean,resultMsg:Message):Boolean {
                if(!alive || !isUserGesture || windows.size>=4)return false
                val child=newWindow();view.onPause();windows.add(child)
                (resultMsg.obj as? WebView.WebViewTransport)?.let {it.webView=child;resultMsg.sendToTarget();return true}
                windows.remove(child);destroy(child);return false
            }
            override fun onCloseWindow(window:WebView){if(alive)closeWindow(window)}
            override fun onShowFileChooser(view:WebView,callback:ValueCallback<Array<Uri>>,params:FileChooserParams):Boolean {
                chooser?.onReceiveValue(null);chooser=callback
                try {filePicker.launch(params.createIntent())}catch(_:Exception){chooser?.onReceiveValue(null);chooser=null}
                return true
            }
            override fun onShowCustomView(view:View,callback:CustomViewCallback) {
                if(fullscreen!=null){callback.onCustomViewHidden();return};fullscreen=view;fullscreenCallback=callback
            }
            override fun onHideCustomView(){hideFullscreen()}
        }
        web.setDownloadListener {url,agent,disposition,mime,_->
            if(alive && secureWebHost(url)!=null)download=SchoolDownload(url,agent.orEmpty(),disposition.orEmpty(),mime.orEmpty())
            else if(alive)notice=tr("هذا النوع من التحميل غير مدعوم داخل التطبيق.","Ce téléchargement n’est pas pris en charge.","This download type is not supported in the app.")
        }
        web.setOnScrollChangeListener {_,_,y,_,oldY->if(alive && kotlin.math.abs(y-oldY)>12)tools=y<oldY || y<32}
        return web
    }
    suspend fun start() {
        loading=true;failed=false
        try {
            require(secureWebHost(initialUrl)!=null)
            val changed=SchoolEngine.prepare(context)
            if(alive && windows.isEmpty()) {
                val web=newWindow();if(changed)web.clearCache(true);windows.add(web);web.loadUrl(initialUrl)
            }
        }catch(e:CancellationException){throw e}catch(_:Exception){loading=false;failed=true}
    }
    LaunchedEffect(initialUrl){start()}
    DisposableEffect(Unit) {
        val activity=context as? ComponentActivity
        val observer=LifecycleEventObserver {_,event->
            if(event==Lifecycle.Event.ON_PAUSE)windows.forEach {it.onPause()}
            if(event==Lifecycle.Event.ON_RESUME)windows.lastOrNull()?.onResume()
        }
        activity?.lifecycle?.addObserver(observer)
        onDispose {
            alive=false;activity?.lifecycle?.removeObserver(observer);chooser?.onReceiveValue(null);chooser=null
            hideFullscreen();windows.toList().forEach {destroy(it)};windows.clear();pending=null
        }
    }
    fun goBack() {
        if(fullscreen!=null){hideFullscreen();return}
        val current=windows.lastOrNull()
        if(current?.canGoBack()==true)current.goBack()else if(current!=null && windows.size>1)closeWindow(current)else close()
    }
    BackHandler {goBack()}
    Box(Modifier.fillMaxSize().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(tools||failed) {
                Row(Modifier.fillMaxWidth().height(52.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
                    IconButton(onClick={goBack()}){SchoolWebIcon("back",tr("رجوع","Retour","Back"))}
                    IconButton(onClick={windows.lastOrNull()?.goForward()},enabled=forward){SchoolWebIcon("forward",tr("الأمام","Suivant","Forward"))}
                    IconButton(onClick={windows.lastOrNull()?.loadUrl(initialUrl)}){SchoolWebIcon("home",tr("البداية","Accueil","Start"))}
                    IconButton(onClick={if(windows.isEmpty())scope.launch{start()}else windows.last().reload()}){SchoolWebIcon("refresh",tr("تحديث","Actualiser","Refresh"))}
                    Box {
                        IconButton(onClick={menu=true}){SchoolWebIcon("more",tr("خيارات","Options","Options"))}
                        DropdownMenu(menu,{menu=false}) {
                            DropdownMenuItem(text={Text(host)},onClick={menu=false})
                            DropdownMenuItem(text={Text(tr("مسح جلسة المواقع","Effacer la session","Clear site session"))},onClick={menu=false;clear=true})
                            DropdownMenuItem(text={Text(label(lang,"close"))},onClick={menu=false;close()})
                        }
                    }
                }
            }
            if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
            if(failed)Text(tr("تعذر تحميل الموقع. تحقق من الاتصال وتحديث Android System WebView ثم أعد المحاولة.","Vérifiez la connexion et la mise à jour d’Android System WebView, puis réessayez.","Check your connection and Android System WebView updates, then retry."),Modifier.padding(16.dp),color=MaterialTheme.colorScheme.error)
            if(notice.isNotEmpty())Text(notice,Modifier.padding(12.dp))
            val front=windows.lastOrNull()
            AndroidView(modifier=Modifier.weight(1f).fillMaxWidth(),factory={FrameLayout(it)},update={frame->
                if(frame.getChildAt(0)!==front){frame.removeAllViews();front?.let {detach(it);frame.addView(it,FrameLayout.LayoutParams(-1,-1));it.onResume();updateNavigation(it)}}
            })
        }
        fullscreen?.let {custom->AndroidView(modifier=Modifier.fillMaxSize(),factory={FrameLayout(it).apply {setBackgroundColor(android.graphics.Color.BLACK)}},update={frame->if(custom.parent!==frame){detach(custom);frame.removeAllViews();frame.addView(custom,FrameLayout.LayoutParams(-1,-1))}})}
    }
    pending?.let {(web,url)->AlertDialog(onDismissRequest={pending=null},title={Text(tr("رابط خارج المنصة","Lien hors plateforme","Link outside the platform"))},text={Text(secureWebHost(url).orEmpty())},confirmButton={TextButton(onClick={accepted.add(url);pending=null;if(web in windows)web.loadUrl(url)}){Text(tr("فتح هنا","Ouvrir ici","Open here"))}},dismissButton={TextButton(onClick={pending=null}){Text(label(lang,"close"))}})}
    if(clear)AlertDialog(onDismissRequest={if(!clearing)clear=false},title={Text(tr("مسح بيانات الدخول؟","Effacer la connexion ?","Clear sign-in data?"))},confirmButton={TextButton(enabled=!clearing,onClick={scope.launch {
        clearing=true
        try {windows.forEach {it.stopLoading();it.clearCache(true);it.clearHistory()};SchoolEngine.clearCurrentSession(context);clear=false;close()}
        catch(e:CancellationException){throw e}catch(_:Exception){notice=tr("تعذر مسح الجلسة. أعد المحاولة.","Impossible d’effacer la session.","Unable to clear the session. Retry.")}
        finally{clearing=false}
    }}){Text(tr("مسح","Effacer","Clear"))}},dismissButton={TextButton(enabled=!clearing,onClick={clear=false}){Text(label(lang,"close"))}})
    download?.let {item->AlertDialog(onDismissRequest={download=null},title={Text(tr("حفظ الملف؟","Enregistrer le fichier ?","Save file?"))},text={Text(secureWebHost(item.url).orEmpty())},confirmButton={TextButton(onClick={
        download=null
        try {
            val name=URLUtil.guessFileName(item.url,item.disposition,item.mime).replace(Regex("[^\\p{L}\\p{N}._ -]"),"_").take(120)
            val request=DownloadManager.Request(Uri.parse(item.url)).setTitle(name).setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(context,Environment.DIRECTORY_DOWNLOADS,System.currentTimeMillis().toString()+"-"+name)
            if(item.mime.isNotBlank())request.setMimeType(item.mime)
            if(item.agent.isNotBlank())request.addRequestHeader("User-Agent",item.agent)
            CookieManager.getInstance().getCookie(item.url)?.let {request.addRequestHeader("Cookie",it)}
            (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
            notice=tr("بدأ التحميل. افتحه من إشعار اكتمال التحميل.","Téléchargement lancé. Ouvrez la notification à la fin.","Download started. Open it from the completion notification.")
        }catch(_:Exception){notice=tr("تعذر بدء التحميل.","Téléchargement impossible.","Unable to start download.")}
    }){Text(tr("حفظ","Enregistrer","Save"))}},dismissButton={TextButton(onClick={download=null}){Text(label(lang,"close"))}})}
}

@Composable
private fun SchoolWebIcon(kind:String,description:String) {
    val color=LocalContentColor.current
    Canvas(Modifier.size(22.dp).semantics {contentDescription=description}) {
        val w=size.width;val h=size.height
        val stroke=androidx.compose.ui.graphics.drawscope.Stroke(width=2.dp.toPx())
        fun path(vararg pts:Pair<Float,Float>) {
            val p=androidx.compose.ui.graphics.Path()
            pts.forEachIndexed {i,v->if(i==0)p.moveTo(w*v.first,h*v.second)else p.lineTo(w*v.first,h*v.second)}
            drawPath(p,color,style=stroke)
        }
        when(kind) {
            "back"->{path(.65f to .15f,.3f to .5f,.65f to .85f);path(.3f to .5f,.95f to .5f)}
            "forward"->{path(.35f to .15f,.7f to .5f,.35f to .85f);path(.05f to .5f,.7f to .5f)}
            "home"->{path(.05f to .45f,.5f to .08f,.95f to .45f);path(.2f to .4f,.2f to .9f,.8f to .9f,.8f to .4f)}
            "refresh"->{drawArc(color,45f,285f,false,topLeft=androidx.compose.ui.geometry.Offset(w*.15f,h*.15f),size=androidx.compose.ui.geometry.Size(w*.7f,h*.7f),style=stroke);path(.85f to .08f,.85f to .38f,.57f to .38f)}
            else->listOf(.2f,.5f,.8f).forEach {drawCircle(color,w*.06f,androidx.compose.ui.geometry.Offset(w*.5f,h*it))}
        }
    }
}
