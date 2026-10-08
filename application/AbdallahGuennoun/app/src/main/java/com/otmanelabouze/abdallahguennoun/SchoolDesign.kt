package com.otmanelabouze.abdallahguennoun

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Typography
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// System fonts provide Arabic shaping offline, without a font download or extra bitmap assets.
internal val SchoolTypography = Typography(
    headlineLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=34.sp,lineHeight=46.sp),
    headlineMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Bold,fontSize=26.sp,lineHeight=36.sp),
    titleLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=21.sp,lineHeight=30.sp),
    titleMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.SemiBold,fontSize=17.sp,lineHeight=26.sp),
    bodyLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=16.sp,lineHeight=27.sp),
    bodyMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=14.sp,lineHeight=23.sp),
    bodySmall=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=12.sp,lineHeight=20.sp),
    labelLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Medium,fontSize=14.sp,lineHeight=22.sp),
    labelMedium=TextStyle(fontFamily=FontFamily.SansSerif,fontWeight=FontWeight.Medium,fontSize=12.sp,lineHeight=18.sp),
    labelSmall=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=11.sp,lineHeight=16.sp)
)

// One short opacity animation per destination, no outgoing duplicate screen/listeners.
// Compose's animation clock follows the system animator duration scale.
@Composable
internal fun ScreenEntrance(destination:String,content:@Composable ()->Unit) {
    val opacity=remember {Animatable(1f)}
    LaunchedEffect(destination) {opacity.snapTo(0f);opacity.animateTo(1f,tween(240))}
    Box(Modifier.fillMaxSize().graphicsLayer {alpha=opacity.value;translationY=(1f-opacity.value)*18f}) {content()}
}

@Composable
internal fun BrandedLoadingScreen() {
    AppThemeWrapper { dark,lang,_,_ ->
        androidx.compose.material3.Surface(Modifier.fillMaxSize(),color=androidx.compose.material3.MaterialTheme.colorScheme.background) {
            ScreenEntrance("startup") {
                androidx.compose.foundation.layout.Column(Modifier.fillMaxSize(),verticalArrangement=androidx.compose.foundation.layout.Arrangement.Center,horizontalAlignment=androidx.compose.ui.Alignment.CenterHorizontally) {
                    androidx.compose.foundation.Image(androidx.compose.ui.res.painterResource(if(dark)R.drawable.logo_dark else R.drawable.logo_light),AppTranslations.getString(lang,"school_name"),Modifier.size(112.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
                    androidx.compose.material3.Text(AppTranslations.getString(lang,"school_name"),style=androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    androidx.compose.foundation.layout.Spacer(Modifier.height(20.dp))
                    androidx.compose.material3.CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=2.dp)
                }
            }
        }
    }
}
