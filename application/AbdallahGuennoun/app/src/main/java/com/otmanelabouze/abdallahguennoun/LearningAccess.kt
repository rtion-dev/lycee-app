package com.otmanelabouze.abdallahguennoun

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// Network validation is an availability hint, NOT a carrier balance check.
@Composable
internal fun rememberLimitedConnection():Boolean {
    val context=LocalContext.current
    val manager=remember {context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager}
    var limited by remember {mutableStateOf(false)}
    DisposableEffect(manager) {
        val main=Handler(Looper.getMainLooper())
        var active=true
        fun update() {main.post {
            if(active) {
                val caps=manager.getNetworkCapabilities(manager.activeNetwork)
                limited=caps==null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
        }}
        val callback=object:ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network:Network)=update()
            override fun onLost(network:Network)=update()
            override fun onCapabilitiesChanged(network:Network,caps:NetworkCapabilities)=update()
        }
        update()
        if(Build.VERSION.SDK_INT>=24)manager.registerDefaultNetworkCallback(callback)
        else manager.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),callback)
        onDispose {active=false;manager.unregisterNetworkCallback(callback);main.removeCallbacksAndMessages(null)}
    }
    return limited
}

@Composable
internal fun LearningAccessScreen(onReturn:()->Unit) {
    AppThemeWrapper { dark,lang,_,onLanguage ->
        var website by remember {mutableStateOf<SchoolWebsite?>(null)}
        if(website!=null) {
            Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
                Box(Modifier.fillMaxSize().safeDrawingPadding()) {SchoolWebScreen(website!!,lang,dark){website=null}}
            }
        } else {
            BackHandler {onReturn()}
            Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                    Spacer(Modifier.height(24.dp))
                    Text(appText(lang,"فضاؤك التعليمي","Votre espace éducatif","Your learning space"),style=MaterialTheme.typography.headlineLarge)
                    Text(appText(lang,"وصول مباشر للمواقع التعليمية، حتى إذا تعذر الاتصال بخدمات التطبيق.","Accès direct aux sites éducatifs, même si les services de l’application sont inaccessibles.","Direct access to learning websites, even when app services are unavailable."),color=MaterialTheme.colorScheme.onSurfaceVariant)
                    SettingsGroup {
                        SettingsRow("Massar · مسار", "massarservice.men.gov.ma"){website=SchoolWebsite.MASSAR}
                        HorizontalDivider(Modifier.padding(horizontal=20.dp))
                        SettingsRow("TelmidTice · تلميذ تيس","telmidtice.men.gov.ma"){website=SchoolWebsite.TELMIDTICE}
                    }
                    Text(appText(lang,"لا يمكن معرفة رصيد خطك من التطبيق. إتاحة المواقع دون رصيد تعتمد على شركة الاتصال وقد لا تشمل الفيديوهات أو الروابط الخارجية.","Le solde de votre ligne est inconnu. L’accès sans forfait dépend de l’opérateur et peut exclure vidéos et liens externes.","Your balance is unknown. Access without a data plan depends on your carrier and may exclude videos or external links."),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    AppSettings(lang,onLanguage,onPublicServices={},showPublicEntry=false)
                    Button(onClick=onReturn,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp)) {
                        Text(appText(lang,"العودة إلى التطبيق","Retour à l’application","Return to app"))
                    }
                }
            }
        }
    }
}
