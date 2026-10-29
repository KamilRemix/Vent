package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.data.model.BotanicalMood
import com.example.ui.components.BotanicalHeroArtCard
import com.example.ui.components.BotanicalMoodIllustration
import com.example.ui.theme.NunitoFontFamily
import com.example.util.AppLocaleHolder
import com.example.util.BiometricAuthHelper
import com.example.util.LocalAppLanguage
import com.example.util.tr
import android.content.ContextWrapper
import android.content.MutableContextWrapper
import android.util.Base64
import com.example.util.trText
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import java.security.SecureRandom
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun generateSecureRandomNonce(byteLength: Int = 32): String {
    val randomBytes = ByteArray(byteLength)
    SecureRandom().nextBytes(randomBytes)
    return Base64.encodeToString(
        randomBytes,
        Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING
    )
}

/**
 * Triggers the official Android Credential Manager Sign-in bottom sheet (formerly One Tap API)
 * using Google's recommended two-step `GetGoogleIdOption` flow:
 * 1. First checks previously authorized accounts (`setFilterByAuthorizedAccounts(true)`, `setAutoSelectEnabled(true)`).
 * 2. If the user hasn't signed into the app before (`NoCredentialException`), immediately shows the
 *    Sign-in bottom sheet with all Google accounts on the device (`setFilterByAuthorizedAccounts(false)`),
 *    where the primary account is pre-selected with an account switcher arrow and "Continue as [Name]".
 * 3. Only if the bottom sheet is blocked by system cooldown on an explicit button click does it fall back
 *    to `GetSignInWithGoogleOption`.
 */
fun launchOneTapOrGoogleSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {},
    silentIfNoCredential: Boolean = false
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val lang = AppLocaleHolder.currentLanguage
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        if (!silentIfNoCredential) {
            onAuthError(
                trText(
                    lang,
                    "Google Sign-In configuration missing: default_web_client_id not found",
                    "Конфигурация Google Sign-In отсутствует: default_web_client_id не найден"
                )
            )
        } else {
            onAuthCancelled()
        }
        return
    }

    val activityContext = context.findActivity()?.let { MutableContextWrapper(it) } ?: context

    scope.launch {
        try {
            val result = if (silentIfNoCredential) {
                // Silent auto sign-in request with GetGoogleIdOption
                val authorizedGoogleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(true)
                    .setNonce(generateSecureRandomNonce())
                    .build()

                val authorizedRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(authorizedGoogleIdOption)
                    .build()

                credentialManager.getCredential(
                    request = authorizedRequest,
                    context = activityContext
                )
            } else {
                // Interactive sign-in request with GetSignInWithGoogleOption
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                    serverClientId = clientId
                ).setNonce(generateSecureRandomNonce()).build()

                val interactiveRequest = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()

                credentialManager.getCredential(
                    request = interactiveRequest,
                    context = activityContext
                )
            }

            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError(
                    trText(
                        lang,
                        "Unexpected credential type received",
                        "Неожиданный тип учетных данных"
                    )
                )
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In bottom sheet cancelled or dismissed: ${e.message}")
            onAuthCancelled()
        } catch (e: NoCredentialException) {
            Log.w("Auth", "No Google account available for bottom sheet: ${e.message}")
            if (silentIfNoCredential) {
                onAuthCancelled()
            } else {
                onAuthError(
                    trText(
                        lang,
                        "No Google account found on this device. Please sign in to a Google account in Android Settings.",
                        "На устройстве не найден аккаунт Google. Добавьте аккаунт Google в настройках Android."
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            if (silentIfNoCredential) {
                onAuthCancelled()
            } else {
                onAuthError(
                    e.localizedMessage ?: trText(
                        lang,
                        "Failed to sign in with Google",
                        "Не удалось выполнить вход через Google"
                    )
                )
            }
        }
    }
}

fun signOutFromGoogle(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@Composable
fun AuthWelcomeScreen(
    onAuthSuccess: () -> Unit,
    onSkipClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun triggerGoogleBottomSheet(silentIfNoCredential: Boolean) {
        if (isLoading) return
        errorMessage = null
        isLoading = true
        launchOneTapOrGoogleSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
            },
            onAuthError = { msg ->
                isLoading = false
                errorMessage = msg
            },
            scope = scope,
            onAuthCancelled = {
                isLoading = false
            },
            silentIfNoCredential = silentIfNoCredential
        )
    }

    // Automatically slide up the native Google Sign-in bottom sheet (One Tap API) when screen opens
    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser != null) {
            onAuthSuccess()
        } else {
            triggerGoogleBottomSheet(silentIfNoCredential = true)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.Center) {
                    BotanicalHeroArtCard(height = 170.dp)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BotanicalMoodIllustration(mood = BotanicalMood.CALM_LEAF, size = 48.dp)
                            BotanicalMoodIllustration(mood = BotanicalMood.BLOOM, size = 60.dp)
                            BotanicalMoodIllustration(mood = BotanicalMood.WARM_ROOT, size = 48.dp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "Vent",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = tr(
                        "A Quiet Space for Your Mind",
                        "Тихое пространство для твоих мыслей"
                    ),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = tr(
                        "Capture daily moments, track your emotional balance, and send sealed letters to your future self with private cloud sync.",
                        "Сохраняйте важные моменты дня, отслеживайте эмоциональный баланс и отправляйте запечатанные письма себе в будущее с облачной синхронизацией."
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Spa,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = tr(
                                    "Custom minimalist emotion icons — no generic emojis",
                                    "Авторские минималистичные иконки эмоций без шаблонных эмодзи"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Spa,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = tr(
                                    "Time capsule letters & On This Day retrospectives",
                                    "Письма себе в будущее и воспоминания «В этот день»"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Spa,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = tr(
                                    "Cloud sync & PIN / Fingerprint biometric protection",
                                    "Облачная синхронизация и защита по PIN / отпечатку пальца"
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                Button(
                    onClick = { triggerGoogleBottomSheet(silentIfNoCredential = false) },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("google_sign_in_button"),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tr("Opening Google Sign-In...", "Открываем вход Google..."),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tr("Continue with Google", "Войти через Google"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = onSkipClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("skip_auth_button")
                ) {
                    Text(
                        text = tr(
                            "Skip for now (Sign in later in Settings)",
                            "Пропустить (можно войти позже в Настройках)"
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BotanicalPinLockScreen(
    isBiometricEnabled: Boolean = true,
    onVerifyPin: (String) -> Boolean,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    var enteredPin by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    val biometricSupported = remember(context) {
        BiometricAuthHelper.isBiometricAvailable(context)
    }
    val canUseBiometric = biometricSupported && isBiometricEnabled

    // Automatically pop up the system Android Fingerprint sheet when the lock screen opens
    LaunchedEffect(canUseBiometric) {
        if (canUseBiometric) {
            BiometricAuthHelper.authenticate(
                context = context,
                lang = lang,
                onSuccess = onUnlocked
            )
        }
    }

    fun onDigitTap(digit: String) {
        if (enteredPin.length < 4) {
            hasError = false
            val next = enteredPin + digit
            enteredPin = next
            if (next.length == 4) {
                if (onVerifyPin(next)) {
                    onUnlocked()
                } else {
                    hasError = true
                    enteredPin = ""
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BotanicalMoodIllustration(mood = BotanicalMood.CALM_LEAF, size = 72.dp)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = tr("Journal Locked", "Дневник защищен"),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (hasError) {
                    tr("Incorrect PIN code. Please try again", "Неверный PIN-код. Попробуйте еще раз")
                } else if (canUseBiometric) {
                    tr(
                        "Enter 4-digit PIN or use fingerprint",
                        "Введите 4-значный PIN или используйте отпечаток пальца"
                    )
                } else {
                    tr("Enter your 4-digit PIN code to unlock", "Введите 4-значный PIN-код для входа")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasError) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 0 until 4) {
                    val filled = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (filled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(if (canUseBiometric) "BIO" else "", "0", "DEL")
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { key ->
                            when (key) {
                                "" -> Spacer(modifier = Modifier.size(68.dp))
                                "BIO" -> Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            CircleShape
                                        )
                                        .clickable {
                                            BiometricAuthHelper.authenticate(
                                                context = context,
                                                lang = lang,
                                                onSuccess = onUnlocked
                                            )
                                        }
                                        .testTag("pin_key_biometric"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Fingerprint,
                                        contentDescription = tr("Fingerprint", "Отпечаток пальца"),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                "DEL" -> Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            if (enteredPin.isNotEmpty()) {
                                                enteredPin = enteredPin.dropLast(1)
                                            }
                                        }
                                        .testTag("pin_key_backspace"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                        contentDescription = tr("Backspace", "Удалить"),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                else -> Box(
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            CircleShape
                                        )
                                        .clickable { onDigitTap(key) }
                                        .testTag("pin_key_$key"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        fontFamily = NunitoFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 24.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (canUseBiometric) {
                Spacer(modifier = Modifier.height(20.dp))
                FilledTonalButton(
                    onClick = {
                        BiometricAuthHelper.authenticate(
                            context = context,
                            lang = lang,
                            onSuccess = onUnlocked
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("biometric_unlock_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        tr(
                            "Unlock with Fingerprint",
                            "Войти по отпечатку пальца"
                        ),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun PinSetupDialog(
    isPinCurrentlyEnabled: Boolean,
    onDismiss: () -> Unit,
    onSavePin: (String) -> Unit,
    onDisablePin: () -> Unit
) {
    var pinInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }
    val enterFourDigitsMsg = tr("Please enter exactly 4 digits", "Введите ровно 4 цифры")

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = if (isPinCurrentlyEnabled) {
                    tr("Manage PIN Lock", "Настройка PIN-кода")
                } else {
                    tr("Set Up PIN Lock", "Установить PIN-код")
                },
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = tr(
                        "Protect your personal journal entries with a 4-digit PIN code and fingerprint biometrics.",
                        "Защитите личные записи дневника 4-значным PIN-кодом и входом по отпечатку пальца."
                    ),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { value ->
                        if (value.length <= 4 && value.all { it.isDigit() }) {
                            pinInput = value
                            errorText = null
                        }
                    },
                    label = { Text(tr("4-digit PIN code", "4-значный PIN-код")) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_setup_input")
                )
                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pinInput.length == 4) {
                        onSavePin(pinInput)
                        onDismiss()
                    } else {
                        errorText = enterFourDigitsMsg
                    }
                },
                modifier = Modifier.testTag("save_pin_button")
            ) {
                Text(tr("Save PIN", "Сохранить PIN"), maxLines = 1)
            }
        },
        dismissButton = {
            Row {
                if (isPinCurrentlyEnabled) {
                    TextButton(
                        onClick = {
                            onDisablePin()
                            onDismiss()
                        },
                        modifier = Modifier.testTag("disable_pin_button")
                    ) {
                        Text(
                            text = tr("Disable", "Отключить"),
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 1
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(tr("Cancel", "Отмена"), maxLines = 1)
                }
            }
        }
    )
}
