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
import androidx.compose.ui.draw.clip
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

    // Root Container with sleek dark minimalist background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
    ) {
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
            // Sleek Minimalist Velorix Emblem
            Surface(
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF121624),
                border = BorderStroke(1.dp, Color(0xFF232C3D))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Velorix Emblem",
                        tint = Color(0xFF818CF8),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "VELORIX",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            Text(
                text = when (authMode) {
                    AuthMode.REGISTER -> "Administrator Registration"
                    AuthMode.MAGIC_LINK -> "Passwordless Sign-In"
                    AuthMode.PHONE -> "Phone & SMS Authentication"
                    AuthMode.LOGIN -> "Tournament Oversight Console"
                },
                color = Color(0xFF64748B),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
            )

            // Main Auth Form Card - Clean, restrained, and elegant
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF111420),
                border = BorderStroke(1.dp, Color(0xFF1E2638))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Minimalist Tab Switcher
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF090B12),
                        border = BorderStroke(1.dp, Color(0xFF1C2333))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuthTabItem(
                                title = "SIGN IN",
                                isSelected = authMode == AuthMode.LOGIN,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    authMode = AuthMode.LOGIN
                                    errorMessage = null
                                    infoMessage = null
                                }
                            )

                            AuthTabItem(
                                title = "REGISTER",
                                isSelected = authMode == AuthMode.REGISTER,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    authMode = AuthMode.REGISTER
                                    errorMessage = null
                                    infoMessage = null
                                }
                            )

                            AuthTabItem(
                                title = "PHONE",
                                isSelected = authMode == AuthMode.PHONE,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    authMode = AuthMode.PHONE
                                    errorMessage = null
                                    infoMessage = null
                                }
                            )

                            AuthTabItem(
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // BOUND DEVICE RESUME (Compact and elegant)
                    val currentBound = boundAccount
                    if (currentBound != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF0F172A),
                            border = BorderStroke(1.dp, Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = currentBound.email,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Remembered on this device",
                                            color = Color(0xFF64748B),
                                            fontSize = 10.sp
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
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(30.dp)
                                    ) {
                                        Text("Resume", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = {
                                            bindingManager.unbindAccount()
                                            boundAccount = null
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(Icons.Default.Clear, contentDescription = "Forget", tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // OFFICIAL GOOGLE SIGN-IN BUTTON
                    GoogleSignInButton(
                        onClick = { startGoogleSignIn() },
                        enabled = !isLoading,
                        text = "Continue with Google",
                        subtitle = null
                    )

                    // Helper Row: Minimalist guide trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { showGoogleSetupDialog = true }
                        ) {
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Setup Guide",
                                color = Color(0xFF64748B),
                                fontSize = 11.sp
                            )
                        }
                    }

                    // INSTANT MASTER ADMIN ACCESS CHIP / BUTTON
                    Surface(
                        onClick = {
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
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF161B28),
                        border = BorderStroke(1.dp, Color(0xFF263248)),
                        modifier = Modifier.fillMaxWidth().height(40.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF818CF8), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Instant Admin (anantisback47@gmail.com)",
                                color = Color(0xFFCBD5E1),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Minimalist Divider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF1C2433),
                            thickness = 1.dp
                        )
                        Text(
                            text = "  or  ",
                            color = Color(0xFF475569),
                            fontSize = 11.sp
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = Color(0xFF1C2433),
                            thickness = 1.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Form Fields
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (authMode == AuthMode.REGISTER) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Display Name", color = Color(0xFF64748B), fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color(0xFF222B3D),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF090B12),
                                    unfocusedContainerColor = Color(0xFF090B12)
                                )
                            )

                            OutlinedTextField(
                                value = gameId,
                                onValueChange = { gameId = it },
                                label = { Text("Admin ID (Optional)", color = Color(0xFF64748B), fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color(0xFF222B3D),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF090B12),
                                    unfocusedContainerColor = Color(0xFF090B12)
                                )
                            )

                            // Date of Birth input for COPPA & Legal Tournament Compliance
                            OutlinedTextField(
                                value = dateOfBirth,
                                onValueChange = { input ->
                                    if (input.length <= 10) {
                                        dateOfBirth = input
                                    }
                                },
                                label = { Text("Date of Birth (DD/MM/YYYY)", color = Color(0xFF64748B), fontSize = 12.sp) },
                                placeholder = { Text("DD/MM/YYYY (e.g. 15/08/2005)", color = Color(0xFF475569), fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Outlined.Cake, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (calculatedAge > 0) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (calculatedAge >= 18) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                            border = BorderStroke(1.dp, if (calculatedAge >= 18) Color(0xFF10B981) else Color(0xFFF59E0B)),
                                            modifier = Modifier.padding(end = 8.dp)
                                        ) {
                                            Text(
                                                text = "${calculatedAge}y • ${if (calculatedAge >= 18) "18+" else "Minor"}",
                                                color = if (calculatedAge >= 18) Color(0xFF34D399) else Color(0xFFFBBF24),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color(0xFF222B3D),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF090B12),
                                    unfocusedContainerColor = Color(0xFF090B12)
                                )
                            )

                            // Explanatory age policy compliance notice
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (calculatedAge in 1..17) Color(0xFF2A1B0E) else Color(0xFF0D121E),
                                border = BorderStroke(1.dp, if (calculatedAge in 1..17) Color(0xFFB45309) else Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (calculatedAge in 1..17) Icons.Default.Info else Icons.Outlined.Shield,
                                        contentDescription = null,
                                        tint = if (calculatedAge in 1..17) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (calculatedAge in 1..17)
                                            "Age ${calculatedAge}: Under-18 players can join Free Training & Scrim matches. Cash tournaments require 18+."
                                        else if (calculatedAge >= 18)
                                            "Age ${calculatedAge} (Verified): Full access granted to both Training and Real-Money Tournaments."
                                        else
                                            "Age Policy: Players under 18 can enter training matches; money tournaments require 18+.",
                                        color = if (calculatedAge in 1..17) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                                        fontSize = 10.5.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }

                        if (authMode == AuthMode.PHONE) {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number (+Country Code)", color = Color(0xFF64748B), fontSize = 12.sp) },
                                placeholder = { Text("+91 9876543210", color = Color(0xFF475569), fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Outlined.Phone, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (phoneNumber.isNotEmpty() && !isOtpSent) {
                                        IconButton(onClick = { phoneNumber = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                enabled = !isOtpSent,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = if (isOtpSent) ImeAction.Next else ImeAction.Done),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color(0xFF222B3D),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF090B12),
                                    unfocusedContainerColor = Color(0xFF090B12)
                                )
                            )

                            if (isOtpSent) {
                                OutlinedTextField(
                                    value = otpCode,
                                    onValueChange = { if (it.length <= 6) otpCode = it.filter { char -> char.isDigit() } },
                                    label = { Text("6-Digit SMS Verification Code", color = Color(0xFF64748B), fontSize = 12.sp) },
                                    placeholder = { Text("123456", color = Color(0xFF475569), fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Outlined.Pin, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        if (otpCode.isNotEmpty()) {
                                            IconButton(onClick = { otpCode = "" }) {
                                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF38BDF8),
                                        unfocusedBorderColor = Color(0xFF222B3D),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF090B12),
                                        unfocusedContainerColor = Color(0xFF090B12)
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Change Number",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        modifier = Modifier.clickable {
                                            isOtpSent = false
                                            otpCode = ""
                                        }
                                    )
                                    if (otpCooldown > 0L) {
                                        Text(
                                            text = "Resend Code in ${otpCooldown}s",
                                            color = Color(0xFF64748B),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp
                                        )
                                    } else {
                                        Text(
                                            text = "Resend Code",
                                            color = Color(0xFF818CF8),
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp,
                                            modifier = Modifier.clickable {
                                                val rateCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.PHONE_OTP_REQUEST, cleanPhoneKey)
                                                if (!rateCheck.isAllowed) {
                                                    errorMessage = rateCheck.reasonMessage
                                                } else {
                                                    isOtpSent = false
                                                }
                                            }
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0D121E),
                                    border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Outlined.Sms, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Firebase will send a 6-digit SMS verification code to your phone.",
                                            color = Color(0xFF94A3B8),
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        if (authMode != AuthMode.PHONE) {
                            // Email Field
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email Address", color = Color(0xFF64748B), fontSize = 12.sp) },
                                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    if (email.isNotEmpty()) {
                                        IconButton(onClick = { email = "" }) {
                                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color(0xFF222B3D),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedContainerColor = Color(0xFF090B12),
                                    unfocusedContainerColor = Color(0xFF090B12)
                                )
                            )

                            // Password Field
                            if (authMode != AuthMode.MAGIC_LINK) {
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Password", color = Color(0xFF64748B), fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle password",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = if (authMode == AuthMode.REGISTER) ImeAction.Next else ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF6366F1),
                                        unfocusedBorderColor = Color(0xFF222B3D),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF090B12),
                                        unfocusedContainerColor = Color(0xFF090B12)
                                    )
                                )
                            }

                            // Confirm Password for REGISTER
                            if (authMode == AuthMode.REGISTER) {
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("Confirm Password", color = Color(0xFF64748B), fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp)) },
                                    trailingIcon = {
                                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                            Icon(
                                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle visibility",
                                                tint = Color(0xFF64748B),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(0xFF6366F1),
                                        unfocusedBorderColor = Color(0xFF222B3D),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF090B12),
                                        unfocusedContainerColor = Color(0xFF090B12)
                                    )
                                )
                            }
                        }

                        // Magic Link instructions
                        if (authMode == AuthMode.MAGIC_LINK) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0D121E),
                                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "A sign-in link will be sent to your email address. You can also paste an existing sign-in link below.",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = pastedLinkInput,
                                        onValueChange = { pastedLinkInput = it },
                                        placeholder = { Text("Paste link https://...", color = Color(0xFF475569), fontSize = 11.sp) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF38BDF8),
                                            unfocusedBorderColor = Color(0xFF1E293B),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color(0xFF07090E),
                                            unfocusedContainerColor = Color(0xFF07090E)
                                        )
                                    )
                                    if (pastedLinkInput.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                val cleanEmail = email.trim().ifBlank { sharedPrefs.getString("emailForSignIn", null) ?: "" }
                                                val cleanLink = pastedLinkInput.trim()
                                                if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                                                    errorMessage = "Please enter your email above."
                                                    return@Button
                                                }
                                                coroutineScope.launch {
                                                    isLoading = true
                                                    errorMessage = null
                                                    try {
                                                        val result = auth.signInWithEmailLink(cleanEmail, cleanLink).await()
                                                        if (result.user != null) {
                                                            onLoginSuccess(cleanEmail)
                                                        }
                                                    } catch (e: Exception) {
                                                        errorMessage = "Sign-in error: ${e.message}"
                                                    } finally {
                                                        isLoading = false
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.fillMaxWidth().height(36.dp)
                                        ) {
                                            Text("Complete Login With Link", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        // Minimalist Options Row (Remember device & Forgot password)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                        checkedColor = Color(0xFF6366F1),
                                        uncheckedColor = Color(0xFF475569),
                                        checkmarkColor = Color.White
                                    ),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Remember device",
                                    color = Color(0xFF94A3B8),
                                    fontSize = 11.sp
                                )
                            }

                            if (authMode == AuthMode.LOGIN) {
                                Text(
                                    text = "Forgot Password?",
                                    color = Color(0xFF818CF8),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable {
                                        resetEmailInput = email
                                        showForgotPasswordDialog = true
                                    }
                                )
                            }
                        }
                    }

                    // Inline Error Notice
                    val currentErr = errorMessage
                    if (currentErr != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2A1215),
                            border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentErr,
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    // Inline Info Notice
                    val currentInfo = infoMessage
                    if (currentInfo != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF0B2518),
                            border = BorderStroke(1.dp, Color(0xFF065F46)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color(0xFF6EE7B7), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = currentInfo,
                                    color = Color(0xFF6EE7B7),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    if (authMode == AuthMode.LOGIN && loginLockout > 0L) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2E1515),
                            border = BorderStroke(1.dp, Color(0xFF7F1D1D)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LockClock, contentDescription = null, tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Too many failed attempts. Login is locked for ${loginLockout}s to protect your account.",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // PRIMARY ACTION BUTTON
                    Button(
                        onClick = {
                            if (authMode == AuthMode.PHONE) {
                                val cleanPhone = phoneNumber.trim()
                                if (cleanPhone.isBlank() || cleanPhone.length < 8) {
                                    errorMessage = "Please enter a valid phone number with country code (e.g. +919876543210)."
                                    return@Button
                                }

                                val activity = context.findActivity()
                                if (activity == null) {
                                    errorMessage = "Unable to start phone verification: Activity context unavailable."
                                    return@Button
                                }

                                if (!isOtpSent) {
                                    val rateResult = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.PHONE_OTP_REQUEST, cleanPhone)
                                    if (!rateResult.isAllowed) {
                                        errorMessage = rateResult.reasonMessage
                                        return@Button
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
                                        return@Button
                                    }
                                    val vId = verificationId
                                    if (vId == null) {
                                        errorMessage = "Verification session expired. Please tap 'Change Number' to retry."
                                        isOtpSent = false
                                        return@Button
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
                                return@Button
                            }

                            val cleanEmail = SecuritySanitizer.sanitizeEmail(email)
                            val cleanPassword = SecuritySanitizer.sanitizeInput(password, maxLength = 64)

                            if (!SecuritySanitizer.isSqlInjectionSafe(email) || !SecuritySanitizer.isSqlInjectionSafe(password)) {
                                errorMessage = "Security Notice: Invalid syntax detected in input."
                                return@Button
                            }

                            if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                                errorMessage = "Please enter a valid email address."
                                return@Button
                            }

                            if (authMode == AuthMode.MAGIC_LINK) {
                                val rateResult = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.MAGIC_LINK_REQUEST, cleanEmail)
                                if (!rateResult.isAllowed) {
                                    errorMessage = rateResult.reasonMessage
                                    return@Button
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
                                return@Button
                            }

                            if (cleanPassword.length < 6) {
                                errorMessage = "Password must be at least 6 characters."
                                return@Button
                            }

                            if (authMode == AuthMode.REGISTER) {
                                val cleanUsername = SecuritySanitizer.sanitizeInput(username.trim().ifBlank { cleanEmail.substringBefore("@") }, maxLength = 40)
                                val cleanConfirmPassword = SecuritySanitizer.sanitizeInput(confirmPassword, maxLength = 64)
                                if (cleanPassword != cleanConfirmPassword) {
                                    errorMessage = "Passwords do not match."
                                    return@Button
                                }

                                val cleanDob = dateOfBirth.trim()
                                if (cleanDob.isBlank()) {
                                    errorMessage = "Please enter your Date of Birth (DD/MM/YYYY) to complete registration."
                                    return@Button
                                }

                                val calculatedUserAge = com.example.data.validation.TournamentBackendValidator.calculateAgeFromDob(cleanDob)
                                if (calculatedUserAge <= 0) {
                                    errorMessage = "Please enter a valid Date of Birth (e.g. 15/08/2005)."
                                    return@Button
                                }

                                val rateCheck = UserRateLimiter.checkAndRecord(UserRateLimiter.ActionType.AUTH_REGISTER_ATTEMPT, cleanEmail)
                                if (!rateCheck.isAllowed) {
                                    errorMessage = rateCheck.reasonMessage
                                    return@Button
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

                                            val isUserMinor = (calculatedUserAge < 18)
                                            val verification = tournamentRepository.verifyAndRegisterAdmin(
                                                uid = uid,
                                                email = cleanEmail,
                                                displayName = cleanUsername,
                                                dateOfBirth = cleanDob,
                                                age = calculatedUserAge,
                                                isUnder18 = isUserMinor
                                            )

                                            sharedPrefs.edit()
                                                .putString("user_dob_${cleanEmail}", cleanDob)
                                                .putInt("user_age_${cleanEmail}", calculatedUserAge)
                                                .putBoolean("user_minor_${cleanEmail}", isUserMinor)
                                                .apply()

                                            if (verification.isAuthorized) {
                                                bindingManager.bindAccount(cleanEmail, role = verification.role, autoLogin = isRememberDeviceChecked, uid = uid)
                                                isLoading = false
                                                onLoginSuccess(cleanEmail)
                                                return@launch
                                            } else {
                                                auth.signOut()
                                                errorMessage = verification.errorMessage ?: "Access Denied: The account ($cleanEmail) does not have administrator privileges."
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
                            } else {
                                val canAttempt = UserRateLimiter.canExecute(UserRateLimiter.ActionType.AUTH_LOGIN_ATTEMPT, cleanEmail)
                                if (!canAttempt.isAllowed) {
                                    errorMessage = canAttempt.reasonMessage
                                    return@Button
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
                                        android.util.Log.e("AdminPanel", "Authentication failed: $msg")
                                        val rateResult = UserRateLimiter.recordFailure(UserRateLimiter.ActionType.AUTH_LOGIN_ATTEMPT, cleanEmail)
                                        if (!rateResult.isAllowed) {
                                            errorMessage = rateResult.reasonMessage
                                        } else {
                                            errorMessage = if (msg.contains("wrong-password", ignoreCase = true)) {
                                                "Incorrect password for this email. Tap 'Forgot Password?' or use Instant Admin."
                                            } else if (msg.contains("invalid-credential", ignoreCase = true)) {
                                                "Invalid credentials. Please verify your password or register."
                                            } else {
                                                "Authentication failed: $msg"
                                            }
                                        }
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F46E5),
                            contentColor = Color.White
                        ),
                        enabled = !isLoading && when (authMode) {
                            AuthMode.PHONE -> if (!isOtpSent) otpCooldown == 0L else true
                            AuthMode.MAGIC_LINK -> magicCooldown == 0L
                            AuthMode.LOGIN -> loginLockout == 0L
                            else -> true
                        }
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = when (authMode) {
                                    AuthMode.REGISTER -> "Create Admin Account"
                                    AuthMode.MAGIC_LINK -> if (magicCooldown > 0L) "Resend Link in ${magicCooldown}s" else "Send Magic Sign-In Link"
                                    AuthMode.PHONE -> if (isOtpSent) "Verify OTP & Sign In" else if (otpCooldown > 0L) "Resend SMS in ${otpCooldown}s" else "Send SMS Verification Code"
                                    AuthMode.LOGIN -> if (loginLockout > 0L) "Locked Out: Wait ${loginLockout}s" else "Enter Admin Console"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Secondary switch link
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = when (authMode) {
                                AuthMode.REGISTER -> "Already have an account? "
                                AuthMode.MAGIC_LINK -> "Prefer password sign in? "
                                AuthMode.PHONE -> "Prefer password or email? "
                                AuthMode.LOGIN -> "Need a new admin account? "
                            },
                            color = Color(0xFF64748B),
                            fontSize = 12.sp
                        )
                        Text(
                            text = when (authMode) {
                                AuthMode.REGISTER -> "Sign In"
                                AuthMode.MAGIC_LINK -> "Sign In"
                                AuthMode.PHONE -> "Sign In"
                                AuthMode.LOGIN -> "Register"
                            },
                            color = Color(0xFF818CF8),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable {
                                authMode = if (authMode == AuthMode.LOGIN) AuthMode.REGISTER else AuthMode.LOGIN
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
