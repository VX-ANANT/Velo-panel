package com.example.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.auth.DeviceAccountBindingManager
import com.example.data.repository.TournamentRepositoryImpl
import com.example.data.validation.SecuritySanitizer
import com.example.data.validation.TournamentBackendValidator
import com.example.data.validation.UserRateLimiter
import com.example.ui.common.AuthGlassInputBox
import com.example.ui.common.FirebaseLogo
import com.example.ui.common.GoogleGlassAuthButton
import com.example.ui.common.GoogleWordmarkLogo
import com.example.ui.common.LoopingBackgroundVideo
import com.example.ui.common.PrimaryGlassAuthButton
import com.example.ui.common.drawLiquidGlassOpticReflections
import com.example.ui.common.drawSelectionBackgroundGlow
import com.example.ui.theme.VelorixFontFamily
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.actionCodeSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

enum class AuthMode {
    LOGIN,
    MAGIC_LINK
}

private fun Context.findActivity(): android.app.Activity? {
    var current = this
    while (current is android.content.ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}

private const val GOOGLE_SERVER_CLIENT_ID = "27931798964-h6fiau2df3i6e3049fontnctpsh06763.apps.googleusercontent.com"
private val LoginCrimson = Color(0xFFFF2E5B)
private val LoginSurface = Color(0xFF0C0C10)
private val LoginBorder = Color(0xFF1F1F28)
private val LoginTextMuted = Color(0xFF9A9AA6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    tournamentRepository: TournamentRepositoryImpl,
    incomingEmailLink: String? = null,
    onEmailLinkHandled: () -> Unit = {},
    onLoginSuccess: (String?) -> Unit
) {
    val auth = tournamentRepository.auth
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val sharedPrefs = remember { context.getSharedPreferences("VelorixPrefs", Context.MODE_PRIVATE) }
    val bindingManager = remember { DeviceAccountBindingManager(context) }

    val rememberedEmail = remember {
        bindingManager.getBoundAccount()?.email?.takeIf { TournamentBackendValidator.isAllowedAdminEmail(it) } ?: ""
    }
    var isRememberDeviceChecked by remember { mutableStateOf(true) }
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }

    var email by remember { mutableStateOf(rememberedEmail) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var pastedLinkInput by remember { mutableStateOf("") }

    val cleanEmailKey = email.trim().lowercase()
    val magicCooldown by UserRateLimiter.observeCooldownSeconds(UserRateLimiter.ActionType.MAGIC_LINK_REQUEST, cleanEmailKey).collectAsStateWithLifecycle()
    val loginLockout by UserRateLimiter.observeCooldownSeconds(UserRateLimiter.ActionType.AUTH_LOGIN_ATTEMPT, cleanEmailKey).collectAsStateWithLifecycle()
    val resetCooldown by UserRateLimiter.observeCooldownSeconds(UserRateLimiter.ActionType.PASSWORD_RESET_REQUEST, resetEmailInput.trim().lowercase()).collectAsStateWithLifecycle()

    // Ensure no stale unauthorized Firebase session lingers while on the login screen
    LaunchedEffect(Unit) {
        val current = auth.currentUser
        if (current != null && !TournamentBackendValidator.isAllowedAdminEmail(current.email)) {
            try {
                auth.signOut()
            } catch (_: Exception) {}
            bindingManager.clearAllRecordedAccounts()
            sharedPrefs.edit().remove("user_email").apply()
        }
    }

    // Failsafe watchdog to avoid hanging loading state
    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(12_000L)
            if (isLoading) {
                isLoading = false
                if (errorMessage == null) {
                    errorMessage = "Authentication request timed out. Please check your internet connection and try again."
                }
            }
        }
    }

    val credentialManager = remember { CredentialManager.create(context) }

    fun startGoogleSignIn() {
        val activity = context.findActivity()
        if (activity == null) {
            errorMessage = "Unable to start Google Sign-In: Activity context unavailable."
            return
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            infoMessage = null

            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(GOOGLE_SERVER_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = withTimeoutOrNull(12_000L) {
                    credentialManager.getCredential(
                        request = request,
                        context = activity
                    )
                }

                if (result == null) {
                    errorMessage = "Google Sign-In timed out or was dismissed. Please select an authorized Velorix Admin Google account."
                    return@launch
                }

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = withTimeoutOrNull(8_000L) {
                        auth.signInWithCredential(authCredential).await()
                    }
                    val signedUser = authResult?.user
                    val userEmail = signedUser?.email?.trim()?.lowercase() ?: ""

                    if (signedUser == null) {
                        errorMessage = "Unable to complete Google authentication. Please try again."
                        return@launch
                    }

                    if (!TournamentBackendValidator.isAllowedAdminEmail(userEmail)) {
                        try { auth.signOut() } catch (_: Exception) {}
                        bindingManager.clearAllRecordedAccounts()
                        errorMessage = "Access Denied: Only authorized Velorix administrators may sign in."
                        return@launch
                    }

                    val name = signedUser.displayName?.ifBlank { null } ?: userEmail.substringBefore("@")
                    val verification = tournamentRepository.verifyAndRegisterAdmin(signedUser.uid, userEmail, name)
                    if (verification.isAuthorized) {
                        bindingManager.bindAccount(userEmail, name, role = verification.role, autoLogin = isRememberDeviceChecked, uid = signedUser.uid)
                        infoMessage = "Administrator verified."
                        isLoading = false
                        onLoginSuccess(userEmail)
                        return@launch
                    } else {
                        try { auth.signOut() } catch (_: Exception) {}
                        errorMessage = verification.errorMessage ?: "Access Denied: Account is not authorized."
                    }
                } else {
                    errorMessage = "Unexpected Google credential received. Please try again."
                }
            } catch (_: GetCredentialCancellationException) {
                // User cancelled Google picker
            } catch (e: GetCredentialException) {
                errorMessage = "Google Sign-In unavailable: ${e.message ?: "Please verify Google Play Services or use Email & Password."}"
            } catch (e: Exception) {
                if (e.javaClass.simpleName.contains("Cancellation", ignoreCase = true) ||
                    e.message?.contains("canceled", ignoreCase = true) == true ||
                    e.message?.contains("cancelled", ignoreCase = true) == true
                ) {
                    // Ignore cancel
                } else {
                    errorMessage = "Sign-In error: ${e.localizedMessage ?: e.message}"
                }
            } finally {
                isLoading = false
            }
        }
    }

    // Handle Incoming Passwordless Email Link
    LaunchedEffect(incomingEmailLink) {
        if (incomingEmailLink != null && auth.isSignInWithEmailLink(incomingEmailLink)) {
            isLoading = true
            val targetEmail = (sharedPrefs.getString("emailForSignIn", null) ?: email).trim().lowercase()

            if (targetEmail.isBlank()) {
                errorMessage = "Magic Link detected. Enter your authorized admin email to complete sign-in."
                authMode = AuthMode.MAGIC_LINK
                isLoading = false
                return@LaunchedEffect
            }

            if (!TournamentBackendValidator.isAllowedAdminEmail(targetEmail)) {
                errorMessage = "Access Denied: Only authorized Velorix administrators may sign in."
                sharedPrefs.edit().remove("emailForSignIn").apply()
                onEmailLinkHandled()
                isLoading = false
                return@LaunchedEffect
            }

            try {
                val result = auth.signInWithEmailLink(targetEmail, incomingEmailLink).await()
                val signedUser = result.user
                val verifiedEmail = signedUser?.email?.trim()?.lowercase() ?: ""
                if (signedUser != null && TournamentBackendValidator.isAllowedAdminEmail(verifiedEmail)) {
                    val name = signedUser.displayName?.ifBlank { null } ?: verifiedEmail.substringBefore("@")
                    val verification = tournamentRepository.verifyAndRegisterAdmin(signedUser.uid, verifiedEmail, name)
                    if (verification.isAuthorized) {
                        infoMessage = "Signed in with Magic Link."
                        sharedPrefs.edit().remove("emailForSignIn").apply()
                        bindingManager.bindAccount(verifiedEmail, name, role = verification.role, autoLogin = isRememberDeviceChecked, uid = signedUser.uid)
                        onEmailLinkHandled()
                        onLoginSuccess(verifiedEmail)
                    } else {
                        try { auth.signOut() } catch (_: Exception) {}
                        errorMessage = verification.errorMessage ?: "Access Denied: Account is not authorized."
                    }
                } else {
                    try { auth.signOut() } catch (_: Exception) {}
                    errorMessage = "Access Denied: This sign-in link does not match an authorized admin account."
                }
            } catch (e: Exception) {
                errorMessage = "Magic Link sign-in failed: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Forgot Password Dialog (Restricted to allowlisted admin emails)
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockReset, contentDescription = null, tint = LoginCrimson, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Admin Password Recovery", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter your authorized Velorix administrator email to receive a password reset link via Firebase Auth.",
                        color = LoginTextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        placeholder = { Text("Authorized admin email", color = Color(0xFF646472)) },
                        label = { Text("Admin Email", color = LoginTextMuted) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = LoginCrimson) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth().testTag("reset_email_input"),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LoginCrimson,
                            unfocusedBorderColor = LoginBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanReset = SecuritySanitizer.sanitizeEmail(resetEmailInput).trim().lowercase()
                        if (!TournamentBackendValidator.isAllowedAdminEmail(cleanReset)) {
                            errorMessage = "Access Denied: Password recovery is only available for authorized Velorix administrators."
                            showForgotPasswordDialog = false
                            return@Button
                        }
                        val rateCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.PASSWORD_RESET_REQUEST, cleanReset)
                        if (!rateCheck.isAllowed) {
                            errorMessage = rateCheck.reasonMessage
                            return@Button
                        }
                        coroutineScope.launch {
                            try {
                                auth.sendPasswordResetEmail(cleanReset).await()
                                infoMessage = "Password reset link dispatched to $cleanReset."
                                showForgotPasswordDialog = false
                            } catch (e: Exception) {
                                errorMessage = e.message ?: "Failed to send reset email."
                            }
                        }
                    },
                    enabled = resetCooldown == 0L,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LoginCrimson),
                    modifier = Modifier.testTag("send_reset_link_button")
                ) {
                    Text(
                        text = if (resetCooldown > 0L) "Wait ${resetCooldown}s" else "Send Link",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("Cancel", color = LoginTextMuted)
                }
            },
            containerColor = LoginSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    fun performLogin() {
        val cleanEmail = SecuritySanitizer.sanitizeEmail(email).trim().lowercase()
        val cleanPassword = SecuritySanitizer.sanitizeInput(password, maxLength = 64)

        if (!SecuritySanitizer.isSqlInjectionSafe(email) || !SecuritySanitizer.isSqlInjectionSafe(password)) {
            errorMessage = "Security Notice: Invalid characters detected in input."
            return
        }

        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            errorMessage = "Please enter your administrator email address."
            return
        }

        if (!TournamentBackendValidator.isAllowedAdminEmail(cleanEmail)) {
            errorMessage = "Access Denied: Only authorized Velorix administrators may sign in."
            return
        }

        if (cleanPassword.length < 6) {
            errorMessage = "Password must be at least 6 characters."
            return
        }

        val canAttempt = UserRateLimiter.canExecute(UserRateLimiter.ActionType.AUTH_LOGIN_ATTEMPT, cleanEmail)
        if (!canAttempt.isAllowed) {
            errorMessage = canAttempt.reasonMessage
            return
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            infoMessage = null
            try {
                val firebaseUser = withTimeoutOrNull(8_000L) {
                    auth.signInWithEmailAndPassword(cleanEmail, cleanPassword).await().user
                }

                if (firebaseUser != null) {
                    val verifiedEmail = firebaseUser.email?.trim()?.lowercase() ?: ""
                    if (!TournamentBackendValidator.isAllowedAdminEmail(verifiedEmail)) {
                        try { auth.signOut() } catch (_: Exception) {}
                        errorMessage = "Access Denied: Only authorized Velorix administrators may sign in."
                        return@launch
                    }

                    val displayName = firebaseUser.displayName?.ifBlank { null } ?: verifiedEmail.substringBefore("@")
                    val verification = tournamentRepository.verifyAndRegisterAdmin(firebaseUser.uid, verifiedEmail, displayName)
                    if (verification.isAuthorized) {
                        UserRateLimiter.recordSuccess(UserRateLimiter.ActionType.AUTH_LOGIN_ATTEMPT, verifiedEmail)
                        tournamentRepository.loadUserData()
                        bindingManager.bindAccount(verifiedEmail, displayName, role = verification.role, autoLogin = isRememberDeviceChecked, uid = firebaseUser.uid)
                        isLoading = false
                        onLoginSuccess(verifiedEmail)
                        return@launch
                    } else {
                        try { auth.signOut() } catch (_: Exception) {}
                        errorMessage = verification.errorMessage ?: "Access Denied: Account is not authorized."
                    }
                } else {
                    errorMessage = "Authentication timed out. Please check your connection and try again."
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Authentication failed"
                val rateResult = UserRateLimiter.recordFailure(UserRateLimiter.ActionType.AUTH_LOGIN_ATTEMPT, cleanEmail)
                errorMessage = if (!rateResult.isAllowed) {
                    rateResult.reasonMessage
                } else if (
                    msg.contains("wrong-password", ignoreCase = true) ||
                    msg.contains("supplied auth credential is incorrect", ignoreCase = true) ||
                    msg.contains("invalid-credential", ignoreCase = true)
                ) {
                    "Incorrect email or password. If this admin account uses Google Sign-In, tap 'Continue with Google' below."
                } else {
                    "Sign-in failed: $msg"
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun performMagicLinkAction() {
        val cleanEmail = SecuritySanitizer.sanitizeEmail(email).trim().lowercase()
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            errorMessage = "Please enter your administrator email address."
            return
        }
        if (!TournamentBackendValidator.isAllowedAdminEmail(cleanEmail)) {
            errorMessage = "Access Denied: Only authorized Velorix administrators may sign in."
            return
        }

        if (pastedLinkInput.isNotBlank() && auth.isSignInWithEmailLink(pastedLinkInput.trim())) {
            coroutineScope.launch {
                isLoading = true
                errorMessage = null
                try {
                    val result = auth.signInWithEmailLink(cleanEmail, pastedLinkInput.trim()).await()
                    val signedUser = result.user
                    val verifiedEmail = signedUser?.email?.trim()?.lowercase() ?: ""
                    if (signedUser != null && TournamentBackendValidator.isAllowedAdminEmail(verifiedEmail)) {
                        val name = signedUser.displayName?.ifBlank { null } ?: verifiedEmail.substringBefore("@")
                        val verification = tournamentRepository.verifyAndRegisterAdmin(signedUser.uid, verifiedEmail, name)
                        if (verification.isAuthorized) {
                            bindingManager.bindAccount(verifiedEmail, name, role = verification.role, autoLogin = isRememberDeviceChecked, uid = signedUser.uid)
                            sharedPrefs.edit().remove("emailForSignIn").apply()
                            isLoading = false
                            onLoginSuccess(verifiedEmail)
                            return@launch
                        }
                    }
                    try { auth.signOut() } catch (_: Exception) {}
                    errorMessage = "Access Denied: Account is not authorized."
                } catch (e: Exception) {
                    errorMessage = "Magic Link sign-in failed: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
            return
        }

        val rateResult = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.MAGIC_LINK_REQUEST, cleanEmail)
        if (!rateResult.isAllowed) {
            errorMessage = rateResult.reasonMessage
            return
        }
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            infoMessage = null
            try {
                sharedPrefs.edit().putString("emailForSignIn", cleanEmail).apply()
                val actionCodeSettings = actionCodeSettings {
                    url = "https://velorix-tournaments.firebaseapp.com/__/auth/action"
                    handleCodeInApp = true
                    setAndroidPackageName(context.packageName, true, "1")
                }
                auth.sendSignInLinkToEmail(cleanEmail, actionCodeSettings).await()
                infoMessage = "Magic sign-in link sent to $cleanEmail. Open it from your inbox on this device."
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to send magic sign-in link."
            } finally {
                isLoading = false
            }
        }
    }

    // Pure #000000 AMOLED Container with Top Looping Hero Video
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .align(Alignment.TopCenter)
        ) {
            LoopingBackgroundVideo(
                videoResId = R.raw.auth_bg_video,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x44000000),
                                Color(0xAA000000),
                                Color.Black
                            )
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Image(
                painter = painterResource(id = R.drawable.velorix_logo_transparent),
                contentDescription = "Velorix Admin Logo",
                modifier = Modifier
                    .width(230.dp)
                    .height(110.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Authorized Admin Sign In",
                color = Color.White,
                fontSize = 20.sp,
                fontFamily = VelorixFontFamily,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
                color = LoginCrimson.copy(alpha = 0.12f),
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(0.5.dp, LoginCrimson.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = LoginCrimson,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RESTRICTED TO VERIFIED VELORIX ADMINS",
                        color = Color(0xFFFF8BA3),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        fontFamily = VelorixFontFamily
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 430.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Clean 2-tab V14 Frosted Switcher: PASSWORD & MAGIC LINK
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(23.dp))
                        .border(
                            width = 0.75.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.25f),
                                    Color.White.copy(alpha = 0.05f),
                                    LoginCrimson.copy(alpha = 0.22f)
                                )
                            ),
                            shape = RoundedCornerShape(23.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(radius = 5.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.10f),
                                        Color.White.copy(alpha = 0.04f)
                                    )
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBehind {
                                drawLiquidGlassOpticReflections(cornerRadius = 23.dp.toPx(), intensity = 0.7f)
                            }
                            .padding(horizontal = 4.dp, vertical = 3.5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuthGlassTabPill(
                                title = "SIGN IN",
                                isSelected = authMode == AuthMode.LOGIN,
                                modifier = Modifier.weight(1f).testTag("auth_tab_signin"),
                                onClick = {
                                    authMode = AuthMode.LOGIN
                                    errorMessage = null
                                    infoMessage = null
                                }
                            )
                            AuthGlassTabPill(
                                title = "MAGIC LINK",
                                isSelected = authMode == AuthMode.MAGIC_LINK,
                                modifier = Modifier.weight(1f).testTag("auth_tab_magic_link"),
                                onClick = {
                                    authMode = AuthMode.MAGIC_LINK
                                    errorMessage = null
                                    infoMessage = null
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Error Notice
                val currentErr = errorMessage
                if (currentErr != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x2EFF2E5B))
                            .border(BorderStroke(0.75.dp, LoginCrimson.copy(alpha = 0.55f)), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Color(0xFFFF6B8A), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = currentErr, color = Color(0xFFFFD6DF), fontSize = 12.sp, lineHeight = 16.sp, fontFamily = VelorixFontFamily)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Info Notice
                val currentInfo = infoMessage
                if (currentInfo != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x2E34D399))
                            .border(BorderStroke(0.75.dp, Color(0x8834D399)), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = currentInfo, color = Color(0xFFD1FAE5), fontSize = 12.sp, fontFamily = VelorixFontFamily)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                when (authMode) {
                    AuthMode.LOGIN -> {
                        AuthGlassInputBox(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            placeholder = "Authorized Admin Email",
                            leadingIcon = Icons.Outlined.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            isError = errorMessage != null,
                            modifier = Modifier.testTag("login_email_input")
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        AuthGlassInputBox(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            placeholder = "Password",
                            leadingIcon = Icons.Outlined.Lock,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            onTogglePassword = { passwordVisible = !passwordVisible },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); performLogin() }),
                            isError = errorMessage != null,
                            modifier = Modifier.testTag("login_password_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable {
                                    val newVal = !isRememberDeviceChecked
                                    isRememberDeviceChecked = newVal
                                    bindingManager.setAutoLoginEnabled(newVal)
                                }
                            ) {
                                Checkbox(
                                    checked = isRememberDeviceChecked,
                                    onCheckedChange = {
                                        isRememberDeviceChecked = it
                                        bindingManager.setAutoLoginEnabled(it)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = LoginCrimson,
                                        uncheckedColor = Color.White.copy(alpha = 0.45f),
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Keep session active",
                                    color = Color.White.copy(alpha = 0.78f),
                                    fontSize = 12.sp,
                                    fontFamily = VelorixFontFamily
                                )
                            }

                            Text(
                                text = "Forgot Password?",
                                color = LoginCrimson,
                                fontSize = 12.sp,
                                fontFamily = VelorixFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable {
                                        resetEmailInput = email
                                        showForgotPasswordDialog = true
                                    }
                                    .testTag("forgot_password_button")
                            )
                        }

                        Spacer(modifier = Modifier.height(22.dp))

                        PrimaryGlassAuthButton(
                            text = if (loginLockout > 0L) "LOCKED (${loginLockout}s)" else "SIGN IN",
                            onClick = { performLogin() },
                            isLoading = isLoading,
                            enabled = !isLoading && loginLockout == 0L,
                            modifier = Modifier.testTag("login_submit_button")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.16f),
                                thickness = 0.5.dp
                            )
                            Text(
                                text = "  OR  ",
                                color = Color.White.copy(alpha = 0.50f),
                                fontSize = 11.sp,
                                fontFamily = VelorixFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.16f),
                                thickness = 0.5.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        GoogleGlassAuthButton(
                            onClick = { startGoogleSignIn() },
                            isLoading = isLoading,
                            modifier = Modifier.testTag("google_signin_button")
                        )
                    }

                    AuthMode.MAGIC_LINK -> {
                        AuthGlassInputBox(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            placeholder = "Authorized Admin Email",
                            leadingIcon = Icons.Outlined.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                            modifier = Modifier.testTag("magic_link_email_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        AuthGlassInputBox(
                            value = pastedLinkInput,
                            onValueChange = { pastedLinkInput = it; errorMessage = null },
                            placeholder = "Optional: paste received sign-in link",
                            leadingIcon = Icons.Outlined.Link,
                            modifier = Modifier.testTag("magic_link_paste_input")
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        PrimaryGlassAuthButton(
                            text = if (pastedLinkInput.isNotBlank()) "VERIFY MAGIC LINK" else if (magicCooldown > 0L) "WAIT ${magicCooldown}s" else "SEND MAGIC LINK",
                            onClick = { performMagicLinkAction() },
                            isLoading = isLoading,
                            enabled = !isLoading && magicCooldown == 0L,
                            modifier = Modifier.testTag("send_magic_link_button")
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Back to Password Sign In",
                            color = LoginCrimson,
                            fontSize = 13.sp,
                            fontFamily = VelorixFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clickable {
                                authMode = AuthMode.LOGIN
                                errorMessage = null
                                infoMessage = null
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Secured with  ",
                    color = Color(0xFF5A5A68),
                    fontSize = 11.sp
                )
                GoogleWordmarkLogo(height = 13.dp)
                Text(
                    text = "  &  ",
                    color = Color(0xFF5A5A68),
                    fontSize = 11.sp
                )
                FirebaseLogo(size = 14.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Firebase Auth",
                    color = Color(0xFF7A7A8A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun AuthGlassTabPill(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.58f),
        animationSpec = tween(durationMillis = 200),
        label = "pill_text"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = if (isSelected) 3.dp else 4.dp, horizontal = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawSelectionBackgroundGlow(intensity = 0.35f)
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                LoginCrimson.copy(alpha = 0.28f),
                                LoginCrimson.copy(alpha = 0.14f)
                            )
                        )
                    )
                    .border(
                        width = 0.75.dp,
                        color = LoginCrimson.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }

        Text(
            text = title,
            color = textColor,
            fontSize = 11.5.sp,
            fontFamily = VelorixFontFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.6.sp
        )
    }
}
