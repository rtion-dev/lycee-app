package com.otmanelabouze.abdallahguennoun

import android.content.Context
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import androidx.credentials.exceptions.GetCredentialCancellationException

class MainActivity : AppCompatActivity() {

    private var videoView: ResponsiveVideoView? = null
    private var isSplashCompleted = false
    private var splashPosition = 0
    private var splashPrepared = false
    private var activityResumed = false
    private var splashDark = false
    private var splashContainer: FrameLayout? = null

    // علم حالة الواجهات لربط التنقل بـ Compose بعد انتهاء الـ Splash
    private val currentScreen = mutableStateOf<Screen>(Screen.Splash)
    private val isLoading = mutableStateOf(false)
    private val errorMessage = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SchoolMediaCache.schedule(applicationContext)
        SchoolNotifications.initialize(applicationContext)
        SchoolNotifications.accept(intent)

        if (savedInstanceState != null) {
            isSplashCompleted = savedInstanceState.getBoolean("KEY_SPLASH_COMPLETED", false)
            splashPosition = savedInstanceState.getInt("KEY_SPLASH_POSITION", 0)
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableFullscreen()

        if (isSplashCompleted) {
            checkCurrentAuthAndNavigate()
        } else {
            showSplashScreen()
        }
    }

    override fun onNewIntent(intent:android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SchoolNotifications.accept(intent)
    }

    private fun showSplashScreen() {
        splashDark = AppThemePreferences.isDark(this)
        val container = FrameLayout(this).apply {
            setBackgroundColor(splashBackgroundColor())
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        val localVideoView = ResponsiveVideoView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = Gravity.CENTER
                setMargins(0, 0, 0, 0)
            }
        }

        splashContainer = container
        videoView = localVideoView
        container.addView(localVideoView)
        setContentView(container)

        playSplashVideo()
    }

    private fun splashBackgroundColor(): Int =
        if (splashDark) Color.rgb(10, 14, 38) else Color.rgb(246, 247, 245)

    private fun playSplashVideo() {
        val targetVideoView = videoView ?: return
        splashPrepared = false
        val resource = if (splashDark) R.raw.splash_video_dark else R.raw.splash_video
        val videoUri = Uri.parse("android.resource://$packageName/$resource")
        // Cover the SurfaceView until its first decoded frame, avoiding a black flash.
        targetVideoView.setBackgroundColor(splashBackgroundColor())
        targetVideoView.setOnPreparedListener { mediaPlayer ->
            if (!isSplashCompleted && videoView === targetVideoView) {
                mediaPlayer.isLooping = false
                mediaPlayer.setVolume(1f, 1f)
                targetVideoView.setVideoSize(mediaPlayer.videoWidth, mediaPlayer.videoHeight)
                mediaPlayer.setOnInfoListener { _, what, _ ->
                    if (what == android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                        targetVideoView.setBackgroundColor(Color.TRANSPARENT)
                    }
                    false
                }
                splashPrepared = true
                if (splashPosition > 0) targetVideoView.seekTo(splashPosition)
                if (activityResumed) targetVideoView.start()
            }
        }
        targetVideoView.setOnCompletionListener { openHomeScreen() }
        targetVideoView.setOnErrorListener { _, _, _ ->
            openHomeScreen()
            true
        }
        targetVideoView.setVideoURI(videoUri)
    }

    // Also handle projects that opt out of Activity recreation for uiMode changes.
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        refreshSplashTheme()
    }

    private fun refreshSplashTheme() {
        if (isSplashCompleted || videoView == null) return
        val dark = AppThemePreferences.isDark(this)
        if (dark == splashDark) return
        if (splashPrepared) splashPosition = videoView?.currentPosition ?: splashPosition
        splashDark = dark
        splashPrepared = false
        videoView?.stopPlayback()
        splashContainer?.setBackgroundColor(splashBackgroundColor())
        playSplashVideo()
    }

    private fun openHomeScreen() {
        if (isSplashCompleted) return
        isSplashCompleted = true
        enableFullscreen()
        splashPrepared = false
        splashPosition = 0

        videoView?.apply {
            try {
                stopPlayback()
            } catch (e: Exception) {}
            setOnPreparedListener(null)
            setOnCompletionListener(null)
            setOnErrorListener(null)
            (parent as? ViewGroup)?.removeView(this)
        }
        videoView = null
        splashContainer = null

        checkCurrentAuthAndNavigate()
    }

    private var composeReady = false

    private fun checkCurrentAuthAndNavigate() {
        if (isLoading.value) return
        if (!composeReady) setupComposeRouting(Screen.Checking)
        lifecycleScope.launch {
            isLoading.value = true
            errorMessage.value = null
            try {
                currentScreen.value = when (AuthManager.checkAuthenticationState()) {
                    AuthState.SignedOut -> Screen.Login
                    AuthState.SignedInProfileIncomplete -> Screen.Profile
                    AuthState.Pending -> Screen.Pending
                    AuthState.Rejected -> Screen.Rejected
                    AuthState.SignedInComplete -> Screen.MainApp
                }
            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                val lang = getSharedPreferences("app_settings", MODE_PRIVATE).getString("pref_lang", "ar") ?: "ar"
                errorMessage.value = serviceErrorMessage(e, lang)
                currentScreen.value = Screen.Error
            } finally { isLoading.value = false }
        }
    }

    private fun signOut() {
        if (isLoading.value) return
        lifecycleScope.launch {
            isLoading.value = true
            try {
                SchoolMediaCache.clear(applicationContext)
                SchoolEngine.clearCurrentSession(applicationContext)
                AuthManager.signOutAndChangeAccount(this@MainActivity)
                errorMessage.value = null
                currentScreen.value = Screen.Login
            } finally { isLoading.value = false }
        }
    }

    private fun setupComposeRouting(initialScreen: Screen) {
        composeReady = true
        currentScreen.value = initialScreen
        setContent {
            NotificationPermissionPrompt()
            val screen by currentScreen
            val loading by isLoading
            val error by errorMessage
            val scope = rememberCoroutineScope()
            var learningAccess by rememberSaveable {mutableStateOf(false)}
            var autoOfferDismissed by rememberSaveable {mutableStateOf(false)}
            val limited=rememberLimitedConnection()
            LaunchedEffect(limited) {
                if(!limited)autoOfferDismissed=false
                if(limited && !autoOfferDismissed) {
                    kotlinx.coroutines.delay(3000)
                    learningAccess=true
                }
            }
            if(learningAccess) {
                LearningAccessScreen {
                    learningAccess=false
                    autoOfferDismissed=true
                    if(screen==Screen.Error || screen==Screen.Checking)checkCurrentAuthAndNavigate()
                }
            } else Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
            when (screen) {
                Screen.Splash, Screen.Checking -> {
                    BrandedLoadingScreen()
                }
                Screen.Login -> GoogleLoginScreen(
                    onGoogleSignInClick = {
                        if (!isLoading.value) scope.launch {
                            isLoading.value = true
                            errorMessage.value = null
                            var signedIn = false
                            try {
                                AuthManager.signIn(this@MainActivity)
                                signedIn = true
                            } catch (_: GetCredentialCancellationException) {
                                // User cancellation is not an authentication success or an error dialog.
                            } catch (e: CancellationException) { throw e } catch (e: Exception) {
                                val lang = getSharedPreferences("app_settings", MODE_PRIVATE)
                                    .getString("pref_lang", "ar") ?: "ar"
                                errorMessage.value = serviceErrorMessage(e, lang)
                            } finally { isLoading.value = false }
                            if (signedIn) checkCurrentAuthAndNavigate()
                        }
                    }, isLoading = loading, errorMessage = error, onPublicServices = {learningAccess=true}
                )
                Screen.Profile -> key(AuthManager.auth.currentUser?.uid) {
                    StudentProfileScreen(onSubmitted = { checkCurrentAuthAndNavigate() },
                        onSignOutClick = { signOut() })
                }
                Screen.Pending, Screen.Rejected, Screen.Error -> ApplicationStatusScreen(
                    rejected = screen == Screen.Rejected, failed = screen == Screen.Error,
                    loading = loading, detail = error, onRefresh = { checkCurrentAuthAndNavigate() },
                    onSignOut = { signOut() }
                )
                Screen.MainApp -> MainAppScreen(
                    userEmail = AuthManager.auth.currentUser?.email,
                    onSignOutClick = { signOut() },
                    onPublicServices = {learningAccess=true}
                )
            }
            }
            if(screen!=Screen.MainApp && screen!=Screen.Login) AppThemeWrapper { _,lang,_,_ ->
                androidx.compose.material3.Surface(color=androidx.compose.material3.MaterialTheme.colorScheme.background) {
                    TextButton(onClick={learningAccess=true},modifier=Modifier.fillMaxWidth().navigationBarsPadding()) {
                        Text(appText(lang,"الفضاء التعليمي المباشر","Accès éducatif direct","Direct learning access"))
                    }
                }
            }
            }
        }
    }

    private fun enableFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            if (isSplashCompleted) show(WindowInsetsCompat.Type.systemBars())
            else hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("KEY_SPLASH_COMPLETED", isSplashCompleted)
        outState.putInt("KEY_SPLASH_POSITION",
            if (!isSplashCompleted && splashPrepared) videoView?.currentPosition ?: splashPosition
            else splashPosition)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableFullscreen()
        }
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        enableFullscreen()
        refreshSplashTheme()
        if (!isSplashCompleted && splashPrepared) videoView?.start()
    }

    override fun onPause() {
        activityResumed = false
        if (!isSplashCompleted && splashPrepared) {
            videoView?.let {
                splashPosition = it.currentPosition
                it.pause()
            }
        }
        super.onPause()
    }

    override fun onDestroy() {
        videoView?.apply {
            setOnPreparedListener(null)
            setOnCompletionListener(null)
            setOnErrorListener(null)
            stopPlayback()
        }
        videoView = null
        super.onDestroy()
    }
}

sealed class Screen {
    object Splash : Screen()
    object Login : Screen()
    object Profile : Screen()
    object MainApp : Screen()
    object Checking : Screen()
    object Pending : Screen()
    object Rejected : Screen()
    object Error : Screen()
}

class ResponsiveVideoView(context: Context) : VideoView(context) {

    private var sourceVideoWidth = 0
    private var sourceVideoHeight = 0

    fun setVideoSize(width: Int, height: Int) {
        sourceVideoWidth = width
        sourceVideoHeight = height
        requestLayout()
    }

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int
    ) {
        val availableWidth = MeasureSpec.getSize(widthMeasureSpec)
        val availableHeight = MeasureSpec.getSize(heightMeasureSpec)

        if (sourceVideoWidth == 0 || sourceVideoHeight == 0 || availableWidth == 0 || availableHeight == 0) {
            setMeasuredDimension(availableWidth, availableHeight)
            return
        }

        val videoRatio = sourceVideoWidth.toFloat() / sourceVideoHeight.toFloat()
        val screenRatio = availableWidth.toFloat() / availableHeight.toFloat()

        val measuredWidth: Int
        val measuredHeight: Int

        if (videoRatio > screenRatio) {
            measuredHeight = availableHeight
            measuredWidth = (availableHeight * videoRatio).toInt()
        } else {
            measuredWidth = availableWidth
            measuredHeight = (availableWidth / videoRatio).toInt()
        }

        setMeasuredDimension(measuredWidth, measuredHeight)
    }
}
