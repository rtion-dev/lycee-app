package com.otmanelabouze.abdallahguennoun

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

internal object SchoolNotifications {
    const val CHANNEL="school_updates"
    val pendingSection=mutableStateOf<String?>(null)
    fun accept(intent:Intent?) {
        val section=intent?.getStringExtra("section")
        if(section in listOf("news","guidance","timetables"))pendingSection.value=if(section=="timetables")"timetable"else section
    }
    fun initialize(context:Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL,"مستجدات المؤسسة",NotificationManager.IMPORTANCE_DEFAULT))
        // Only generic school publication alerts are broadcast. Never send student data to a topic.
        FirebaseMessaging.getInstance().subscribeToTopic("school_publications")
    }
}

@Composable
internal fun NotificationPermissionPrompt() {
    val context=LocalContext.current
    val prefs=remember {context.getSharedPreferences("notifications",Context.MODE_PRIVATE)}
    val launcher=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if(Build.VERSION.SDK_INT>=33 && !prefs.getBoolean("asked",false) &&
            ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) {
            prefs.edit().putBoolean("asked",true).apply()
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

class SchoolMessagingService:FirebaseMessagingService() {
    override fun onNewToken(token:String) {SchoolNotifications.initialize(this)}
    override fun onMessageReceived(message:RemoteMessage) {
        if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return
        val notification=message.notification ?: return
        val tag=message.data["eventId"] ?: message.messageId ?: "school"
        val intent=Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP).putExtra("section",message.data["section"])
        val pending=PendingIntent.getActivity(this,tag.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        NotificationManagerCompat.from(this).notify(tag,0,NotificationCompat.Builder(this,SchoolNotifications.CHANNEL)
            .setSmallIcon(R.drawable.ic_school_notification).setContentTitle(notification.title)
            .setContentText(notification.body).setContentIntent(pending).setAutoCancel(true).build())
    }
}
