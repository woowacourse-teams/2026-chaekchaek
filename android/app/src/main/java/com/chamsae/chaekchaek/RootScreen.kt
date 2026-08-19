package com.chamsae.chaekchaek

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.chamsae.chaekchaek.ui.home.HomeScreen
import com.chamsae.chaekchaek.ui.archive.ArchiveRoute
import com.chamsae.chaekchaek.ui.search.SearchRoute

private enum class RootTab(
  val label: String,
  @param:DrawableRes val selectedIcon: Int,
  @param:DrawableRes val unselectedIcon: Int,
) {
  Home("홈", R.drawable.ic_tab_home_filled, R.drawable.ic_tab_home_outline),
  Discover("발견", R.drawable.ic_tab_discover_filled, R.drawable.ic_tab_discover_outline),
  Shelf("내 서재", R.drawable.ic_tab_shelf_filled, R.drawable.ic_tab_shelf_outline),
}

@Composable
fun RootScreen(
  appContainer: AppContainer,
  modifier: Modifier = Modifier,
) {
  var selectedTab by rememberSaveable { mutableStateOf(RootTab.Home) }
  var archiveEditing by rememberSaveable { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize()) {
    val showBottomBar = selectedTab != RootTab.Discover && !(selectedTab == RootTab.Shelf && archiveEditing)
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
        SearchRoute(
          bookSearchRepository = appContainer.bookSearchRepository,
          libraryRepository = appContainer.libraryRepository,
          modifier = contentModifier,
          onBack = { selectedTab = RootTab.Home },
        )
      RootTab.Shelf ->
        ArchiveRoute(
          libraryRepository = appContainer.libraryRepository,
          editing = archiveEditing,
          onEditingChange = { archiveEditing = it },
          modifier = contentModifier,
        )
    }
    if (showBottomBar) {
      ChaekBottomBar(
        selectedTab = selectedTab,
        onTabSelected = {
          archiveEditing = false
          selectedTab = it
        },
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
            painter = androidx.compose.ui.res.painterResource(if (selected) tab.selectedIcon else tab.unselectedIcon),
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
