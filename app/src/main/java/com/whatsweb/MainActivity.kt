package com.whatsweb

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.whatsweb.engine.WaWebChromeClient
import com.whatsweb.engine.WaWebConfig
import com.whatsweb.engine.WaWebViewClient
import com.whatsweb.security.BiometricLockHelper

class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var progressBar: LinearProgressIndicator
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var webView: WebView
    private lateinit var errorLayout: LinearLayout
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: MaterialButton
    private lateinit var lockOverlay: View
    private lateinit var btnUnlockBiometric: MaterialButton

    private var lastBackgroundTime: Long = 0L
    private var isAppLocked: Boolean = false
    private var isBiometricPromptActive: Boolean = false

    private var pendingFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var pendingPermissionRequest: PermissionRequest? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = pendingFilePathCallback
        pendingFilePathCallback = null
        if (callback == null) return@registerForActivityResult

        val results: Array<Uri>? = if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, data)
                ?: data?.data?.let { arrayOf(it) }
                ?: data?.clipData?.let { clipData ->
                    Array(clipData.itemCount) { clipData.getItemAt(it).uri }
                }
        } else {
            null
        }
        callback.onReceiveValue(results)
    }

    private val permissionRequestLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val request = pendingPermissionRequest
        pendingPermissionRequest = null
        if (request == null) return@registerForActivityResult

        val grantedResources = mutableListOf<String>()
        request.resources?.forEach { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> {
                    if (permissions[Manifest.permission.CAMERA] == true ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    ) {
                        grantedResources.add(resource)
                    }
                }
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> {
                    if (permissions[Manifest.permission.RECORD_AUDIO] == true ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    ) {
                        grantedResources.add(resource)
                    }
                }
                else -> {
                    grantedResources.add(resource)
                }
            }
        }

        if (grantedResources.isNotEmpty()) {
            request.grant(grantedResources.toTypedArray())
        } else {
            request.deny()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        swipeRefresh.isEnabled = false
        setupToolbar()
        setupBackPress()
        configureWebView()

        if (savedInstanceState != null) {
            lastBackgroundTime = savedInstanceState.getLong(KEY_LAST_BACKGROUND_TIME, 0L)
            isAppLocked = savedInstanceState.getBoolean(KEY_IS_APP_LOCKED, false)
            if (isAppLocked) {
                lockOverlay.visibility = View.VISIBLE
            }
            webView.restoreState(savedInstanceState)
        } else {
            if (isBiometricEnabled()) {
                isAppLocked = true
                lockOverlay.visibility = View.VISIBLE
            }
            webView.loadUrl(WaWebConfig.WHATSAPP_WEB_URL)
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.topToolbar)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        webView = findViewById(R.id.webView)
        errorLayout = findViewById(R.id.errorLayout)
        tvErrorMessage = findViewById(R.id.tvErrorMessage)
        btnRetry = findViewById(R.id.btnRetry)
        lockOverlay = findViewById(R.id.lockOverlay)
        btnUnlockBiometric = findViewById(R.id.btnUnlockBiometric)

        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }

        btnRetry.setOnClickListener {
            errorLayout.visibility = View.GONE
            swipeRefresh.visibility = View.VISIBLE
            webView.loadUrl(WaWebConfig.WHATSAPP_WEB_URL)
        }

        btnUnlockBiometric.setOnClickListener {
            showBiometricPrompt()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        toolbar.overflowIcon = ContextCompat.getDrawable(this, R.drawable.ic_more_vert)
    }

    private fun setupBackPress() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isAppLocked) {
                    finish()
                    return
                }
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun isBiometricEnabled(): Boolean {
        val prefs = getSharedPreferences(BiometricLockHelper.PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(BiometricLockHelper.PREF_BIOMETRIC_ENABLED, false)
    }

    private fun setBiometricEnabled(enabled: Boolean) {
        val prefs = getSharedPreferences(BiometricLockHelper.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(BiometricLockHelper.PREF_BIOMETRIC_ENABLED, enabled).apply()
    }

    private fun showBiometricPrompt() {
        if (isBiometricPromptActive) return
        isBiometricPromptActive = true

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isBiometricPromptActive = false
                isAppLocked = false
                lockOverlay.visibility = View.GONE
                lastBackgroundTime = 0L
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isBiometricPromptActive = false
                if (isAppLocked) {
                    lockOverlay.visibility = View.VISIBLE
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                if (isAppLocked) {
                    lockOverlay.visibility = View.VISIBLE
                }
            }
        })

        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            promptBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            promptBuilder.setDeviceCredentialAllowed(true)
        }

        val promptInfo = promptBuilder.build()
        biometricPrompt.authenticate(promptInfo)
    }

    private fun verifyAndEnableBiometric() {
        if (isBiometricPromptActive) return
        isBiometricPromptActive = true

        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isBiometricPromptActive = false
                setBiometricEnabled(true)
                Toast.makeText(this@MainActivity, R.string.biometric_enabled_toast, Toast.LENGTH_SHORT).show()
                invalidateOptionsMenu()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isBiometricPromptActive = false
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
            }
        })

        val promptBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.biometric_prompt_title))
            .setSubtitle(getString(R.string.biometric_prompt_subtitle))

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            promptBuilder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            promptBuilder.setDeviceCredentialAllowed(true)
        }

        prompt.authenticate(promptBuilder.build())
    }

    override fun onStart() {
        super.onStart()
        if (isBiometricPromptActive) return

        if (isAppLocked || BiometricLockHelper.shouldLock(isBiometricEnabled(), lastBackgroundTime, System.currentTimeMillis())) {
            isAppLocked = true
            lockOverlay.visibility = View.VISIBLE
            showBiometricPrompt()
        } else {
            isAppLocked = false
            lockOverlay.visibility = View.GONE
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBiometricEnabled() && !isBiometricPromptActive) {
            lastBackgroundTime = System.currentTimeMillis()
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        webView.settings.apply {
            userAgentString = WaWebConfig.DESKTOP_USER_AGENT
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            useWideViewPort = true
            loadWithOverviewMode = true
            loadsImagesAutomatically = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = true
            allowContentAccess = true
        }

        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = WaWebViewClient(
            onPageLoadingState = { isLoading ->
                if (isLoading) {
                    progressBar.visibility = View.VISIBLE
                    errorLayout.visibility = View.GONE
                    swipeRefresh.visibility = View.VISIBLE
                } else {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                }
            },
            onError = { _, _, _ ->
                progressBar.visibility = View.GONE
                swipeRefresh.isRefreshing = false
                swipeRefresh.visibility = View.GONE
                errorLayout.visibility = View.VISIBLE
                tvErrorMessage.text = getString(R.string.error_offline)
            }
        )

        webView.webChromeClient = WaWebChromeClient(
            onProgressChanged = { progress ->
                if (progress < 100) {
                    progressBar.visibility = View.VISIBLE
                    progressBar.progress = progress
                } else {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                }
            },
            onFileChooser = { callback, params ->
                handleFileChooser(callback, params)
            },
            onPermissionRequest = { request ->
                handlePermissionRequest(request)
            },
            onPermissionRequestCanceled = { request ->
                if (request == null || pendingPermissionRequest == request) {
                    pendingPermissionRequest = null
                }
            }
        )
    }

    private fun handleFileChooser(
        callback: ValueCallback<Array<Uri>>?,
        params: WebChromeClient.FileChooserParams?
    ): Boolean {
        pendingFilePathCallback?.onReceiveValue(null)
        pendingFilePathCallback = callback

        val intent = try {
            params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
        } catch (e: Exception) {
            Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
        }

        return try {
            fileChooserLauncher.launch(intent)
            true
        } catch (e: ActivityNotFoundException) {
            pendingFilePathCallback?.onReceiveValue(null)
            pendingFilePathCallback = null
            false
        } catch (e: Exception) {
            pendingFilePathCallback?.onReceiveValue(null)
            pendingFilePathCallback = null
            false
        }
    }

    private fun handlePermissionRequest(request: PermissionRequest?) {
        if (request == null) return
        val resources = request.resources ?: run {
            request.deny()
            return
        }

        val neededPermissions = mutableListOf<String>()
        for (res in resources) {
            if (res == PermissionRequest.RESOURCE_VIDEO_CAPTURE &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
            ) {
                neededPermissions.add(Manifest.permission.CAMERA)
            }
            if (res == PermissionRequest.RESOURCE_AUDIO_CAPTURE &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            ) {
                neededPermissions.add(Manifest.permission.RECORD_AUDIO)
            }
        }

        if (neededPermissions.isEmpty()) {
            request.grant(resources)
        } else {
            pendingPermissionRequest?.deny()
            pendingPermissionRequest = request
            permissionRequestLauncher.launch(neededPermissions.toTypedArray())
        }
    }

    private fun showClearSessionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_clear_session_title)
            .setMessage(R.string.dialog_clear_session_message)
            .setPositiveButton(R.string.dialog_clear_session_confirm) { _, _ ->
                WebStorage.getInstance().deleteAllData()
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies {
                    cookieManager.flush()
                    webView.clearCache(true)
                    webView.clearFormData()
                    webView.clearHistory()
                    webView.loadUrl(WaWebConfig.WHATSAPP_WEB_URL)
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_biometric_lock)?.isChecked = isBiometricEnabled()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                webView.reload()
                true
            }
            R.id.action_clear_session -> {
                showClearSessionDialog()
                true
            }
            R.id.action_biometric_lock -> {
                if (BiometricLockHelper.canAuthenticate(this)) {
                    if (!isBiometricEnabled()) {
                        verifyAndEnableBiometric()
                    } else {
                        setBiometricEnabled(false)
                        Toast.makeText(this, R.string.biometric_disabled_toast, Toast.LENGTH_SHORT).show()
                        invalidateOptionsMenu()
                    }
                } else {
                    Toast.makeText(this, R.string.biometric_not_supported, Toast.LENGTH_LONG).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
        webView.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
        outState.putLong(KEY_LAST_BACKGROUND_TIME, lastBackgroundTime)
        outState.putBoolean(KEY_IS_APP_LOCKED, isAppLocked)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        lastBackgroundTime = savedInstanceState.getLong(KEY_LAST_BACKGROUND_TIME, 0L)
        isAppLocked = savedInstanceState.getBoolean(KEY_IS_APP_LOCKED, false)
        if (isAppLocked) {
            lockOverlay.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        pendingFilePathCallback?.onReceiveValue(null)
        pendingFilePathCallback = null
        pendingPermissionRequest?.deny()
        pendingPermissionRequest = null
        webView.destroy()
        super.onDestroy()
    }

    companion object {
        private const val KEY_LAST_BACKGROUND_TIME = "last_background_time"
        private const val KEY_IS_APP_LOCKED = "is_app_locked"
    }
}
