package com.javelinco.localmusicplayer.ui.library

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun MetadataListScreen(groups: Map<String, Int>) {
    LazyColumn {
        items(groups.entries.sortedBy { it.key.lowercase() }, key = { it.key }) { group ->
            ListItem(
                headlineContent = { Text(group.key) },
                supportingContent = { Text("${group.value} tracks") },
            )
        }
    }
}
