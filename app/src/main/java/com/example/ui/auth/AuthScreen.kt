package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EnhancedEncryption
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.data.util.DocFlowLegalConstants
import com.example.data.util.OnDeviceAiAndBackupEngine
import com.example.ui.components.DocFlowBrandLogo
import com.example.ui.components.DocFlowCard
import com.example.ui.components.DocFlowInAppLegalViewerDialog
import com.example.ui.components.DocFlowLegalConsentCaption
import com.example.ui.components.DocFlowPrimaryButton
import com.example.ui.components.DocFlowSecondaryButton
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AuthScreen(
    palette: DocFlowPalette = DocFlowTokens.palette(false),
    onSignInSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val webClientId = stringResource(R.string.default_web_client_id)

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var workspaceOwnerInput by rememberSaveable { mutableStateOf("") }
    var activeLegalViewerUrl by rememberSaveable { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        DocFlowCard(
            palette = palette,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DocFlowBrandLogo(
                    iconSize = 68.dp,
                    showWordmark = false
                )

                DocFlowBrandLogo(
                    iconSize = 0.dp,
                    showWordmark = true,
                    wordmarkSize = 28.sp
                )

                Text(
                    text = "Local-first document scanner, converter, Word editor, and note workspace • Stored on this device.",
                    style = DocFlowTokens.bodyStyle,
                    color = palette.textSecondary,
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = workspaceOwnerInput,
                    onValueChange = { workspaceOwnerInput = it },
                    label = { Text("Your name or email (optional)", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = DocFlowTokens.AccentBlue
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("login_workspace_owner_input")
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FlatFeatureRow(
                        icon = Icons.Default.DocumentScanner,
                        title = "CameraX Batch Scanner & PDF Conversions",
                        subtitle = "Image to PDF, PDF to Word, Word to PDF — stored on this device",
                        palette = palette
                    )
                    FlatFeatureRow(
                        icon = Icons.Default.OfflinePin,
                        title = "Local SQLite & FTS4 Instant Search",
                        subtitle = "Search documents, versions, and notes offline",
                        palette = palette
                    )
                    FlatFeatureRow(
                        icon = Icons.Default.EnhancedEncryption,
                        title = "Private Safe & Client-Side Note Links",
                        subtitle = "AES-256-GCM encrypted notes & local biometric lock",
                        palette = palette
                    )
                }

                if (errorMessage != null) {
                    Surface(
                        color = DocFlowTokens.IconCircleRed.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 13.sp,
                            color = DocFlowTokens.IconCircleRed,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                DocFlowPrimaryButton(
                    text = if (isLoading) "Signing in..." else "Continue on This Device",
                    enabled = !isLoading,
                    onClick = {
                        OnDeviceAiAndBackupEngine.acceptCurrentLegalPolicy(context)
                        onSignInSuccess()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("continue_local_device_button")
                )

                DocFlowSecondaryButton(
                    text = "Sign in with Google",
                    enabled = !isLoading,
                    palette = palette,
                    onClick = {
                        val activity = context.findActivity()
                        if (activity == null) {
                            errorMessage = "Unable to access Activity context for Sign-In."
                            return@DocFlowSecondaryButton
                        }
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            val result = signInWithGoogle(activity, webClientId)
                            isLoading = false
                            if (result.isSuccess) {
                                OnDeviceAiAndBackupEngine.acceptCurrentLegalPolicy(context)
                                onSignInSuccess()
                            } else {
                                val ex = result.exceptionOrNull()
                                if (ex !is GetCredentialCancellationException) {
                                    errorMessage = ex?.localizedMessage ?: "Sign-in failed. Please try again."
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("google_sign_in_button")
                )

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = DocFlowTokens.AccentBlue
                    )
                }

                // Under the primary button: centered caption (12-13sp, secondary text color)
                // "By continuing, you agree to our Terms of Service and Privacy Policy."
                DocFlowLegalConsentCaption(
                    palette = palette,
                    onOpenTerms = { activeLegalViewerUrl = DocFlowLegalConstants.TERMS_OF_SERVICE_URL },
                    onOpenPrivacy = { activeLegalViewerUrl = DocFlowLegalConstants.PRIVACY_POLICY_URL }
                )
            }
        }
    }

    activeLegalViewerUrl?.let { url ->
        DocFlowInAppLegalViewerDialog(
            url = url,
            palette = palette,
            onDismiss = { activeLegalViewerUrl = null }
        )
    }
}

@Composable
private fun FlatFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    palette: DocFlowPalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.background, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            color = DocFlowTokens.SelectedRowTintLight,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = DocFlowTokens.AccentBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = palette.textSecondary
            )
        }
    }
}

private suspend fun signInWithGoogle(
    activityContext: Context,
    webClientId: String
): Result<Unit> = runCatching {
    val credentialManager = CredentialManager.create(activityContext)

    val response: GetCredentialResponse = try {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(true)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()
        credentialManager.getCredential(activityContext, request)
    } catch (e: NoCredentialException) {
        Log.i("AuthScreen", "NoCredentialException on GetGoogleIdOption, falling back to GetSignInWithGoogleOption", e)
        val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(webClientId).build()
        val fallbackRequest = GetCredentialRequest.Builder()
            .addCredentialOption(signInWithGoogleOption)
            .build()
        credentialManager.getCredential(activityContext, fallbackRequest)
    } catch (e: GetCredentialException) {
        throw e
    }

    val credential = response.credential
    if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
        val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(firebaseCredential).await()
    } else {
        throw IllegalStateException("Received unexpected credential type: ${credential.type}")
    }
}
