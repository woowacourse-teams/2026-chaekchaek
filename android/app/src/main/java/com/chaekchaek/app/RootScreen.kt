package com.chaekchaek.app

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.data.ArchiveRepository
import com.chaekchaek.app.ui.archive.ArchiveScreen
import com.chaekchaek.app.ui.home.HomeScreen
import com.chaekchaek.app.ui.search.SearchScreen

private enum class RootTab(val label: String, @DrawableRes val icon: Int) {
  Home("홈", R.drawable.ic_tab_home),
  Discover("발견", R.drawable.ic_tab_discover),
  Shelf("내 서재", R.drawable.ic_tab_shelf),
}

@Composable
fun RootScreen(modifier: Modifier = Modifier) {
  val context = LocalContext.current
  val archiveRepository = remember { ArchiveRepository(context) }
  var selectedTab by rememberSaveable { mutableStateOf(RootTab.Home) }

  Box(modifier = modifier.fillMaxSize()) {
    val showBottomBar = selectedTab != RootTab.Discover
    val contentModifier =
      Modifier
        .fillMaxSize()
        .navigationBarsPadding()
        .then(if (showBottomBar) Modifier.padding(bottom = 56.dp) else Modifier)
    when (selectedTab) {
      RootTab.Home -> HomeScreen(
        modifier = contentModifier,
        onSearchBook = { selectedTab = RootTab.Discover },
      )
      RootTab.Discover ->
        SearchScreen(
          modifier = contentModifier,
          onBack = { selectedTab = RootTab.Home },
        )
      RootTab.Shelf -> ArchiveScreen(archiveRepository, contentModifier)
    }
    if (showBottomBar) {
      ChaekBottomBar(
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        modifier = Modifier.align(Alignment.BottomCenter),
      )
    }
  }
}

@Composable
private fun ChaekBottomBar(
  selectedTab: RootTab,
  onTabSelected: (RootTab) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surface)
      .navigationBarsPadding(),
  ) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
      modifier = Modifier.fillMaxWidth().height(55.dp).selectableGroup(),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      RootTab.entries.forEach { tab ->
        val selected = selectedTab == tab
        Box(
          modifier = Modifier
            .width(64.dp)
            .height(55.dp)
            .selectable(
              selected = selected,
              onClick = { onTabSelected(tab) },
              role = Role.Tab,
            ),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            painter = androidx.compose.ui.res.painterResource(tab.icon),
            contentDescription = tab.label,
            modifier = Modifier.size(24.dp),
            tint = if (selected) {
              MaterialTheme.colorScheme.onSurface
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
          )
        }
      }
    }
  }
}
