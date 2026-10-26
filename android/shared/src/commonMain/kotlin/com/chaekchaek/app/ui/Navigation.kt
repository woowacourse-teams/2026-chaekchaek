package com.chaekchaek.app.ui

import com.chaekchaek.app.domain.profile.AuthorProfile
import com.chaekchaek.app.domain.profile.LibraryAccess
import com.chaekchaek.app.data.remote.PublicLibraryRemoteRepository
import com.chaekchaek.app.domain.library.PublicLibrary
import com.chaekchaek.app.domain.library.PublicBookshelfDetail
import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import com.chaekchaek.app.ui.common.LocalOpenAuthorLibrary
import com.chaekchaek.app.ui.publiclibrary.PublicLibraryViewModel
import com.chaekchaek.app.ui.publiclibrary.PublicLibraryScreen
import com.chaekchaek.app.ui.publiclibrary.PublicBookshelfScreen

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
import com.chaekchaek.app.ui.archive.MemberSettingsViewModel
import com.chaekchaek.app.ui.archive.MyPageScreen
import com.chaekchaek.app.ui.bookdetail.toUiModel
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
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi

@Serializable
private data object Root : NavKey

@Serializable
@OptIn(ExperimentalUuidApi::class)
internal data class BookDetailKey(
    val book: BookDetailArgs,
    val entryId: String = Uuid.random().toString(),
) : NavKey

@Serializable
private data class PublicLibraryKey(val memberId: Long, val ownerName: String) : NavKey

@Serializable
private data class PublicBookshelfKey(val memberId: Long, val libraryId: Long, val ownerName: String) : NavKey

@Serializable
private data object MyPageKey : NavKey

private val navigationConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Root::class, Root.serializer())
            subclass(BookDetailKey::class, BookDetailKey.serializer())
            subclass(MyPageKey::class, MyPageKey.serializer())
            subclass(PublicLibraryKey::class, PublicLibraryKey.serializer())
            subclass(PublicBookshelfKey::class, PublicBookshelfKey.serializer())
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
    val authViewModel = remember(authPlatform) { AuthViewModel(authPlatform, errorReporter = analytics.errors) }
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
            onNicknameChange = { nickname ->
                previewState = previewState.copy(nickname = nickname, saveRevision = previewState.saveRevision + 1)
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
    LaunchedEffect(analytics, authTokens?.accessToken) {
        analytics.updateAuthentication(authTokens != null)
    }
    val onboardingViewModel: FirstLibraryOnboardingViewModel = viewModel {
        FirstLibraryOnboardingViewModel(initiallyVisible = shouldShowFirstLibraryOnboarding(onboardingStorage), errorReporter = analytics.errors)
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
        LaunchedEffect(onboardingState.step) {
            analytics.screenView("onboarding_${onboardingState.step.name.lowercase()}")
        }
        val skipOnboarding = {
            analytics.log("cc_onboarding_exit", strings = mapOf("outcome" to "skipped"), longs = mapOf("selected_count" to onboardingState.selectedBooks.size.toLong()))
            authViewModel.clearError()
            authViewModel.cancelPendingAuthentication()
            onboardingViewModel.cancelPendingWork()
            completeOnboarding()
        }
        LaunchedEffect(onboardingState.saveCompleted) {
            if (onboardingState.saveCompleted) {
                analytics.log("cc_onboarding_exit", strings = mapOf("outcome" to "completed"), longs = mapOf("selected_count" to onboardingState.selectedBooks.size.toLong()))
                completeOnboarding()
            }
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
            onToggleBook = { book ->
                analytics.log("cc_onboarding_selection", strings = buildMap {
                    com.chaekchaek.app.analytics.analyticsBookKey(book.isbn13, null)?.let { put("book_key", it) }
                    put("effect", if (onboardingState.selectedBooks.any { it.isbn13 == book.isbn13 }) "removed" else "added")
                })
                onboardingViewModel.toggle(book)
            },
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
                        analytics.updateAuthentication(true)
                        showOnboardingLogin = false
                        onboardingViewModel.save(token)
                    }
                },
                onGoogleSignIn = {
                    authViewModel.clearError()
                    authViewModel.requireAuthentication { token ->
                        analytics.updateAuthentication(true)
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
    val bookshelfRepository = remember { com.chaekchaek.app.data.remote.BookshelfRemoteRepository() }
    val bookshelfViewModel = remember { com.chaekchaek.app.ui.archive.BookshelfViewModel(bookshelfRepository) }
    val shelfPlatform = com.chaekchaek.app.ui.archive.rememberBookshelfPlatform()
    val librarySaveViewModel = remember {
        LibrarySaveViewModel(
            addBook = detailRepository::addToLibrary,
            onSaved = { _, _ -> bookshelfViewModel.refresh() },
            shelves = bookshelfRepository,
            readRecentShelf = shelfPlatform::readRecent,
            writeRecentShelf = shelfPlatform::writeRecent,
            analytics = analytics,
        )
    }
    val memberSettingsViewModel = remember { MemberSettingsViewModel(memberRepository, errorReporter = analytics.errors) }
    ErrorRecoveryEffect(bookshelfViewModel.errors)
    ErrorRecoveryEffect(librarySaveViewModel.errors)
    ErrorRecoveryEffect(memberSettingsViewModel.errors)
    ErrorRecoveryEffect(homeViewModel.errors)
    val librarySaveState by librarySaveViewModel.uiState.collectAsState()
    val bookshelfState by bookshelfViewModel.uiState.collectAsState()
    val memberSettingsState by memberSettingsViewModel.uiState.collectAsState()
    var anonymousReviewsOverride by remember(authPlatform) { mutableStateOf(authPlatform.readAnonymousReviews()) }
    val anonymousReviews = anonymousReviewsOverride ?: memberSettingsState.anonymousReviews
    val updateAnonymousReviews: (Boolean) -> Unit = { value ->
        authPlatform.writeAnonymousReviews(value)
        anonymousReviewsOverride = value
    }
    LaunchedEffect(authTokens?.accessToken) {
        val accessToken = authTokens?.accessToken
        homeViewModel.authenticate(accessToken)
        bookshelfViewModel.authenticate(accessToken, authViewModel.memberIdFor(accessToken))
        librarySaveViewModel.authenticate(accessToken, authViewModel.memberIdFor(accessToken))
        memberSettingsViewModel.authenticate(accessToken, authViewModel.memberIdFor(accessToken))
    }
    val backStack = rememberNavBackStack(navigationConfig, Root)
    val publicLibraryRepository = remember { PublicLibraryRemoteRepository() }
    val profileErrors = remember { ErrorRecoveryState() }
    ErrorRecoveryEffect(profileErrors)
    fun openAuthorLibrary(source: NavKey, profile: AuthorProfile) {
        if (!backStack.isCurrentDestination(source)) return
        if (profile.libraryAccess != LibraryAccess.Available) {
            profileErrors.confirm(profile.libraryAccess.message.orEmpty(), title = "서재를 볼 수 없어요")
            return
        }
        val destination = PublicLibraryKey(requireNotNull(profile.memberId), profile.displayName)
        backStack.navigateToExistingFromCurrent(source, destination) {
            it is PublicLibraryKey && it.memberId == destination.memberId
        }
    }
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
                            analytics = analytics,
                        )
                    }
                    feedViewModel.attachActions(
                        reviewActions = reviewActions,
                        readGuestToken = { authPlatform.readGuest()?.token },
                    )
                    val searchViewModel = viewModel<SearchViewModel> {
                        SearchViewModel(
                            bookSearchRepository = BookSearchRemoteRepository(),
                            registerBook = { book -> librarySaveViewModel.save(BookDetailArgs(id = book.isbn13, isbn13 = book.isbn13, title = book.title, creator = book.creator, coverUrl = book.coverUrl, totalPages = book.totalPages)) },
                            isSignedIn = { authViewModel.tokens.value != null },
                            recentSearchStorage = recentSearchStorage,
                            analytics = analytics,
                            savedStateHandle = createSavedStateHandle(),
                        )
                    }
                    searchViewModel.attachRegistration(
                        registerBook = { book -> librarySaveViewModel.save(BookDetailArgs(id = book.isbn13, isbn13 = book.isbn13, title = book.title, creator = book.creator, coverUrl = book.coverUrl, totalPages = book.totalPages)) },
                        isSignedIn = { authViewModel.tokens.value != null },
                    )
                    LaunchedEffect(authTokens?.accessToken, restoringAuthentication) {
                        if (!restoringAuthentication) feedViewModel.authenticate(authTokens?.accessToken)
                    }
                    CompositionLocalProvider(LocalOpenAuthorLibrary provides { profile -> openAuthorLibrary(Root, profile) }) {
                        RootScreen(
                            homeViewModel = homeViewModel,
                            feedViewModel = feedViewModel,
                            searchViewModel = searchViewModel,
                            librarySaveViewModel = librarySaveViewModel,
                            bookshelfViewModel = bookshelfViewModel,
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
                }
                entry<PublicLibraryKey> { key ->
                    val viewModel = viewModel<PublicLibraryViewModel<PublicLibrary>> {
                        PublicLibraryViewModel { publicLibraryRepository.library(key.memberId) }
                    }
                    ErrorRecoveryEffect(viewModel.errors)
                    val state by viewModel.uiState.collectAsState()
                    PublicLibraryScreen(key.ownerName, state,
                        onBack = { backStack.popDestinationIfCurrent(key) },
                        onBookshelf = { shelf ->
                            val destination = PublicBookshelfKey(key.memberId, shelf.libraryId, key.ownerName)
                            backStack.navigateToExistingFromCurrent(key, destination) {
                                it is PublicBookshelfKey && it.libraryId == shelf.libraryId && it.memberId == key.memberId
                            }
                        }, modifier = safeContent)
                }
                entry<PublicBookshelfKey> { key ->
                    val viewModel = viewModel<PublicLibraryViewModel<PublicBookshelfDetail>> {
                        PublicLibraryViewModel {
                            publicLibraryRepository.bookshelf(key.libraryId).also {
                                check(it.memberId == key.memberId) { "책장 소유자가 일치하지 않아요" }
                            }
                        }
                    }
                    ErrorRecoveryEffect(viewModel.errors)
                    val state by viewModel.uiState.collectAsState()
                    PublicBookshelfScreen(key.ownerName, state,
                        onBack = { backStack.popDestinationIfCurrent(key) },
                        onBook = { backStack.navigateFromCurrent(key, BookDetailKey(it)) },
                        modifier = safeContent)
                }
                entry<MyPageKey> {
                    LaunchedEffect(Unit) { analytics.screenView("my_page"); memberSettingsViewModel.retry() }
                    MyPageScreen(
                        state = memberSettingsState.copy(anonymousReviews = anonymousReviews),
                        onBack = { backStack.popDestinationIfCurrent(MyPageKey) },
                        onNicknameChange = memberSettingsViewModel::updateNickname,
                        onAnonymousReviewsChange = { anonymous, nickname ->
                            updateAnonymousReviews(anonymous)
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
                    } else CompositionLocalProvider(LocalOpenAuthorLibrary provides { profile -> openAuthorLibrary(key, profile) }) {
                        BookDetailScreen(
                            state = state,
                            question = state.detail?.bookQuestion?.toUiModel(displayBook.isbn13),
                            onBack = { bookshelfViewModel.refresh(); backStack.popDestinationIfCurrent(key) },
                            modifier = safeContent,
                            savedToLibrary = state.detail?.isRegisteredInMyLibrary ?: bookshelfState.shelves.any { shelf -> shelf.items.any { it.isbn13 == displayBook.isbn13 } },
                            externalLibrarySaving = librarySaveState.savingIsbn == displayBook.isbn13,
                            externalLibraryLoading = librarySaveState.showLoading,
                            anonymousReviews = anonymousReviews,
                            onAnonymousReviewsChange = updateAnonymousReviews,
                            nickname = if (state.signedIn) {
                                memberSettingsState.nickname
                            } else {
                                state.guestNickname.orEmpty()
                            },
                            coverContent = { book, modifier ->
                                RemoteBookImage(book.coverUrl, "${book.title} 표지", modifier)
                            },
                            resumedAuthenticatedAction = resumedAction,
                            onAuthenticatedActionHandled = { resumedAction = null },
                            onLoginRequired = viewModel::requestAuthentication,
                            onAddToLibrary = { librarySaveViewModel.save(displayBook) },
                            onLibraryMessageShown = viewModel::clearLibraryMessage,
                            onStatusChange = { status ->
                                viewModel.updateStatus(
                                    status,
                                    onSuccess = bookshelfViewModel::refresh,
                                    onLibraryAdded = bookshelfViewModel::refresh,
                                )
                            },
                            onPageSave = { page, onSaved ->
                                viewModel.savePage(
                                    page,
                                    onSuccess = {
                                        bookshelfViewModel.refresh()
                                        onSaved()
                                    },
                                    onLibraryAdded = bookshelfViewModel::refresh,
                                )
                            },
                            onRatingCriterionChange = viewModel::loadRatingComparison,
                            onRatingSave = { rating, onSaved ->
                                viewModel.saveRating(
                                    rating,
                                    onSuccess = {
                                        bookshelfViewModel.refresh()
                                        onSaved()
                                    },
                                )
                            },
                            errorRecovery = viewModel.errors,
                            onReviewCreate = viewModel::createReview,
                            onQuestionAReviewCreate = { draft ->
                                viewModel.createReviewWithRating(draft.toReviewRequest(), draft.rating, onRatingSaved = bookshelfViewModel::refresh, anonymous = draft.anonymous)
                            },
                            onQuestionAReviewUpdate = { reviewId, draft, request ->
                                viewModel.updateReviewWithRating(reviewId, request, draft.rating, onRatingSaved = bookshelfViewModel::refresh)
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
                    }

                    if (!restoringAuthentication && state.pendingAction != null) {
                        LoginRequiredSheet(
                            message = if (state.pendingAction == BookDetailAuthenticatedAction.AddToLibrary) "책을 담으려면 로그인이 필요해요." else "내 독서 기록을 남기고 감상에 참여하려면 로그인해 주세요.",
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
        com.chaekchaek.app.ui.archive.ShelfChoiceSheet(librarySaveViewModel)
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
