package com.otmanelabouze.abdallahguennoun

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

internal val CampusTeal = Color(0xFF87ABC9)

@Composable
internal fun CampusBackdrop(dark: Boolean) {
    val clock = rememberInfiniteTransition(label="campus waves")
    val phase by clock.animateFloat(0f,6.283185f,infiniteRepeatable(tween(16000,easing=LinearEasing)),label="wave phase")
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Brush.verticalGradient(if(dark) listOf(Color(0xFF071420),Color(0xFF12283D),Color(0xFF091A2A)) else listOf(Color(0xFFEDF2F8),Color(0xFFF6FCFF),Color(0xFFDCE6F1))))
        for(i in 0..3) {
            val y=size.height*(.12f+i*.27f)
            val drift=sin(phase+i)*size.width*.13f
            val p=Path().apply {
                moveTo(-size.width*.2f,y)
                cubicTo(size.width*.25f+drift,y-size.height*.16f,size.width*.7f-drift,y+size.height*.17f,size.width*1.2f,y-size.height*.05f)
            }
            drawPath(p,CampusTeal.copy(alpha=if(dark).045f else .07f),style=Stroke(42.dp.toPx()))
            drawPath(p,CampusTeal.copy(alpha=if(dark).2f else .23f),style=Stroke(1.dp.toPx()))
        }
        drawCircle(Brush.radialGradient(listOf(CampusTeal.copy(alpha=.12f),Color.Transparent),center=Offset(size.width*(.6f+.15f*sin(phase)),size.height*.2f),radius=size.width*.8f),radius=size.width*.8f,center=Offset(size.width*(.6f+.15f*sin(phase)),size.height*.2f))
    }
}

@Composable
internal fun CampusReveal(order:Int=0,content:@Composable ()->Unit) {
    val progress=remember {Animatable(0f)}
    LaunchedEffect(Unit) {delay(order*75L);progress.animateTo(1f,tween(650,easing=FastOutSlowInEasing))}
    Box(Modifier.graphicsLayer {alpha=progress.value;translationY=(1-progress.value)*65f;scaleX=.94f+.06f*progress.value;scaleY=scaleX}) {content()}
}

@Composable
internal fun CampusPanel(modifier:Modifier=Modifier,accent:Color=CampusTeal,onClick:(()->Unit)?=null,content:@Composable BoxScope.()->Unit) {
    val dark=MaterialTheme.colorScheme.background.luminance()<.3f
    val interaction=remember {MutableInteractionSource()}
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if(pressed).96f else 1f,spring(dampingRatio=.6f,stiffness=380f),label="card press")
    CompositionLocalProvider(LocalContentColor provides if(dark)Color.White else MaterialTheme.colorScheme.onSurface) {
    Box(modifier.graphicsLayer {scaleX=scale;scaleY=scale}.clip(RoundedCornerShape(22.dp))
        .background(Brush.linearGradient(listOf(accent.copy(alpha=if(dark).23f else .16f),if(dark)Color(0xFF091D2A) else Color.White)))
        .border(1.dp,Brush.linearGradient(listOf(accent.copy(alpha=.85f),accent.copy(alpha=.12f),accent.copy(alpha=.48f))),RoundedCornerShape(22.dp))
        .then(if(onClick!=null)Modifier.clickable(interactionSource=interaction,indication=null,onClick=onClick)else Modifier),content=content)
    }
}

@Composable
internal fun CampusDashboardHeader(lang:String,dark:Boolean,navigate:(String)->Unit) {
    val pages=rememberPagerState(pageCount={4})
    LaunchedEffect(pages) {
        while(true) {delay(6500);if(!pages.isScrollInProgress)pages.animateScrollToPage((pages.currentPage+1)%4,animationSpec=tween(850))}
    }
    CampusReveal {
        CampusPanel(Modifier.fillMaxWidth()) {
            SchoolPhotograph(Modifier.matchParentSize(),.35f)
            Column(Modifier.fillMaxWidth().padding(22.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(appText(lang,"مرحباً بك 👋","Bienvenue 👋","Welcome 👋"),fontSize=30.sp,fontWeight=FontWeight.Bold,color=Color.White)
                Text(appText(lang,"في فضائك المدرسي الرقمي","Votre espace scolaire numérique","Your digital school space"),fontSize=18.sp,color=Color(0xFFE0E8EF))
                Surface(color=Color(0xFF132D45).copy(alpha=.8f),shape=RoundedCornerShape(50),border=BorderStroke(1.dp,FormalBlue.copy(alpha=.5f))) {
                    HorizontalPager(pages,Modifier.fillMaxWidth()) {page->
                        val quote=when(page) {
                            1->appText(lang,"اجعل اليوم خطوة نحو مستقبل أفضل","Un pas vers un meilleur avenir","A step towards a brighter future")
                            2->appText(lang,"تابع أخبار مؤسستك وكن على اطلاع","Suivez les actualités de votre lycée","Keep up with your school news")
                            3->appText(lang,"خدماتك المدرسية، في متناولك","Vos services scolaires à portée de main","Your school services, close at hand")
                            else->appText(lang,"اكتشف، تعلم، شارك، وكن جزءاً من مجتمع متميز","Découvrez, apprenez et partagez","Discover, learn and share")
                        }
                        Text("❝  $quote  ❞",Modifier.fillMaxWidth().padding(horizontal=14.dp,vertical=10.dp),fontSize=12.sp,color=Color.White,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                Row(Modifier.align(Alignment.CenterHorizontally),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    repeat(4){i->Box(Modifier.size(width=if(i==pages.currentPage)18.dp else 5.dp,height=5.dp).clip(CircleShape).background(if(i==pages.currentPage)FormalBlue else Color.White.copy(alpha=.3f)))}
                }
            }
        }
    }
    Spacer(Modifier.height(16.dp))
    val ids=listOf("news","lessons","documents","timetable","account","guidance")
    val colors=listOf(CampusTeal,Color(0xFF7395BA),Color(0xFF8C98B1),Color(0xFFB3A58C),CampusTeal,Color(0xFFA695A3))
    val subtitles=listOf(
        appText(lang,"آخر المستجدات والإعلانات","Actualités et annonces","News and announcements"),
        appText(lang,"المواد والموارد التعليمية","Ressources pédagogiques","Learning resources"),
        appText(lang,"الطلبات والتتبع والإشعارات","Demandes et suivi","Requests and tracking"),
        appText(lang,"الجداول والتغييرات","Horaires et changements","Schedules and updates"),
        appText(lang,"بياناتك وإعدادات التطبيق","Profil et préférences","Profile and preferences"),
        appText(lang,"الشعب والمسارات","Filières et parcours","Explore your pathways"))
    for(row in 0..2) {
        CampusReveal(row+1) {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                for(i in row*2..row*2+1) {
                    CampusPanel(Modifier.weight(1f).fillMaxHeight(),colors[i],{navigate(ids[i])}) {
                        val art=when(ids[i]) {"lessons"->R.drawable.reference_books;"guidance"->R.drawable.reference_compass;"timetable"->R.drawable.reference_calendar;"account"->R.drawable.reference_profile;else->R.drawable.reference_paper}
                        Image(painterResource(art),null,Modifier.align(AbsoluteAlignment.CenterRight).size(115.dp).graphicsLayer {alpha=.4f;rotationZ=-8f})
                        Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(5.dp),horizontalAlignment=AbsoluteAlignment.Left) {
                            ReferenceSymbol(ids[i],Modifier.size(39.dp),colors[i])
                            Text(when(ids[i]){"documents"->appText(lang,"طلب الوثائق","Documents","Documents");"guidance"->appText(lang,"التوجيه المدرسي","Orientation scolaire","School guidance");else->label(lang,ids[i])},fontWeight=FontWeight.Bold,fontSize=17.sp,color=if(dark)Color.White else MaterialTheme.colorScheme.onSurface)
                            Text(subtitles[i],fontSize=10.sp,lineHeight=16.sp,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                        Text("›",Modifier.align(AbsoluteAlignment.CenterRight).padding(end=8.dp).offset(y=12.dp).clip(CircleShape).background(colors[i].copy(alpha=.22f)).border(1.dp,colors[i],CircleShape).padding(horizontal=8.dp,vertical=0.dp),color=colors[i],fontSize=23.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
internal fun CampusNavigation(tab:String,lang:String,onSelect:(String)->Unit) {
    val clock=rememberInfiniteTransition(label="navigation halo")
    val glow by clock.animateFloat(.18f,.38f,infiniteRepeatable(tween(2200),RepeatMode.Reverse),label="navigation breathing")
    // Physical order matches the reference in every language; labels remain localized.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal=10.dp,vertical=5.dp).height(112.dp)) {
            Canvas(Modifier.matchParentSize()) {
                val w=size.width;val h=size.height;val top=h*.25f;val bottom=h-3.dp.toPx()
                val path=Path().apply {
                    moveTo(h*.38f,top)
                    lineTo(w*.32f,top)
                    cubicTo(w*.40f,top,w*.405f,h*.025f,w*.5f,h*.025f)
                    cubicTo(w*.595f,h*.025f,w*.60f,top,w*.68f,top)
                    lineTo(w-h*.38f,top)
                    cubicTo(w,top,w,bottom,w-h*.38f,bottom)
                    lineTo(h*.38f,bottom)
                    cubicTo(0f,bottom,0f,top,h*.38f,top)
                    close()
                }
                val rim=Brush.horizontalGradient(listOf(Color(0xFF7085B6),Color(0xFF6C9CBF),Color(0xFF92C8DD),Color(0xFFB4A88D),Color(0xFFA496B8)))
                drawPath(path,rim,alpha=glow,style=Stroke(7.dp.toPx()))
                drawPath(path,Brush.verticalGradient(listOf(Color(0xFF1F3D53),Color(0xFF091D2D),Color(0xFF11283E))))
                drawPath(path,rim,style=Stroke(1.3.dp.toPx()))
            }
            Row(Modifier.fillMaxSize().padding(horizontal=8.dp),verticalAlignment=Alignment.Bottom) {
                listOf("guidance","lessons","home","documents","account").forEach {id->
                    val selected=tab==id || (id=="home" && tab in listOf("news","timetable"))
                    val accent=when(id) {
                        "guidance"->Color(0xFFA9ACD5)
                        "lessons"->Color(0xFF8EBDE1)
                        "documents"->Color(0xFFD6C398)
                        "account"->Color(0xFFC2ABBF)
                        else->Color(0xFFB3DFED)
                    }
                    val scale by animateFloatAsState(if(selected)1.07f else 1f,spring(dampingRatio=.65f),label="selected tab scale")
                    Column(Modifier.weight(1f).height(if(id=="home")108.dp else 85.dp)
                        .clip(RoundedCornerShape(28.dp)).selectable(selected=selected,role=Role.Tab,onClick={onSelect(id)})
                        .padding(bottom=9.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Bottom) {
                        Box(Modifier.size(if(id=="home")64.dp else 40.dp).graphicsLayer {scaleX=scale;scaleY=scale}
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(accent.copy(alpha=if(selected).65f else .28f),Color(0xFF0B2235))))
                            .border(if(id=="home"||selected)1.5.dp else .7.dp,accent.copy(alpha=if(selected).95f else .35f),CircleShape),contentAlignment=Alignment.Center) {
                            Box(Modifier.size(if(id=="home")48.dp else 31.dp).clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Color.White.copy(alpha=if(selected).16f else .04f),Color.Transparent))),contentAlignment=Alignment.Center) {
                                if(id=="home") Canvas(Modifier.size(28.dp)) {
                                    val w=size.width;val h=size.height
                                    val house=Path().apply {moveTo(w*.07f,h*.45f);lineTo(w*.5f,h*.07f);lineTo(w*.93f,h*.45f);lineTo(w*.78f,h*.45f);lineTo(w*.78f,h*.94f);lineTo(w*.59f,h*.94f);lineTo(w*.59f,h*.65f);lineTo(w*.41f,h*.65f);lineTo(w*.41f,h*.94f);lineTo(w*.22f,h*.94f);lineTo(w*.22f,h*.45f);close()}
                                    drawPath(house,if(selected)Color.White else accent)
                                } else CompositionLocalProvider(LocalContentColor provides if(selected)Color.White else accent){PortalIcon(id)}
                            }
                        }
                        Spacer(Modifier.height(5.dp))
                        Text(label(lang,id),fontSize=11.sp,lineHeight=16.sp,fontWeight=if(selected)FontWeight.Bold else FontWeight.Medium,color=Color.White,maxLines=1)
                        Spacer(Modifier.height(3.dp))
                        Box(Modifier.width(19.dp).height(3.dp).clip(CircleShape).background(if(selected)accent else Color.Transparent))
                    }
                }
            }
        }
    }
}
