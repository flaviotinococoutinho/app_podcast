package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.engine.LanguageCatalog
import com.example.engine.SupportedLanguage
import com.example.ui.theme.AmberPodcast
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LanguageSelectorRow(
    selectedLanguageCode: String,
    onLanguageSelected: (SupportedLanguage) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LanguageCatalog.languages.forEach { lang ->
            val isSelected = lang.code.equals(selectedLanguageCode, ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onLanguageSelected(lang) },
                label = {
                    Text(
                        text = "${lang.flag} ${lang.displayName}",
                        color = if (isSelected) TextPrimary else TextSecondary
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AmberPodcast.copy(alpha = 0.25f),
                    selectedLabelColor = TextPrimary,
                    containerColor = StudioSurfaceVariant.copy(alpha = 0.6f),
                    labelColor = TextSecondary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    selectedBorderColor = AmberPodcast,
                    borderColor = StudioSurfaceVariant
                )
            )
        }
    }
}
