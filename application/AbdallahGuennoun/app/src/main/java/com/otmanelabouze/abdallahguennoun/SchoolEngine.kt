package com.otmanelabouze.abdallahguennoun

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebViewDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** System WebView has an app-wide cookie store; never reuse it across Firebase accounts. */
internal object SchoolEngine {
    private val gate=Mutex()
    private suspend fun clearCookies(context:Context) {
        suspendCancellableCoroutine<Unit> { continuation ->
            CookieManager.getInstance().removeAllCookies {
                if(continuation.isActive)continuation.resume(Unit)
            }
        }
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
        WebViewDatabase.getInstance(context).clearHttpAuthUsernamePassword()
    }
    suspend fun prepare(context:Context):Boolean=withContext(Dispatchers.Main.immediate) {
        gate.withLock {
            val prefs=context.getSharedPreferences("school_web",Context.MODE_PRIVATE)
            val owner=AuthManager.auth.currentUser?.uid ?: "guest"
            val changed=prefs.getString("owner",null)!=owner
            if(changed) {
                clearCookies(context)
                prefs.edit().putString("owner",owner).apply()
            }
            changed
        }
    }
    suspend fun clearCurrentSession(context:Context)=withContext(Dispatchers.Main.immediate) {
        gate.withLock {
            context.getSharedPreferences("school_web",Context.MODE_PRIVATE).edit().remove("owner").apply()
            clearCookies(context)
        }
    }
}
