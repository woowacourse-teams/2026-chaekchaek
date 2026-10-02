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
import androidx.navigation3.ui.defaultPopTransitionSpec
import androidx.savedstate.serialization.SavedStateConfiguration
import com.chaekchaek.app.auth.AuthPlatformCallbacks
import com.chaekchaek.app.auth.AuthViewModel
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.data.remote.BookDetailRemoteRepository
import com.chaekchaek.app.data.remote.BookSearchRemoteRepository
import com.chaekchaek.app.data.remote.LibraryRemoteRepository
import com.chaekchaek.app.data.remote.MemberRemoteRepository
import com.chaekchaek.app.data.remote.PopularBooksRemoteRepository
import com.chaekchaek.app.data.remote.RemoteFeedReviewActions
import com.chaekchaek.app.data.remote.ReviewFeedRemoteRepository
import com.chaekchaek.app.presentation.home.HomeViewModel
import com.chaekchaek.app.ui.archive.ArchiveViewModel
import com.chaekchaek.app.ui.archive.MemberSettingsViewModel
import com.chaekchaek.app.ui.archive.MyPageScreen
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.bookdetail.BookDetailAuthenticatedAction
import com.chaekchaek.app.ui.bookdetail.BookDetailScreen
import com.chaekchaek.app.ui.bookdetail.BookDetailViewModel
import com.chaekchaek.app.ui.bookdetail.analyticsName
import com.chaekchaek.app.ui.common.LoginRequiredSheet
import com.chaekchaek.app.ui.feed.FeedViewModel
import com.chaekchaek.app.ui.home.LocalRemoteBookCover
import com.chaekchaek.app.ui.register.BookRegistrationViewModel
import com.chaekchaek.app.ui.search.SearchViewModel
import com.chaekchaek.app.ui.search.RecentSearchStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.time.Clock

@Serializable
private data object Root : NavKey

@Serializable
private data class BookDetailKey(val book: BookDetailArgs) : NavKey

@Serializable
private data object MyPageKey : NavKey

private val navigationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Root::class, Root.serializer())
            subclass(BookDetailKey::class, BookDetailKey.serializer())
            subclass(MyPageKey::class, MyPageKey.serializer())
        }
    }
}

@Composable
internal fun AppNavigation(
    authPlatform: AuthPlatformCallbacks,
    recentSearchStorage: RecentSearchStorage,
    analytics: AnalyticsTracker,
    uiTestingMyPage: Boolean = false,
) {
    val safeContent = Modifier.windowInsetsPadding(
        WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    )
    if (uiTestingMyPage) {
        var previewState by remember {
            mutableStateOf(
                com.chaekchaek.app.ui.archive.MemberSettingsUiState(
                    signedIn = true,
                    anonymousReviews = true,
                    nickname = "책책이",
                    anonymousNickname = "정다운 참새",
                ),
            )
        }
        MyPageScreen(
            state = previewState,
            onBack = {},
            onAnonymousReviewsChange = { anonymous, nickname ->
                previewState = previewState.copy(anonymousReviews = anonymous, nickname = nickname)
            },
            onSignOut = {},
            onWithdraw = {},
            modifier = safeContent,
        )
        return
    }
    val authViewModel = remember(authPlatform) { AuthViewModel(authPlatform) }
    DisposableEffect(authViewModel) { onDispose(authViewModel::close) }
    val authTokens by authViewModel.tokens.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    LaunchedEffect(authState.errorMessage) {
        if (authState.errorMessage != null) analytics.endAuthAttempt("failure", "unknown")
    }
    val detailRepository = remember { BookDetailRemoteRepository() }
    val homeViewModel = remember {
        HomeViewModel(
            feedRepository = PopularBooksRemoteRepository(),
            clock = Clock.System,
            analytics = analytics,
        )
    }
    val feedViewModel = remember(authPlatform, detailRepository) {
        FeedViewModel(
            repository = ReviewFeedRemoteRepository(),
            clock = Clock.System,
            reviewActions = RemoteFeedReviewActions(detailRepository, authPlatform),
            readGuestToken = { authPlatform.readGuest()?.token },
        )
    }
    val libraryRepository = remember { LibraryRemoteRepository() }
    val memberRepository = remember { MemberRemoteRepository() }
    val registrationViewModel = remember { BookRegistrationViewModel(libraryRepository, analytics) }
    val archiveViewModel = remember { ArchiveViewModel(libraryRepository, analytics) }
    val memberSettingsViewModel = remember { MemberSettingsViewModel(memberRepository) }
    val archiveState by archiveViewModel.uiState.collectAsState()
    val memberSettingsState by memberSettingsViewModel.uiState.collectAsState()
    val searchViewModel = remember(registrationViewModel, authViewModel, recentSearchStorage, analytics) {
        SearchViewModel(
            bookSearchRepository = BookSearchRemoteRepository(),
            registerBook = { registrationViewModel.register(it) },
            isSignedIn = { authViewModel.tokens.value != null },
            recentSearchStorage = recentSearchStorage,
            analytics = analytics,
        )
    }
    LaunchedEffect(authTokens?.accessToken) {
        val accessToken = authTokens?.accessToken
        analytics.updateAuthentication(accessToken != null)
        registrationViewModel.authenticate(accessToken)
        archiveViewModel.authenticate(accessToken)
        memberSettingsViewModel.authenticate(accessToken)
        feedViewModel.authenticate(accessToken)
    }
    val backStack = rememberNavBackStack(navigationConfig, Root)
    CompositionLocalProvider(
        LocalRemoteBookCover provides { url, description, modifier ->
            RemoteBookImage(url, description, modifier)
        },
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            predictivePopTransitionSpec = { _ ->
                defaultPopTransitionSpec<NavKey>().invoke(this)
            },
            entryProvider = entryProvider {
                entry<Root> {
                    RootScreen(
                        homeViewModel = homeViewModel,
                        feedViewModel = feedViewModel,
                        searchViewModel = searchViewModel,
                        registrationViewModel = registrationViewModel,
                        archiveViewModel = archiveViewModel,
                        memberSettingsViewModel = memberSettingsViewModel,
                        authViewModel = authViewModel,
                        analytics = analytics,
                        onBookClick = { backStack.add(BookDetailKey(it)) },
                        onMyPage = { backStack.add(MyPageKey) },
                        onExitRequested = { backStack.removeLastOrNull() },
                        modifier = safeContent,
                    )
                }
                entry<MyPageKey> {
                    LaunchedEffect(Unit) { analytics.screenView("my_page") }
                    MyPageScreen(
                        state = memberSettingsState,
                        onBack = { backStack.removeLastOrNull() },
                        onAnonymousReviewsChange = memberSettingsViewModel::setAnonymousReviews,
                        onSignOut = {
                            authViewModel.signOut {
                                if (backStack.lastOrNull() == MyPageKey) backStack.removeLastOrNull()
                            }
                        },
                        onWithdraw = {
                            memberSettingsViewModel.withdraw {
                                authViewModel.signOut {
                                    if (backStack.lastOrNull() == MyPageKey) backStack.removeLastOrNull()
                                }
                            }
                        },
                        modifier = safeContent,
                    )
                }
                entry<BookDetailKey> { key ->
                    val viewModel = remember(key.book) {
                        BookDetailViewModel(detailRepository, libraryRepository, authPlatform, analytics = analytics)
                    }
                    val state by viewModel.uiState.collectAsState()
                    val displayBook = state.displayBook ?: key.book
                    val archivedBook = archiveState.items.firstOrNull {
                        it.id == displayBook.isbn13.ifBlank { displayBook.id }
                    }
                    val savedBookId = archivedBook?.bookId
                        ?: state.detail?.takeIf { it.myRecord != null }?.bookId
                    var resumedAction by remember(key.book) {
                        mutableStateOf<BookDetailAuthenticatedAction?>(null)
                    }
                    LaunchedEffect(key.book) {
                        analytics.screenView("book_detail")
                        viewModel.open(key.book, authTokens?.accessToken)
                    }
                    LaunchedEffect(authTokens?.accessToken) {
                        val token = authTokens?.accessToken
                        val pendingAction = viewModel.syncAuthentication(token)
                        if (token == null) resumedAction = null
                        else if (pendingAction != null) resumedAction = pendingAction
                    }

                    BookDetailScreen(
                        state = state,
                        onBack = { backStack.removeLastOrNull() },
                        modifier = safeContent,
                        savedToLibrary = savedBookId != null,
                        anonymousReviews = memberSettingsState.anonymousReviews,
                        nickname = if (state.signedIn) {
                            if (memberSettingsState.anonymousReviews) memberSettingsState.anonymousNickname
                            else memberSettingsState.nickname
                        } else {
                            state.guestNickname.orEmpty()
                        },
                        coverContent = { book, modifier ->
                            RemoteBookImage(book.coverUrl, "${book.title} 표지", modifier)
                        },
                        resumedAuthenticatedAction = resumedAction,
                        onAuthenticatedActionHandled = { resumedAction = null },
                        onLoginRequired = viewModel::requestAuthentication,
                        onToggleLibrary = {
                            viewModel.toggleLibrary(savedBookId, archiveViewModel::retry)
                        },
                        onStatusChange = { status ->
                            viewModel.updateStatus(
                                status,
                                onSuccess = archiveViewModel::retry,
                                onLibraryAdded = archiveViewModel::retry,
                            )
                        },
                        onPageSave = { page ->
                            viewModel.savePage(
                                page,
                                onSuccess = archiveViewModel::retry,
                                onLibraryAdded = archiveViewModel::retry,
                            )
                        },
                        onRatingCriterionChange = viewModel::loadRatingComparison,
                        onRatingSave = { rating, onSaved ->
                            viewModel.saveRating(
                                rating,
                                onSuccess = {
                                    archiveViewModel.retry()
                                    onSaved()
                                },
                                onLibraryAdded = archiveViewModel::retry,
                            )
                        },
                        onReviewCreate = viewModel::createReview,
                        onReviewOpen = viewModel::openReviewComposer,
                        onReplyOpen = viewModel::openReplyComposer,
                        onReviewEditOpen = viewModel::openReviewEditor,
                        onReplyEditOpen = viewModel::openReplyEditor,
                        onComposerDismiss = viewModel::dismissComposer,
                        onReviewUpdate = viewModel::updateReview,
                        onReviewDelete = viewModel::deleteReview,
                        onReviewLike = viewModel::likeReview,
                        onLoadReplies = viewModel::loadReplies,
                        onReplyCreate = viewModel::createReply,
                        onReplyUpdate = viewModel::updateReply,
                        onReplyDelete = viewModel::deleteReply,
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
                            appleSignInAvailable = authViewModel.appleSignInAvailable,
                            onDismiss = {
                                if (!authState.signingIn) {
                                    authViewModel.clearError()
                                    authViewModel.cancelPendingAuthentication()
                                    analytics.endAuthAttempt("cancelled")
                                    viewModel.dismissAuthentication()
                                }
                            },
                            onAppleSignIn = {
                                analytics.beginAuthAttempt("apple", state.pendingAction?.analyticsName ?: "book_action")
                                authViewModel.clearError()
                                authViewModel.requireAppleAuthentication { token ->
                                    analytics.endAuthAttempt("success")
                                    resumedAction = viewModel.authenticate(token)
                                }
                            },
                            onGoogleSignIn = {
                                analytics.beginAuthAttempt("google", state.pendingAction?.analyticsName ?: "book_action")
                                authViewModel.clearError()
                                authViewModel.requireAuthentication { token ->
                                    analytics.endAuthAttempt("success")
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
