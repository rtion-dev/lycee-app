package com.otmanelabouze.abdallahguennoun

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val portalText = mapOf(
    "notifications" to listOf("التنبيهات", "Notifications", "Notifications"),
    "home" to listOf("الرئيسية", "Accueil", "Home"),
    "guidance" to listOf("التوجيه", "Orientation", "Guidance"),
    "lessons" to listOf("الدروس", "Cours", "Lessons"),
    "documents" to listOf("الوثائق", "Documents", "Documents"),
    "account" to listOf("حسابي", "Compte", "Account"),
    "future" to listOf("خطوتك نحو المستقبل", "Un pas vers votre avenir", "Your next step forward"),
    "intro" to listOf("اكتشف المدارس والمسارات بعد البكالوريا", "Découvrez les écoles et les parcours après le bac", "Explore schools and pathways after graduation"),
    "search" to listOf("ابحث عن مدرسة أو تخصص", "Rechercher une école ou une spécialité", "Search for a school or subject"),
    "all" to listOf("الكل", "Tout", "All"),
    "schools" to listOf("المدارس", "Écoles", "Schools"),
    "exams" to listOf("المباريات", "Concours", "Admissions"),
    "scholarships" to listOf("المنح", "Bourses", "Scholarships"),
    "latest" to listOf("أخبار التوجيه", "Actualités de l’orientation", "Guidance news"),
    "news" to listOf("أخبار المؤسسة", "Actualités du lycée", "School news"),
    "timetable" to listOf("استعمال الزمن", "Emploi du temps", "Timetable"),
    "empty" to listOf("لا توجد أخبار منشورة حالياً.", "Aucune actualité publiée pour le moment.", "No published news yet."),
    "noResults" to listOf("لا توجد نتائج مطابقة.", "Aucun résultat correspondant.", "No matching results."),
    "error" to listOf("تعذر تحميل الأخبار. تحقق من الاتصال والصلاحيات.", "Chargement impossible. Vérifiez la connexion et les autorisations.", "Unable to load news. Check connectivity and permissions."),
    "refresh" to listOf("تحديث", "Actualiser", "Refresh"),
    "retry" to listOf("إعادة المحاولة", "Réessayer", "Retry"),
    "more" to listOf("قراءة المزيد", "Lire la suite", "Read more"),
    "close" to listOf("إغلاق", "Fermer", "Close"),
    "source" to listOf("فتح المصدر الرسمي", "Ouvrir la source officielle", "Open official source"),
    "badLink" to listOf("تعذر فتح الرابط.", "Impossible d’ouvrir le lien.", "Unable to open link."),
    "hello" to listOf("مرحباً بك", "Bienvenue", "Welcome"),
    "daily" to listOf("كل ما تحتاجه لمتابعة يومك الدراسي", "Suivez votre quotidien au lycée", "Keep up with your school day"),
    "soon" to listOf("هذه الخدمة قيد الإعداد.", "Ce service est en préparation.", "This service is being prepared."),
    "settings" to listOf("اللغة والمظهر", "Langue et apparence", "Language and appearance"),
    "logout" to listOf("تسجيل الخروج", "Déconnexion", "Sign out"),
    "loadMore" to listOf("تحميل المزيد", "Charger plus", "Load more"),
    "deadline" to listOf("آخر أجل", "Date limite", "Deadline"),
    "offline" to listOf("بيانات محفوظة؛ قد لا تكون محدّثة.", "Données en cache, potentiellement anciennes.", "Cached content may be out of date.")
)
internal fun label(lang: String, key: String): String =
    portalText.getValue(key)[when(lang) { "fr" -> 1; "en" -> 2; else -> 0 }]

internal data class PortalArticle(val id: String, val fields: Map<String, Any>) {
    fun text(key: String, lang: String): String {
        val localized = fields[key] as? Map<*, *>
        return (localized?.get(lang) as? String)?.takeIf { it.isNotBlank() }
            ?: (localized?.get("ar") as? String)?.takeIf { it.isNotBlank() }
            ?: (fields[key] as? String).orEmpty()
    }
    val category get() = fields["category"] as? String ?: "schools"
}

@Composable
fun MainAppScreen(userEmail: String?, onSignOutClick: () -> Unit, onPublicServices: () -> Unit) {
    AppThemeWrapper { dark, lang, themeChange, langChange ->
        var tab by rememberSaveable { mutableStateOf("home") }
        var accountPanel by remember {mutableStateOf<String?>(null)}
        var massar by rememberSaveable { mutableStateOf(false) }
        var notifications by rememberSaveable { mutableStateOf(false) }
        val pushSection by SchoolNotifications.pendingSection
        LaunchedEffect(pushSection) {
            pushSection?.let { tab=it;notifications=false;massar=false;SchoolNotifications.pendingSection.value=null }
        }
        BackHandler(enabled = tab != "home" || notifications || massar) { if(massar) massar=false else if(notifications) notifications=false else tab = "home" }
        if(massar || (tab=="lessons" && !notifications)) {
            Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                    SchoolWebScreen(if(massar) SchoolWebsite.MASSAR else SchoolWebsite.TELMIDTICE,lang,dark) {
                        if(massar) massar=false else tab="home"
                    }
                }
            }
            return@AppThemeWrapper
        }
        accountPanel?.let { panel ->
            androidx.compose.ui.window.Dialog(onDismissRequest={accountPanel=null},properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
                Surface(Modifier.fillMaxWidth().padding(16.dp),shape=RoundedCornerShape(24.dp)) {
                    Column(Modifier.heightIn(max=620.dp).verticalScroll(rememberScrollState()).padding(16.dp)) {
                        TextButton(onClick={accountPanel=null}) {Text(label(lang,"close"))}
                        if(panel=="profile")StudentProfileCard(lang,userEmail)else AppSettings(lang,langChange,onPublicServices)
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
        CampusBackdrop(dark)
        Scaffold(
            containerColor=Color.Transparent,
            contentColor=MaterialTheme.colorScheme.onBackground,
            bottomBar={
                CampusNavigation(if(notifications) "" else tab,lang) { id -> tab=id;notifications=false;massar=false }

            }
        ) {padding->
            ScreenEntrance(if(notifications)"notifications"else tab) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                if(tab in listOf("documents","account") && !notifications) {
                    Box(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp)) {
                        Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally) {
                            Text(label(lang,tab),fontSize=27.sp,fontWeight=FontWeight.Bold)
                            Text(if(tab=="documents")appText(lang,"الوثائق الإدارية وطلباتها ومتابعتها","Demandes et suivi des documents","Request and track school documents")else appText(lang,"إدارة بياناتك وإعدادات التطبيق","Profil et paramètres","Manage your profile and settings"),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if(tab=="account")Box(Modifier.align(Alignment.CenterStart)){AppPreferencesButton(lang,langChange)}
                    }
                } else Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Surface(shape=androidx.compose.foundation.shape.CircleShape,border=BorderStroke(1.dp,FormalBlue),color=MaterialTheme.colorScheme.primaryContainer) {
                        Image(painterResource(if(dark)R.drawable.logo_dark else R.drawable.logo_light),null,Modifier.padding(5.dp).size(44.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(appText(lang,"ثانوية عبد الله كنون","Lycée Abdallah Guennoun","Abdallah Guennoun School"),style=MaterialTheme.typography.titleMedium)
                        Text(appText(lang,"القليعة - فضاء التلميذ","Lqliaa · Espace élève","Lqliaa · Student space"),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    FilledTonalIconButton(onClick={tab="news";notifications=false}) {Box(Modifier.semantics {contentDescription=appText(lang,"البحث في الأخبار","Rechercher","Search news")}){PortalIcon("search")}}
                    FilledTonalIconButton(onClick={notifications=true}) {Box(Modifier.semantics {contentDescription=label(lang,"notifications")}){PortalIcon("bell")}}
                }
                if(notifications) NotificationCenter(lang)
                else when(tab) {
                    "guidance" -> ArticleFeed("guidance", lang)
                    "home" -> {
                        ArticleFeed("news",lang,onViewAll={tab="news"},header={
                            CampusDashboardHeader(lang,dark) { id ->
                                tab=id
                            }
                        })
                    }
                    "news" -> ArticleFeed("news",lang)
                    "timetable" -> TimetableFeed(lang)

                    "documents" -> DocumentRequestsScreen(lang)
                    "account" -> ReferenceAccount(lang,{tab="documents"},{accountPanel="settings"},{accountPanel="profile"},onSignOutClick,{massar=true})
                    else -> PortalNotice(label(lang,"soon"))
                }
            }
            }
        }
    }
    }
}

@Composable
private fun PortalNotice(text: String) {
    Surface(Modifier.fillMaxWidth().padding(20.dp), shape=RoundedCornerShape(16.dp), color=MaterialTheme.colorScheme.surface) {
        Text(text,Modifier.padding(24.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ArticleFeed(collection: String, lang: String, onViewAll:(()->Unit)?=null, header: (@Composable () -> Unit)? = null) {
    // No sample announcements are presented as real school news.
    key(collection) {
        var search by rememberSaveable { mutableStateOf("") }
        var category by rememberSaveable { mutableStateOf("all") }
        var articles by remember { mutableStateOf<List<PortalArticle>>(emptyList()) }
        var cursor by remember { mutableStateOf<DocumentSnapshot?>(null) }
        var hasMore by remember { mutableStateOf(false) }
        var loading by remember { mutableStateOf(false) }
        var failed by remember { mutableStateOf(false) }
        var selected by remember { mutableStateOf<PortalArticle?>(null) }
        val scope = rememberCoroutineScope()
        suspend fun fetch(reset: Boolean) {
            if(loading)return
            loading=true; failed=false
            try {
                var query = FirebaseFirestore.getInstance().collection(collection)
                    .whereEqualTo("status", "published").orderBy("publishedAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(30)
                if(!reset) cursor?.let { query=query.startAfter(it) }
                val result=query.get(Source.SERVER).await()
                val page=result.documents.mapNotNull { d -> d.data?.let { PortalArticle(d.id,it) } }
                articles=if(reset) page else (articles+page).distinctBy { it.id }
                cursor=result.documents.lastOrNull(); hasMore=result.size()==30
            } catch(e: CancellationException) { throw e } catch(_: Exception) { failed=true } finally { loading=false }
        }
        LaunchedEffect(collection) { fetch(true) }
        val visible=remember(articles,category,search,lang) { articles.filter { article ->
            (category=="all" || article.category==category) &&
                listOf(article.text("title",lang),article.text("summary",lang),article.text("body",lang))
                    .any { it.contains(search.trim(),ignoreCase=true) }
        }
        }
        LazyColumn(Modifier.fillMaxSize(), contentPadding=PaddingValues(20.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
            if(header!=null) item(key="feed-header") {header()}
            if(header!=null && articles.isNotEmpty())item(key="featured-announcement") {
                val article=articles.first()
                CampusPanel(Modifier.fillMaxWidth(),FormalSilver,onClick={selected=article}) {
                    Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        Text(if(lang=="ar")"‹"else "›",fontSize=28.sp,color=FormalBlue)
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                            Text(article.text("title",lang),fontWeight=FontWeight.Bold,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(article.text("summary",lang),fontSize=12.sp,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ReferenceSymbol("news",Modifier.size(45.dp),FormalSilver)
                    }
                }
            }
            if(collection=="news" && header==null) item {
                OutlinedTextField(search,{search=it},modifier=Modifier.fillMaxWidth(),singleLine=true,
                    label={Text(appText(lang,"ابحث في الأخبار","Rechercher une actualité","Search news"))},shape=RoundedCornerShape(20.dp))
            }
            if(collection=="guidance") {
                item { Text(label(lang,"future"),style=MaterialTheme.typography.headlineMedium) }
                item { Text(label(lang,"intro"),color=MaterialTheme.colorScheme.onSurfaceVariant) }
                item { OutlinedTextField(search,{search=it},modifier=Modifier.fillMaxWidth(),singleLine=true,
                    label={Text(label(lang,"search"))},shape=RoundedCornerShape(16.dp)) }
                item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("all","schools","exams","scholarships").forEach { id ->
                        FilterChip(category==id,{category=id},{Text(label(lang,id))},shape=RoundedCornerShape(50))
                    }
                } }
            }
            item {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(label(lang,if(collection=="guidance") "latest" else "news"),Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
                    if(onViewAll!=null)TextButton(onClick=onViewAll){Text(appText(lang,"عرض الكل","Tout voir","View all"))}
                    TextButton(onClick={scope.launch {fetch(true)}},enabled=!loading) {Text(label(lang,"refresh"))}
                }
            }
            if(failed) item {
                Text(label(lang,"error"),color=MaterialTheme.colorScheme.error)
                TextButton(onClick={scope.launch { fetch(articles.isEmpty()) }},enabled=!loading) {Text(label(lang,"retry"))}
            }
            if(!loading && !failed && visible.isEmpty()) item {
                Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp),color=MaterialTheme.colorScheme.surfaceVariant) {
                    Text(label(lang,if(articles.isEmpty()) "empty" else "noResults"),Modifier.padding(24.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if(header!=null && visible.isNotEmpty()) item(key="latest-news-strip") {
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    visible.take(6).forEachIndexed { index,article ->
                        CampusReveal(index.coerceAtMost(3)) {
                            CampusPanel(Modifier.width(136.dp).height(125.dp),onClick={selected=article}) {
                                Column(Modifier.padding(10.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                    if(article.cover().isNotBlank()) MediaImage(article.cover(),Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)),lang=lang)
                                    else ReferenceSymbol("news",Modifier.size(30.dp))
                                    Text(article.text("title",lang),fontWeight=FontWeight.Bold,fontSize=12.sp,lineHeight=18.sp,maxLines=2,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    (article.fields["publishedAt"] as? com.google.firebase.Timestamp)?.let {
                                        Text(java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT,java.util.Locale.forLanguageTag(lang)).format(it.toDate()),fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            items(if(header==null)visible else emptyList(),key={it.id}) { article ->
                CampusReveal {
                CampusPanel(Modifier.fillMaxWidth(),if(collection=="guidance")Color(0xFFAD72FF)else CampusTeal,onClick={selected=article}) {
                Column {
                    if(article.cover().isNotBlank()) MediaImage(article.cover(), Modifier.fillMaxWidth().height(190.dp),lang=lang)
                    Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        if(collection=="guidance") Text(label(lang,article.category.takeIf { it in listOf("schools","exams","scholarships") } ?: "schools"),color=MaterialTheme.colorScheme.primary)
                        Text(article.text("title",lang),style=MaterialTheme.typography.titleLarge,maxLines=3,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(article.text("summary",lang),color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=3,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Text(label(lang,"more")+if(lang=="ar")" ←"else " →",color=MaterialTheme.colorScheme.primary)
                    }
                }
                }
                }
            }
            if(loading) item { CircularProgressIndicator() }
            if(hasMore && !loading && header==null) item { OutlinedButton(onClick={scope.launch{fetch(false)}},modifier=Modifier.fillMaxWidth()) {Text(label(lang,"loadMore"))} }
        }
        selected?.let { article -> ArticleDetails(article,lang) {selected=null} }
    }
}

@Composable
private fun ArticleDetails(article: PortalArticle, lang: String, close: () -> Unit) {
    var gallery by remember { mutableStateOf(false) }
    var sourceOpen by remember {mutableStateOf(false)}
    val context=LocalContext.current
    val source=article.text("sourceUrl",lang)
    val uri=remember(source) { runCatching { Uri.parse(source) }.getOrNull() }
    val safe=uri?.scheme=="https" && !uri.host.isNullOrBlank() && uri.userInfo==null
    if(sourceOpen && safe) {
        androidx.compose.ui.window.Dialog(onDismissRequest={sourceOpen=false},properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false,decorFitsSystemWindows=false)) {
            Surface(Modifier.fillMaxSize()) {Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                SchoolWebScreen(SchoolWebsite.TELMIDTICE,lang,androidx.compose.foundation.isSystemInDarkTheme(),initialUrl=source){sourceOpen=false}
            }}
        }
        return
    }
    if(gallery) {
        AlbumViewer(article.images(),lang) {gallery=false}
        return
    }
    androidx.compose.ui.window.Dialog(onDismissRequest=close,
        properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false, decorFitsSystemWindows=false)) {
        Surface(Modifier.fillMaxSize(), color=MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment=Alignment.CenterVertically) {
                    TextButton(onClick=close) { Text(label(lang,"close")) }
                    Text(label(lang,"more"), fontWeight=FontWeight.Bold, modifier=Modifier.weight(1f))
                }
                LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(bottom=32.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
                    if(article.images().isNotEmpty()) item {
                        Column {
                            MediaImage(article.images().first(),Modifier.fillMaxWidth().height(260.dp).clickable {gallery=true},true,lang)
                            TextButton(onClick={gallery=true},modifier=Modifier.padding(horizontal=12.dp)) {
                                Text(mediaLabel(lang,"album")+" (${article.images().size})")
                            }
                        }
                    }
                    if(article.images().isNotEmpty()) item {GallerySaveButton(article.images().first(),lang)}
                    item { Text(article.text("title",lang),Modifier.padding(horizontal=24.dp),fontSize=28.sp,lineHeight=38.sp,fontWeight=FontWeight.Bold) }
                    (article.fields["publishedAt"] as? com.google.firebase.Timestamp)?.let { timestamp ->
                        item { Text(java.text.DateFormat.getDateInstance(java.text.DateFormat.LONG,java.util.Locale.forLanguageTag(lang)).format(timestamp.toDate()),Modifier.padding(horizontal=24.dp),color=MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    item { androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(article.text("body",lang).ifBlank {article.text("summary",lang)},Modifier.padding(horizontal=24.dp),fontSize=18.sp,lineHeight=31.sp)
                    } }
                    val deadline=article.text("deadline",lang)
                    if(deadline.isNotBlank()) item { Text(label(lang,"deadline")+": "+deadline,Modifier.padding(horizontal=24.dp)) }
                    if(safe) item { OutlinedButton(onClick={
                        sourceOpen=true
                    },modifier=Modifier.padding(horizontal=24.dp).fillMaxWidth()) {Text(label(lang,"source"))} }
                }
            }
        }
    }
}

@Composable
internal fun PortalIcon(id: String) {
    val color=LocalContentColor.current
    Canvas(Modifier.size(24.dp)) {
        val w=size.width; val h=size.height
        val stroke=androidx.compose.ui.graphics.drawscope.Stroke(width=2.dp.toPx())
        fun path(vararg points: Pair<Float,Float>, closed: Boolean=false) {
            val p=androidx.compose.ui.graphics.Path()
            points.forEachIndexed { i, v -> if(i==0) p.moveTo(w*v.first,h*v.second) else p.lineTo(w*v.first,h*v.second) }
            if(closed) p.close()
            drawPath(p,color,style=stroke)
        }
        when(id) {
            "search" -> {drawCircle(color,radius=w*.29f,center=androidx.compose.ui.geometry.Offset(w*.4f,h*.4f),style=stroke);path(.62f to .62f,.92f to .92f)}
            "number" -> {path(.35f to .1f,.25f to .9f);path(.7f to .1f,.6f to .9f);path(.1f to .35f,.9f to .35f);path(.06f to .65f,.85f to .65f)}
            "settings" -> {drawCircle(color,radius=w*.3f,style=stroke);drawCircle(color,radius=w*.1f,style=stroke);path(.5f to 0f,.5f to .18f);path(.5f to .82f,.5f to 1f);path(0f to .5f,.18f to .5f);path(.82f to .5f,1f to .5f)}
            "help" -> {drawCircle(color,radius=w*.43f,style=stroke);path(.3f to .3f,.45f to .22f,.65f to .3f,.65f to .43f,.5f to .53f,.5f to .6f);drawCircle(color,w*.035f,androidx.compose.ui.geometry.Offset(w*.5f,h*.76f))}
            "logout" -> {path(.45f to .1f,.15f to .1f,.15f to .9f,.45f to .9f);path(.4f to .5f,.9f to .5f);path(.7f to .3f,.9f to .5f,.7f to .7f)}
            "news" -> {path(.15f to .1f,.85f to .1f,.85f to .9f,.15f to .9f,closed=true);path(.3f to .3f,.7f to .3f);path(.3f to .5f,.7f to .5f);path(.3f to .7f,.55f to .7f)}
            "timetable" -> {path(.12f to .22f,.88f to .22f,.88f to .9f,.12f to .9f,closed=true);path(.12f to .4f,.88f to .4f);path(.3f to .08f,.3f to .3f);path(.7f to .08f,.7f to .3f);drawCircle(color,w*.06f,androidx.compose.ui.geometry.Offset(w*.35f,h*.6f));drawCircle(color,w*.06f,androidx.compose.ui.geometry.Offset(w*.65f,h*.6f))}
            "bell" -> {path(.18f to .72f,.27f to .6f,.27f to .32f,.36f to .18f,.64f to .18f,.73f to .32f,.73f to .6f,.82f to .72f,closed=true);drawCircle(color,radius=w*.08f,center=androidx.compose.ui.geometry.Offset(w*.5f,h*.86f))}
            "home" -> {path(.08f to .45f,.5f to .08f,.92f to .45f);path(.2f to .38f,.2f to .9f,.8f to .9f,.8f to .38f)}
            "guidance" -> {drawCircle(color,radius=w*.43f,style=stroke);path(.65f to .28f,.58f to .58f,.3f to .72f,.4f to .42f,closed=true)}
            "lessons" -> {path(.5f to .9f,.08f to .78f,.08f to .12f,.5f to .24f,.92f to .12f,.92f to .78f,.5f to .9f);path(.5f to .24f,.5f to .9f)}
            "documents" -> {path(.22f to .08f,.62f to .08f,.82f to .28f,.82f to .92f,.22f to .92f,closed=true);path(.62f to .08f,.62f to .28f,.82f to .28f);path(.34f to .48f,.7f to .48f);path(.34f to .64f,.7f to .64f);path(.34f to .8f,.58f to .8f)}
            else -> {drawCircle(color,radius=w*.2f,center=androidx.compose.ui.geometry.Offset(w*.5f,h*.26f),style=stroke);path(.14f to .9f,.14f to .75f,.3f to .59f,.7f to .59f,.86f to .75f,.86f to .9f)}
        }
    }
}

@Composable
private fun NotificationCenter(lang: String) {
    var section by rememberSaveable { mutableStateOf("news") }
    Column(Modifier.fillMaxSize()) {
        Text(when(lang){"fr"->"Les dernières publications de votre établissement.";"en"->"The latest publications from your school.";else->"آخر المنشورات من المؤسسة والتوجيه."},
            Modifier.padding(horizontal=20.dp,vertical=8.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(Modifier.padding(horizontal=20.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(section=="news",{section="news"},{Text(label(lang,"news"))})
            FilterChip(section=="guidance",{section="guidance"},{Text(label(lang,"guidance"))})
        }
        ArticleFeed(section,lang)
    }
}

@Composable
private fun StudentProfileCard(lang: String, email: String?) {
    var profile by remember { mutableStateOf<Map<String,Any>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    val uid=AuthManager.auth.currentUser?.uid
    DisposableEffect(uid,retry) {
        loading=true; failed=false
        val listener=uid?.let {
            AuthManager.db.collection("users").document(it).addSnapshotListener { snapshot,error ->
                loading=false
                failed=error!=null
                if(error==null) profile=snapshot?.data.orEmpty()
            }
        }
        if(uid==null) { loading=false;failed=true }
        onDispose { listener?.remove() }
    }
    fun tr(ar:String,fr:String,en:String)=when(lang){"fr"->fr;"en"->en;else->ar}
    val name=profile["fullName"] as? String ?: ""
    val initials=name.trim().split(Regex("\\s+")).filter{it.isNotEmpty()}.let { words ->
        if(words.size>1) "${words.first().first()}${words.last().first()}" else words.firstOrNull()?.take(1).orEmpty()
    }
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
            AccountPhoto(lang, initials)
            if(loading) CircularProgressIndicator()
            if(failed) {
                Text(tr("تعذر تحميل الملف الشخصي.","Impossible de charger le profil.","Unable to load your profile."),color=MaterialTheme.colorScheme.error)
                TextButton(onClick={retry++}){Text(label(lang,"retry"))}
            }
            Text(name, style=MaterialTheme.typography.headlineMedium,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            Text(email.orEmpty(),color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium.copy(textDirection=androidx.compose.ui.text.style.TextDirection.Ltr),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
            HorizontalDivider()
            val fields=listOf(
                tr("المستوى","Niveau","Level") to profile["levelId"],
                tr("القسم","Classe","Class") to profile["className"],
                tr("الرقم الترتيبي","Numéro d’ordre","Roll number") to profile["ordinalNumber"],
                tr("رقم مسار","Code Massar","Massar ID") to profile["massarId"],
                tr("الموسم الدراسي","Année scolaire","Academic year") to profile["academicYear"]
            )
            fields.forEach { (title,value) ->
                Column(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=10.dp)) {
                    Text(title,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value?.toString()?.takeIf{it.isNotBlank()} ?: "—",fontSize=18.sp,fontWeight=FontWeight.Medium,style=MaterialTheme.typography.bodyLarge.copy(textDirection=androidx.compose.ui.text.style.TextDirection.Ltr))
                }
            }
            Text(tr("لتصحيح المعلومات الدراسية، تواصل مع الإدارة.","Contactez l’administration pour corriger vos informations scolaires.","Contact the administration to correct your school details."),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Spacer(Modifier.height(20.dp))
}

@Composable
private fun CampusShortcut(title:String,subtitle:String,icon:String,modifier:Modifier,onClick:()->Unit) {
    Surface(onClick=onClick,modifier=modifier,shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.secondaryContainer) {
                Box(Modifier.padding(12.dp)){PortalIcon(icon)}
            }
            Text(title,style=MaterialTheme.typography.titleMedium)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
