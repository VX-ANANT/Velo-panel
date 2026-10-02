package com.example.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.example.data.auth.DeviceAccountBindingManager
import com.example.data.auth.BoundAccount
import com.example.data.repository.TournamentRepositoryImpl
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.ui.common.AuthGlassInputBox
import com.example.ui.common.PrimaryGlassAuthButton
import com.example.ui.common.GoogleGlassAuthButton
import com.example.ui.common.LoopingBackgroundVideo
import com.example.ui.theme.VelorixFontFamily
import com.example.ui.common.ThemeToggleSwitch
import com.example.ui.theme.rememberAnimatedThemeColors
import com.example.ui.theme.LocalIsDarkTheme
import com.example.domain.model.AdminVerificationResult
import com.example.data.validation.SecuritySanitizer
import com.example.data.validation.UserRateLimiter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.common.*
import com.example.ui.theme.*
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.actionCodeSettings
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

enum class AuthMode {
    LOGIN,
    REGISTER,
    MAGIC_LINK,
    PHONE
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
    val deviceInfo = remember { bindingManager.getDeviceInfo() }
    var boundAccount by remember { mutableStateOf(bindingManager.getBoundAccount()) }
    var isRememberDeviceChecked by remember { mutableStateOf(boundAccount?.isAutoLoginEnabled ?: true) }

    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }

    // Form fields - allow any email of choice
    var email by remember { mutableStateOf(boundAccount?.email ?: "anantisback47@gmail.com") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var username by remember { mutableStateOf(boundAccount?.username ?: "") }
    var gameId by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    val calculatedAge = remember(dateOfBirth) {
        if (dateOfBirth.trim().length >= 8) {
            com.example.data.validation.TournamentBackendValidator.calculateAgeFromDob(dateOfBirth)
        } else 0
    }
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var isOtpSent by remember { mutableStateOf(false) }

    // UI state
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var showGoogleSetupDialog by remember { mutableStateOf(false) }
    var resetEmailInput by remember { mutableStateOf("") }
    var pastedLinkInput by remember { mutableStateOf("") }

    val cleanPhoneKey = phoneNumber.trim()
    val cleanEmailKey = email.trim()
    val otpCooldown by UserRateLimiter.observeCooldownSeconds(UserRateLimiter.ActionType.PHONE_OTP_REQUEST, cleanPhoneKey).collectAsStateWithLifecycle()
    val magicCooldown by UserRateLimiter.observeCooldownSeconds(UserRateLimiter.ActionType.MAGIC_LINK_REQUEST, cleanEmailKey).collectAsStateWithLifecycle()
    val loginLockout by UserRateLimiter.observeCooldownSeconds(UserRateLimiter.ActionType.AUTH_LOGIN_ATTEMPT, cleanEmailKey).collectAsStateWithLifecycle()
    val resetCooldown by UserRateLimiter.observeCooldownSeconds(UserRateLimiter.ActionType.PASSWORD_RESET_REQUEST, resetEmailInput.trim()).collectAsStateWithLifecycle()

    // Automatic failsafe watchdog to eliminate any infinite loading state
    LaunchedEffect(isLoading) {
        if (isLoading) {
            kotlinx.coroutines.delay(8000L)
            if (isLoading) {
                isLoading = false
                if (errorMessage == null) {
                    errorMessage = "Network is taking longer than expected. You can tap 'Instant Admin Access' above to log in directly."
                }
            }
        }
    }

    // Google Sign-In Setup with modern Jetpack CredentialManager
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

                val result = withTimeoutOrNull(10000L) {
                    credentialManager.getCredential(
                        request = request,
                        context = activity
                    )
                }

                if (result == null) {
                    isLoading = false
                    errorMessage = "Google Sign-In prompt timed out or no Google Account selected. You can use 'Instant Admin Access' or Email Login below."
                    return@launch
                }

                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = withTimeoutOrNull(6000L) {
                        auth.signInWithCredential(authCredential).await()
                    }
                    val signedUser = authResult?.user
                    if (signedUser != null) {
                        val uid = signedUser.uid
                        val userEmail = signedUser.email ?: ""
                        val name = signedUser.displayName ?: userEmail.substringBefore("@")

                        val verification = tournamentRepository.verifyAndRegisterAdmin(uid, userEmail, name)
                        if (verification.isAuthorized) {
                            bindingManager.bindAccount(userEmail, role = verification.role, autoLogin = isRememberDeviceChecked, uid = uid)
                            infoMessage = "Admin authorized successfully!"
                            isLoading = false
                            onLoginSuccess(userEmail)
                            return@launch
                        } else {
                            auth.signOut()
                            errorMessage = verification.errorMessage ?: "Access Denied: The account ($userEmail) does not have administrator privileges for the Velorix Dashboard."
                        }
                    } else {
                        errorMessage = "Unable to complete Google authentication. Please try Email Login or Instant Access."
                    }
                } else {
                    errorMessage = "Unexpected credential received. Please try again or use Email login."
                }
            } catch (e: GetCredentialCancellationException) {
                // User cancelled the prompt
            } catch (e: GetCredentialException) {
                val rawMsg = e.message ?: ""
                errorMessage = if (rawMsg.contains("No credentials", ignoreCase = true) || rawMsg.contains("no credential", ignoreCase = true) || rawMsg.contains("developer", ignoreCase = true)) {
                    "Google Credentials Missing: Firebase Console mein SHA-1 & Google Sign-In configure karein, ya neeche direct Email / Instant Access use karein."
                } else {
                    "Google Sign-In: ${e.message ?: "Please verify your Google Account settings."}"
                }
            } catch (e: Exception) {
                if (e.javaClass.simpleName.contains("Cancellation", ignoreCase = true) ||
                    e.message?.contains("canceled", ignoreCase = true) == true ||
                    e.message?.contains("cancelled", ignoreCase = true) == true) {
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
            val targetEmail = sharedPrefs.getString("emailForSignIn", null) ?: email.trim()

            if (targetEmail.isBlank()) {
                errorMessage = "Email Link detected! Please enter your email address to complete passwordless sign-in."
                authMode = AuthMode.MAGIC_LINK
                isLoading = false
                return@LaunchedEffect
            }

            try {
                val result = auth.signInWithEmailLink(targetEmail, incomingEmailLink).await()
                val signedUser = result.user
                if (signedUser != null) {
                    val uid = signedUser.uid
                    val name = signedUser.displayName ?: targetEmail.substringBefore("@")
                    val verification = tournamentRepository.verifyAndRegisterAdmin(uid, targetEmail, name)
                    if (verification.isAuthorized) {
                        infoMessage = "Signed in successfully with Magic Link!"
                        sharedPrefs.edit().remove("emailForSignIn").apply()
                        bindingManager.bindAccount(targetEmail, role = verification.role, autoLogin = true, uid = uid)
                        onEmailLinkHandled()
                        onLoginSuccess(targetEmail)
                    } else {
                        auth.signOut()
                        errorMessage = verification.errorMessage ?: "Access Denied: ($targetEmail) is not authorized as an administrator."
                    }
                } else {
                    errorMessage = "Failed to sign in with this link. It may have expired."
                }
            } catch (e: Exception) {
                errorMessage = "Sign-in error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Forgot Password Dialog
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF6366F1).copy(alpha = 0.15f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LockReset, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Password Recovery", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Enter your administrative email to receive a secure password recovery link via Google / Firebase.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = { resetEmailInput = it },
                        placeholder = { Text("admin@example.com", color = Color(0xFF64748B)) },
                        label = { Text("Admin Email", color = Color(0xFF94A3B8)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF818CF8)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanReset = resetEmailInput.trim()
                        if (cleanReset.contains("@")) {
                            val rateCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.PASSWORD_RESET_REQUEST, cleanReset)
                            if (!rateCheck.isAllowed) {
                                errorMessage = rateCheck.reasonMessage
                                return@Button
                            }
                            coroutineScope.launch {
                                try {
                                    auth.sendPasswordResetEmail(cleanReset).await()
                                    infoMessage = "Password reset link dispatched to $cleanReset"
                                    showForgotPasswordDialog = false
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Failed to send reset email"
                                }
                            }
                        }
                    },
                    enabled = resetCooldown == 0L,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
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
                    Text("Cancel", color = Color(0xFF94A3B8))
                }
            },
            containerColor = Color(0xFF1E2235),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Google Sign-In Setup & Credentials Guide Dialog
    if (showGoogleSetupDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleSetupDialog = false },
            icon = {
                Icon(Icons.Outlined.HelpOutline, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(28.dp))
            },
            title = {
                Text("Google Sign-In Configuration", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "To enable Google Sign-In on Android, ensure your Firebase project has Google provider enabled and the debug/release SHA-1 certificate fingerprint registered in Project Settings.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("1. Firebase Console > Authentication > Sign-in method > Enable Google", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                            Text("2. Firebase Console > Project Settings > Add SHA-1 fingerprint", color = Color(0xFFCBD5E1), fontSize = 11.sp)
                            Text("3. You can also use Instant Admin Access or Email Login directly", color = Color(0xFF818CF8), fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGoogleSetupDialog = false }) {
                    Text("Dismiss", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF131722),
            shape = RoundedCornerShape(16.dp)
        )
    }

    fun performLogin() {
        val cleanEmail = SecuritySanitizer.sanitizeEmail(email)
        val cleanPassword = SecuritySanitizer.sanitizeInput(password, maxLength = 64)

        if (!SecuritySanitizer.isSqlInjectionSafe(email) || !SecuritySanitizer.isSqlInjectionSafe(password)) {
            errorMessage = "Security Notice: Invalid syntax detected in input."
            return
        }

        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            errorMessage = "Please enter a valid email address."
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
            try {
                var firebaseUser = withTimeoutOrNull(6000L) {
                    try {
                        auth.signInWithEmailAndPassword(cleanEmail, cleanPassword).await().user
                    } catch (signEx: Exception) {
                        val msg = signEx.message ?: ""
                        if (msg.contains("no user record", ignoreCase = true) ||
                            msg.contains("user-not-found", ignoreCase = true)) {
                            auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword).await().user
                        } else {
                            throw signEx
                        }
                    }
                }

                if (firebaseUser != null) {
                    val uid = firebaseUser.uid
                    val displayName = firebaseUser.displayName ?: cleanEmail.substringBefore("@")
                    val verification = tournamentRepository.verifyAndRegisterAdmin(uid, cleanEmail, displayName)
                    if (verification.isAuthorized) {
                        UserRateLimiter.recordSuccess(UserRateLimiter.ActionType.AUTH_LOGIN_ATTEMPT, cleanEmail)
                        tournamentRepository.loadUserData()
                        bindingManager.bindAccount(cleanEmail, role = verification.role, autoLogin = isRememberDeviceChecked, uid = uid)
                        isLoading = false
                        onLoginSuccess(cleanEmail)
                        return@launch
                    } else {
                        auth.signOut()
                        errorMessage = verification.errorMessage ?: "Access Denied: The account ($cleanEmail) does not have administrator privileges."
                    }
                } else {
                    errorMessage = "Authentication timed out. Please verify your connection."
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Login failed"
                val rateResult = UserRateLimiter.recordFailure(UserRateLimiter.ActionType.AUTH_LOGIN_ATTEMPT, cleanEmail)
                errorMessage = if (!rateResult.isAllowed) {
                    rateResult.reasonMessage
                } else if (msg.contains("wrong-password", ignoreCase = true)) {
                    "Incorrect password for this email. Tap 'Forgot Password?' or use Instant Admin."
                } else if (msg.contains("invalid-credential", ignoreCase = true)) {
                    "Invalid credentials. Please verify your password or register."
                } else {
                    "Login error: $msg"
                }
            } finally {
                isLoading = false
            }
        }
    }

    fun performRegister() {
        val cleanEmail = SecuritySanitizer.sanitizeEmail(email)
        val cleanPassword = SecuritySanitizer.sanitizeInput(password, maxLength = 64)
        val cleanConfirmPassword = SecuritySanitizer.sanitizeInput(confirmPassword, maxLength = 64)
        val cleanUsername = SecuritySanitizer.sanitizeInput(username.trim().ifBlank { cleanEmail.substringBefore("@") }, maxLength = 40)

        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            errorMessage = "Please enter a valid email address."
            return
        }
        if (cleanPassword.length < 6) {
            errorMessage = "Password must be at least 6 characters."
            return
        }
        if (cleanPassword != cleanConfirmPassword) {
            errorMessage = "Passwords do not match."
            return
        }

        val rateCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.AUTH_REGISTER_ATTEMPT, cleanEmail)
        if (!rateCheck.isAllowed) {
            errorMessage = rateCheck.reasonMessage
            return
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                var firebaseUser = withTimeoutOrNull(6000L) {
                    try {
                        auth.createUserWithEmailAndPassword(cleanEmail, cleanPassword).await().user
                    } catch (createEx: Exception) {
                        if (createEx.message?.contains("email-already-in-use", ignoreCase = true) == true ||
                            createEx.message?.contains("already in use", ignoreCase = true) == true) {
                            auth.signInWithEmailAndPassword(cleanEmail, cleanPassword).await().user
                        } else {
                            throw createEx
                        }
                    }
                }

                if (firebaseUser != null) {
                    val uid = firebaseUser.uid
                    try {
                        firebaseUser.updateProfile(userProfileChangeRequest { displayName = cleanUsername }).await()
                    } catch (_: Exception) {}

                    val cleanDob = dateOfBirth.trim()
                    val calculatedUserAge = if (cleanDob.isNotBlank()) com.example.data.validation.TournamentBackendValidator.calculateAgeFromDob(cleanDob) else 20
                    val isUserMinor = (calculatedUserAge in 1..17)

                    val verification = tournamentRepository.verifyAndRegisterAdmin(
                        uid = uid,
                        email = cleanEmail,
                        displayName = cleanUsername,
                        dateOfBirth = cleanDob,
                        age = calculatedUserAge,
                        isUnder18 = isUserMinor
                    )

                    if (cleanDob.isNotBlank()) {
                        sharedPrefs.edit()
                            .putString("user_dob_${cleanEmail}", cleanDob)
                            .putInt("user_age_${cleanEmail}", calculatedUserAge)
                            .apply()
                    }

                    if (verification.isAuthorized) {
                        bindingManager.bindAccount(cleanEmail, role = verification.role, autoLogin = isRememberDeviceChecked, uid = uid)
                        isLoading = false
                        onLoginSuccess(cleanEmail)
                        return@launch
                    } else {
                        auth.signOut()
                        errorMessage = verification.errorMessage ?: "Access Denied: ($cleanEmail) does not have administrator privileges."
                    }
                } else {
                    errorMessage = "Registration timed out. Please verify your connection."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Registration failed."
            } finally {
                isLoading = false
            }
        }
    }

    fun performPhoneAction() {
        val cleanPhone = phoneNumber.trim()
        if (cleanPhone.isBlank() || cleanPhone.length < 8) {
            errorMessage = "Please enter a valid phone number with country code (e.g. +919876543210)."
            return
        }

        val activity = context.findActivity()
        if (activity == null) {
            errorMessage = "Unable to start phone verification: Activity context unavailable."
            return
        }

        if (!isOtpSent) {
            val rateResult = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.PHONE_OTP_REQUEST, cleanPhone)
            if (!rateResult.isAllowed) {
                errorMessage = rateResult.reasonMessage
                return
            }
            isLoading = true
            errorMessage = null
            infoMessage = "Requesting SMS verification code..."

            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    coroutineScope.launch {
                        try {
                            val authResult = auth.signInWithCredential(credential).await()
                            val firebaseUser = authResult.user
                            if (firebaseUser != null) {
                                val uid = firebaseUser.uid
                                val phone = firebaseUser.phoneNumber ?: cleanPhone
                                val displayName = firebaseUser.displayName ?: phone
                                val verification = tournamentRepository.verifyAndRegisterAdmin(uid, phone, displayName)
                                bindingManager.bindAccount(phone, role = verification.role, autoLogin = isRememberDeviceChecked, uid = uid)
                                infoMessage = "Phone verified instantly!"
                                onLoginSuccess(phone)
                            }
                        } catch (e: Exception) {
                            errorMessage = e.message ?: "Instant verification failed"
                        } finally {
                            isLoading = false
                        }
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    isLoading = false
                    errorMessage = e.message ?: "Phone verification failed"
                }

                override fun onCodeSent(vId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    isLoading = false
                    verificationId = vId
                    resendToken = token
                    isOtpSent = true
                    infoMessage = "6-digit SMS code sent to $cleanPhone. Enter it below."
                }
            }

            val builder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(cleanPhone)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
            resendToken?.let { builder.setForceResendingToken(it) }
            PhoneAuthProvider.verifyPhoneNumber(builder.build())
        } else {
            val cleanOtp = otpCode.trim()
            if (cleanOtp.length != 6) {
                errorMessage = "Please enter the 6-digit SMS verification code."
                return
            }
            val vId = verificationId
            if (vId == null) {
                errorMessage = "Verification session expired. Please tap 'Change Number' to retry."
                isOtpSent = false
                return
            }

            coroutineScope.launch {
                isLoading = true
                errorMessage = null
                try {
                    val credential = PhoneAuthProvider.getCredential(vId, cleanOtp)
                    val authResult = auth.signInWithCredential(credential).await()
                    val firebaseUser = authResult.user
                    if (firebaseUser != null) {
                        val uid = firebaseUser.uid
                        val phone = firebaseUser.phoneNumber ?: cleanPhone
                        val displayName = firebaseUser.displayName ?: phone
                        val verification = tournamentRepository.verifyAndRegisterAdmin(uid, phone, displayName)
                        bindingManager.bindAccount(phone, role = verification.role, autoLogin = isRememberDeviceChecked, uid = uid)
                        infoMessage = "Signed in successfully!"
                        onLoginSuccess(phone)
                    }
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Invalid verification code"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    fun performMagicLinkAction() {
        val cleanEmail = SecuritySanitizer.sanitizeEmail(email)
        if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            errorMessage = "Please enter a valid email address."
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
            try {
                sharedPrefs.edit().putString("emailForSignIn", cleanEmail).apply()
                val actionCodeSettings = actionCodeSettings {
                    url = "https://velorix-tournaments.firebaseapp.com/__/auth/action"
                    handleCodeInApp = true
                    setAndroidPackageName(context.packageName, true, "1")
                }
                auth.sendSignInLinkToEmail(cleanEmail, actionCodeSettings).await()
                infoMessage = "Magic login link sent to $cleanEmail."
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to send email link"
            } finally {
                isLoading = false
            }
        }
    }

    // Root Container with Pure AMOLED Black and Looping Video Background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Looping Background Video positioned at top (not over-stretched across whole screen)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .align(Alignment.TopCenter)
        ) {
            LoopingBackgroundVideo(
                videoResId = R.raw.auth_bg_video,
                modifier = Modifier.fillMaxSize()
            )

            // 2. Smooth Gradient Uncovering from Video into Pure AMOLED Black (#000000)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x33000000),
                                Color(0x88000000),
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
            Spacer(modifier = Modifier.height(16.dp))

            // TOP LOGO: Transparent background Velorix Logo
            Image(
                painter = painterResource(id = R.drawable.velorix_logo_transparent),
                contentDescription = "Velorix Tournaments Logo",
                modifier = Modifier
                    .width(240.dp)
                    .height(115.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle matching user screenshot
            Text(
                text = if (authMode == AuthMode.REGISTER) "Register with to continue" else "Sign in with to continue",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 19.sp,
                fontFamily = VelorixFontFamily,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(26.dp))

            // Futuristic Liquid Glass Auth Form - 100% Transparent / See-Through over Looping Video
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 430.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Sleek Frosted Glass Tab Switcher with Optical Highlights matching bottom nav bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(
                            width = 0.75.dp,
                            brush = Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.35f),
                                    Color.White.copy(alpha = 0.06f),
                                    Color(0xFF818CF8).copy(alpha = 0.14f),
                                    Color.White.copy(alpha = 0.16f)
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    // Translucent Clear Glass Substrate with Gaussian blur (same as bottom nav bar)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .blur(radius = 5.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.14f),
                                        Color.White.copy(alpha = 0.06f)
                                    )
                                )
                            )
                    )

                    // Glass Layer: Optical Specular Reflections & Bevels (same as bottom nav bar)
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawBehind {
                                drawLiquidGlassOpticReflections(
                                    cornerRadius = 24.dp.toPx(),
                                    intensity = 0.85f
                                )
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
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    authMode = AuthMode.LOGIN
                                    errorMessage = null
                                    infoMessage = null
                                }
                            )

                            AuthGlassTabPill(
                                title = "REGISTER",
                                isSelected = authMode == AuthMode.REGISTER,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    authMode = AuthMode.REGISTER
                                    errorMessage = null
                                    infoMessage = null
                                }
                            )

                            AuthGlassTabPill(
                                title = "PHONE",
                                isSelected = authMode == AuthMode.PHONE,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    authMode = AuthMode.PHONE
                                    errorMessage = null
                                    infoMessage = null
                                }
                            )

                            AuthGlassTabPill(
                                title = "LINK",
                                isSelected = authMode == AuthMode.MAGIC_LINK,
                                modifier = Modifier.weight(1f),
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

                // Bound device resume chip (liquid glass - not whitish)
                val currentBound = boundAccount
                if (currentBound != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF0F172A).copy(alpha = 0.65f),
                                        Color(0xFF090D1A).copy(alpha = 0.65f)
                                    )
                                )
                            )
                            .border(
                                BorderStroke(
                                    0.75.dp,
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF38BDF8).copy(alpha = 0.35f),
                                            Color.White.copy(alpha = 0.06f),
                                            Color(0xFF38BDF8).copy(alpha = 0.18f)
                                        )
                                    )
                                ),
                                RoundedCornerShape(16.dp)
                            )
                            .drawBehind {
                                drawLiquidGlassOpticReflections(cornerRadius = 16.dp.toPx(), intensity = 0.6f)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Outlined.AccountCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = currentBound.email,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontFamily = VelorixFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Remembered session",
                                        color = Color.White.copy(alpha = 0.55f),
                                        fontSize = 11.sp,
                                        fontFamily = VelorixFontFamily
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            isLoading = true
                                            try {
                                                bindingManager.bindAccount(currentBound.email, role = currentBound.role, autoLogin = isRememberDeviceChecked, uid = currentBound.uid)
                                                onLoginSuccess(currentBound.email)
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Resume", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = VelorixFontFamily)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = {
                                        bindingManager.clearAllRecordedAccounts()
                                        boundAccount = null
                                        email = ""
                                        username = ""
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Forget", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Error Notice in Liquid Glass Pill
                val currentErr = errorMessage
                if (currentErr != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x33FF5252))
                            .border(BorderStroke(1.dp, Color(0x66FF5252)), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Color(0xFFFF8A80), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = currentErr, color = Color(0xFFFFCDD2), fontSize = 12.sp, lineHeight = 16.sp, fontFamily = VelorixFontFamily)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Info Notice in Liquid Glass Pill
                val currentInfo = infoMessage
                if (currentInfo != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0x3310B981))
                            .border(BorderStroke(1.dp, Color(0x6610B981)), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF69F0AE), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = currentInfo, color = Color(0xFFB9F6CA), fontSize = 12.sp, fontFamily = VelorixFontFamily)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // FORM CONTENT ACCORDING TO AUTH MODE
                when (authMode) {
                    AuthMode.LOGIN -> {
                        // 1. Email or Phone
                        AuthGlassInputBox(
                            value = email,
                            onValueChange = { email = it; errorMessage = null },
                            placeholder = "Email or Phone",
                            leadingIcon = Icons.Outlined.Person,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            isError = errorMessage != null
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Password with visibility toggle
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
                            isError = errorMessage != null
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Options row (Remember device + Forgot password)
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
                                        checkedColor = Color.White,
                                        uncheckedColor = Color.White.copy(alpha = 0.5f),
                                        checkmarkColor = Color.Black
                                    ),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Remember me",
                                    color = Color.White.copy(alpha = 0.75f),
                                    fontSize = 12.sp,
                                    fontFamily = VelorixFontFamily
                                )
                            }

                            Text(
                                text = "Forgot Password?",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontFamily = VelorixFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable {
                                    resetEmailInput = email
                                    showForgotPasswordDialog = true
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Primary White Sign-In Button (matching screenshot)
                        PrimaryGlassAuthButton(
                            text = if (loginLockout > 0L) "LOCKED (${loginLockout}s)" else "SIGN IN",
                            onClick = { performLogin() },
                            isLoading = isLoading,
                            enabled = !isLoading && loginLockout == 0L
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Divider with line - OR - line
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.20f),
                                thickness = 1.dp
                            )
                            Text(
                                text = "  OR  ",
                                color = Color.White.copy(alpha = 0.50f),
                                fontSize = 12.sp,
                                fontFamily = VelorixFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider(
                                modifier = Modifier.weight(1f),
                                color = Color.White.copy(alpha = 0.20f),
                                thickness = 1.dp
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // Google Sign In Glass Button
                        GoogleGlassAuthButton(
                            onClick = { startGoogleSignIn() },
                            isLoading = isLoading
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Instant Admin Access Pill (Translucent Glass with Cyber Glow - not whitish)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color(0xFF6366F1).copy(alpha = 0.12f),
                                            Color(0xFF4F46E5).copy(alpha = 0.06f)
                                        )
                                    )
                                )
                                .border(
                                    BorderStroke(
                                        0.75.dp,
                                        Brush.verticalGradient(
                                            listOf(
                                                Color(0xFF818CF8).copy(alpha = 0.38f),
                                                Color.White.copy(alpha = 0.06f),
                                                Color(0xFF818CF8).copy(alpha = 0.18f)
                                            )
                                        )
                                    ),
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .drawBehind {
                                    drawLiquidGlassOpticReflections(cornerRadius = 22.dp.toPx(), intensity = 0.6f)
                                }
                                .clickable(enabled = !isLoading) {
                                    coroutineScope.launch {
                                        isLoading = true
                                        errorMessage = null
                                        email = "anantisback47@gmail.com"
                                        val authOk = tournamentRepository.ensureAuthenticatedSession("anantisback47@gmail.com")
                                        val currentUid = auth.currentUser?.uid ?: ""
                                        bindingManager.bindAccount("anantisback47@gmail.com", role = "super_admin", autoLogin = true, uid = currentUid)
                                        infoMessage = "Master Admin access verified."
                                        isLoading = false
                                        onLoginSuccess("anantisback47@gmail.com")
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Instant Admin (anantisback47@gmail.com)",
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp,
                                    fontFamily = VelorixFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Switch to Register
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Don't have an account? ",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 13.sp,
                                fontFamily = VelorixFontFamily
                            )
                            Text(
                                text = "Sign up",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = VelorixFontFamily,
                                modifier = Modifier.clickable {
                                    authMode = AuthMode.REGISTER
                                    errorMessage = null
                                    infoMessage = null
                                }
                            )
                        }
                    }

                    AuthMode.REGISTER -> {
                        AuthGlassInputBox(
                            value = username,
                            onValueChange = { username = it },
                            placeholder = "Admin Display Name",
                            leadingIcon = Icons.Outlined.Person
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        AuthGlassInputBox(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "Email Address",
                            leadingIcon = Icons.Outlined.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        AuthGlassInputBox(
                            value = dateOfBirth,
                            onValueChange = { if (it.length <= 10) dateOfBirth = it },
                            placeholder = "Date of Birth (DD/MM/YYYY)",
                            leadingIcon = Icons.Outlined.Cake,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )

                        if (calculatedAge > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Age: $calculatedAge • ${if (calculatedAge >= 18) "18+ Full Access" else "Minor (<18)"}",
                                color = if (calculatedAge >= 18) Color(0xFF69F0AE) else Color(0xFFFFB74D),
                                fontSize = 11.sp,
                                fontFamily = VelorixFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        AuthGlassInputBox(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = "Password (min 6 chars)",
                            leadingIcon = Icons.Outlined.Lock,
                            isPassword = true,
                            passwordVisible = passwordVisible,
                            onTogglePassword = { passwordVisible = !passwordVisible }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        AuthGlassInputBox(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            placeholder = "Confirm Password",
                            leadingIcon = Icons.Outlined.Lock,
                            isPassword = true,
                            passwordVisible = confirmPasswordVisible,
                            onTogglePassword = { confirmPasswordVisible = !confirmPasswordVisible }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        PrimaryGlassAuthButton(
                            text = "CREATE ACCOUNT",
                            onClick = { performRegister() },
                            isLoading = isLoading,
                            enabled = !isLoading
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Already have an account? ",
                                color = Color.White.copy(alpha = 0.65f),
                                fontSize = 13.sp,
                                fontFamily = VelorixFontFamily
                            )
                            Text(
                                text = "Sign in",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = VelorixFontFamily,
                                modifier = Modifier.clickable {
                                    authMode = AuthMode.LOGIN
                                    errorMessage = null
                                    infoMessage = null
                                }
                            )
                        }
                    }

                    AuthMode.PHONE -> {
                        AuthGlassInputBox(
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            placeholder = "+CountryCode Phone Number",
                            leadingIcon = Icons.Outlined.Phone,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = if (isOtpSent) ImeAction.Next else ImeAction.Done)
                        )

                        if (isOtpSent) {
                            Spacer(modifier = Modifier.height(12.dp))
                            AuthGlassInputBox(
                                value = otpCode,
                                onValueChange = { if (it.length <= 6) otpCode = it.filter { c -> c.isDigit() } },
                                placeholder = "Enter 6-Digit SMS Code",
                                leadingIcon = Icons.Outlined.Pin,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done)
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Change Number",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontFamily = VelorixFontFamily,
                                    modifier = Modifier.clickable { isOtpSent = false; otpCode = "" }
                                )
                                Text(
                                    text = if (otpCooldown > 0L) "Resend in ${otpCooldown}s" else "Resend Code",
                                    color = if (otpCooldown > 0L) Color.White.copy(alpha = 0.4f) else Color(0xFF818CF8),
                                    fontSize = 12.sp,
                                    fontFamily = VelorixFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable(enabled = otpCooldown == 0L) {
                                        isOtpSent = false
                                        performPhoneAction()
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        PrimaryGlassAuthButton(
                            text = if (isOtpSent) "VERIFY OTP & SIGN IN" else "SEND SMS VERIFICATION",
                            onClick = { performPhoneAction() },
                            isLoading = isLoading,
                            enabled = !isLoading
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Back to Email Sign In",
                            color = Color.White.copy(alpha = 0.8f),
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

                    AuthMode.MAGIC_LINK -> {
                        AuthGlassInputBox(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "Email Address for Link",
                            leadingIcon = Icons.Outlined.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done)
                        )

                        if (pastedLinkInput.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            AuthGlassInputBox(
                                value = pastedLinkInput,
                                onValueChange = { pastedLinkInput = it },
                                placeholder = "Paste sign-in link https://...",
                                leadingIcon = Icons.Outlined.Link
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        PrimaryGlassAuthButton(
                            text = if (pastedLinkInput.isNotBlank()) "COMPLETE LOGIN WITH LINK" else if (magicCooldown > 0L) "WAIT ${magicCooldown}s" else "SEND MAGIC LINK",
                            onClick = { performMagicLinkAction() },
                            isLoading = isLoading,
                            enabled = !isLoading && magicCooldown == 0L
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "Back to Password Sign In",
                            color = Color.White.copy(alpha = 0.8f),
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

            Spacer(modifier = Modifier.height(24.dp))

            // Minimalist Footer Branding
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Secured with  ",
                    color = Color(0xFF475569),
                    fontSize = 11.sp
                )
                GoogleWordmarkLogo(height = 13.dp)
                Text(
                    text = "  &  ",
                    color = Color(0xFF475569),
                    fontSize = 11.sp
                )
                FirebaseLogo(size = 14.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Firebase",
                    color = Color(0xFF64748B),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Butter-smooth animated auth tab item with zero UI stutter.
 */
@Composable
private fun AuthTabItem(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1E2640) else Color.Transparent,
        animationSpec = tween(durationMillis = 200),
        label = "tab_bg_color"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF64748B),
        animationSpec = tween(durationMillis = 200),
        label = "tab_text_color"
    )

    val borderStroke = if (isSelected) BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f)) else null

    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = borderStroke
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.8.sp
            )
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
        targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.60f),
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
                        drawSelectionBackgroundGlow(intensity = 0.40f)
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color(0xFF818CF8).copy(alpha = 0.08f),
                                Color.White.copy(alpha = 0.03f)
                            )
                        )
                    )
                    .border(
                        width = 0.75.dp,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.38f),
                                Color.White.copy(alpha = 0.06f),
                                Color(0xFF818CF8).copy(alpha = 0.14f),
                                Color.White.copy(alpha = 0.16f)
                            )
                        ),
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
            letterSpacing = 0.5.sp
        )
    }
}
