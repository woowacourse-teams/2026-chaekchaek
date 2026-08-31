package com.chaekchaek.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
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
import chaekchaek.shared.generated.resources.ic_tab_discover_filled
import chaekchaek.shared.generated.resources.ic_tab_discover_outline
import chaekchaek.shared.generated.resources.ic_tab_home_filled
import chaekchaek.shared.generated.resources.ic_tab_home_outline
import chaekchaek.shared.generated.resources.ic_tab_shelf_filled
import chaekchaek.shared.generated.resources.ic_tab_shelf_outline
import coil3.compose.AsyncImage
import com.chaekchaek.app.auth.AuthViewModel
import com.chaekchaek.app.presentation.home.HomeViewModel
import com.chaekchaek.app.ui.archive.ArchiveBookUiModel
import com.chaekchaek.app.ui.archive.ArchiveRoute
import com.chaekchaek.app.ui.archive.ArchiveViewModel
import com.chaekchaek.app.ui.archive.MemberSettingsViewModel
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.common.LoginRequiredSheet
import com.chaekchaek.app.ui.home.BookDetailTarget
import com.chaekchaek.app.ui.home.HomeScreen
import com.chaekchaek.app.ui.register.BookRegistrationViewModel
import com.chaekchaek.app.ui.search.SearchRoute
import com.chaekchaek.app.ui.search.SearchViewModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private enum class RootTab(
    val label: String,
    val selectedIcon: DrawableResource,
    val unselectedIcon: DrawableResource,
) {
    Home("홈", Res.drawable.ic_tab_home_filled, Res.drawable.ic_tab_home_outline),
    Discover("발견", Res.drawable.ic_tab_discover_filled, Res.drawable.ic_tab_discover_outline),
    Shelf("내 서재", Res.drawable.ic_tab_shelf_filled, Res.drawable.ic_tab_shelf_outline),
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
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(RootTab.Home) }
    var homeScrollTopRequest by rememberSaveable { mutableIntStateOf(0) }
    var archiveScrollTopRequest by rememberSaveable { mutableIntStateOf(0) }
    var archiveEditing by rememberSaveable { mutableStateOf(false) }
    var showArchiveLoginSheet by rememberSaveable { mutableStateOf(false) }
    val tokens by authViewModel.tokens.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val pendingRegistration by searchViewModel.pendingRegistration.collectAsState()
    val registrationState by registrationViewModel.uiState.collectAsState()
    val archiveState by archiveViewModel.uiState.collectAsState()
    val memberSettingsState by memberSettingsViewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val accessToken = tokens?.accessToken

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

    Box(modifier = modifier.fillMaxSize()) {
        val showBottomBar = !(selectedTab == RootTab.Shelf && archiveEditing)
        val contentModifier = Modifier.fillMaxSize().navigationBarsPadding()
            .then(if (showBottomBar) Modifier.padding(bottom = 56.dp) else Modifier)
        when (selectedTab) {
            RootTab.Home -> HomeScreen(
                homeViewModel = homeViewModel,
                accessToken = accessToken,
                scrollTopRequest = homeScrollTopRequest,
                modifier = contentModifier,
                onSearchBook = { selectedTab = RootTab.Discover },
                onBookClick = { onBookClick(it.toBookDetailArgs()) },
            )
            RootTab.Discover -> SearchRoute(
                viewModel = searchViewModel,
                registeredBookIds = archiveState.items.mapTo(mutableSetOf()) { it.id },
                modifier = contentModifier,
                onBack = { selectedTab = RootTab.Home },
                onBookClick = { onBookClick(it.toBookDetailArgs()) },
            )
            RootTab.Shelf -> ArchiveRoute(
                viewModel = archiveViewModel,
                memberSettingsViewModel = memberSettingsViewModel,
                editing = archiveEditing,
                scrollTopRequest = archiveScrollTopRequest,
                onEditingChange = { editing ->
                    if (!editing || accessToken != null) archiveEditing = editing
                    else showArchiveLoginSheet = true
                },
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
                            RootTab.Discover -> Unit
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
                }
            },
            onAppleSignIn = {
                authViewModel.clearError()
                authViewModel.requireAppleAuthentication {
                    showArchiveLoginSheet = false
                    archiveEditing = true
                }
            },
            onGoogleSignIn = {
                authViewModel.clearError()
                authViewModel.requireAuthentication {
                    showArchiveLoginSheet = false
                    archiveEditing = true
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
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        placeholder = painterResource(Res.drawable.app_logo_square),
        error = painterResource(Res.drawable.app_logo_square),
        contentScale = ContentScale.Crop,
    )
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
            modifier = Modifier.fillMaxWidth().height(55.dp).selectableGroup(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RootTab.entries.forEach { tab ->
                val selected = selectedTab == tab
                Box(
                    modifier = Modifier.width(64.dp).height(55.dp).selectable(
                        selected = selected,
                        onClick = { onTabSelected(tab) },
                        role = Role.Tab,
                        interactionSource = remember(tab) { MutableInteractionSource() },
                        indication = null,
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(if (selected) tab.selectedIcon else tab.unselectedIcon),
                        contentDescription = tab.label,
                        modifier = Modifier.size(24.dp),
                        tint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
