package com.javelinco.localmusicplayer.ui.library

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.javelinco.localmusicplayer.data.db.NamedGroupSummary

@Composable
fun MetadataListScreen(groups: List<NamedGroupSummary>) {
    LazyColumn {
        items(groups, key = NamedGroupSummary::normalizedName) { group ->
            ListItem(
                headlineContent = { Text(group.displayName) },
                supportingContent = { Text("${group.trackCount} tracks") },
            )
        }
    }
}
