package com.chaekchaek.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.data.ArchiveRepository
import com.chaekchaek.app.ui.archive.ArchiveScreen
import com.chaekchaek.app.ui.search.SearchScreen

private enum class RootTab(val label: String, val mark: String) {
  Search("검색", "⌕"),
  Archive("내 서재", "▤"),
}

@Composable
fun RootScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val archiveRepository = remember { ArchiveRepository(context) }
  var selectedTab by rememberSaveable { mutableStateOf(RootTab.Search) }

  Scaffold(
    modifier = modifier,
    containerColor = MaterialTheme.colorScheme.background,
    bottomBar = {
      NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        RootTab.entries.forEach { tab ->
          NavigationBarItem(
            selected = selectedTab == tab,
            onClick = { selectedTab = tab },
            icon = { Text(tab.mark, style = MaterialTheme.typography.titleMedium) },
            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
            colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer),
          )
        }
      }
    },
  ) { padding ->
    when (selectedTab) {
      RootTab.Search -> SearchScreen(archiveRepository, Modifier.padding(padding))
      RootTab.Archive -> ArchiveScreen(archiveRepository, Modifier.padding(padding))
    }
  }
}
