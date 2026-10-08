package com.otmanelabouze.abdallahguennoun

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

internal fun appText(lang:String,ar:String,fr:String,en:String)=when(lang){"fr"->fr;"en"->en;else->ar}

@Composable
internal fun SettingsRow(title:String,subtitle:String="",onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(horizontal=20.dp,vertical=18.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
            Text(title,style=MaterialTheme.typography.titleMedium)
            if(subtitle.isNotBlank())Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(if(androidx.compose.ui.platform.LocalLayoutDirection.current==androidx.compose.ui.unit.LayoutDirection.Rtl) "‹" else "›",style=MaterialTheme.typography.titleLarge,color=MaterialTheme.colorScheme.primary)
    }
}

@Composable
internal fun SettingsGroup(content:@Composable ColumnScope.()->Unit) {
    Surface(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface) {
        Column(content=content)
    }
}

@Composable
internal fun AppSettings(lang:String,onLanguage:(String)->Unit,onPublicServices:()->Unit,showPublicEntry:Boolean=true) {
    var panel by remember {mutableStateOf<String?>(null)}
    val context=LocalContext.current
    val mode=LocalThemeMode.current
    val setMode=LocalSetThemeMode.current
    fun t(ar:String,fr:String,en:String)=appText(lang,ar,fr,en)
    val themeName=when(mode){"dark"->t("مظلم","Sombre","Dark");"light"->t("فاتح","Clair","Light");else->t("حسب النظام","Selon le système","Follow system")}
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(t("الإعدادات","Paramètres","Settings"),style=MaterialTheme.typography.headlineMedium,modifier=Modifier.padding(vertical=12.dp))
        SettingsGroup {
            SettingsRow(t("المظهر","Apparence","Appearance"),themeName){panel="theme"}
            HorizontalDivider(Modifier.padding(horizontal=20.dp),color=MaterialTheme.colorScheme.outline.copy(alpha=.15f))
            SettingsRow(t("اللغة","Langue","Language"),when(lang){"fr"->"Français";"en"->"English";else->"العربية"}){panel="language"}
        }
        SettingsGroup {
            SettingsRow(t("إشعارات المؤسسة","Notifications du lycée","School notifications"),t("الأخبار والتوجيه واستعمال الزمن","Actualités, orientation et emplois du temps","News, guidance and timetables")) {
                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE,context.packageName))
            }
        }
        if(showPublicEntry) SettingsGroup {
            SettingsRow(t("الفضاء التعليمي المباشر","Accès éducatif direct","Direct learning access"),t("مسار وتلميذ تيس دون انتظار تسجيل التطبيق","Massar et TelmidTice sans attendre la connexion de l’application","Massar and TelmidTice without waiting for app sign-in"),onPublicServices)
        }
        SettingsGroup {
            SettingsRow(t("حول التطبيق","À propos","About the app"),"RTION"){panel="about"}
            HorizontalDivider(Modifier.padding(horizontal=20.dp),color=MaterialTheme.colorScheme.outline.copy(alpha=.15f))
            SettingsRow(t("اتفاقية الاستخدام والخصوصية","Utilisation et confidentialité","Use and privacy")){panel="terms"}
        }
    }
    when(panel) {
        "theme","language" -> {
            val theme=panel=="theme"
            val options=if(theme) listOf("system" to t("حسب النظام","Selon le système","Follow system"),"light" to t("فاتح","Clair","Light"),"dark" to t("مظلم","Sombre","Dark"))
                else listOf("ar" to "العربية","fr" to "Français","en" to "English")
            AlertDialog(onDismissRequest={panel=null},shape=RoundedCornerShape(16.dp),
                title={Text(if(theme)t("المظهر","Apparence","Appearance")else t("اللغة","Langue","Language"))},
                text={Column {options.forEach { (id,title) ->
                    Row(Modifier.fillMaxWidth().clickable {if(theme)setMode(id)else onLanguage(id);panel=null}.padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically) {
                        RadioButton(selected=id==(if(theme)mode else lang),onClick=null)
                        Text(title,Modifier.padding(start=12.dp))
                    }
                }}},confirmButton={TextButton(onClick={panel=null}){Text(label(lang,"close"))}})
        }
        "about" -> AppInformation(lang,false){panel=null}
        "terms" -> AppInformation(lang,true){panel=null}
    }
}

@Composable
internal fun AppInformation(lang:String,terms:Boolean,close:()->Unit) {
    val context=LocalContext.current
    val version=remember {runCatching {context.packageManager.getPackageInfo(context.packageName,0).versionName}.getOrNull().orEmpty().ifBlank {"—"}}
    fun t(ar:String,fr:String,en:String)=appText(lang,ar,fr,en)
    val title=if(terms)t("اتفاقية الاستخدام والخصوصية","Utilisation et confidentialité","Use and privacy")else t("حول التطبيق","À propos","About the app")
    val body=if(terms)t(
        "استخدم حسابك وبياناتك الصحيحة، واحترم الآخرين ولا تنشر محتوى مسيئاً أو بيانات الغير دون إذن.\n\nتراجع الإدارة طلب التسجيل قبل تفعيل الخدمات الخاصة. يُخزّن ملف التلميذ في Firebase، وتُعالج الصور عبر خدمة الرفع وCloudinary. الأخبار والصور المنشورة ضمن الأخبار عمومية؛ لا ترسل وثائق شخصية ضمنها.\n\nتفتح مسار وتلميذ تيس كمواقع خارجية رسمية داخل عارض ويب. لا يقرأ التطبيق كلمات المرور ولا ينسخها إلى قاعدة بياناته؛ قد يحتفظ المحرك المدمج بملفات جلسة محلية، ويمكن مسحها من قائمة الصفحة. تُحفظ صور التطبيق مؤقتاً لمدة أقصاها 24 ساعة، وقد يحذفها النظام قبل ذلك. النسخ المحفوظة بالمعرض تبقى حتى تحذفها أنت. تخضع هذه المواقع لشروط الجهات المشغّلة لها.\n\nالاتصال المجاني أو المحدود مرتبط بشركة الاتصال والخدمة والروابط المحمّلة؛ التطبيق لا يضمن مجانية البيانات ولا يحدد رصيد خطك.\n\nلتصحيح بياناتك أو طلب حذفها، تواصل مع إدارة المؤسسة. لا ترسل كلمة المرور للمطور أو للإدارة. هذه الصفحة توضح شروط الاستخدام؛ لا تدّعي اعتماداً من الوزارة. النسخة: 23 سبتمبر 2026.",
        "Utilisez votre propre compte et des informations exactes. Respectez les autres et ne publiez pas leurs données sans autorisation.\n\nL’administration examine les inscriptions. Le profil est stocké dans Firebase et les images passent par le service d’envoi et Cloudinary. Les actualités et leurs images sont publiques : n’y envoyez pas de documents personnels.\n\nMassar et TelmidTice sont des sites officiels externes affichés dans une vue web. L’application ne lit ni ne copie leurs mots de passe dans sa base. Des cookies locaux peuvent être conservés par le moteur intégré et effacés depuis son menu. Les images de l’application sont mises en cache pour 24 heures au maximum. Les copies enregistrées dans la galerie restent jusqu’à leur suppression par vous. Les conditions de ces services s’appliquent.\n\nLa gratuité des données dépend de l’opérateur, du service et des ressources chargées. L’application ne garantit pas la gratuité et ne connaît pas votre solde.\n\nPour corriger ou supprimer vos données, contactez l’administration du lycée. Ne communiquez jamais votre mot de passe. Cette notice ne constitue pas une approbation ministérielle. Version : 23 septembre 2026.",
        "Use your own account and accurate details. Respect others and do not publish their information without permission.\n\nThe school reviews registrations. Student profiles are stored in Firebase; images use the upload service and Cloudinary. News and its images are public: do not upload personal documents there.\n\nMassar and TelmidTice are external official websites displayed in a web view. The app does not read or copy their passwords into its database. The embedded engine may retain local session cookies; clear them from the page menu. App images are cached for up to 24 hours. Gallery copies remain until you delete them. Those services have their own terms.\n\nData charges depend on your carrier, service and loaded resources. This app does not guarantee free data or know your balance.\n\nContact school administration to correct or request deletion of your data. Never share your password. This notice does not claim ministry endorsement. Version: 23 September 2026.")
    else t("ثانوية عبد الله كنون — القليعة\nالإصدار: $version\nالمطور: RTION — Otman Elabouze\n\nيجمع التطبيق أخبار المؤسسة، التوجيه، استعمالات الزمن والملف الشخصي. تدير الإدارة المحتوى وطلبات التسجيل عبر موقع الإدارة.\n\nيفتح الفضاء التعليمي مسار وتلميذ تيس مباشرة، بينما تحتاج خدمات المؤسسة إلى تسجيل الدخول والصلاحيات والاتصال بخدماتها.",
        "Lycée Abdallah Guennoun — Lqliaa\nVersion : $version\nDéveloppeur : RTION — Otman Elabouze\n\nActualités, orientation, emplois du temps et profil scolaire. L’administration gère le contenu et les inscriptions depuis son portail.\n\nL’espace éducatif ouvre directement Massar et TelmidTice. Les services du lycée nécessitent une connexion, une autorisation et un accès à leurs serveurs.",
        "Abdallah Guennoun High School — Lqliaa\nVersion: $version\nDeveloper: RTION — Otman Elabouze\n\nSchool news, guidance, timetables and student profiles. Staff manage content and registration requests through the admin portal.\n\nThe learning space opens Massar and TelmidTice directly. School services require sign-in, authorization and access to their servers.")
    AlertDialog(onDismissRequest=close,shape=RoundedCornerShape(16.dp),title={Text(title)},
        text={Column(Modifier.heightIn(max=480.dp).verticalScroll(rememberScrollState())) {
            Text(body,style=MaterialTheme.typography.bodyMedium)
            if(!terms)TextButton(onClick={
                try {
                    context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://www.instagram.com/otman.elab/")))
                } catch(_:android.content.ActivityNotFoundException) {
                    android.widget.Toast.makeText(context,t("افتح instagram.com/otman.elab في المتصفح","Ouvrez instagram.com/otman.elab dans le navigateur","Open instagram.com/otman.elab in your browser"),android.widget.Toast.LENGTH_LONG).show()
                }
            }) {Text("Instagram · @otman.elab")}
        }},
        confirmButton={TextButton(onClick=close){Text(label(lang,"close"))}})
}
