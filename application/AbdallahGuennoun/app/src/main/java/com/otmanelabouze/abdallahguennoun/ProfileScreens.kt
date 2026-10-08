package com.otmanelabouze.abdallahguennoun

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

fun profileText(context: Context, lang: String, key: String): String {
    val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(lang)) }
    val localized = context.createConfigurationContext(config)
    val id = localized.resources.getIdentifier("profile_$key", "string", context.packageName)
    return if (id != 0) localized.getString(id) else key
}

fun normalizeDigits(value: String): String = value.map { char ->
    if (char.isDigit()) Character.digit(char, 10).let { if (it >= 0) ('0'.code + it).toChar() else char }
    else char
}.joinToString("")

private fun initials(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when (words.size) {
        0 -> "؟"
        1 -> words.first().take(1)
        else -> words.first().take(1) + words.last().take(1)
    }.uppercase(Locale.ROOT)
}

@Composable
private fun ProfileCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.clip(RoundedCornerShape(26.dp))
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(26.dp))
        .padding(14.dp), content = content)
}

@Composable
private fun ProfileField(value: String, onValueChange: (String) -> Unit, label: String,
                         error: String? = null, modifier: Modifier = Modifier, enabled: Boolean = true,
                         numeric: Boolean = false, ltr: Boolean = false, placeholder: String? = null) {
    OutlinedTextField(value = value, onValueChange = onValueChange,
        modifier = modifier, label = { Text(label, fontSize = 13.sp) },
        placeholder = { if (placeholder != null) Text(placeholder) }, singleLine = true,
        enabled = enabled, isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
        shape = RoundedCornerShape(26.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface),
        textStyle = LocalTextStyle.current.copy(
            textDirection = if (ltr) TextDirection.Ltr else TextDirection.Content),
        keyboardOptions = KeyboardOptions(keyboardType =
            if (numeric) KeyboardType.Number else KeyboardType.Text))
}

// Shared label placement, padding, border and minimum height for the paired fields.
@Composable
private fun ProfilePairContainer(
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Column(modifier.heightIn(min = 80.dp).clip(shape)
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, if (isError) MaterialTheme.colorScheme.error
        else MaterialTheme.colorScheme.outline, shape)
        .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)) {
        Text(label, modifier = Modifier.fillMaxWidth(), fontSize = 13.sp,
            color = if (isError) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Start)
        content()
    }
}

@Composable
fun StudentProfileScreen(onSubmitted: () -> Unit, onSignOutClick: () -> Unit) {
    AppThemeWrapper { isDark, lang, changeTheme, changeLang ->
        val context = LocalContext.current
        val uid = AuthManager.auth.currentUser?.uid ?: return@AppThemeWrapper
        val scope = rememberCoroutineScope()
        fun t(key: String) = profileText(context, lang, key)
        var name by rememberSaveable(uid) { mutableStateOf("") }
        var level by rememberSaveable(uid) { mutableStateOf("") }
        var classId by rememberSaveable(uid) { mutableStateOf("") }
        var number by rememberSaveable(uid) { mutableStateOf("") }
        var massar by rememberSaveable(uid) { mutableStateOf("") }
        var photoPath by rememberSaveable(uid) { mutableStateOf<String?>(null) }
        var uploadedUrl by rememberSaveable(uid) { mutableStateOf<String?>(null) }
        var cameraUri by rememberSaveable(uid) { mutableStateOf<String?>(null) }
        var loaded by rememberSaveable(uid) { mutableStateOf(false) }
        var loadingDraft by remember { mutableStateOf(!loaded) }
        var draftRetry by remember { mutableIntStateOf(0) }
        var busy by remember { mutableStateOf(false) }
        var photoBusy by remember { mutableStateOf(false) }
        var errorKey by remember { mutableStateOf<String?>(null) }
        var attempted by remember { mutableStateOf(false) }
        var classes by remember { mutableStateOf<List<SchoolClass>>(emptyList()) }
        var classLoading by remember { mutableStateOf(false) }
        var classError by remember { mutableStateOf<String?>(null) }
        var retry by remember { mutableIntStateOf(0) }
        var expanded by remember { mutableStateOf(false) }
        var accountMenu by remember { mutableStateOf(false) }
        val enabled = !busy && !photoBusy && !loadingDraft && loaded

        LaunchedEffect(uid, draftRetry) {
            if (loaded) return@LaunchedEffect
            loadingDraft = true
            try {
                val doc = AuthManager.db.collection("users").document(uid).get(Source.SERVER).await()
                if (doc.getString("status") in listOf("pending", "approved", "rejected")) {
                    onSubmitted()
                    return@LaunchedEffect
                }
                name = doc.getString("fullName") ?: listOfNotNull(
                    doc.getString("firstName"), doc.getString("lastName")).joinToString(" ")
                level = doc.getString("levelId")?.takeIf { it in listOf("TC", "1BAC", "2BAC") } ?: ""
                classId = doc.getString("classId") ?: ""
                number = doc.getLong("ordinalNumber")?.toString() ?: ""
                massar = doc.getString("massarId") ?: ""
                // Do not silently reuse or submit a photo that was not previewed/selected in this draft.
                loaded = true
                errorKey = null
            } catch (e: CancellationException) { throw e } catch (_: Exception) { errorKey = "network_error" } finally { loadingDraft = false }
        }

        LaunchedEffect(uid, level, retry) {
            classes = emptyList()
            classError = null
            if (level.isBlank()) { classLoading = false; return@LaunchedEffect }
            classLoading = true
            try {
                val result = AuthManager.loadClasses(level)
                classes = result
                if (classId.isNotBlank() && result.none { it.id == classId }) classId = ""
            } catch (e: CancellationException) { throw e } catch (e: Exception) { classError = (e as? ProfileIssue)?.key ?: "network_error" } finally { classLoading = false }
        }

        fun acceptPhoto(uri: Uri) {
            photoBusy = true
            scope.launch {
                try {
                    val path = ProfilePhotoStore.importImage(context, uri, uid)
                    photoPath?.let { File(it).delete() }
                    photoPath = path
                    uploadedUrl = null
                    errorKey = null
                } catch (e: CancellationException) { throw e } catch (_: Exception) { errorKey = "photo_error" } finally { photoBusy = false }
            }
        }
        val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) acceptPhoto(uri)
        }
        val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
            val uri = cameraUri
            if (ok && uri != null) acceptPhoto(Uri.parse(uri))
            cameraUri = null
        }
        fun openCamera() {
            try {
                val dir = File(context.cacheDir, "profile_capture").apply { mkdirs() }
                // These files are only temporary camera destinations.
                dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86400000 }
                    ?.forEach { it.delete() }
                val target = File.createTempFile("capture_", ".jpg", dir)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.profile.files", target)
                cameraUri = uri.toString()
                camera.launch(uri)
            } catch (_: Exception) { errorKey = "camera_error" }
        }

        val bitmap by produceState<android.graphics.Bitmap?>(null, photoPath) {
            value = withContext(Dispatchers.IO) {
                photoPath?.let { path -> runCatching { BitmapFactory.decodeFile(path) }.getOrNull() }
            }
        }
        val nameError = if (attempted && name.isBlank()) t("name_error") else null
        val ordinal = normalizeDigits(number).toIntOrNull()
        val numberError = if (attempted && (ordinal == null || ordinal <= 0)) t("number_error") else null
        val normalizedMassar = normalizeDigits(massar.trim()).uppercase(Locale.ROOT)
        val massarError = if (attempted && !normalizedMassar.matches(Regex("[A-Z][0-9]{9}"))) t("massar_error") else null
        val chosen = classes.firstOrNull { it.id == classId }

        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            TopUtilityRow(isDark, lang, changeTheme, changeLang)
            Column(Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(t("title"), fontSize = 27.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                Text(t("subtitle"), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center)
                Spacer(Modifier.height(2.dp))
                Box(Modifier.size(132.dp)) {
                    Box(Modifier.fillMaxSize().clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                        if (bitmap != null) Image(bitmap!!.asImageBitmap(), t("photo_preview"),
                            Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        else Text(initials(name), fontSize = 44.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    FilledIconButton(onClick = { openCamera() }, enabled = enabled,
                        modifier = Modifier.align(Alignment.BottomEnd).size(42.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)) {
                        ProfileGlyph("camera", Color.White)
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("camera", "album", "initials").forEach { key ->
                        OutlinedButton(onClick = {
                            when (key) {
                                "camera" -> openCamera()
                                "album" -> try { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) } catch (_: Exception) { errorKey = "photo_error" }
                                else -> {
                                    photoPath?.let { File(it).delete() }
                                    photoPath = null; uploadedUrl = null; errorKey = null
                                }
                            }
                        }, modifier = Modifier.weight(1f), enabled = enabled,
                            shape = RoundedCornerShape(22.dp), contentPadding = PaddingValues(8.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ProfileGlyph(key, MaterialTheme.colorScheme.primary)
                                Text(t(key), fontSize = 12.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                if (photoBusy || loadingDraft) CircularProgressIndicator(Modifier.size(24.dp))
                ProfileField(name, { name = it.take(160) }, t("full_name"), nameError,
                    Modifier.fillMaxWidth(), enabled)
                ProfileCard(Modifier.fillMaxWidth()) {
                    Text(t("level"), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides
                            androidx.compose.ui.unit.LayoutDirection.Ltr) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf("TC", "1BAC", "2BAC").forEach { item ->
                                TextButton(onClick = {
                                    if (level != item) { level = item; classId = ""; expanded = false }
                                }, enabled = enabled, modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = if (level == item) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        contentColor = if (level == item) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)) {
                                    Text(item)
                                }
                            }
                        }
                    }
                    if (attempted && level.isBlank()) Text(t("level_error"), color = MaterialTheme.colorScheme.error)
                }
                // Measure both field containers together; helper/error text stays outside this row.
                val canChooseClass = enabled && !classLoading && classes.isNotEmpty()
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top) {
                    Box(Modifier.weight(2f).fillMaxHeight()) {
                        ProfilePairContainer(
                            label = t("class"),
                            modifier = Modifier.fillMaxWidth().fillMaxHeight()
                                .clickable(enabled = canChooseClass,
                                    role = androidx.compose.ui.semantics.Role.Button) { expanded = true },
                            isError = attempted && chosen == null && !classLoading
                        ) {
                            Row(Modifier.fillMaxWidth().heightIn(min = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(chosen?.name ?: "—", modifier = Modifier.weight(1f),
                                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDirection = TextDirection.Ltr,
                                        textAlign = TextAlign.Start))
                                val arrowColor = MaterialTheme.colorScheme.onSurfaceVariant
                                Canvas(Modifier.size(20.dp)) {
                                    val path = androidx.compose.ui.graphics.Path().apply {
                                        moveTo(size.width * .25f, size.height * .4f)
                                        lineTo(size.width * .5f, size.height * .65f)
                                        lineTo(size.width * .75f, size.height * .4f)
                                    }
                                    drawPath(path, arrowColor,
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                                }
                            }
                        }
                        DropdownMenu(expanded = expanded && canChooseClass,
                            onDismissRequest = { expanded = false }) {
                            classes.forEach { item ->
                                DropdownMenuItem(text = { Text(item.name) }, onClick = {
                                    classId = item.id; expanded = false
                                })
                            }
                        }
                    }
                    ProfilePairContainer(label = t("number"),
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        isError = numberError != null) {
                        BasicTextField(value = number,
                            onValueChange = { number = normalizeDigits(it).filter(Char::isDigit).take(6) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 24.dp)
                                .semantics { contentDescription = t("number") },
                            enabled = enabled, singleLine = true,
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                textDirection = TextDirection.Ltr, textAlign = TextAlign.Center),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    }
                }
                if (numberError != null) Text(numberError, color = MaterialTheme.colorScheme.error)
                when {
                    level.isBlank() -> Text(t("choose_level"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    classLoading -> CircularProgressIndicator(Modifier.size(22.dp))
                    classError != null -> {
                        Text(t(classError!!), color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { retry++ }) { Text(t("retry")) }
                    }
                    classes.isEmpty() -> {
                        Text(t("empty_classes"), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { retry++ }) { Text(t("retry")) }
                    }
                    attempted && chosen == null -> Text(t("class_error"), color = MaterialTheme.colorScheme.error)
                    else -> Text(t("hint"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                ProfileField(massar, { massar = normalizeDigits(it).uppercase(Locale.ROOT).take(10) },
                    t("massar"), massarError, Modifier.fillMaxWidth(), enabled,
                    ltr = true, placeholder = "D155343657")
                errorKey?.let { Text(t(it), color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
                if (!loaded && !loadingDraft) TextButton(onClick = { draftRetry++ }) { Text(t("retry")) }
                Button(onClick = {
                    attempted = true
                    if (name.isNotBlank() && level.isNotBlank() && chosen != null &&
                        ordinal != null && ordinal > 0 && normalizedMassar.matches(Regex("[A-Z][0-9]{9}"))) {
                        busy = true; errorKey = null
                        // Capture values to avoid edits/recompositions changing an in-flight request.
                        val captured = StudentApplication(name.trim(), level, classId, ordinal, normalizedMassar)
                        val localPhoto = photoPath
                        scope.launch {
                            try {
                                val url = if (localPhoto == null) null else uploadedUrl
                                    ?: ProfilePhotoStore.upload(localPhoto).also { uploadedUrl = it }
                                AuthManager.saveStudentProfile(captured.copy(photoUrl = url))
                                File(context.filesDir, "profile_drafts/$uid").deleteRecursively()
                                onSubmitted()
                            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                                val issue = generateSequence<Throwable>(e) { it.cause }
                                    .filterIsInstance<ProfileIssue>().firstOrNull()
                                if (issue?.key == "already_submitted") onSubmitted()
                                else errorKey = issue?.key ?: "network_error"
                            } finally { busy = false }
                        }
                    }
                }, enabled = enabled && !classLoading && classError == null,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text(t("submit"), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                Text(t("note"), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center, fontSize = 13.sp)
                OutlinedButton(onClick = { accountMenu = true }, enabled = !busy && !photoBusy,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
                    Text(t("logout"), color = if (isDark) Color(0xFFFF7575) else Color(0xFFB42331))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
        if (accountMenu) AlertDialog(onDismissRequest = { accountMenu = false },
            title = { Text(t("logout")) },
            text = { Text(AuthManager.auth.currentUser?.email ?: "") },
            confirmButton = { TextButton(onClick = { accountMenu = false; onSignOutClick() }) { Text(t("switch")) } },
            dismissButton = {
                Row {
                    TextButton(onClick = { accountMenu = false; onSignOutClick() }) { Text(t("signout")) }
                    TextButton(onClick = { accountMenu = false }) { Text(t("cancel")) }
                }
            })
    }
}

@Composable
private fun ProfileGlyph(kind: String, color: Color) {
    if (kind == "initials") { Text("Aa", color = color, fontWeight = FontWeight.Bold, fontSize = 22.sp); return }
    Canvas(Modifier.size(26.dp)) {
        val w = size.width; val h = size.height
        val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        drawRoundRect(color, androidx.compose.ui.geometry.Offset(w * .1f, h * .25f),
            androidx.compose.ui.geometry.Size(w * .8f, h * .6f), androidx.compose.ui.geometry.CornerRadius(w * .08f), style = stroke)
        if (kind == "camera") {
            drawCircle(color, w * .17f, androidx.compose.ui.geometry.Offset(w * .5f, h * .55f), style = stroke)
            drawLine(color, androidx.compose.ui.geometry.Offset(w * .35f, h * .15f),
                androidx.compose.ui.geometry.Offset(w * .65f, h * .15f), 2.dp.toPx())
        } else {
            drawCircle(color, w * .07f, androidx.compose.ui.geometry.Offset(w * .32f, h * .42f))
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(w * .15f, h * .8f); lineTo(w * .43f, h * .57f)
                lineTo(w * .56f, h * .68f); lineTo(w * .72f, h * .46f); lineTo(w * .86f, h * .8f)
            }
            drawPath(path, color, style = stroke)
        }
    }
}

@Composable
fun ApplicationStatusScreen(rejected: Boolean = false, failed: Boolean = false,
                            loading: Boolean, detail: String? = null, onRefresh: () -> Unit, onSignOut: () -> Unit) {
    AppThemeWrapper { dark, lang, themeChange, langChange ->
        val context = LocalContext.current
        fun t(key: String) = profileText(context, lang, key)
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally) {
            TopUtilityRow(dark, lang, themeChange, langChange)
            Column(Modifier.widthIn(max = 520.dp).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(22.dp)) {
                Text(t(if (failed) "error_title" else if (rejected) "rejected_title" else "pending_title"),
                    fontSize = 26.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
                Text(if(failed && !detail.isNullOrBlank()) detail else t(if (failed) "network_error" else if (rejected) "rejected_body" else "pending_body"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text(AuthManager.auth.currentUser?.email ?: "", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (loading) CircularProgressIndicator()
                Button(onClick = onRefresh, enabled = !loading) { Text(t(if (failed) "retry" else "refresh")) }
                TextButton(onClick = onSignOut, enabled = !loading) { Text(t("logout")) }
            }
        }
    }
}
