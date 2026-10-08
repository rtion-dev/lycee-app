package com.otmanelabouze.abdallahguennoun

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val FormalBlue=Color(0xFF87ABC9)
internal val FormalSilver=Color(0xFFB8C4D3)

/** Shared glossy medallions and background illustrations, sized independently of screen density. */
@Composable
internal fun ReferenceSymbol(id:String,modifier:Modifier=Modifier,accent:Color=FormalBlue) {
    Box(modifier.clip(RoundedCornerShape(26)).background(Brush.linearGradient(listOf(accent.copy(alpha=.58f),Color(0xFF153550),accent.copy(alpha=.12f)))).border(1.dp,accent.copy(alpha=.35f),RoundedCornerShape(26)),contentAlignment=Alignment.Center) {
        Box(Modifier.graphicsLayer {scaleX=1.4f;scaleY=1.4f}) {CompositionLocalProvider(LocalContentColor provides accent){PortalIcon(id)}}
    }
}

@Composable
internal fun SchoolPhotograph(modifier:Modifier=Modifier,dim:Float=.6f) {
    Box(modifier) {
        Image(painterResource(R.drawable.school_reference),null,Modifier.matchParentSize(),contentScale=ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(Color(0xFF061421).copy(alpha=dim),Color(0xFF061421).copy(alpha=(dim+.22f).coerceAtMost(.95f))))))
    }
}

@Composable
internal fun ReferenceLogin(dark:Boolean,lang:String,onLanguage:(String)->Unit,onSignIn:()->Unit,loading:Boolean,error:String?,onPublic:()->Unit) {
    var terms by remember {mutableStateOf(false)}
    val motion=rememberInfiniteTransition(label="logo orbit")
    val orbit by motion.animateFloat(0f,360f,infiniteRepeatable(tween(26000,easing=LinearEasing)),label="orbit angle")
    val breathe by motion.animateFloat(.97f,1.02f,infiniteRepeatable(tween(2700,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="logo breathing")
    Box(Modifier.fillMaxSize()) {
        CampusBackdrop(dark)
        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal=20.dp),horizontalAlignment=Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().padding(top=12.dp,bottom=24.dp).height(44.dp)) {
                Box(Modifier.align(androidx.compose.ui.AbsoluteAlignment.CenterRight)) {
                    AppPreferencesButton(lang,onLanguage,rounded=true)
                }
            }
            Box(Modifier.fillMaxWidth().height(265.dp),contentAlignment=Alignment.Center) {
                SchoolPhotograph(Modifier.fillMaxWidth().height(230.dp).clip(RoundedCornerShape(70.dp)),.7f)
                Canvas(Modifier.size(258.dp).graphicsLayer {rotationZ=orbit}) {
                    drawCircle(Brush.radialGradient(listOf(FormalBlue.copy(alpha=.05f),FormalBlue.copy(alpha=.3f),Color.Transparent)))
                    drawCircle(FormalBlue.copy(alpha=.25f),radius=size.minDimension*.46f,style=Stroke(1.dp.toPx()))
                    for(i in 0..3)drawArc(if(i%2==0)FormalSilver else FormalBlue,i*90f+8f,58f,false,style=Stroke(2.dp.toPx(),cap=StrokeCap.Round))
                }
                Box(Modifier.size(194.dp).graphicsLayer {scaleX=breathe;scaleY=breathe}.clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFF7897B4),Color(0xFF17354F),Color(0xFF071827)))).border(1.dp,FormalBlue,CircleShape),contentAlignment=Alignment.Center) {
                    Image(painterResource(if(dark)R.drawable.logo_dark else R.drawable.logo_light),appText(lang,"شعار المؤسسة","Logo du lycée","School logo"),Modifier.size(162.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(shape=CircleShape,color=MaterialTheme.colorScheme.primaryContainer.copy(alpha=.55f),border=BorderStroke(1.dp,FormalBlue.copy(alpha=.4f))) {Text(appText(lang,"مرحباً بك في","Bienvenue au","Welcome to"),Modifier.padding(horizontal=18.dp,vertical=5.dp),fontSize=17.sp)}
            Text(appText(lang,"ثانوية عبد الله كنون","Lycée Abdallah Guennoun","Abdallah Guennoun School"),Modifier.padding(top=10.dp),fontSize=28.sp,lineHeight=39.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                HorizontalDivider(Modifier.width(64.dp),color=FormalBlue.copy(alpha=.5f))
                Text(appText(lang,"القليعة","Lqliaa","Lqliaa"),fontSize=25.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
                HorizontalDivider(Modifier.width(64.dp),color=FormalBlue.copy(alpha=.5f))
            }
            Text(appText(lang,"فضاؤك المدرسي، أقرب إليك","Votre lycée, plus proche de vous","Your school, closer to you"),Modifier.padding(vertical=14.dp),fontSize=17.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center)
            Row(Modifier.fillMaxWidth().padding(vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(9.dp)) {
                listOf("news","lessons","guidance","timetable").forEach {id->
                    CampusPanel(Modifier.weight(1f),FormalBlue,if(id=="lessons")onPublic else onSignIn) {
                        Column(Modifier.fillMaxWidth().padding(horizontal=3.dp,vertical=13.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(9.dp)) {
                            ReferenceSymbol(id,Modifier.size(35.dp));Text(label(lang,id),fontSize=10.sp,textAlign=TextAlign.Center,lineHeight=15.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Button(onClick=onSignIn,enabled=!loading,shape=CircleShape,modifier=Modifier.fillMaxWidth().height(62.dp),border=BorderStroke(1.dp,FormalSilver),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF355F83),contentColor=Color.White)) {
                Text(if(lang=="ar")"←"else "→",fontSize=25.sp)
                Text(AppTranslations.getString(lang,"google_btn"),Modifier.weight(1f),fontSize=17.sp,textAlign=TextAlign.Center)
                if(loading)CircularProgressIndicator(Modifier.size(25.dp),strokeWidth=2.dp)else Image(painterResource(R.drawable.ic_google_logo),null,Modifier.size(29.dp))
            }
            if(!error.isNullOrBlank())Text(error,Modifier.padding(12.dp),color=MaterialTheme.colorScheme.error,textAlign=TextAlign.Center)
            Row(Modifier.fillMaxWidth().padding(top=22.dp,bottom=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                HorizontalDivider(Modifier.weight(1f));Text(appText(lang,"أو","ou","or"),fontSize=12.sp);HorizontalDivider(Modifier.weight(1f))
            }
            Text(appText(lang,"بتسجيل الدخول، أقرّ بالموافقة على سياسة الاستخدام.","En vous connectant, vous acceptez les conditions d’utilisation.","By signing in, you agree to the terms of use."),Modifier.clickable {terms=true}.padding(6.dp),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,textAlign=TextAlign.Center)
        }
        if(terms)AppInformation(lang,true){terms=false}
    }
}

@Composable
internal fun ReferenceDocumentLanding(lang:String,types:List<String>,empty:Boolean,onRequest:()->Unit,onExplore:()->Unit) {
    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)) {
        val motion=rememberInfiniteTransition(label="document float")
        val y by motion.animateFloat(-5f,5f,infiniteRepeatable(tween(2600,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="document levitation")
        Image(painterResource(R.drawable.document_reference),null,Modifier.fillMaxWidth().height(235.dp).graphicsLayer {translationY=y},contentScale=ContentScale.Fit)
        Text(appText(lang,if(empty)"لا توجد طلبات وثائق حالياً"else "طلباتك الإدارية",if(empty)"Aucune demande pour le moment"else "Vos demandes",if(empty)"No document requests yet"else "Your document requests"),fontSize=24.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
        Text(appText(lang,"يمكنك طلب وثيقة إدارية من المدرسة وتتبع حالة الطلب من هنا بسهولة.","Demandez un document et suivez son état ici.","Request a school document and track its progress here."),fontSize=15.sp,lineHeight=25.sp,textAlign=TextAlign.Center,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick=onRequest,shape=CircleShape,modifier=Modifier.fillMaxWidth(.86f).height(54.dp)) {PortalIcon("documents");Spacer(Modifier.width(12.dp));Text(appText(lang,"طلب وثيقة جديدة","Nouvelle demande","Request a document"))}
        OutlinedButton(onClick=onExplore,shape=CircleShape,modifier=Modifier.fillMaxWidth(.86f).height(50.dp)) {PortalIcon("lessons");Spacer(Modifier.width(12.dp));Text(appText(lang,"استكشاف الخدمة","Découvrir le service","Explore the service"))}
        CampusPanel(Modifier.fillMaxWidth(),FormalBlue) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(appText(lang,"الوثائق المتاحة من الإدارة","Documents disponibles","Available documents"),fontWeight=FontWeight.Bold)
                Text(appText(lang,"الاستلام من إدارة المؤسسة بعد تجهيز الطلب.","Retrait à l’administration après préparation.","Collect from the school office when ready."),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    types.forEach {name->CampusPanel(Modifier.width(115.dp),FormalSilver,onRequest) {
                        Column(Modifier.padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {ReferenceSymbol("documents",Modifier.size(38.dp));Text(name,fontSize=12.sp,textAlign=TextAlign.Center)}
                    }}
                }
                if(types.isEmpty())Text(appText(lang,"لم تفعّل الإدارة أنواع الوثائق بعد.","Aucun type activé pour le moment.","No document types enabled yet."),fontSize=12.sp)
            }
        }
    }
}

@Composable
internal fun ReferenceAccount(lang:String,onDocuments:()->Unit,onSettings:()->Unit,onProfile:()->Unit,onSignOut:()->Unit,onMassar:()->Unit) {
    var profile by remember {mutableStateOf<Map<String,Any>>(emptyMap())}
    var failed by remember {mutableStateOf(false)}
    var support by remember {mutableStateOf(false)}
    var retry by remember {mutableIntStateOf(0)}
    val uid=AuthManager.auth.currentUser?.uid
    DisposableEffect(uid,retry) {
        val listener=uid?.let {AuthManager.db.collection("users").document(it).addSnapshotListener {doc,error->failed=error!=null;if(error==null)profile=doc?.data.orEmpty()}}
        onDispose {listener?.remove()}
    }
    fun value(key:String)=profile[key]?.toString()?.takeIf {it.isNotBlank()} ?: "—"
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(13.dp)) {
        if(failed)TextButton(onClick={retry++}){Text(appText(lang,"تعذر تحميل البيانات — إعادة المحاولة","Chargement impossible — réessayer","Unable to load — retry"))}
        CampusReveal {
            CampusPanel(Modifier.fillMaxWidth()) {
                SchoolPhotograph(Modifier.matchParentSize(),.65f)
                Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.width(74.dp)) {
                        Image(painterResource(R.drawable.logo_dark),null,Modifier.size(66.dp))
                        Text(appText(lang,"ثانوية عبد الله كنون","Lycée Guennoun","Guennoun School"),fontSize=10.sp,textAlign=TextAlign.Center,color=Color.White)
                    }
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                        Text(value("fullName"),fontSize=21.sp,fontWeight=FontWeight.Bold,color=Color.White)
                        Text(appText(lang,"التلميذ رقم ","Élève n° ","Student # ")+value("ordinalNumber"),fontSize=13.sp,color=FormalSilver)
                        Text(appText(lang,"القسم: ","Classe : ","Class: ")+value("className"),fontSize=12.sp,color=FormalSilver)
                        Surface(shape=CircleShape,color=Color(0xFF213B53),border=BorderStroke(1.dp,FormalBlue)) {
                            Text(if(profile["status"]=="approved")appText(lang,"● حساب مفعّل","● Compte actif","● Active account")else appText(lang,"حالة الحساب: ","Statut : ","Status: ")+value("status"),Modifier.padding(8.dp),fontSize=10.sp,color=Color.White)
                        }
                    }
                    Box(Modifier.size(61.dp).clickable(onClick=onProfile),contentAlignment=Alignment.Center) {ReferenceSymbol("account",Modifier.fillMaxSize())}
                }
            }
        }
        CampusReveal(1) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(Triple("guidance",value("levelId"),appText(lang,"المستوى","Niveau","Level")),Triple("timetable",value("academicYear"),appText(lang,"الموسم الدراسي","Année","School year")),Triple("number",value("ordinalNumber"),appText(lang,"الرقم الترتيبي","Numéro","Number")),Triple("lessons",value("className"),appText(lang,"القسم","Classe","Class"))).forEach {(icon,value,title)->
                    CampusPanel(Modifier.weight(1f)) {Column(Modifier.fillMaxWidth().padding(horizontal=4.dp,vertical=13.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        ReferenceSymbol(icon,Modifier.size(34.dp));Text(value,fontSize=12.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center);Text(title,fontSize=9.sp,textAlign=TextAlign.Center)
                    }}
                }
            }
        }
        val actions=listOf(
            Triple("account",appText(lang,"معلوماتي الشخصية","Informations personnelles","Personal information"),appText(lang,"الاسم، قسمك، رقم مسار، وصورتك","Nom, classe, Massar et photo","Name, class, Massar ID and photo")),
            Triple("massar",appText(lang,"الدخول إلى فضاء مسار","Accéder à Massar","Open Massar"),appText(lang,"فضاؤك الخاص لتتبع النتائج الدراسية","Votre espace de suivi des résultats scolaires","Your personal school results space")),
            Triple("documents",appText(lang,"طلباتي للوثائق","Mes demandes","My document requests"),appText(lang,"تتبع حالة طلباتك السابقة والحالية","Suivre vos demandes","Track current and past requests")),
            Triple("settings",appText(lang,"إعدادات التطبيق","Paramètres de l’application","App settings"),appText(lang,"اللغة، المظهر، والإشعارات","Langue, thème et notifications","Language, theme and notifications")),
            Triple("help",appText(lang,"المساعدة والدعم","Aide et assistance","Help and support"),appText(lang,"مساعدة في استعمال التطبيق","Aide à l’utilisation","Get help using the app")),
            Triple("logout",appText(lang,"تغيير الحساب","Changer de compte","Change account"),appText(lang,"تسجيل خروج أو إضافة حساب آخر","Se déconnecter ou changer de compte","Sign out or use another account")))
        actions.forEachIndexed {i,(icon,title,subtitle)->CampusReveal(i+2) {
            CampusPanel(Modifier.fillMaxWidth(),if(icon=="logout")Color(0xFFB29199)else FormalBlue,onClick={when(icon){"account"->onProfile();"massar"->onMassar();"documents"->onDocuments();"settings"->onSettings();"help"->support=true;else->onSignOut()}}) {
                Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(13.dp)) {
                    if(icon=="massar") {
                        Box(Modifier.size(43.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF153550)).border(1.dp,FormalBlue,RoundedCornerShape(12.dp)),contentAlignment=Alignment.Center) {
                            Image(painterResource(R.drawable.massar_school),"مسار",Modifier.fillMaxSize().padding(2.dp),contentScale=ContentScale.Fit)
                        }
                    } else ReferenceSymbol(icon,Modifier.size(43.dp))
                    Column(Modifier.weight(1f)) {Text(title,fontWeight=FontWeight.Bold,fontSize=17.sp);Text(subtitle,fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)};Text(if(lang=="ar")"‹"else "›",fontSize=27.sp,color=FormalSilver)
                }
            }
        }}
        ExamCountdownCard(lang,profile["levelId"] as? String)
    }
    if(support)SupportDialog(lang){support=false}
}
