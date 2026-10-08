package com.otmanelabouze.abdallahguennoun

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.Date

internal val documentFieldLabels = linkedMapOf(
    "nameAr" to listOf("الاسم الكامل بالعربية", "Nom complet en arabe", "Full name in Arabic"),
    "nameLatin" to listOf("الاسم الكامل بالحروف اللاتينية", "Nom complet en caractères latins", "Full name in Latin letters"),
    "birthDate" to listOf("تاريخ الازدياد (YYYY-MM-DD)", "Date de naissance (YYYY-MM-DD)", "Date of birth (YYYY-MM-DD)"),
    "massarId" to listOf("رقم مسار", "Code Massar", "Massar ID"),
    "classId" to listOf("القسم", "Classe", "Class"),
    "ordinalNumber" to listOf("الرقم الترتيبي", "Numéro d'ordre", "Roll number")
)
private fun documentText(lang: String, values: List<String>) = values[when (lang) { "fr" -> 1; "en" -> 2; else -> 0 }]
private fun documentStatus(lang: String, status: String) = documentText(lang, when (status) {
    "submitted" -> listOf("طلب جديد", "Demande envoyée", "Submitted")
    "preparing" -> listOf("مقبول وقيد التجهيز", "Acceptée, en préparation", "Accepted, preparing")
    "ready" -> listOf("جاهز للاستلام من الإدارة", "Prêt à retirer à l'administration", "Ready for collection at school")
    "delivered" -> listOf("تم التسليم — الطلب مكتمل", "Remis — demande terminée", "Collected — completed")
    "rejected" -> listOf("مرفوض", "Refusée", "Rejected")
    else -> listOf(status, status, status)
})

/** Validates only the director-selected fields; no unrequested profile data is submitted. */
internal fun documentAnswer(key: String, raw: String): Any {
    val value = raw.trim()
    require(value.isNotEmpty() && value.length <= 160)
    return when (key) {
        "ordinalNumber" -> value.toLong().also { require(it in 1..999) }
        "massarId" -> value.uppercase().also { require(Regex("^[A-Z][0-9]{9}$").matches(it)) }
        "birthDate" -> {
            require(Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$").matches(value))
            val date = LocalDate.parse(value)
            require(!date.isAfter(LocalDate.now()) && date.year >= 1900)
            value
        }
        else -> value
    }
}

@Composable
fun DocumentRequestsScreen(lang: String) {
    val db = remember { FirebaseFirestore.getInstance() }
    val uid = AuthManager.auth.currentUser?.uid ?: return
    val scope = rememberCoroutineScope()
    fun t(ar: String, fr: String, en: String) = appText(lang, ar, fr, en)
    var types by remember(uid) { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var classes by remember(uid) { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var requests by remember(uid) { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var selectedId by remember(uid) { mutableStateOf<String?>(null) }
    val answers = remember(uid) { mutableStateMapOf<String, String>() }
    var busy by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(false) }
    var refresh by remember { mutableIntStateOf(0) }
    var composeRequest by remember {mutableStateOf(false)}
    var explain by remember {mutableStateOf(false)}
    var showTypes by remember { mutableStateOf(false) }
    var showClasses by remember { mutableStateOf(false) }
    // Retain the generated request id after an uncertain network result to avoid duplicate submissions.
    var pendingRequestId by remember(uid) { mutableStateOf<String?>(null) }
    val selected = types.firstOrNull { it.id == selectedId }
    val required = (selected?.get("fields") as? List<*>)?.filterIsInstance<String>().orEmpty()
    LaunchedEffect(uid, refresh) {
        loading = true
        try {
            val catalog = db.collection("schoolConfig").document("catalog").get(Source.SERVER).await()
            classes = db.collection("classes").get(Source.SERVER).await().documents.filter {
                it.getBoolean("active") == true && it.getString("academicYear") == catalog.getString("academicYear")
            }.sortedBy { it.getString("name") }
            types = db.collection("documentTypes").whereEqualTo("active", true).get(Source.SERVER).await().documents.sortedBy { it.getString("name") }
            message = ""
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) { message = t("تعذر تحميل الخدمة. تحقق من الاتصال ثم أعد المحاولة.", "Chargement impossible. Réessayez.", "Unable to load. Check connection and retry.") }
        finally { loading = false }
    }
    DisposableEffect(uid) {
        val registration = db.collection("documentRequests").whereEqualTo("uid", uid).addSnapshotListener { snapshot, error ->
            if (error != null) message = t("تعذر تحديث طلباتك. تحقق من الاتصال والصلاحية.", "Impossible d'actualiser vos demandes.", "Unable to update your requests.")
            else if (snapshot != null) requests = snapshot.documents.sortedByDescending { it.getTimestamp("createdAt")?.toDate() ?: Date(0) }
        }
        onDispose { registration.remove() }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if(!composeRequest)ReferenceDocumentLanding(lang,types.mapNotNull {it.getString("name")},requests.isEmpty(),{composeRequest=true;showTypes=true},{explain=true})
        else TextButton(enabled=!busy && pendingRequestId==null,onClick={composeRequest=false;showTypes=false;selectedId=null}){Text(t("رجوع إلى الوثائق","Retour aux documents","Back to documents"))}
        if(explain)AlertDialog(onDismissRequest={explain=false},title={Text(t("من الطلب إلى الاستلام","De la demande au retrait","From request to collection"))},text={Text(t("اختر وثيقة متاحة، أكمل المعلومات المطلوبة وأرسل الطلب. تابع التجهيز هنا، ثم استلم الوثيقة من إدارة المؤسسة عندما تصبح جاهزة. لا توجد ملفات PDF للتحميل.","Choisissez un document, remplissez les informations et suivez la préparation. Retirez-le à l’administration. Aucun PDF à télécharger.","Choose a document, fill in the required fields, and track preparation. Collect it from the school office when ready. No PDF downloads."))},confirmButton={TextButton(onClick={explain=false}){Text(label(lang,"close"))}})
        if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.primary)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        OutlinedButton(onClick = { refresh++ }, enabled = !loading && !busy) { Text(t("تحديث الأنواع والأقسام", "Actualiser les types et classes", "Refresh types and classes")) }
        if (!loading && types.isEmpty()) Text(t("لا توجد وثائق متاحة حالياً.", "Aucun document disponible.", "No documents currently available."))
        if(composeRequest) Box {
            OutlinedButton(onClick = { showTypes = true }, enabled = !loading && !busy && pendingRequestId == null && types.isNotEmpty()) {
                Text(selected?.getString("name") ?: t("اختر الوثيقة", "Choisir un document", "Choose a document"))
            }
            DropdownMenu(expanded = showTypes, onDismissRequest = { showTypes = false }) {
                types.forEach { item -> DropdownMenuItem(text = { Text(item.getString("name").orEmpty()) }, onClick = {
                    selectedId = item.id; answers.clear(); consent = false; showTypes = false
                }) }
            }
        }
        if (selected != null) {
            required.forEach { key ->
                val fieldLabel = documentText(lang, documentFieldLabels[key] ?: listOf(key,key,key))
                if (key == "classId") {
                    Box {
                        OutlinedButton(onClick = { showClasses = true }, enabled = !busy && pendingRequestId == null && classes.isNotEmpty()) {
                            Text(fieldLabel + ": " + (classes.firstOrNull { it.id == answers[key] }?.getString("name") ?: t("اختر القسم", "Choisir la classe", "Choose class")))
                        }
                        DropdownMenu(expanded = showClasses, onDismissRequest = { showClasses = false }) {
                            classes.forEach { c -> DropdownMenuItem(text = { Text(c.getString("name").orEmpty()) }, onClick = { answers[key] = c.id; showClasses = false }) }
                        }
                    }
                    if (classes.isEmpty()) Text(t("لا توجد أقسام مفعلة للموسم الحالي. تواصل مع الإدارة.", "Aucune classe active. Contactez l'administration.", "No active classes. Contact the school office."))
                } else OutlinedTextField(value = answers[key].orEmpty(), onValueChange = { if (it.length <= 160) answers[key] = it }, label = { Text(fieldLabel) }, singleLine = true, enabled = !busy && pendingRequestId == null, modifier = Modifier.fillMaxWidth())
            }
            Row {
                Checkbox(checked = consent, onCheckedChange = { consent = it }, enabled = !busy && pendingRequestId == null)
                Text(t("أؤكد صحة المعلومات وأفهم أن الاستلام من إدارة المؤسسة.", "Je confirme ces informations et le retrait à l'administration.", "I confirm my information and collection at the school office."), modifier = Modifier.padding(top = 10.dp).weight(1f))
            }
            Button(enabled = !busy && !loading && consent && required.isNotEmpty() && required.all { !answers[it].isNullOrBlank() }, onClick = {
                scope.launch {
                    busy = true
                    try {
                        val payload = required.associateWith { documentAnswer(it, answers[it].orEmpty()) }
                        val request = db.collection("documentRequests").document(pendingRequestId ?: db.collection("documentRequests").document().id)
                        pendingRequestId = request.id
                        db.runTransaction { tx ->
                            val existing = tx.get(request)
                            if (!existing.exists()) {
                                val currentType = tx.get(db.collection("documentTypes").document(selected.id))
                                require(currentType.getBoolean("active") == true && currentType.get("fields") == required)
                                val event = request.collection("events").document()
                                tx.set(request, mapOf("uid" to uid, "typeId" to selected.id, "typeName" to currentType.getString("name").orEmpty(), "fields" to required, "answers" to payload,
                                    "status" to "submitted", "note" to "", "revision" to 1L, "lastEventId" to event.id, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()))
                                tx.set(event, mapOf("actorUid" to uid, "at" to FieldValue.serverTimestamp(), "from" to "", "to" to "submitted", "note" to "", "revision" to 1L))
                            }
                        }.await()
                        pendingRequestId = null; selectedId = null; answers.clear(); consent = false; composeRequest = false
                        message = t("تم إرسال طلبك. تابع حالته أدناه.", "Demande envoyée. Suivez son état ci-dessous.", "Request submitted. Track its status below.")
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) {
                        if (e is IllegalArgumentException || e is DateTimeParseException) pendingRequestId = null
                        if (e is com.google.firebase.firestore.FirebaseFirestoreException && e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) pendingRequestId = null
                        message = t("تعذر تأكيد الإرسال. تحقق من الحقول والاتصال وتوفّر الوثيقة، ثم أعد المحاولة. تاريخ الازدياد بصيغة YYYY-MM-DD ورقم مسار حرف و9 أرقام.", "Envoi non confirmé. Vérifiez les champs, la connexion et la disponibilité, puis réessayez.", "Submission not confirmed. Check fields, connection and availability, then retry.")
                    } finally { busy = false }
                }
            }) { Text(if (busy) t("جاري الإرسال…", "Envoi…", "Sending…") else t("تأكيد وإرسال الطلب", "Confirmer et envoyer", "Confirm and submit")) }
        }
        HorizontalDivider()
        Text(t("طلباتي", "Mes demandes", "My requests"), style = MaterialTheme.typography.titleLarge)
        if (requests.isEmpty()) Text(t("لم ترسل طلبات بعد.", "Aucune demande envoyée.", "No requests yet."))
        requests.forEach { request ->
            Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface),border=androidx.compose.foundation.BorderStroke(1.dp,MaterialTheme.colorScheme.outline.copy(alpha=.25f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(request.getString("typeName").orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Text(documentStatus(lang, request.getString("status").orEmpty()), color = MaterialTheme.colorScheme.primary)
                    Text(t("رقم الطلب: ", "Référence : ", "Reference: ") + request.id, style = MaterialTheme.typography.bodySmall)
                    request.getTimestamp("createdAt")?.let { Text(java.text.DateFormat.getDateTimeInstance().format(it.toDate()), style = MaterialTheme.typography.bodySmall) }
                    request.getString("note")?.takeIf { it.isNotBlank() }?.let { Text(it) }
                }
            }
        }
    }
}
