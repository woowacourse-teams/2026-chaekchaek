package com.chaekchaek.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.chaekchaek.app.data.ArchiveRepository
import com.chaekchaek.app.ui.archive.ArchiveScreen
import com.chaekchaek.app.ui.search.SearchScreen

private enum class RootTab(val label: String) {
  Search("검색"),
  Archive("아카이브"),
}

@Composable
fun RootScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val archiveRepository = remember { ArchiveRepository(context) }
  var selectedTab by rememberSaveable { mutableStateOf(RootTab.Search) }

  Scaffold(
    modifier = modifier,
    bottomBar = {
      NavigationBar {
        RootTab.entries.forEach { tab ->
          NavigationBarItem(
            selected = selectedTab == tab,
            onClick = { selectedTab = tab },
            icon = {},
            label = { Text(tab.label) },
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
