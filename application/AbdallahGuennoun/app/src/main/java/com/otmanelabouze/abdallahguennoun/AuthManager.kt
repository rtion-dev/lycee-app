package com.otmanelabouze.abdallahguennoun

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class ProfileIssue(val key: String) : Exception(key)

sealed class AuthState {
    object SignedOut : AuthState()
    object SignedInProfileIncomplete : AuthState()
    object Pending : AuthState()
    object Rejected : AuthState()
    object SignedInComplete : AuthState()
}

data class SchoolClass(val id: String, val name: String, val levelId: String,
    val academicYear: String, val sortOrder: Long)

data class StudentApplication(
    val fullName: String, val levelId: String, val classId: String,
    val ordinalNumber: Int, val massarId: String, val photoUrl: String? = null
)

object AuthManager {
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    suspend fun signIn(context: Context) {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false).setAutoSelectEnabled(false)
            .setServerClientId(context.getString(R.string.default_web_client_id)).build()
        val response = CredentialManager.create(context).getCredential(
            context, GetCredentialRequest.Builder().addCredentialOption(option).build())
        val credential = response.credential
        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw ProfileIssue("login_error")
        }
        val token = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
    }

    suspend fun checkAuthenticationState(): AuthState {
        val uid = auth.currentUser?.uid ?: return AuthState.SignedOut
        // Never interpret an offline/permission error as a missing profile.
        val doc = db.collection("users").document(uid).get(Source.SERVER).await()
        return when (doc.getString("status")) {
            "approved" -> AuthState.SignedInComplete
            "pending" -> AuthState.Pending
            "rejected" -> AuthState.Rejected
            else -> AuthState.SignedInProfileIncomplete
        }
    }

    suspend fun activeAcademicYear(): String {
        return db.collection("schoolConfig").document("catalog").get(Source.SERVER).await()
            .getString("academicYear")?.takeIf { it.isNotBlank() }
            ?: throw ProfileIssue("catalog_setup")
    }

    suspend fun loadClasses(level: String): List<SchoolClass> {
        val year = activeAcademicYear()
        // Deliberately use a simple indexed query; filter the remaining catalog fields locally.
        return db.collection("classes").whereEqualTo("levelId", level).get(Source.SERVER).await()
            .documents.filter { it.getBoolean("active") == true && it.getString("academicYear") == year }
            .mapNotNull { d ->
                val name = d.getString("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                SchoolClass(d.id, name, level, year, d.getLong("sortOrder") ?: 0)
            }.sortedWith(compareBy<SchoolClass> { it.sortOrder }.thenBy { it.name })
    }

    suspend fun saveStudentProfile(data: StudentApplication) {
        val uid = auth.currentUser?.uid ?: throw ProfileIssue("login_error")
        if (data.fullName.trim().isEmpty() || data.fullName.trim().length > 160) throw ProfileIssue("name_error")
        if (data.levelId !in listOf("TC", "1BAC", "2BAC")) throw ProfileIssue("level_error")
        if (data.ordinalNumber <= 0) throw ProfileIssue("number_error")
        if (!data.massarId.matches(Regex("[A-Z][0-9]{9}"))) throw ProfileIssue("massar_error")
        val ref = db.collection("users").document(uid)
        val classRef = db.collection("classes").document(data.classId)
        val configRef = db.collection("schoolConfig").document("catalog")
        db.runTransaction { tx ->
            val existing = tx.get(ref)
            val schoolClass = tx.get(classRef)
            val config = tx.get(configRef)
            if (existing.getString("status") in listOf("approved", "pending", "rejected")) {
                throw ProfileIssue("already_submitted")
            }
            if (schoolClass.getBoolean("active") != true ||
                schoolClass.getString("levelId") != data.levelId ||
                schoolClass.getString("academicYear") != config.getString("academicYear")) {
                throw ProfileIssue("class_error")
            }
            val fields = mutableMapOf<String, Any>(
                "uid" to uid, "fullName" to data.fullName.trim(),
                "levelId" to data.levelId, "classId" to data.classId,
                "className" to (schoolClass.getString("name") ?: ""),
                "academicYear" to (config.getString("academicYear") ?: ""),
                "ordinalNumber" to data.ordinalNumber, "massarId" to data.massarId,
                "avatarType" to if (data.photoUrl == null) "initials" else "photo",
                "status" to "pending", "updatedAt" to FieldValue.serverTimestamp(),
                "createdAt" to (existing.getTimestamp("createdAt") ?: FieldValue.serverTimestamp())
            )
            fields["photoUrl"] = data.photoUrl ?: FieldValue.delete()
            // Preserve unrelated existing fields; rules limit which fields students can alter.
            tx.set(ref, fields, com.google.firebase.firestore.SetOptions.merge())
            null
        }.await()
    }

    suspend fun signOutAndChangeAccount(context: Context) {
        val uid = auth.currentUser?.uid
        auth.signOut()
        uid?.let { java.io.File(context.filesDir, "profile_drafts/$it").deleteRecursively() }
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: CancellationException) { throw e } catch (_: Exception) { }
    }
}
