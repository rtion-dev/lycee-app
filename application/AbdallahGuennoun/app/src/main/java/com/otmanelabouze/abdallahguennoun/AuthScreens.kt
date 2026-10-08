package com.otmanelabouze.abdallahguennoun

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// الألوان المعتمدة للهوية البصرية لثانوية عبد الله كنون القليعة
val WarmIvoryLight = Color(0xFFF0F5F7)
val DarkNavyBgDark = Color(0xFF091724)

val SkyBlueAccentLight = Color(0xFFE1F5FE)
val SkyBlueAccentDark = Color(0xFF1E2654)

val DarkNavyTextLight = Color(0xFF1A237E)
val LightTextDark = Color(0xFFF0F5F7)

val BlueGrayMutedLight = Color(0xFF78909C)
val BlueGrayMutedDark = Color(0xFFB0BEC5)

// مستودع الترجمات للامتثال لمتطلبات توحيد اللغة
object AppTranslations {
    private val data = mapOf(
        "ar" to mapOf(
            "welcome" to "مرحباً بكم",
            "school_name" to "ثانوية عبد الله كنون القليعة",
            "subtitle" to "سجّل الدخول للوصول إلى خدمات الثانوية",
            "google_btn" to "المتابعة باستخدام Google",
            "profile_title" to "إكمال ملف التلميذ الإلكتروني",
            "profile_sub" to "يرجى إدخال البيانات التالية بدقة لتفعيل حسابك",
            "first_name" to "الاسم الشخصي",
            "last_name" to "النسب (الاسم العائلي)",
            "class_label" to "القسم",
            "massar_label" to "رقم مسار (مثال: D155343657)",
            "save_btn" to "حفظ ومتابعة",
            "signout_btn" to "تبديل الحساب / تسجيل الخروج",
            "error_first" to "الاسم الشخصي مطلوب",
            "error_last" to "النسب مطلوب",
            "error_class" to "يرجى اختيار القسم",
            "error_massar" to "صيغة رقم مسار غير صحيحة",
            "main_title" to "ثانوية عبد الله كنون القليعة",
            "main_success" to "تم تسجيل الدخول بنجاح وملف التلميذ مكتمل.",
            "main_account" to "الحساب:",
            "main_logout" to "تسجيل الخروج"
        ),
        "fr" to mapOf(
            "welcome" to "Bienvenue",
            "school_name" to "Lycée Abdallah Guennoun — Lqliaa",
            "subtitle" to "Connectez-vous pour accéder aux services du lycée",
            "google_btn" to "Continuer avec Google",
            "profile_title" to "Compléter le profil de l'élève",
            "profile_sub" to "Veuillez saisir vos informations avec précision",
            "first_name" to "Prénom",
            "last_name" to "Nom de famille",
            "class_label" to "Classe",
            "massar_label" to "Code Massar (Ex: D155343657)",
            "save_btn" to "Enregistrer et continuer",
            "signout_btn" to "Changer de compte / Déconnexion",
            "error_first" to "Le prénom est requis",
            "error_last" to "Le nom est requis",
            "error_class" to "Veuillez choisir une classe",
            "error_massar" to "Format du code Massar incorrect",
            "main_title" to "Lycée Abdallah Guennoun — Lqliaa",
            "main_success" to "Connexion réussie et profil complété.",
            "main_account" to "Compte:",
            "main_logout" to "Déconnexion"
        ),
        "en" to mapOf(
            "welcome" to "Welcome",
            "school_name" to "Abdallah Guennoun High School — Lqliaa",
            "subtitle" to "Sign in to access school services",
            "google_btn" to "Continue with Google",
            "profile_title" to "Complete Student Profile",
            "profile_sub" to "Please enter your details accurately to activate your account",
            "first_name" to "First Name",
            "last_name" to "Last Name",
            "class_label" to "Class",
            "massar_label" to "Massar Code (E.g., D155343657)",
            "save_btn" to "Save and Continue",
            "signout_btn" to "Switch Account / Sign Out",
            "error_first" to "First name is required",
            "error_last" to "Last name is required",
            "error_class" to "Please select a class",
            "error_massar" to "Incorrect Massar code format",
            "main_title" to "Abdallah Guennoun High School — Lqliaa",
            "main_success" to "Successfully signed in and profile completed.",
            "main_account" to "Account:",
            "main_logout" to "Sign Out"
        )
    )

    fun getString(lang: String, key: String): String {
        return data[lang]?.get(key) ?: data["ar"]?.get(key) ?: ""
    }
}

// A single preference shared by the native splash and every Compose screen.
// Missing preference means system mode; the old boolean no longer forces light mode.
object AppThemePreferences {
    const val KEY = "pref_theme_mode"
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"

    fun read(prefs: SharedPreferences): String =
        prefs.getString(KEY, SYSTEM)?.takeIf { it in listOf(SYSTEM, LIGHT, DARK) } ?: SYSTEM

    fun isDark(context: Context): Boolean {
        val mode = read(context.getSharedPreferences("app_settings", Context.MODE_PRIVATE))
        return when (mode) {
            DARK -> true
            LIGHT -> false
            else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        }
    }
}

internal val LocalThemeMode = compositionLocalOf { AppThemePreferences.SYSTEM }
internal val LocalSetThemeMode = compositionLocalOf<(String) -> Unit> { {} }

@Composable
fun AppThemeWrapper(content: @Composable (isDark: Boolean, lang: String, onThemeChange: (Boolean) -> Unit, onLangChange: (String) -> Unit) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) }

    var themeMode by remember { mutableStateOf(AppThemePreferences.read(prefs)) }
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppThemePreferences.DARK -> true
        AppThemePreferences.LIGHT -> false
        else -> systemDark
    }
    val setThemeMode: (String) -> Unit = { mode ->
        themeMode = mode
        prefs.edit().putString(AppThemePreferences.KEY, mode).apply()
    }
    var lang by remember { mutableStateOf(prefs.getString("pref_lang", "ar") ?: "ar") }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == AppThemePreferences.KEY) themeMode = AppThemePreferences.read(prefs)
            if (key == "pref_lang") lang = prefs.getString("pref_lang", "ar") ?: "ar"
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val onThemeChange: (Boolean) -> Unit = { newValue ->
        setThemeMode(if (newValue) AppThemePreferences.DARK else AppThemePreferences.LIGHT)
    }

    val onLangChange: (String) -> Unit = { newValue ->
        lang = newValue
        prefs.edit().putString("pref_lang", newValue).apply()
    }

    androidx.compose.runtime.SideEffect {
        val activity = context as? android.app.Activity
        activity?.let {
            androidx.core.view.WindowInsetsControllerCompat(it.window, it.window.decorView).apply {
                show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }
    val scheme = if (isDark) darkColorScheme(
        primary=Color(0xFF9ABAD5),onPrimary=Color(0xFF102238),
        background=DarkNavyBgDark,onBackground=Color(0xFFECF4F7),
        surface=Color(0xFF112738),onSurface=Color(0xFFECF4F7),
        surfaceVariant=Color(0xFF193546),onSurfaceVariant=Color(0xFFB1C6D2),
        primaryContainer=Color(0xFF223D58),onPrimaryContainer=Color(0xFFD7E5F1),
        secondary=Color(0xFFB9CEF4),onSecondary=Color(0xFF183044),
        secondaryContainer=Color(0xFF263C58),onSecondaryContainer=Color(0xFFD9E6FF),
        outline=Color(0xFF54717F)
    ) else lightColorScheme(
        primary=Color(0xFF345E82),onPrimary=Color.White,
        background=WarmIvoryLight,onBackground=Color(0xFF152C40),
        surface=Color.White,onSurface=Color(0xFF152C40),
        surfaceVariant=Color(0xFFE6EEF2),onSurfaceVariant=Color(0xFF536B7D),
        primaryContainer=Color(0xFFDEE8F1),onPrimaryContainer=Color(0xFF264966),
        secondary=Color(0xFF365F99),onSecondary=Color.White,
        secondaryContainer=Color(0xFFE2ECFD),onSecondaryContainer=Color(0xFF244673),
        outline=Color(0xFF9CB5C3)
    )
    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalSetThemeMode provides setThemeMode,
        androidx.compose.ui.platform.LocalLayoutDirection provides
            if (lang == "ar") androidx.compose.ui.unit.LayoutDirection.Rtl
            else androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        MaterialTheme(colorScheme = scheme, typography = SchoolTypography,
            shapes=Shapes(extraSmall=RoundedCornerShape(6.dp),small=RoundedCornerShape(10.dp),medium=RoundedCornerShape(16.dp),large=RoundedCornerShape(20.dp),extraLarge=RoundedCornerShape(24.dp))) {
            CompositionLocalProvider(LocalContentColor provides if(isDark)Color.White else scheme.onBackground) {
                content(isDark, lang, onThemeChange, onLangChange)
            }
        }
    }
}

@Composable
fun BottomDecorativeCurves(isDark: Boolean) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.25f)
    ) {
        val width = size.width
        val height = size.height

        val color1 = if (isDark) Color(0x1F4FC3F7) else Color(0x264FC3F7)
        val color2 = if (isDark) Color(0x141A237E) else Color(0x1A1A237E)

        val path1 = Path().apply {
            moveTo(0f, height * 0.6f)
            cubicTo(width * 0.3f, height * 0.3f, width * 0.7f, height * 0.9f, width, height * 0.4f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(path = path1, color = color1)

        val path2 = Path().apply {
            moveTo(0f, height * 0.8f)
            cubicTo(width * 0.4f, height * 0.5f, width * 0.6f, height * 0.95f, width, height * 0.5f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(path = path2, color = color2)
    }
}

@Composable
fun PremiumSchoolEmblem(isDark: Boolean) {
    // ملاحظة: يرجى وضع ملفات الشعارات في مجلد app/src/main/res/drawable/
    // وتسميتها logo_light.png و logo_dark.png لتختفي أخطاء البناء
    val logoRes = if (isDark) R.drawable.logo_dark else R.drawable.logo_light
    
    Image(
        painter = painterResource(id = logoRes),
        contentDescription = "School Logo",
        modifier = Modifier
            .size(175.dp)
            .padding(8.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
internal fun AppPreferencesButton(lang: String, onLangChange: (String) -> Unit, rounded: Boolean = false) {
    var opened by remember { mutableStateOf(false) }
    val mode = LocalThemeMode.current
    val setMode = LocalSetThemeMode.current
    val title = appText(lang,"اللغة والمظهر","Langue et apparence","Language and appearance")
    IconButton(onClick={opened=true}) {
        Icon(painterResource(R.drawable.ic_settings),title,Modifier.size(22.dp),tint=MaterialTheme.colorScheme.onSurface)
    }
    if(opened) AlertDialog(onDismissRequest={opened=false},shape=RoundedCornerShape(if(rounded)36.dp else 24.dp),
        title={Row(verticalAlignment=Alignment.CenterVertically) {
            Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
            IconButton(onClick={opened=false}) {Icon(painterResource(R.drawable.ic_close),appText(lang,"إغلاق","Fermer","Close"))}
        }},
        text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text(appText(lang,"اللغة","Langue","Language"),color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge)
            listOf("ar" to "العربية","fr" to "Français","en" to "English").forEach {(value,name)->
                PreferenceOption(name,lang==value){onLangChange(value)}
            }
            HorizontalDivider(Modifier.padding(vertical=10.dp))
            Text(appText(lang,"المظهر","Apparence","Appearance"),color=MaterialTheme.colorScheme.primary,style=MaterialTheme.typography.labelLarge)
            listOf(AppThemePreferences.SYSTEM to appText(lang,"حسب الجهاز","Selon l’appareil","Device setting"),
                AppThemePreferences.LIGHT to appText(lang,"فاتح","Clair","Light"),
                AppThemePreferences.DARK to appText(lang,"مظلم","Sombre","Dark")).forEach {(value,name)->
                PreferenceOption(name,mode==value){setMode(value)}
            }
        }},
        confirmButton={Button(onClick={opened=false},modifier=Modifier.fillMaxWidth(),shape=if(rounded)CircleShape else RoundedCornerShape(12.dp)){Text(appText(lang,"تم","Terminé","Done"))}})
}

@Composable
private fun PreferenceOption(text: String, selected: Boolean, choose: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).selectable(selected=selected,role=androidx.compose.ui.semantics.Role.RadioButton,onClick=choose).padding(horizontal=8.dp,vertical=4.dp),verticalAlignment=Alignment.CenterVertically) {
        RadioButton(selected=selected,onClick=null)
        Spacer(Modifier.width(12.dp))
        Text(text,style=MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun TopUtilityRow(isDark: Boolean, lang: String, onThemeChange: (Boolean) -> Unit, onLangChange: (String) -> Unit,includeStatusBarPadding: Boolean = true) {
    Box(Modifier.fillMaxWidth().then(if(includeStatusBarPadding)Modifier.statusBarsPadding() else Modifier).padding(horizontal=20.dp,vertical=4.dp)) {
        Box(Modifier.align(androidx.compose.ui.AbsoluteAlignment.CenterRight)) { AppPreferencesButton(lang,onLangChange) }
    }
}

@Composable
fun GoogleLoginScreen(onGoogleSignInClick:()->Unit,isLoading:Boolean,errorMessage:String?,onPublicServices:()->Unit) {
    AppThemeWrapper { dark,lang,_,language ->
        ReferenceLogin(dark,lang,language,onGoogleSignInClick,isLoading,errorMessage,onPublicServices)
    }
}
