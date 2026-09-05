package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.components.AutoFixDialog
import com.example.ui.components.ChatMessageItem
import com.example.ui.components.DeviceInfoDialog
import com.example.ui.components.GoogleLoginDialog
import com.example.ui.components.GoogleLoginFirstScreen
import com.example.ui.components.LogAnalyzerSheet
import com.example.ui.components.QuickActionChips
import com.example.ui.components.ZyronTopBar
import com.example.ui.theme.BedrockBlack
import com.example.ui.theme.BedrockBorder
import com.example.ui.theme.BedrockCard
import com.example.ui.theme.BedrockSurface
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.RedstoneLight
import com.example.ui.theme.RedstoneRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ZyronHomeScreen(
    viewModel: ZyronViewModel,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isGuestMode by viewModel.isGuestModeActive.collectAsStateWithLifecycle()
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val profile by viewModel.deviceProfile.collectAsStateWithLifecycle()
    val detectedSpecs by viewModel.detectedDeviceSpecs.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val attachedLog by viewModel.attachedLog.collectAsStateWithLifecycle()
    val attachedLogTitle by viewModel.attachedLogTitle.collectAsStateWithLifecycle()
    val selectedAutoFix by viewModel.selectedAutoFix.collectAsStateWithLifecycle()

    var showAccountDialog by remember { mutableStateOf(false) }
    var showDeviceInfoDialog by remember { mutableStateOf(false) }
    var showLogAnalyzerSheet by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    // If user is neither logged in with Google nor has opted for guest session, show the first Google Login Screen
    if (currentUser == null && !isGuestMode) {
        GoogleLoginFirstScreen(
            onGoogleSignIn = {
                viewModel.signInWithGoogle()
            },
            onContinueAsGuest = {
                viewModel.continueAsGuest()
            }
        )
        return
    }

    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.snackbarEvent) {
        viewModel.snackbarEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val deviceSummary = "${profile.manufacturer} ${profile.model} • MC ${profile.minecraftVersion} (${profile.loader})"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ZyronTopBar(
                deviceSummary = deviceSummary,
                currentUser = currentUser,
                onOpenAccount = { showAccountDialog = true },
                onOpenDeviceInfo = { showDeviceInfoDialog = true },
                onOpenPresets = { showLogAnalyzerSheet = true },
                onClearChat = { viewModel.clearChat() }
            )
        },
        containerColor = BedrockBlack,
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Chat messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // Top Hero Card
                item {
                    ZyronHeroCard(
                        deviceSummary = deviceSummary,
                        onAttachLogClick = { showLogAnalyzerSheet = true },
                        onEditProfileClick = { showDeviceInfoDialog = true }
                    )
                }

                items(messages, key = { it.id }) { msg ->
                    ChatMessageItem(
                        message = msg,
                        onFollowUpClicked = {
                            viewModel.onUserSendMessage("The fix didn't work, launcher is still crashing with the same problem.")
                        },
                        onOpenAutoFix = if (selectedAutoFix != null && msg.role == "assistant") {
                            { /* Handled by auto fix state */ }
                        } else null
                    )
                }

                if (isGenerating) {
                    item {
                        ZyronGeneratingIndicator()
                    }
                }
            }

            // Active Attached Log Banner
            AnimatedVisibility(visible = attachedLog != null) {
                Surface(
                    color = BedrockCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = "Attached Log",
                                tint = EmeraldGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ready to analyze: ${attachedLogTitle ?: "Crash Log"} (${attachedLog?.lines()?.size ?: 0} lines)",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        IconButton(
                            onClick = { viewModel.clearAttachedLog() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove attached log",
                                tint = RedstoneLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Quick suggestion chips
            QuickActionChips(
                onChipClicked = { prompt ->
                    inputText = prompt
                    viewModel.onUserSendMessage(prompt)
                    inputText = ""
                }
            )

            // Bottom Input Bar
            Surface(
                color = BedrockSurface,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Attach Log Button
                    IconButton(
                        onClick = { showLogAnalyzerSheet = true },
                        modifier = Modifier.testTag("btn_attach_log")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attach error log",
                            tint = if (attachedLog != null) EmeraldGreen else DiamondCyan
                        )
                    }

                    // Input Text Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                "Describe crash or paste error...",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = BedrockBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = BedrockCard,
                            unfocusedContainerColor = BedrockCard
                        ),
                        shape = RoundedCornerShape(22.dp),
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_chat_text")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Send Button
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank() || attachedLog != null) {
                                val textToSend = inputText
                                inputText = ""
                                viewModel.onUserSendMessage(textToSend)
                            }
                        },
                        containerColor = EmeraldGreen,
                        contentColor = Color.Black,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("btn_send_message")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAccountDialog) {
        GoogleLoginDialog(
            currentUser = currentUser,
            onDismiss = { showAccountDialog = false },
            onGoogleSignIn = { email, name ->
                viewModel.signInWithGoogle(email, name)
            },
            onSignOut = {
                viewModel.signOut()
            }
        )
    }

    if (showDeviceInfoDialog) {
        DeviceInfoDialog(
            profile = profile,
            detectedSpecs = detectedSpecs,
            onDismiss = { showDeviceInfoDialog = false },
            onSaveProfile = { updated -> viewModel.updateProfile(updated) },
            onReDetectSpecs = { viewModel.detectDeviceSpecs() }
        )
    }

    if (showLogAnalyzerSheet) {
        LogAnalyzerSheet(
            onDismiss = { showLogAnalyzerSheet = false },
            onLogAttached = { title, content ->
                viewModel.setAttachedLog(title, content)
            },
            onPresetSelected = { preset ->
                viewModel.loadPreset(preset)
            }
        )
    }

    selectedAutoFix?.let { action ->
        AutoFixDialog(
            action = action,
            onConfirm = { act -> viewModel.applyAutoFix(act) },
            onDismiss = { viewModel.dismissAutoFix() }
        )
    }
}

@Composable
fun ZyronHeroCard(
    deviceSummary: String,
    onAttachLogClick: () -> Unit,
    onEditProfileClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BedrockCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BedrockBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Column {
            // Hero Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.zyron_banner),
                    contentDescription = "Zyron AI Tech Workshop Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(Color.Transparent, BedrockCard.copy(alpha = 0.95f))
                            )
                        )
                )

                // Overlay Text
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Zyron Launcher Diagnostic Console",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Real-time Minecraft root-cause analysis & auto-fixer",
                        color = EmeraldLight,
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onEditProfileClick() }
                ) {
                    Text(
                        text = "⚙️ Configure Setup",
                        color = DiamondCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onAttachLogClick() }
                ) {
                    Text(
                        text = "📋 Load Test Logs",
                        color = EmeraldLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ZyronGeneratingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            color = EmeraldGreen,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Zyron AI is reading stack traces and analyzing dependencies...",
            color = TextSecondary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
