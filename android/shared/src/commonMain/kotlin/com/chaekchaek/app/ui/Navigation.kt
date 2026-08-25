package com.chaekchaek.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.auth.AuthViewModel
import com.chaekchaek.app.data.remote.BookDetailRemoteRepository
import com.chaekchaek.app.data.remote.BookSearchRemoteRepository
import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import com.chaekchaek.app.data.remote.PopularBooksRemoteRepository
import com.chaekchaek.app.presentation.home.HomeViewModel
import com.chaekchaek.app.ui.archive.ArchiveViewModel
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.bookdetail.BookDetailAuthenticatedAction
import com.chaekchaek.app.ui.bookdetail.BookDetailScreen
import com.chaekchaek.app.ui.bookdetail.BookDetailViewModel
import com.chaekchaek.app.ui.common.LoginRequiredSheet
import com.chaekchaek.app.ui.home.LocalRemoteBookCover
import com.chaekchaek.app.ui.register.BookRegistrationViewModel
import com.chaekchaek.app.ui.search.SearchViewModel
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.time.Clock

@Serializable
private data object Root : NavKey

@Serializable
private data class BookDetailKey(val book: BookDetailArgs) : NavKey

private val navigationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Root::class, Root.serializer())
            subclass(BookDetailKey::class, BookDetailKey.serializer())
        }
    }
}

@Composable
internal fun AppNavigation(authPlatform: AuthPlatformCallbacks) {
    val authViewModel = remember(authPlatform) { AuthViewModel(authPlatform) }
    DisposableEffect(authViewModel) { onDispose(authViewModel::close) }
    val authTokens by authViewModel.tokens.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val homeViewModel = remember { HomeViewModel(PopularBooksRemoteRepository(), Clock.System) }
    val libraryRepository = remember { LibraryRemoteRepository() }
    val registrationViewModel = remember { BookRegistrationViewModel(libraryRepository) }
    val archiveViewModel = remember { ArchiveViewModel(libraryRepository) }
    val searchViewModel = remember(registrationViewModel, authViewModel) {
        SearchViewModel(
            bookSearchRepository = BookSearchRemoteRepository(),
            registerBook = { registrationViewModel.register(it) },
            isSignedIn = { authViewModel.tokens.value != null },
        )
    }
    val detailRepository = remember { BookDetailRemoteRepository() }
    val backStack = rememberNavBackStack(navigationConfig, Root)
    val safeContent = Modifier.windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    )

    CompositionLocalProvider(
        LocalRemoteBookCover provides { url, description, modifier ->
            RemoteBookImage(url, description, modifier)
        },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<Root> {
                    RootScreen(
                        homeViewModel = homeViewModel,
                        searchViewModel = searchViewModel,
                        registrationViewModel = registrationViewModel,
                        archiveViewModel = archiveViewModel,
                        authViewModel = authViewModel,
                        onBookClick = { backStack.add(BookDetailKey(it)) },
                        modifier = safeContent,
                    )
                }
                entry<BookDetailKey> { key ->
                    val viewModel = remember(key.book) { BookDetailViewModel(detailRepository) }
                    val state by viewModel.uiState.collectAsState()
                    var resumedAction by remember(key.book) {
                        mutableStateOf<BookDetailAuthenticatedAction?>(null)
                    }
                    LaunchedEffect(key.book) {
                        viewModel.open(key.book, authTokens?.accessToken)
                    }
                    LaunchedEffect(authTokens?.accessToken) {
                        val token = authTokens?.accessToken
                        if (token == null && viewModel.uiState.value.signedIn) {
                            viewModel.signOut()
                        } else if (token != null && !viewModel.uiState.value.signedIn) {
                            resumedAction = viewModel.authenticate(token)
                        }
                    }

                    BookDetailScreen(
                        state = state,
                        onBack = { backStack.removeLastOrNull() },
                        modifier = safeContent,
                        coverContent = { book, modifier ->
                            RemoteBookImage(book.coverUrl, "${book.title} 표지", modifier)
                        },
                        resumedAuthenticatedAction = resumedAction,
                        onAuthenticatedActionHandled = { resumedAction = null },
                        onLoginRequired = viewModel::requestAuthentication,
                        onAddToLibrary = viewModel::addToLibrary,
                        onStatusChange = viewModel::updateStatus,
                        onPageSave = viewModel::savePage,
                        onRatingSave = viewModel::saveRating,
                        onReviewCreate = viewModel::createReview,
                        onReviewLike = viewModel::likeReview,
                        onLoadReplies = viewModel::loadReplies,
                        onReplyCreate = viewModel::createReply,
                        onReplyLike = viewModel::likeReply,
                        onReviewScopeChange = viewModel::changeReviewScope,
                        onReviewSortChange = viewModel::changeReviewSort,
                        onLoadMoreReviews = viewModel::loadMoreReviews,
                        onRequestErrorShown = viewModel::clearRequestError,
                    )

                    if (state.pendingAction != null) {
                        LoginRequiredSheet(
                            signingIn = authState.signingIn,
                            error = authState.errorMessage,
                            onDismiss = {
                                if (!authState.signingIn) {
                                    authViewModel.clearError()
                                    authViewModel.cancelPendingAuthentication()
                                    viewModel.dismissAuthentication()
                                }
                            },
                            onGoogleSignIn = {
                                authViewModel.clearError()
                                authViewModel.requireAuthentication { token ->
                                    resumedAction = viewModel.authenticate(token)
                                }
                            },
                        )
                    }
                }
            },
        )
    }
}
