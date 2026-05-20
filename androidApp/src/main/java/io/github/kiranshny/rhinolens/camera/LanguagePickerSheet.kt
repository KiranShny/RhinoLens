@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.kiranshny.rhinolens.camera

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kiranshny.rhinolens.shared.domain.Language
import io.github.kiranshny.rhinolens.shared.domain.Languages

@Composable
fun LanguagePickerSheet(
    title: String,
    allowAuto: Boolean,
    selected: Language?,
    onSelect: (Language?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        if (query.isBlank()) {
            Languages.all
        } else {
            val q = query.trim().lowercase()
            Languages.all.filter {
                it.displayName.lowercase().contains(q) ||
                    it.nativeName.lowercase().contains(q) ||
                    it.code.value.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            if (allowAuto && query.isBlank()) {
                item {
                    ListItem(
                        headlineContent = { Text("Auto-detect") },
                        leadingContent = { Text("Auto") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(null) },
                    )
                    HorizontalDivider()
                }
            }
            items(items = filtered, key = { it.code.value }) { lang ->
                val isSelected = selected?.code == lang.code
                ListItem(
                    headlineContent = { Text(lang.displayName) },
                    supportingContent = { Text(lang.nativeName) },
                    trailingContent = if (isSelected) {
                        { Text("Selected") }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(lang) },
                )
                HorizontalDivider()
            }
        }
    }
}
