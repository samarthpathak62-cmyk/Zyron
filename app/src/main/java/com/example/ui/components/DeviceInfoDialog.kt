package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.DeviceProfileEntity
import com.example.data.model.DeviceSpecs
import com.example.ui.theme.BedrockBlack
import com.example.ui.theme.BedrockBorder
import com.example.ui.theme.BedrockCard
import com.example.ui.theme.BedrockSurface
import com.example.ui.theme.BedrockSurfaceVariant
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.DiamondCyanLight
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.RedstoneLight
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoDialog(
    profile: DeviceProfileEntity,
    detectedSpecs: DeviceSpecs?,
    onDismiss: () -> Unit,
    onSaveProfile: (DeviceProfileEntity) -> Unit,
    onReDetectSpecs: () -> Unit
) {
    var mcVersion by remember { mutableStateOf(profile.minecraftVersion) }
    var modLoader by remember { mutableStateOf(profile.loader) }
    var installedMods by remember { mutableStateOf(profile.installedMods) }
    var selectedRenderer by remember { mutableStateOf(profile.renderer) }
    var allocatedRamMb by remember { mutableFloatStateOf(profile.allocatedRamMb.toFloat()) }
    var whatHappenedBefore by remember { mutableStateOf(profile.whatHappenedBefore) }

    val mcVersionsList = listOf("1.21.1", "1.21", "1.20.6", "1.20.4", "1.20.1", "1.19.4", "1.18.2", "1.16.5", "1.12.2")
    val loadersList = listOf("Fabric", "Forge", "NeoForge", "Quilt", "Vanilla")
    val renderersList = listOf(
        "Holy GL4ES (Universal Compatibility)",
        "GL4ES 1.1.5 (Fast & Stable)",
        "Zink (Mesa Turnip Vulkan - Snapdragon)",
        "VirGL (Compatibility Mode)",
        "ANGLE (Experimental)"
    )

    var mcExpanded by remember { mutableStateOf(false) }
    var loaderExpanded by remember { mutableStateOf(false) }
    var rendererExpanded by remember { mutableStateOf(false) }

    val maxSafeRamMb = ((profile.totalRamGb * 1024) - 1500).coerceAtLeast(1024.0).toFloat()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = BedrockSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, BedrockBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhoneAndroid,
                            contentDescription = "Device info",
                            tint = EmeraldGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Device & Launcher Profile",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hardware Detection Card
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BedrockCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BedrockBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Auto-Detected Device Hardware",
                                color = EmeraldLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedButton(
                                onClick = onReDetectSpecs,
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = EmeraldGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Re-Scan", color = EmeraldGreen, fontSize = 10.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "• Model: ${profile.manufacturer} ${profile.model}",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• OS: ${profile.androidVersion}",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• RAM: ${profile.totalRamGb} GB Total (${profile.availableRamGb} GB Avail)",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• Architecture: ${profile.cpuArchitecture}",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• Storage: ${profile.freeStorageGb} GB Free",
                            color = TextPrimary,
                            fontSize = 12.sp
                        )

                        if (detectedSpecs?.knownLimitations?.isNotBlank() == true) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(BedrockBlack)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = detectedSpecs.knownLimitations,
                                    color = GoldAmber,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Minecraft Version & Loader Selection
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // MC Version Dropdown
                    ExposedDropdownMenuBox(
                        expanded = mcExpanded,
                        onExpandedChange = { mcExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = mcVersion,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("MC Version", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mcExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = BedrockBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = BedrockBlack,
                                unfocusedContainerColor = BedrockBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .height(54.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = mcExpanded,
                            onDismissRequest = { mcExpanded = false },
                            modifier = Modifier.background(BedrockCard)
                        ) {
                            mcVersionsList.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(v, color = TextPrimary) },
                                    onClick = {
                                        mcVersion = v
                                        mcExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Mod Loader Dropdown
                    ExposedDropdownMenuBox(
                        expanded = loaderExpanded,
                        onExpandedChange = { loaderExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = modLoader,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Mod Loader", fontSize = 11.sp) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = loaderExpanded) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldGreen,
                                unfocusedBorderColor = BedrockBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedContainerColor = BedrockBlack,
                                unfocusedContainerColor = BedrockBlack
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .menuAnchor()
                                .height(54.dp)
                        )

                        ExposedDropdownMenu(
                            expanded = loaderExpanded,
                            onDismissRequest = { loaderExpanded = false },
                            modifier = Modifier.background(BedrockCard)
                        ) {
                            loadersList.forEach { l ->
                                DropdownMenuItem(
                                    text = { Text(l, color = TextPrimary) },
                                    onClick = {
                                        modLoader = l
                                        loaderExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Renderer selector
                ExposedDropdownMenuBox(
                    expanded = rendererExpanded,
                    onExpandedChange = { rendererExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedRenderer,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Graphic Renderer", fontSize = 11.sp) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rendererExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldGreen,
                            unfocusedBorderColor = BedrockBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = BedrockBlack,
                            unfocusedContainerColor = BedrockBlack
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = rendererExpanded,
                        onDismissRequest = { rendererExpanded = false },
                        modifier = Modifier.background(BedrockCard)
                    ) {
                        renderersList.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r, color = TextPrimary, fontSize = 13.sp) },
                                onClick = {
                                    selectedRenderer = r
                                    rendererExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // RAM Allocation Slider
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Allocated RAM",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${allocatedRamMb.roundToInt()} MB (${(allocatedRamMb / 1024.0 * 10).roundToInt() / 10.0} GB)",
                            color = if (allocatedRamMb > maxSafeRamMb) RedstoneLight else EmeraldLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Slider(
                        value = allocatedRamMb,
                        onValueChange = { allocatedRamMb = it },
                        valueRange = 1024f..(profile.totalRamGb.toFloat() * 1024f).coerceAtLeast(2048f),
                        steps = 7,
                        colors = SliderDefaults.colors(
                            thumbColor = EmeraldGreen,
                            activeTrackColor = EmeraldGreen,
                            inactiveTrackColor = BedrockBorder
                        )
                    )

                    if (allocatedRamMb > maxSafeRamMb) {
                        Text(
                            text = "⚠️ Warning: Allocating >${maxSafeRamMb.roundToInt()}MB on a ${profile.totalRamGb}GB phone may cause Android to kill Minecraft with SIGKILL!",
                            color = RedstoneLight,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Installed Mods Input
                OutlinedTextField(
                    value = installedMods,
                    onValueChange = { installedMods = it },
                    label = { Text("Installed Mods / Shaders", fontSize = 11.sp) },
                    placeholder = { Text("e.g. Sodium, Iris, Fabric API, Lithium...", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = BedrockBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = BedrockBlack,
                        unfocusedContainerColor = BedrockBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // What happened before error
                OutlinedTextField(
                    value = whatHappenedBefore,
                    onValueChange = { whatHappenedBefore = it },
                    label = { Text("What happened before error?", fontSize = 11.sp) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreen,
                        unfocusedBorderColor = BedrockBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = BedrockBlack,
                        unfocusedContainerColor = BedrockBlack
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val updated = profile.copy(
                                minecraftVersion = mcVersion,
                                loader = modLoader,
                                installedMods = installedMods,
                                renderer = selectedRenderer,
                                allocatedRamMb = allocatedRamMb.roundToInt(),
                                whatHappenedBefore = whatHappenedBefore
                            )
                            onSaveProfile(updated)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f).testTag("btn_save_device_profile")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
