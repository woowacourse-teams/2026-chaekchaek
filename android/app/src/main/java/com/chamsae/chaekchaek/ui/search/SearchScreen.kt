package com.chamsae.chaekchaek.ui.search

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import com.chamsae.chaekchaek.R
import com.chamsae.chaekchaek.auth.AuthSession
import com.chamsae.chaekchaek.auth.requestGoogleIdToken
import com.chamsae.chaekchaek.data.ArchivedBook
import com.chamsae.chaekchaek.data.LibraryRepository
import com.chamsae.chaekchaek.data.toArchivedBook
import com.chaekchaek.app.domain.book.BookSearchRepository
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chamsae.chaekchaek.theme.ChaekAccent
import com.chamsae.chaekchaek.theme.ChaekAccentInk
import com.chamsae.chaekchaek.theme.ChaekBand
import com.chamsae.chaekchaek.theme.ChaekBorder
import com.chamsae.chaekchaek.theme.ChaekBorderSoft
import com.chamsae.chaekchaek.theme.ChaekInkTertiary
import com.chamsae.chaekchaek.ui.bookdetail.BookDetailArgs
import com.chamsae.chaekchaek.ui.bookdetail.toBookDetailArgs
import com.chamsae.chaekchaek.ui.common.LoginRequiredSheet
import com.chaekchaek.app.data.remote.MobileAuthRemoteRepository
import com.chaekchaek.app.data.remote.MobileLoginException
import kotlinx.coroutines.launch

@Composable
fun SearchRoute(
  bookSearchRepository: BookSearchRepository,
  libraryRepository: LibraryRepository,
  mobileAuthRepository: MobileAuthRemoteRepository,
  authSession: AuthSession,
  modifier: Modifier = Modifier,
  onBack: () -> Unit = {},
  onBookClick: (BookDetailArgs) -> Unit = {},
) {
  val factory =
    remember(bookSearchRepository, libraryRepository, authSession) {
      viewModelFactory {
        initializer {
          SearchViewModel(bookSearchRepository, libraryRepository) {
            authSession.tokens.value != null
          }
        }
      }
    }
  val viewModel: SearchViewModel = viewModel(factory = factory)
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val pendingRegistration by viewModel.pendingRegistration.collectAsStateWithLifecycle()
  val archivedBooks by libraryRepository.items.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var signingIn by rememberSaveable { mutableStateOf(false) }
  var loginError by rememberSaveable { mutableStateOf<String?>(null) }

  SearchScreen(
    state = state,
    registeredBookIds = archivedBooks.mapTo(mutableSetOf(), ArchivedBook::id),
    onSearch = viewModel::search,
    onClear = viewModel::clear,
    onRegister = viewModel::register,
    onBack = onBack,
    onBookClick = onBookClick,
    modifier = modifier,
  )

  if (pendingRegistration != null) {
    LoginRequiredSheet(
      signingIn = signingIn,
      error = loginError,
      onDismiss = {
        if (!signingIn) {
          loginError = null
          viewModel.cancelRegistration()
        }
      },
      onGoogleSignIn = {
        if (signingIn) return@LoginRequiredSheet
        scope.launch {
          signingIn = true
          loginError = null
          runCatching { requestGoogleIdToken(context) }
            .mapCatching { idToken ->
              authSession.signIn(mobileAuthRepository.loginWithGoogle(idToken))
            }
            .onSuccess { viewModel.resumeRegistration() }
            .onFailure {
              val code = (it as? MobileLoginException)?.code ?: it::class.simpleName.orEmpty()
              Log.w("ChaekchaekAuth", "Google login failed: $code")
              loginError = "로그인하지 못했어요. 다시 시도해 주세요."
            }
          signingIn = false
        }
      },
    )
  }
}

@Composable
fun SearchScreen(
  state: SearchUiState,
  registeredBookIds: Set<String>,
  onSearch: (String) -> Unit,
  onClear: () -> Unit,
  onRegister: (BookSearchResult) -> Unit,
  modifier: Modifier = Modifier,
  onBack: () -> Unit = {},
  onBookClick: (BookDetailArgs) -> Unit = {},
) {
  var query by remember { mutableStateOf("") }
  val leaveSearch = {
    onClear()
    onBack()
  }

  BackHandler(onBack = leaveSearch)

  Column(modifier = modifier.fillMaxSize()) {
    SearchTopBar(
      query = query,
      onQueryChange = {
        query = it
        if (it.isEmpty()) onClear()
      },
      onSearch = { onSearch(query) },
      onBack = leaveSearch,
    )

    when (val current = state) {
      SearchUiState.Idle ->
        SearchMessage(
          title = "찾고 싶은 책을 검색해 보세요",
          body = "책 제목이나 저자를 입력해 주세요.",
          modifier = Modifier.weight(1f),
        )
      SearchUiState.Loading -> SearchLoading(Modifier.weight(1f))
      SearchUiState.Empty ->
        Column(modifier = Modifier.weight(1f)) {
          SearchResultHeader(count = 0)
          SearchMessage(
            title = "검색 결과가 없어요",
            body = "다른 검색어로 다시 찾아보세요.",
            modifier = Modifier.weight(1f),
          )
        }
      is SearchUiState.Error ->
        SearchMessage(
          title = "검색 결과를 불러오지 못했어요",
          body = "잠시 후 다시 검색해 주세요.",
          modifier = Modifier.weight(1f),
        )
      is SearchUiState.Success ->
        SearchResults(
          results = current.results,
          registeredBookIds = registeredBookIds,
          onRegister = onRegister,
          onBookClick = onBookClick,
          modifier = Modifier.weight(1f),
        )
    }
  }
}

@Composable
private fun SearchTopBar(
  query: String,
  onQueryChange: (String) -> Unit,
  onSearch: () -> Unit,
  onBack: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier.size(48.dp).clickable(role = Role.Button, onClick = onBack),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        painter = painterResource(R.drawable.ic_back),
        contentDescription = "뒤로 가기",
        modifier = Modifier.size(22.dp),
      )
    }
    SearchField(
      query = query,
      onQueryChange = onQueryChange,
      onSearch = onSearch,
      modifier = Modifier.weight(1f),
    )
  }
  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SearchField(
  query: String,
  onQueryChange: (String) -> Unit,
  onSearch: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(6.dp)
  BasicTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier =
      modifier
        .height(44.dp)
        .background(MaterialTheme.colorScheme.surface, shape)
        .border(1.dp, MaterialTheme.colorScheme.outline, shape)
        .padding(start = 12.dp),
    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
    singleLine = true,
    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    decorationBox = { field ->
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          painter = painterResource(R.drawable.ic_search),
          contentDescription = null,
          modifier = Modifier.size(16.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f)) {
          if (query.isEmpty()) {
            Text(
              "책 제목, 저자로 검색",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          field()
        }
        if (query.isNotEmpty()) {
          Box(
            modifier = Modifier.size(44.dp).clickable(role = Role.Button) { onQueryChange("") },
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              painter = painterResource(R.drawable.ic_close),
              contentDescription = "검색어 지우기",
              modifier = Modifier.size(18.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    },
  )
}

@Composable
private fun SearchResults(
  results: List<BookSearchResult>,
  registeredBookIds: Set<String>,
  onRegister: (BookSearchResult) -> Unit,
  onBookClick: (BookDetailArgs) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.fillMaxWidth()) {
    SearchResultHeader(results.size)
    LazyColumn(modifier = Modifier.weight(1f)) {
      items(results) { book ->
        SearchResultRow(
          book = book,
          isReading = book.toArchivedBook().id in registeredBookIds,
          onRegister = { onRegister(book) },
          onClick = { onBookClick(book.toBookDetailArgs()) },
        )
        HorizontalDivider(color = ChaekBand)
      }
    }
  }
}

@Composable
private fun SearchResultHeader(count: Int) {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      "ARCHIVE SEARCH · 검색 결과 ${count}건",
      style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
      color = ChaekAccentInk,
    )
    Text("최신순", style = MaterialTheme.typography.bodySmall)
  }
  HorizontalDivider(color = ChaekBand)
}

@Composable
private fun SearchResultRow(
  book: BookSearchResult,
  isReading: Boolean,
  onRegister: () -> Unit,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Surface(
      modifier = Modifier.size(width = 56.dp, height = 80.dp).shadow(4.dp, RectangleShape),
      shape = RectangleShape,
      color = MaterialTheme.colorScheme.surfaceVariant,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text("책", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AsyncImage(
          model = book.coverUrl,
          contentDescription = book.title,
          modifier = Modifier.fillMaxSize(),
          contentScale = ContentScale.Crop,
        )
      }
    }
    Spacer(Modifier.width(14.dp))
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
      Text(
        book.title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        book.creator,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        listOf(book.publisher, book.year).filter(String::isNotBlank).joinToString(" · "),
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Surface(
        modifier =
          Modifier
            .align(Alignment.End)
            .height(32.dp)
            .clickable(enabled = !isReading, role = Role.Button, onClick = onRegister),
        shape = RoundedCornerShape(6.dp),
        color = if (isReading) ChaekBorderSoft else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isReading) ChaekBorder else MaterialTheme.colorScheme.onSurface),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (!isReading) Text("+", style = MaterialTheme.typography.labelMedium)
          Text(
            if (isReading) "읽는 중" else "읽는 중 시작",
            style = MaterialTheme.typography.labelMedium,
            color = if (isReading) ChaekInkTertiary else MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}

@Composable
private fun SearchLoading(modifier: Modifier = Modifier) {
  Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Surface(
        modifier = Modifier.size(42.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      ) {
        Box(contentAlignment = Alignment.Center) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = ChaekAccent,
            strokeWidth = 2.dp,
          )
        }
      }
      Text(
        "검색 결과를 불러오고 있어요.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun SearchMessage(
  title: String,
  body: String,
  modifier: Modifier = Modifier,
) {
  Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
