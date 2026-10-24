package com.example.ui.components

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.util.DocFlowLegalConstants
import com.example.ui.theme.DocFlowPalette
import com.example.ui.theme.DocFlowTokens

/**
 * Centered legal caption for Login, Onboarding, and Consent screens:
 * "By continuing, you agree to our Terms of Service and Privacy Policy."
 * Each link is accent-blue, underlined on press, with a 48dp touch target.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DocFlowLegalConsentCaption(
    palette: DocFlowPalette,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val termsInteraction = remember { MutableInteractionSource() }
    val termsPressed by termsInteraction.collectIsPressedAsState()

    val privacyInteraction = remember { MutableInteractionSource() }
    val privacyPressed by privacyInteraction.collectIsPressedAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("legal_consent_caption_container"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "By continuing, you agree to our",
            fontSize = 13.sp,
            color = palette.textSecondary,
            textAlign = TextAlign.Center
        )
        FlowRow(
            horizontalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(
                        interactionSource = termsInteraction,
                        indication = null,
                        onClick = onOpenTerms
                    )
                    .padding(horizontal = 6.dp)
                    .testTag("legal_link_terms"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Terms of Service",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DocFlowTokens.AccentBlue,
                    textDecoration = if (termsPressed) TextDecoration.Underline else TextDecoration.None
                )
            }
            Text(
                text = "and",
                fontSize = 13.sp,
                color = palette.textSecondary,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable(
                        interactionSource = privacyInteraction,
                        indication = null,
                        onClick = onOpenPrivacy
                    )
                    .padding(horizontal = 6.dp)
                    .testTag("legal_link_privacy"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Privacy Policy",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DocFlowTokens.AccentBlue,
                    textDecoration = if (privacyPressed) TextDecoration.Underline else TextDecoration.None
                )
            }
        }
    }
}

/**
 * In-app legal viewer dialog that strictly enforces allow-listed URLs
 * (https://docflow.free.je/privacy/ and https://docflow.free.je/terms/),
 * tints the toolbar to the current theme, preserves underlying screen state on close,
 * and shows the bundled offline copy when offline or when online loading fails.
 */
@Composable
fun DocFlowInAppLegalViewerDialog(
    url: String,
    palette: DocFlowPalette,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isAllowListed = remember(url) { DocFlowLegalConstants.isAllowListedLegalUrl(url) }
    val isPrivacy = url.trim() == DocFlowLegalConstants.PRIVACY_POLICY_URL
    val docTitle = if (isPrivacy) "Privacy Policy" else "Terms of Service"

    val hasNetwork = remember {
        runCatching {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            val net = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(net)
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        }.getOrDefault(false)
    }

    var tryOnlineMode by remember { mutableStateOf(false) }
    var onlineLoadFailed by remember { mutableStateOf(!hasNetwork) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
                .testTag("in_app_legal_viewer_dialog")
        ) {
            // Themed Top Toolbar with Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .border(1.dp, palette.divider)
                    .padding(horizontal = DocFlowTokens.space2, vertical = DocFlowTokens.space2),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(DocFlowTokens.minTouchTarget)
                        .testTag("in_app_legal_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close legal viewer",
                        tint = palette.textPrimary
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = docTitle,
                        style = DocFlowTokens.rowTitleStyle,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = DocFlowTokens.AccentBlue,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = if (isAllowListed) url else "Blocked URL",
                            fontSize = 12.sp,
                            color = palette.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = {
                        tryOnlineMode = !tryOnlineMode
                        onlineLoadFailed = false
                    },
                    modifier = Modifier
                        .size(DocFlowTokens.minTouchTarget)
                        .testTag("in_app_legal_try_online_icon")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Try online version",
                        tint = DocFlowTokens.AccentBlue
                    )
                }
            }

            if (!isAllowListed) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(DocFlowTokens.space4),
                    contentAlignment = Alignment.Center
                ) {
                    DocFlowCard(palette = palette) {
                        Text(
                            text = "URL Not Allow-Listed",
                            style = DocFlowTokens.rowTitleStyle,
                            fontWeight = FontWeight.Bold,
                            color = DocFlowTokens.IconCircleRed
                        )
                        Spacer(modifier = Modifier.height(DocFlowTokens.space2))
                        Text(
                            text = "Only official DocFlow Privacy Policy and Terms of Service HTTPS links are permitted inside the in-app viewer.",
                            style = DocFlowTokens.bodyStyle,
                            color = palette.textSecondary
                        )
                    }
                }
            } else if (tryOnlineMode && !onlineLoadFailed && hasNetwork) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = false
                            settings.domStorageEnabled = false
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val target = request?.url?.toString() ?: ""
                                    return !DocFlowLegalConstants.isAllowListedLegalUrl(target)
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        onlineLoadFailed = true
                                    }
                                }
                            }
                            loadUrl(url)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )
            } else {
                // Bundled Offline Copy in shared flat style
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(DocFlowTokens.screenHorizontalMargin)
                ) {
                    Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(palette.surface)
                            .border(1.dp, palette.divider, RoundedCornerShape(16.dp))
                            .padding(DocFlowTokens.space3),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Offline copy, last updated ${DocFlowLegalConstants.LEGAL_LAST_UPDATED_DATE}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textPrimary
                            )
                            Text(
                                text = "Bundled in-app copy kept in sync with $url",
                                fontSize = 12.sp,
                                color = palette.textSecondary
                            )
                        }
                        DocFlowSecondaryButton(
                            text = "Try online version",
                            onClick = {
                                tryOnlineMode = true
                                onlineLoadFailed = !hasNetwork
                            },
                            palette = palette,
                            modifier = Modifier.testTag("in_app_legal_try_online_button")
                        )
                    }

                    Spacer(modifier = Modifier.height(DocFlowTokens.space3))

                    DocFlowCard(palette = palette) {
                        Text(
                            text = if (isPrivacy) {
                                DocFlowLegalConstants.OFFLINE_PRIVACY_POLICY_CONTENT
                            } else {
                                DocFlowLegalConstants.OFFLINE_TERMS_OF_SERVICE_CONTENT
                            },
                            style = DocFlowTokens.bodyStyle,
                            color = palette.textPrimary,
                            lineHeight = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(DocFlowTokens.space4))
                }
            }
        }
    }
}
