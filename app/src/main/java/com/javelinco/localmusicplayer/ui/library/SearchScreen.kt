package com.javelinco.localmusicplayer.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.javelinco.localmusicplayer.data.db.TrackEntity
import com.javelinco.localmusicplayer.library.SearchFilter

@Composable
fun SearchScreen(
    results: List<TrackEntity>,
    favorites: Set<String>,
    onQuery: (String, SearchFilter) -> Unit,
    onPlay: (TrackEntity) -> Unit,
    onFavorite: (TrackEntity, Boolean) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(SearchFilter.ALL) }
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; onQuery(query, filter) },
            label = { Text("Search local metadata and filenames") },
        )
        Row(Modifier.horizontalScroll(rememberScrollState())) {
            SearchFilter.entries.forEach { candidate ->
                FilterChip(
                    selected = candidate == filter,
                    onClick = { filter = candidate; onQuery(query, filter) },
                    label = { Text(candidate.name.replace('_', ' ')) },
                )
            }
        }
        TrackList(results, favorites, onPlay, onFavorite)
    }
}
