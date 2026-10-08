package com.otmanelabouze.abdallahguennoun

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
internal fun SupportDialog(lang:String,onDismiss:()->Unit) {
    val context=LocalContext.current
    var unavailable by remember {mutableStateOf(false)}
    AlertDialog(onDismissRequest=onDismiss,shape=RoundedCornerShape(32.dp),
        title={Text(appText(lang,"المساعدة والدعم","Aide et assistance","Help and support"))},
        text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(appText(lang,"واجهت مشكلاً في التطبيق؟ تواصل مع المطور عبر Telegram واشرح ما حدث.","Un problème dans l’application ? Contactez le développeur sur Telegram et décrivez ce qui s’est passé.","Having an app problem? Contact the developer on Telegram and describe what happened."))
            TextButton(onClick={
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://t.me/r1ion")))
                    unavailable=false
                } catch(_:ActivityNotFoundException) {unavailable=true}
            }) {Text(appText(lang,"الإبلاغ عن مشكل عبر Telegram","Signaler un problème sur Telegram","Report a problem on Telegram"))}
            if(unavailable) {
                Text(appText(lang,"تعذر فتح الرابط. انسخه وافتحه في المتصفح أو Telegram.","Impossible d’ouvrir le lien. Copiez-le dans votre navigateur ou Telegram.","Unable to open the link. Copy it into your browser or Telegram."))
                SelectionContainer {Text("https://t.me/r1ion")}
            }
        }},
        confirmButton={TextButton(onClick=onDismiss){Text(label(lang,"close"))}})
}
