package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BedrockBorder
import com.example.ui.theme.BedrockCard
import com.example.ui.theme.DiamondCyan
import com.example.ui.theme.EmeraldLight
import com.example.ui.theme.GoldAmber
import com.example.ui.theme.RedstoneLight
import com.example.ui.theme.TextPrimary

@Composable
fun QuickActionChips(
    onChipClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val chips = listOf(
        Pair("👑 Tum kon ho?", "Tum kon ho aur tumhare owner kon hain?"),
        Pair("💥 Exit Code 1 Crash", "Minecraft crashed with Exit Code 1 during startup"),
        Pair("⏳ Mojang Screen Freeze", "The game freezes and crashes on the red Mojang loading screen"),
        Pair("📉 Low FPS & Lag", "How to optimize RAM and renderer for best FPS on my phone?"),
        Pair("☕ Java Version Mismatch", "Getting java.lang.UnsupportedClassVersionError class file version 65.0"),
        Pair("📦 Missing Fabric API", "Fabric loader says a mod requires version >=0.100 of fabric-api"),
        Pair("⚠️ Fix didn't work / Still crashing", "The fix didn't work, launcher is still crashing with the same problem")
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        chips.forEachIndexed { index, (label, prompt) ->
            val textColor = when {
                label.contains("Exit Code") -> RedstoneLight
                label.contains("Still crashing") -> GoldAmber
                label.contains("Low FPS") -> DiamondCyan
                else -> EmeraldLight
            }

            AssistChip(
                onClick = { onChipClicked(prompt) },
                label = {
                    Text(
                        text = label,
                        color = textColor,
                        fontSize = 11.sp
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = BedrockCard
                ),
                border = AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = BedrockBorder
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("quick_chip_$index")
            )
        }
    }
}
