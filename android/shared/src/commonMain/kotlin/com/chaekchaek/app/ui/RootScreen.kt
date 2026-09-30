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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.chaekchaek.app.auth.AuthViewModel
import com.chaekchaek.app.presentation.home.HomeViewModel
import com.chaekchaek.app.ui.archive.ArchiveBookUiModel
import com.chaekchaek.app.ui.archive.ArchiveRoute
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
import com.chaekchaek.app.ui.register.BookRegistrationViewModel
import com.chaekchaek.app.ui.search.SearchRoute
import com.chaekchaek.app.ui.search.SearchViewModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private enum class RootTab(
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
    searchViewModel: SearchViewModel,
    registrationViewModel: BookRegistrationViewModel,
    archiveViewModel: ArchiveViewModel,
    memberSettingsViewModel: MemberSettingsViewModel,
    authViewModel: AuthViewModel,
    onBookClick: (BookDetailArgs) -> Unit,
    onMyPage: () -> Unit,
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
    val memberSettingsState by memberSettingsViewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val accessToken = tokens?.accessToken
    val openProfile: () -> Unit = {
        if (accessToken != null) onMyPage()
        else {
            archiveLoginOpensMyPage = true
            showArchiveLoginSheet = true
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
            .then(if (showBottomBar) Modifier.padding(bottom = 74.dp) else Modifier)
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
                onBookClick = { onBookClick(it.toBookDetailArgs()) },
            )
            RootTab.Feed -> FeedScreen(
                homeViewModel = homeViewModel,
                modifier = contentModifier,
                onProfileClick = openProfile,
                onBookClick = { onBookClick(it.toBookDetailArgs()) },
            )
            RootTab.Discover -> SearchRoute(
                viewModel = searchViewModel,
                homeViewModel = homeViewModel,
                registeredBookIds = archiveState.items.mapTo(mutableSetOf()) { it.id },
                modifier = contentModifier,
                onBack = { selectedTab = RootTab.Home },
                onProfileClick = openProfile,
                onBookClick = { onBookClick(it.toBookDetailArgs()) },
            )
            RootTab.Shelf -> ArchiveRoute(
                viewModel = archiveViewModel,
                memberSettingsViewModel = memberSettingsViewModel,
                editing = archiveEditing,
                scrollTopRequest = archiveScrollTopRequest,
                onEditingChange = { editing ->
                    if (!editing || accessToken != null) archiveEditing = editing
                    else {
                        archiveLoginOpensMyPage = false
                        showArchiveLoginSheet = true
                    }
                },
                onProfileClick = openProfile,
                onBookClick = { onBookClick(it.toBookDetailArgs()) },
                modifier = contentModifier,
                bookCover = { book ->
                    RemoteBookImage(book.coverUrl, "${book.title} 표지", Modifier.fillMaxSize())
                },
            )
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
        SnackbarHost(snackbarHost, Modifier.align(Alignment.BottomCenter))
    }
    }

    if (pendingRegistration != null) {
        LoginRequiredSheet(
            signingIn = authState.signingIn,
            error = authState.errorMessage,
            appleSignInAvailable = authViewModel.appleSignInAvailable,
            onDismiss = {
                if (!authState.signingIn) {
                    authViewModel.clearError()
                    authViewModel.cancelPendingAuthentication()
                    searchViewModel.cancelRegistration()
                }
            },
            onAppleSignIn = {
                authViewModel.clearError()
                authViewModel.requireAppleAuthentication { token ->
                    registrationViewModel.authenticate(token)
                    searchViewModel.resumeRegistration()
                }
            },
            onGoogleSignIn = {
                authViewModel.clearError()
                authViewModel.requireAuthentication { token ->
                    registrationViewModel.authenticate(token)
                    searchViewModel.resumeRegistration()
                }
            },
        )
    }

    if (showArchiveLoginSheet) {
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
                }
            },
            onAppleSignIn = {
                authViewModel.clearError()
                authViewModel.requireAppleAuthentication {
                    showArchiveLoginSheet = false
                    if (archiveLoginOpensMyPage) onMyPage() else archiveEditing = true
                    archiveLoginOpensMyPage = false
                }
            },
            onGoogleSignIn = {
                authViewModel.clearError()
                authViewModel.requireAuthentication {
                    showArchiveLoginSheet = false
                    if (archiveLoginOpensMyPage) onMyPage() else archiveEditing = true
                    archiveLoginOpensMyPage = false
                }
            },
        )
    }
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
            contentScale = ContentScale.Crop,
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
            modifier = Modifier.fillMaxWidth().height(74.dp).padding(horizontal = 12.dp).selectableGroup(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top,
        ) {
            RootTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                Box(
                    modifier = Modifier.weight(1f).height(74.dp).selectable(
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
