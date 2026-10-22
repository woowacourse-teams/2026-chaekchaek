package com.chaekchaek.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
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
import com.chaekchaek.app.ui.archive.LibrarySaveViewModel
import com.chaekchaek.app.ui.archive.ArchiveViewModel
import com.chaekchaek.app.ui.archive.MemberSettingsViewModel
import com.chaekchaek.app.ui.archive.MyPageScreen
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.bookdetail.BookDetailAuthenticatedAction
import com.chaekchaek.app.ui.bookdetail.toReviewRequest
import com.chaekchaek.app.ui.bookdetail.BookDetailScreen
import com.chaekchaek.app.ui.bookdetail.BookDetailViewModel
import com.chaekchaek.app.ui.bookdetail.analyticsName
import com.chaekchaek.app.ui.common.ErrorRecoveryHost
import com.chaekchaek.app.ui.common.ErrorRecoveryEffect
import com.chaekchaek.app.ui.common.LoginRequiredSheet
import com.chaekchaek.app.ui.feed.FeedViewModel
import com.chaekchaek.app.ui.home.LocalRemoteBookCover
import com.chaekchaek.app.ui.register.BookRegistrationViewModel
import com.chaekchaek.app.ui.search.SearchViewModel
import com.chaekchaek.app.ui.search.RecentSearchStorage
import com.chaekchaek.app.ui.onboarding.FirstLibraryOnboardingScreen
import com.chaekchaek.app.ui.onboarding.FirstLibraryOnboardingViewModel
import com.chaekchaek.app.ui.onboarding.OnboardingStep
import com.chaekchaek.app.ui.onboarding.OnboardingStorage
import com.chaekchaek.app.ui.onboarding.shouldShowFirstLibraryOnboarding
import kotlinx.coroutines.flow.collect
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
    onboardingStorage: OnboardingStorage,
    analytics: AnalyticsTracker,
    uiTestingMyPage: Boolean = false,
    homeHeaderAction: @Composable () -> Unit = {},
    onExitRequested: () -> Unit = {},
) {
    val authViewModel = remember(authPlatform) { AuthViewModel(authPlatform) }
    DisposableEffect(authViewModel) { onDispose(authViewModel::close) }
    ErrorRecoveryHost(authViewModel) {
        AppNavigationContent(authPlatform, recentSearchStorage, onboardingStorage, analytics, uiTestingMyPage, homeHeaderAction, onExitRequested, authViewModel)
    }
}

@Composable
private fun AppNavigationContent(
    authPlatform: AuthPlatformCallbacks,
    recentSearchStorage: RecentSearchStorage,
    onboardingStorage: OnboardingStorage,
    analytics: AnalyticsTracker,
    uiTestingMyPage: Boolean,
    homeHeaderAction: @Composable () -> Unit,
    onExitRequested: () -> Unit,
    authViewModel: AuthViewModel,
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
    val authTokens by authViewModel.tokens.collectAsState()
    val restoringAuthentication by authViewModel.isRestoringSession.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val onboardingViewModel: FirstLibraryOnboardingViewModel = viewModel {
        FirstLibraryOnboardingViewModel(initiallyVisible = shouldShowFirstLibraryOnboarding(onboardingStorage))
    }
    val onboardingState by onboardingViewModel.state.collectAsState()
    val showOnboarding = onboardingState.visible
    ErrorRecoveryEffect(onboardingViewModel.errors)
    var showOnboardingLogin by remember { mutableStateOf(false) }
    LaunchedEffect(showOnboarding) {
        if (showOnboarding) onboardingStorage.markShown()
    }
    LaunchedEffect(authState.errorMessage) {
        if (authState.errorMessage != null) analytics.endAuthAttempt("failure", "unknown")
    }
    if (showOnboarding && !uiTestingMyPage) {
        val completeOnboarding = {
            showOnboardingLogin = false
            onboardingViewModel.finish()
        }
        val skipOnboarding = {
            authViewModel.clearError()
            authViewModel.cancelPendingAuthentication()
            onboardingViewModel.cancelPendingWork()
            completeOnboarding()
        }
        LaunchedEffect(onboardingState.saveCompleted) {
            if (onboardingState.saveCompleted) completeOnboarding()
        }
        val saveSelectedBooks = {
            val token = authTokens?.accessToken
            if (token != null) onboardingViewModel.retrySave(token)
            else {
                authViewModel.clearError()
                showOnboardingLogin = true
            }
        }
        val backState = rememberNavigationEventState(NavigationEventInfo.None)
        val returnToPreviousStep = {
            onboardingViewModel.returnToPreviousStep(onboardingState.step, onboardingState.stepRevision)
        }
        NavigationBackHandler(
            state = backState,
            isBackEnabled = !showOnboardingLogin && onboardingState.step != OnboardingStep.INTRO,
            onBackCompleted = returnToPreviousStep,
        )
        FirstLibraryOnboardingScreen(
            state = onboardingState,
            onStartSearch = onboardingViewModel::startSearch,
            onBack = returnToPreviousStep,
            onQueryChange = onboardingViewModel::updateQuery,
            onSearch = { onboardingViewModel.search() },
            onToggleBook = onboardingViewModel::toggle,
            onReviewSelection = onboardingViewModel::reviewSelection,
            onLogin = saveSelectedBooks,
            onRetrySave = saveSelectedBooks,
            onSkip = skipOnboarding,
            modifier = safeContent,
        )
        if (showOnboardingLogin) {
            LoginRequiredSheet(
                signingIn = authState.signingIn,
                error = authState.errorMessage,
                appleSignInAvailable = authViewModel.appleSignInAvailable,
                onDismiss = {
                    if (!authState.signingIn) {
                        authViewModel.clearError()
                        authViewModel.cancelPendingAuthentication()
                        showOnboardingLogin = false
                    }
                },
                onAppleSignIn = {
                    authViewModel.clearError()
                    authViewModel.requireAppleAuthentication { token ->
                        showOnboardingLogin = false
                        onboardingViewModel.save(token)
                    }
                },
                onGoogleSignIn = {
                    authViewModel.clearError()
                    authViewModel.requireAuthentication { token ->
                        showOnboardingLogin = false
                        onboardingViewModel.save(token)
                    }
                },
                skipLabel = "건너뛰기",
                onSkip = skipOnboarding,
            )
        }
        return
    }
    val detailRepository = remember { BookDetailRemoteRepository() }
    val homeViewModel = remember {
        HomeViewModel(
            feedRepository = PopularBooksRemoteRepository(),
            clock = Clock.System,
            analytics = analytics,
        )
    }
    val libraryRepository = remember { LibraryRemoteRepository() }
    val memberRepository = remember { MemberRemoteRepository() }
    val registrationViewModel = remember { BookRegistrationViewModel(libraryRepository, analytics) }
    val archiveViewModel = remember { ArchiveViewModel(libraryRepository, analytics) }
    val librarySaveViewModel = remember {
        LibrarySaveViewModel(
            addBook = detailRepository::addToLibrary,
            onSaved = archiveViewModel::acceptSavedBook,
            analytics = analytics,
            isSaved = { isbn -> archiveViewModel.uiState.value.items.any { it.id == isbn } },
        )
    }
    val memberSettingsViewModel = remember { MemberSettingsViewModel(memberRepository) }
    ErrorRecoveryEffect(registrationViewModel.errors)
    ErrorRecoveryEffect(archiveViewModel.errors)
    ErrorRecoveryEffect(librarySaveViewModel.errors)
    ErrorRecoveryEffect(memberSettingsViewModel.errors)
    ErrorRecoveryEffect(homeViewModel.errors)
    val archiveState by archiveViewModel.uiState.collectAsState()
    val librarySaveState by librarySaveViewModel.uiState.collectAsState()
    val memberSettingsState by memberSettingsViewModel.uiState.collectAsState()
    LaunchedEffect(authTokens?.accessToken) {
        val accessToken = authTokens?.accessToken
        analytics.updateAuthentication(accessToken != null)
        homeViewModel.authenticate(accessToken)
        registrationViewModel.authenticate(accessToken)
        archiveViewModel.authenticate(accessToken)
        librarySaveViewModel.authenticate(accessToken)
        memberSettingsViewModel.authenticate(accessToken)
    }
    val backStack = rememberNavBackStack(navigationConfig, Root)
    CompositionLocalProvider(
        LocalRemoteBookCover provides { url, description, modifier ->
            RemoteBookImage(url, description, modifier)
        },
    ) {
        NavDisplay(
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            onBack = { backStack.popTopDestination() },
            predictivePopTransitionSpec = { _ ->
                defaultPopTransitionSpec<NavKey>().invoke(this)
            },
            entryProvider = entryProvider {
                entry<Root> {
                    val reviewActions = remember(detailRepository, authPlatform) {
                        RemoteFeedReviewActions(detailRepository, authPlatform)
                    }
                    val feedViewModel = viewModel<FeedViewModel> {
                        FeedViewModel(
                            repository = ReviewFeedRemoteRepository(),
                            clock = Clock.System,
                            reviewActions = reviewActions,
                            readGuestToken = { authPlatform.readGuest()?.token },
                            savedStateHandle = createSavedStateHandle(),
                            initialAccessToken = authTokens?.accessToken,
                            deferInitialLoad = restoringAuthentication,
                        )
                    }
                    feedViewModel.attachActions(
                        reviewActions = reviewActions,
                        readGuestToken = { authPlatform.readGuest()?.token },
                    )
                    val searchViewModel = viewModel<SearchViewModel> {
                        SearchViewModel(
                            bookSearchRepository = BookSearchRemoteRepository(),
                            registerBook = registrationViewModel::register,
                            isSignedIn = { authViewModel.tokens.value != null },
                            recentSearchStorage = recentSearchStorage,
                            analytics = analytics,
                            savedStateHandle = createSavedStateHandle(),
                        )
                    }
                    searchViewModel.attachRegistration(
                        registerBook = registrationViewModel::register,
                        isSignedIn = { authViewModel.tokens.value != null },
                    )
                    LaunchedEffect(authTokens?.accessToken, restoringAuthentication) {
                        if (!restoringAuthentication) feedViewModel.authenticate(authTokens?.accessToken)
                    }
                    RootScreen(
                        homeViewModel = homeViewModel,
                        feedViewModel = feedViewModel,
                        searchViewModel = searchViewModel,
                        registrationViewModel = registrationViewModel,
                        archiveViewModel = archiveViewModel,
                        librarySaveViewModel = librarySaveViewModel,
                        memberSettingsViewModel = memberSettingsViewModel,
                        authViewModel = authViewModel,
                        analytics = analytics,
                        onBookClick = { backStack.navigateFromCurrent(Root, BookDetailKey(it)) },
                        onMyPage = { backStack.navigateFromCurrent(Root, MyPageKey) },
                        rootBackEnabled = backStack.isCurrentDestination(Root),
                        homeHeaderAction = homeHeaderAction,
                        onExitRequested = {
                            if (backStack.prepareRootExit(Root)) onExitRequested()
                        },
                        modifier = safeContent,
                    )
                }
                entry<MyPageKey> {
                    LaunchedEffect(Unit) { analytics.screenView("my_page"); memberSettingsViewModel.retry() }
                    MyPageScreen(
                        state = memberSettingsState,
                        onBack = { backStack.popDestinationIfCurrent(MyPageKey) },
                        onNicknameChange = memberSettingsViewModel::updateNickname,
                        onAnonymousReviewsChange = { anonymous, nickname ->
                            authPlatform.writeAnonymousReviews(anonymous)
                            memberSettingsViewModel.setAnonymousReviews(anonymous, nickname)
                        },
                        onSignOut = {
                            authViewModel.signOut {
                                backStack.popDestinationIfCurrent(MyPageKey)
                            }
                        },
                        onWithdraw = {
                            memberSettingsViewModel.withdraw {
                                authViewModel.signOut {
                                    backStack.popDestinationIfCurrent(MyPageKey)
                                }
                            }
                        },
                        modifier = safeContent,
                    )
                }
                entry<BookDetailKey> { key ->
                    val viewModel = viewModel<BookDetailViewModel> {
                        BookDetailViewModel(
                            repository = detailRepository,
                            libraryRepository = libraryRepository,
                            authPlatform = authPlatform,
                            analytics = analytics,
                            savedStateHandle = createSavedStateHandle(),
                        )
                    }
                    viewModel.attachAuthPlatform(authPlatform)
                    val state by viewModel.uiState.collectAsState()
                    val refreshState by viewModel.refreshState.collectAsState()
                    val displayBook = state.displayBook ?: key.book
                    val archivedBook = archiveState.items.firstOrNull {
                        it.id == displayBook.isbn13.ifBlank { displayBook.id }
                    }
                    val savedBookId = archivedBook?.bookId
                        ?: state.detail?.myRecord?.let { it.bookId ?: state.detail?.bookId }
                    var resumedAction by remember(key.book) {
                        mutableStateOf<BookDetailAuthenticatedAction?>(null)
                    }
                    LaunchedEffect(key.book, restoringAuthentication) {
                        if (!restoringAuthentication) {
                            analytics.screenView("book_detail")
                            viewModel.open(key.book, authTokens?.accessToken)
                        }
                    }
                    LaunchedEffect(authTokens?.accessToken, restoringAuthentication) {
                        if (restoringAuthentication) return@LaunchedEffect
                        val token = authTokens?.accessToken
                        val pendingAction = viewModel.syncAuthentication(token)
                        if (token == null) resumedAction = null
                        else if (pendingAction != null) resumedAction = pendingAction
                    }
                    LaunchedEffect(viewModel) {
                        librarySaveViewModel.savesCompletedWhileObserving().collect { savedBook ->
                            viewModel.acceptSavedLibraryRecord(savedBook.isbn13, savedBook.record)
                        }
                    }

                    if (restoringAuthentication) {
                        AuthenticationRestorationContent(safeContent)
                    } else BookDetailScreen(
                        state = state,
                        onBack = { backStack.popDestinationIfCurrent(key) },
                        modifier = safeContent,
                        savedToLibrary = savedBookId != null,
                        externalLibrarySaving = librarySaveState.savingIsbn == displayBook.isbn13,
                        externalLibraryLoading = librarySaveState.showLoading,
                        anonymousReviews = authPlatform.readAnonymousReviews() ?: memberSettingsState.anonymousReviews,
                        onAnonymousReviewsChange = authPlatform.writeAnonymousReviews,
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
                        onAddToLibrary = {
                            if (savedBookId == null && librarySaveViewModel.uiState.value.savingIsbn != displayBook.isbn13) viewModel.addToLibrary { record ->
                                archiveViewModel.acceptSavedBook(displayBook, record)
                            }
                        },
                        onRemoveFromLibrary = {
                            savedBookId?.let { id ->
                                viewModel.removeFromLibrary(id) {
                                    archiveViewModel.acceptRemovedBook(displayBook.isbn13)
                                }
                            }
                        },
                        onLibraryMessageShown = viewModel::clearLibraryMessage,
                        onStatusChange = { status ->
                            viewModel.updateStatus(
                                status,
                                onSuccess = archiveViewModel::retry,
                                onLibraryAdded = archiveViewModel::retry,
                            )
                        },
                        onPageSave = { page, onSaved ->
                            viewModel.savePage(
                                page,
                                onSuccess = {
                                    archiveViewModel.retry()
                                    onSaved()
                                },
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
                        errorRecovery = viewModel.errors,
                        onReviewCreate = viewModel::createReview,
                        onQuestionAReviewCreate = { draft ->
                            viewModel.createReviewWithRating(draft.toReviewRequest(), draft.rating, archiveViewModel::retry, onRatingSaved = archiveViewModel::retry, anonymous = draft.anonymous)
                        },
                        onQuestionAReviewUpdate = { reviewId, draft, request ->
                            viewModel.updateReviewWithRating(reviewId, request, draft.rating, archiveViewModel::retry, onRatingSaved = archiveViewModel::retry)
                        },
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
                        onRetryReviews = viewModel::retryReviews,
                        onRequestErrorShown = viewModel::clearRequestError,
                        refreshState = refreshState,
                        onRefresh = viewModel::refresh,
                    )

                    if (!restoringAuthentication && state.pendingAction != null) {
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

@Composable
private fun AuthenticationRestorationContent(modifier: Modifier) {
    var showLoading by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(500)
        showLoading = true
    }
    if (showLoading) {
        com.chaekchaek.app.ui.bookdetail.RequestLoadingOverlay(modifier.fillMaxSize())
    }
}
