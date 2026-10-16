package com.chaekchaek.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.chaekchaek.app.ui.common.ChaekSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.app_logo_square
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import chaekchaek.shared.generated.resources.ic_nav_home_active
import chaekchaek.shared.generated.resources.ic_nav_home_inactive
import chaekchaek.shared.generated.resources.ic_nav_feed_active
import chaekchaek.shared.generated.resources.ic_nav_feed_inactive
import chaekchaek.shared.generated.resources.ic_nav_discover_active
import chaekchaek.shared.generated.resources.ic_nav_discover_inactive
import chaekchaek.shared.generated.resources.ic_nav_library_active
import chaekchaek.shared.generated.resources.ic_nav_library_inactive
import coil3.compose.AsyncImage
import com.chaekchaek.app.analytics.AnalyticsTracker
import com.chaekchaek.app.analytics.analyticsBookKey
import com.chaekchaek.app.auth.AuthViewModel
import com.chaekchaek.app.presentation.home.HomeViewModel
import com.chaekchaek.app.ui.archive.ArchiveBookUiModel
import com.chaekchaek.app.ui.archive.ArchiveRoute
import com.chaekchaek.app.ui.archive.LibrarySaveViewModel
import com.chaekchaek.app.ui.archive.ArchiveViewModel
import com.chaekchaek.app.ui.archive.MemberSettingsViewModel
import com.chaekchaek.app.ui.archive.publicNickname
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.common.BookCoverShimmer
import com.chaekchaek.app.ui.common.BookCoverShimmerDelayMillis
import com.chaekchaek.app.ui.common.LoginRequiredSheet
import com.chaekchaek.app.ui.home.BookDetailTarget
import com.chaekchaek.app.ui.home.HomeScreen
import com.chaekchaek.app.ui.home.LocalRemoteBookCover
import com.chaekchaek.app.ui.feed.FeedScreen
import com.chaekchaek.app.ui.feed.FeedViewModel
import com.chaekchaek.app.ui.register.BookRegistrationViewModel
import com.chaekchaek.app.ui.search.SearchRoute
import com.chaekchaek.app.ui.search.SearchViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import kotlin.time.Clock
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

internal enum class RootTab(
    val label: String,
    val selectedIcon: DrawableResource,
    val unselectedIcon: DrawableResource,
) {
    Home("홈", Res.drawable.ic_nav_home_active, Res.drawable.ic_nav_home_inactive),
    Feed("피드", Res.drawable.ic_nav_feed_active, Res.drawable.ic_nav_feed_inactive),
    Discover("발견", Res.drawable.ic_nav_discover_active, Res.drawable.ic_nav_discover_inactive),
    Shelf("내 서재", Res.drawable.ic_nav_library_active, Res.drawable.ic_nav_library_inactive),
}

@Composable
internal fun RootScreen(
    homeViewModel: HomeViewModel,
    feedViewModel: FeedViewModel,
    searchViewModel: SearchViewModel,
    registrationViewModel: BookRegistrationViewModel,
    archiveViewModel: ArchiveViewModel,
    librarySaveViewModel: LibrarySaveViewModel,
    memberSettingsViewModel: MemberSettingsViewModel,
    authViewModel: AuthViewModel,
    analytics: AnalyticsTracker,
    onBookClick: (BookDetailArgs) -> Unit,
    onMyPage: () -> Unit,
    rootBackEnabled: Boolean,
    homeHeaderAction: @Composable () -> Unit = {},
    onExitRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(RootTab.Home) }
    var homeScrollTopRequest by remember { mutableIntStateOf(0) }
    var archiveScrollTopRequest by remember { mutableIntStateOf(0) }
    var archiveEditing by rememberSaveable { mutableStateOf(false) }
    var showArchiveLoginSheet by rememberSaveable { mutableStateOf(false) }
    var archiveLoginOpensMyPage by rememberSaveable { mutableStateOf(false) }
    val tokens by authViewModel.tokens.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val pendingRegistration by searchViewModel.pendingRegistration.collectAsState()
    val registrationState by registrationViewModel.uiState.collectAsState()
    val archiveState by archiveViewModel.uiState.collectAsState()
    val librarySaveState by librarySaveViewModel.uiState.collectAsState()
    val savedIsbns = archiveState.items.mapTo(mutableSetOf()) { it.id }
    val memberSettingsState by memberSettingsViewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val backNavigationEventState = rememberNavigationEventState(NavigationEventInfo.None)
    val backScope = rememberCoroutineScope()
    var exitConfirmationSnackbarJob by remember { mutableStateOf<Job?>(null) }
    var exitConfirmationDeadlineMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    val tabStateHolder = rememberSaveableStateHolder()
    val feedListState = rememberLazyListState()
    val searchResultsListState = rememberLazyListState()
    var tabStateToClear by remember { mutableStateOf<RootTab?>(null) }
    val accessToken = tokens?.accessToken
    val clearExitConfirmation: () -> Unit = {
        exitConfirmationDeadlineMillis = null
        exitConfirmationSnackbarJob?.cancel()
        exitConfirmationSnackbarJob = null
        snackbarHost.currentSnackbarData
            ?.takeIf { it.visuals.message == EXIT_CONFIRMATION_MESSAGE }
            ?.dismiss()
    }
    val openProfile: () -> Unit = {
        clearExitConfirmation()
        if (accessToken != null) onMyPage()
        else {
            archiveLoginOpensMyPage = true
            showArchiveLoginSheet = true
        }
    }
    val openBook: (BookDetailArgs, String) -> Unit = { book, origin ->
        if (origin == "search") analytics.ensureFlow(origin) else analytics.startFlow(origin)
        analytics.bookSelect(analyticsBookKey(book.isbn13, book.id))
        clearExitConfirmation()
        onBookClick(book)
    }

    val saveBook: (BookDetailTarget) -> Unit = { book ->
        if (book.isbn13 !in savedIsbns) librarySaveViewModel.save(book.toBookDetailArgs())
    }

    NavigationBackHandler(
        state = backNavigationEventState,
        isBackEnabled = rootBackEnabled,
        onBackCompleted = {
            when (
                val action = rootBackAction(
                    selectedTab = selectedTab,
                    exitConfirmationDeadlineMillis = exitConfirmationDeadlineMillis,
                    nowMillis = Clock.System.now().toEpochMilliseconds(),
                )
            ) {
                RootBackAction.ReturnToHome -> {
                    exitConfirmationDeadlineMillis = null
                    if (selectedTab == RootTab.Feed || selectedTab == RootTab.Discover) {
                        tabStateToClear = selectedTab
                    }
                    selectedTab = RootTab.Home
                }
                is RootBackAction.ConfirmExit -> {
                    exitConfirmationDeadlineMillis = action.deadlineMillis
                    exitConfirmationSnackbarJob = backScope.launch {
                        snackbarHost.showSnackbar(EXIT_CONFIRMATION_MESSAGE)
                    }
                }
                RootBackAction.Exit -> onExitRequested()
            }
        },
    )

    LaunchedEffect(selectedTab) {
        if (selectedTab != RootTab.Home) clearExitConfirmation()
        analytics.screenView(selectedTab.analyticsScreenName)
    }
    LaunchedEffect(selectedTab, tabStateToClear) {
        tabStateToClear?.takeIf { it != selectedTab }?.let {
            tabStateHolder.removeState(it.name)
            when (it) {
                RootTab.Feed -> feedListState.scrollToItem(0)
                RootTab.Discover -> searchResultsListState.scrollToItem(0)
                else -> Unit
            }
            tabStateToClear = null
        }
    }

    LaunchedEffect(registrationState.completedRegistrationCount) {
        if (registrationState.completedRegistrationCount > 0) {
            archiveViewModel.retry()
        }
    }
    LaunchedEffect(registrationState.errorMessage) {
        registrationState.errorMessage?.let {
            snackbarHost.showSnackbar(it)
            registrationViewModel.clearError()
        }
    }
    LaunchedEffect(librarySaveState.savedCount) {
        if (librarySaveViewModel.consumeSaveNotice()) snackbarHost.showSnackbar("내 서재에 담았어요")
    }
    LaunchedEffect(librarySaveState.errorMessage) {
        librarySaveState.errorMessage?.let { message ->
            val failedBook = librarySaveState.failedBook
            val result = snackbarHost.showSnackbar(message, actionLabel = failedBook?.let { "다시 시도" })
            librarySaveViewModel.clearError()
            if (result == SnackbarResult.ActionPerformed && failedBook != null && archiveViewModel.uiState.value.items.none { it.id == failedBook.isbn13 }) {
                librarySaveViewModel.save(failedBook)
            }
        }
    }
    LaunchedEffect(authState.errorMessage) {
        if (authState.errorMessage != null) analytics.endAuthAttempt("failure", "unknown")
    }
    LaunchedEffect(memberSettingsState.errorMessage) {
        memberSettingsState.errorMessage?.let { message ->
            val result = snackbarHost.showSnackbar(message, actionLabel = "다시 시도")
            memberSettingsViewModel.clearError()
            if (result == SnackbarResult.ActionPerformed) memberSettingsViewModel.retry()
        }
    }

    CompositionLocalProvider(
        LocalRemoteBookCover provides { url, description, imageModifier ->
            RemoteBookImage(url, description, imageModifier)
        },
    ) {
    Box(modifier = modifier.fillMaxSize()) {
        val showBottomBar = !(selectedTab == RootTab.Shelf && archiveEditing)
        val contentModifier = Modifier.fillMaxSize().navigationBarsPadding()
            .then(if (showBottomBar) Modifier.padding(bottom = RootTabBarHeight) else Modifier)
        tabStateHolder.SaveableStateProvider(selectedTab.name) {
        when (selectedTab) {
            RootTab.Home -> HomeScreen(
                homeViewModel = homeViewModel,
                myDisplayName = memberSettingsState.publicNickname,
                accessToken = accessToken,
                scrollTopRequest = homeScrollTopRequest,
                modifier = contentModifier,
                onSearchBook = { selectedTab = RootTab.Discover },
                onOpenFeed = { selectedTab = RootTab.Feed },
                onProfileClick = openProfile,
                homeHeaderAction = homeHeaderAction,
                onBookClick = { openBook(it.toBookDetailArgs(), "home") },
                savedIsbns = savedIsbns,
                librarySaveState = librarySaveState,
                onSaveBook = saveBook,
            )
            RootTab.Feed -> FeedScreen(
                viewModel = feedViewModel,
                listState = feedListState,
                modifier = contentModifier,
                onProfileClick = openProfile,
                onBookClick = { openBook(it.toBookDetailArgs(), "feed") },
                savedIsbns = savedIsbns,
                librarySaveState = librarySaveState,
                onSaveBook = saveBook,
            )
            RootTab.Discover -> SearchRoute(
                viewModel = searchViewModel,
                homeViewModel = homeViewModel,
                searchResultsListState = searchResultsListState,
                registeredBookIds = archiveState.items.mapTo(mutableSetOf()) { it.id },
                modifier = contentModifier,
                onBack = {
                    tabStateToClear = RootTab.Discover
                    selectedTab = RootTab.Home
                },
                onProfileClick = openProfile,
                onBookClick = { openBook(it.toBookDetailArgs(), "search") },
            )
            RootTab.Shelf -> ArchiveRoute(
                viewModel = archiveViewModel,
                memberSettingsViewModel = memberSettingsViewModel,
                editing = archiveEditing,
                scrollTopRequest = archiveScrollTopRequest,
                onEditingChange = { editing ->
                    if (!editing || accessToken != null) archiveEditing = editing
                    else {
                        clearExitConfirmation()
                        archiveLoginOpensMyPage = false
                        showArchiveLoginSheet = true
                    }
                },
                onProfileClick = openProfile,
                onBookClick = { openBook(it.toBookDetailArgs(), "library") },
                modifier = contentModifier,
                bookCover = { book ->
                    RemoteBookImage(book.coverUrl, "${book.title} 표지", Modifier.fillMaxSize())
                },
                bookSpine = { book, imageModifier, showGeneratedSpine, onSpineImageLoadFailed ->
                    com.chaekchaek.app.ui.archive.LibraryBookSpine(
                        book = book,
                        modifier = imageModifier,
                        showGeneratedSpine = showGeneratedSpine,
                        onSpineImageLoadFailed = onSpineImageLoadFailed,
                    )
                },
            )
        }
        }
        if (showBottomBar) {
            ChaekBottomBar(
                selectedTab = selectedTab,
                onTabSelected = {
                    archiveEditing = false
                    if (selectedTab == it) {
                        when (it) {
                            RootTab.Home -> homeScrollTopRequest += 1
                            RootTab.Shelf -> archiveScrollTopRequest += 1
                            RootTab.Feed, RootTab.Discover -> Unit
                        }
                    }
                    selectedTab = it
                },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        if (registrationState.showLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
        ChaekSnackbarHost(
            snackbarHost,
            bottomContentHeight = (if (showBottomBar) RootTabBarHeight else 0.dp) +
                (if (selectedTab == RootTab.Home) 96.dp else 0.dp),
        )
    }
    }

    if (pendingRegistration != null || librarySaveState.pendingBook != null) {
        LaunchedEffect(pendingRegistration) {
            analytics.log(
                name = "cc_auth_prompt",
                strings = mapOf("trigger_action" to "library_add", "surface" to "sheet"),
            )
        }
        LoginRequiredSheet(
            signingIn = authState.signingIn,
            error = authState.errorMessage,
            appleSignInAvailable = authViewModel.appleSignInAvailable,
            onDismiss = {
                if (!authState.signingIn) {
                    authViewModel.clearError()
                    authViewModel.cancelPendingAuthentication()
                    searchViewModel.cancelRegistration()
                    analytics.endAuthAttempt("cancelled")
                    analytics.log(
                        name = "cc_auth_dismiss",
                        strings = mapOf("trigger_action" to "library_add"),
                    )
                    registrationViewModel.cancelRegistration()
                    librarySaveViewModel.cancelPendingSave()
                }
            },
            onAppleSignIn = {
                analytics.beginAuthAttempt("apple", "library_add")
                authViewModel.clearError()
                authViewModel.requireAppleAuthentication { token ->
                    analytics.endAuthAttempt("success")
                    archiveViewModel.authenticate(token)
                    librarySaveViewModel.authenticate(token)
                    registrationViewModel.authenticate(token)
                    searchViewModel.resumeRegistration()
                }
            },
            onGoogleSignIn = {
                analytics.beginAuthAttempt("google", "library_add")
                authViewModel.clearError()
                authViewModel.requireAuthentication { token ->
                    analytics.endAuthAttempt("success")
                    archiveViewModel.authenticate(token)
                    librarySaveViewModel.authenticate(token)
                    registrationViewModel.authenticate(token)
                    searchViewModel.resumeRegistration()
                }
            },
        )
    }

    if (showArchiveLoginSheet) {
        LaunchedEffect(Unit) {
            analytics.log(
                name = "cc_auth_prompt",
                strings = mapOf(
                    "trigger_action" to if (archiveLoginOpensMyPage) "profile" else "library_edit",
                    "surface" to "sheet",
                ),
            )
        }
        LoginRequiredSheet(
            signingIn = authState.signingIn,
            error = authState.errorMessage,
            appleSignInAvailable = authViewModel.appleSignInAvailable,
            onDismiss = {
                if (!authState.signingIn) {
                    authViewModel.clearError()
                    authViewModel.cancelPendingAuthentication()
                    showArchiveLoginSheet = false
                    archiveLoginOpensMyPage = false
                    analytics.endAuthAttempt("cancelled")
                    analytics.log("cc_auth_dismiss", strings = mapOf("trigger_action" to "library"))
                }
            },
            onAppleSignIn = {
                analytics.beginAuthAttempt("apple", if (archiveLoginOpensMyPage) "profile" else "library_edit")
                authViewModel.clearError()
                authViewModel.requireAppleAuthentication {
                    analytics.endAuthAttempt("success")
                    showArchiveLoginSheet = false
                    if (archiveLoginOpensMyPage) onMyPage() else archiveEditing = true
                    archiveLoginOpensMyPage = false
                }
            },
            onGoogleSignIn = {
                analytics.beginAuthAttempt("google", if (archiveLoginOpensMyPage) "profile" else "library_edit")
                authViewModel.clearError()
                authViewModel.requireAuthentication {
                    analytics.endAuthAttempt("success")
                    showArchiveLoginSheet = false
                    if (archiveLoginOpensMyPage) onMyPage() else archiveEditing = true
                    archiveLoginOpensMyPage = false
                }
            },
        )
    }
}

private val RootTabBarHeight = 74.dp

internal const val EXIT_CONFIRMATION_WINDOW_MILLIS = 2_000L
internal const val EXIT_CONFIRMATION_MESSAGE = "한 번 더 누르면 종료됩니다"

internal sealed interface RootBackAction {
    data object ReturnToHome : RootBackAction
    data class ConfirmExit(val deadlineMillis: Long) : RootBackAction
    data object Exit : RootBackAction
}

internal fun rootBackAction(
    selectedTab: RootTab,
    exitConfirmationDeadlineMillis: Long?,
    nowMillis: Long,
): RootBackAction = when {
    selectedTab != RootTab.Home -> RootBackAction.ReturnToHome
    exitConfirmationDeadlineMillis != null && nowMillis <= exitConfirmationDeadlineMillis -> RootBackAction.Exit
    else -> RootBackAction.ConfirmExit(nowMillis + EXIT_CONFIRMATION_WINDOW_MILLIS)
}

private val RootTab.analyticsScreenName: String
    get() = when (this) {
        RootTab.Home -> "home"
        RootTab.Feed -> "feed"
        RootTab.Discover -> "discover"
        RootTab.Shelf -> "library"
    }

@Composable
internal fun RemoteBookImage(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    var loading by remember(url) { mutableStateOf(true) }
    var showShimmer by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url, loading) {
        showShimmer = false
        if (loading) {
            delay(BookCoverShimmerDelayMillis)
            showShimmer = true
        }
    }

    Box(modifier = modifier) {
        if (showShimmer) BookCoverShimmer(Modifier.fillMaxSize())
        AsyncImage(
            model = url,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            error = painterResource(Res.drawable.app_logo_square),
            onLoading = { loading = true },
            onSuccess = { loading = false },
            onError = { loading = false },
            contentScale = ContentScale.Fit,
        )
    }
}

@Composable
private fun ChaekBottomBar(
    selectedTab: RootTab,
    onTabSelected: (RootTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).navigationBarsPadding(),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth().height(RootTabBarHeight).padding(horizontal = 12.dp).selectableGroup(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top,
        ) {
            RootTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                Box(
                    modifier = Modifier.weight(1f).height(RootTabBarHeight).selectable(
                        selected = selected,
                        onClick = { onTabSelected(tab) },
                        role = Role.Tab,
                        interactionSource = remember(tab) { MutableInteractionSource() },
                        indication = null,
                    ),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Column(
                        modifier = Modifier.padding(top = 13.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            painter = painterResource(if (selected) tab.selectedIcon else tab.unselectedIcon),
                            contentDescription = null,
                            modifier = Modifier.size(23.dp),
                            tint = Color.Unspecified,
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            ),
                            color = if (selected) Color(0xFF191919) else Color(0xFF858585),
                        )
                    }
                }
            }
        }
    }
}

private fun BookDetailTarget.toBookDetailArgs() = BookDetailArgs(
    id = id,
    isbn13 = isbn13,
    bookId = bookId,
    title = title,
    creator = creator,
    publisher = publisher,
    year = year,
    category = category,
    totalPages = totalPages,
    coverUrl = coverUrl,
    coverId = coverId,
)

private fun ArchiveBookUiModel.toBookDetailArgs() = BookDetailArgs(
    id = id,
    isbn13 = id,
    bookId = bookId,
    title = title,
    creator = creator,
    publisher = publisher,
    category = category,
    totalPages = totalPages,
    coverUrl = coverUrl,
)
