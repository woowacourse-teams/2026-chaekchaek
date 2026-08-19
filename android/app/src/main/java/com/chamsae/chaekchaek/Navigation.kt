package com.chamsae.chaekchaek

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.chamsae.chaekchaek.ui.bookdetail.BookDetailRoute

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Main)
  val applicationContext = LocalContext.current.applicationContext
  val appContainer = remember(applicationContext) { AppContainer(applicationContext) }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider = entryProvider {
      entry<Main> {
        RootScreen(
          appContainer = appContainer,
          onBookClick = { backStack.add(BookDetailKey(it)) },
          modifier = Modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
          ),
        )
      }
      entry<BookDetailKey> { key ->
        BookDetailRoute(
          book = key.book,
          bookDetailRepository = appContainer.bookDetailRepository,
          libraryRepository = appContainer.libraryRepository,
          onBack = { backStack.removeLastOrNull() },
          modifier = Modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
          ),
        )
      }
    },
  )
}
