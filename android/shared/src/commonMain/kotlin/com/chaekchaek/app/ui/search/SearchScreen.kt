package com.chaekchaek.app.ui.search

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import chaekchaek.shared.generated.resources.Res
import chaekchaek.shared.generated.resources.*
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.domain.book.BookSearchSort
import com.chaekchaek.app.analytics.analyticsBookKey
import com.chaekchaek.app.analytics.analyticsImpression
import com.chaekchaek.app.ui.theme.ChaekAccent
import com.chaekchaek.app.ui.theme.ChaekAccentInk
import com.chaekchaek.app.ui.theme.ChaekBand
import com.chaekchaek.app.ui.theme.ChaekBorder
import com.chaekchaek.app.ui.theme.ChaekInkSecondary
import com.chaekchaek.app.ui.theme.ChaekIconFontFamily
import com.chaekchaek.app.ui.theme.ChaekSurfaceMuted
import com.chaekchaek.app.ui.theme.discoverPopularBookShadow
import com.chaekchaek.app.ui.theme.discoverReflectionBookShadow
import com.chaekchaek.app.ui.home.BookDetailTarget
import com.chaekchaek.app.ui.home.LocalRemoteBookCover
import com.chaekchaek.app.ui.common.BrandHeader
import com.chaekchaek.app.ui.common.HomeLoadErrorContent
import com.chaekchaek.app.presentation.home.FeedSectionUiModel
import com.chaekchaek.app.presentation.home.HomeUiState
import com.chaekchaek.app.presentation.home.HomeViewModel
import com.chaekchaek.app.presentation.home.QuoteCardUiModel
import com.chaekchaek.app.presentation.home.TrendingBookUiModel
import kotlinx.coroutines.delay

@Composable
fun SearchRoute(
  viewModel: SearchViewModel,
  homeViewModel: HomeViewModel,
  registeredBookIds: Set<String>,
  modifier: Modifier = Modifier,
  onBack: () -> Unit = {},
  onProfileClick: () -> Unit = {},
  onBookClick: (BookDetailTarget) -> Unit = {},
) {
  val state by viewModel.uiState.collectAsState()
  val sort by viewModel.sort.collectAsState()
  val query by viewModel.query.collectAsState()
  val homeState by homeViewModel.uiState.collectAsState()
  SearchScreen(
    state = state,
    sort = sort,
    query = query,
    homeState = homeState,
    registeredBookIds = registeredBookIds,
    onSearch = viewModel::search,
    onQueryChange = viewModel::updateQuery,
    onClear = viewModel::clear,
    onRegister = viewModel::register,
    onLoadMore = viewModel::loadMore,
    onSortSelect = viewModel::selectSort,
    onHomeRetry = homeViewModel::retry,
    modifier = modifier,
    onBack = onBack,
    onProfileClick = onProfileClick,
    onBookClick = onBookClick,
  )
}

@Composable
fun SearchScreen(
  state: SearchUiState,
  sort: BookSearchSort,
  query: String,
  homeState: HomeUiState = HomeUiState.Content(emptyList()),
  registeredBookIds: Set<String>,
  onSearch: (String) -> Unit,
  onQueryChange: (String) -> Unit,
  onClear: () -> Unit,
  onRegister: (BookSearchResult) -> Unit,
  onLoadMore: () -> Unit,
  onSortSelect: (BookSearchSort) -> Unit,
  onHomeRetry: () -> Unit = {},
  modifier: Modifier = Modifier,
  onBack: () -> Unit = {},
  onProfileClick: () -> Unit = {},
  onBookClick: (BookDetailTarget) -> Unit = {},
) {
  val leaveSearch = {
    onClear()
    onBack()
  }
  val navigationEventState = rememberNavigationEventState(NavigationEventInfo.None)

  NavigationBackHandler(
    state = navigationEventState,
    onBackCompleted = leaveSearch,
  )

  Column(modifier = modifier.fillMaxSize()) {
    BrandHeader(onProfileClick)
    SearchTopBar(
      query = query,
      onQueryChange = {
        onQueryChange(it)
        if (it.isEmpty()) onClear()
      },
      onSearch = { onSearch(query) },
    )

    when (val current = state) {
      SearchUiState.Idle -> DiscoverLanding(homeState, onBookClick, onHomeRetry, Modifier.weight(1f))
      SearchUiState.Loading -> SearchLoading(Modifier.weight(1f))
      SearchUiState.Empty ->
        Column(modifier = Modifier.weight(1f)) {
          SearchResultHeader(count = 0, sort = sort, onSortSelect = onSortSelect)
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
          totalCount = current.totalCount,
          nextPage = current.nextPage,
          sort = sort,
          registeredBookIds = registeredBookIds,
          onRegister = onRegister,
          onLoadMore = onLoadMore,
          onSortSelect = onSortSelect,
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
) {
  Row(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 16.dp, end = 24.dp)) {
    SearchField(
      query = query,
      onQueryChange = onQueryChange,
      onSearch = onSearch,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun DiscoverLanding(
  homeState: HomeUiState,
  onBookClick: (BookDetailTarget) -> Unit,
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  when (homeState) {
    HomeUiState.Loading -> SearchLoading(modifier)
    HomeUiState.Empty -> DiscoverContent(emptyList(), onBookClick, modifier)
    is HomeUiState.Failure -> HomeLoadErrorContent(homeState.error, onRetry, modifier)
    is HomeUiState.Content -> DiscoverContent(homeState.sections, onBookClick, modifier)
  }
}

@Composable
private fun DiscoverContent(
  sections: List<FeedSectionUiModel>,
  onBookClick: (BookDetailTarget) -> Unit,
  modifier: Modifier = Modifier,
) {
  val popular = sections.filterIsInstance<FeedSectionUiModel.TrendingBooks>().firstOrNull()?.books.orEmpty()
  val recent = sections.filterIsInstance<FeedSectionUiModel.RecentQuotes>().flatMap { it.cards }
  LazyColumn(modifier = modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 20.dp)) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 12.dp, end = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          "감상이 많은 책",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
        )
        Text(
          "전체 보기",
          color = ChaekInkSecondary,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        )
      }
      if (popular.isEmpty()) {
        Text(
          "인기 책 정보를 불러오면 표시돼요.",
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
        )
      } else {
        LazyRow(
          modifier = Modifier.fillMaxWidth().height(228.dp),
          contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 24.dp, top = 27.dp, end = 24.dp, bottom = 18.dp),
          horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
          itemsIndexed(popular, key = { _, it -> it.bookId.value }) { index, book ->
            DiscoverPopularBook(
              index + 1,
              book,
              onClick = { onBookClick(book.toBookDetailTarget()) },
              modifier = Modifier.analyticsImpression(
                contentType = "book",
                contentId = analyticsBookKey(book.isbn13, book.bookId.value) ?: book.bookId.value,
                bookKey = analyticsBookKey(book.isbn13, book.bookId.value),
                listId = "discover_popular_books",
                position = index + 1,
              ),
            )
          }
        }
        HorizontalDivider(
          modifier = Modifier.padding(horizontal = 24.dp),
          thickness = 3.dp,
          color = Color(0xFFDDDDDD),
        )
      }
    }
    item {
      Text(
        "방금 기록된 감상",
        modifier = Modifier.padding(start = 24.dp, top = 28.dp, bottom = 15.dp),
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
      )
    }
    if (recent.isEmpty()) {
      item {
        Text(
          "아직 도착한 감상이 없어요.",
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodySmall,
        )
      }
    } else {
      itemsIndexed(recent.take(4), key = { _, card -> card.noteId.value }) { index, card ->
        DiscoverReflection(
          card,
          onClick = { onBookClick(card.toBookDetailTarget()) },
          modifier = Modifier.analyticsImpression(
            contentType = "review",
            contentId = card.noteId.value,
            bookKey = analyticsBookKey(card.isbn13, card.bookId.value),
            listId = "discover_recent_reviews",
            position = index + 1,
            isOwn = false,
          ),
        )
      }
    }
  }
}

@Composable
private fun DiscoverPopularBook(
  rank: Int,
  book: TrendingBookUiModel,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.width(105.dp).clickable(role = Role.Button, onClick = onClick),
  ) {
    Box(modifier = Modifier.fillMaxWidth().height(135.dp)) {
      Box(
        modifier = Modifier.padding(start = 4.dp).size(width = 92.dp, height = 131.dp)
          .discoverPopularBookShadow().clip(RoundedCornerShape(2.dp)),
      ) {
        LocalRemoteBookCover.current(book.coverId, "${book.title} 표지", Modifier.fillMaxSize())
      }
      Box(
        Modifier.align(Alignment.BottomStart).size(width = 22.dp, height = 25.dp)
          .background(Color(0xFF191919), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
      ) {
        Text(rank.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black, lineHeight = 25.sp)
      }
    }
    Text(
      book.title,
      modifier = Modifier.padding(top = 10.dp),
      style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    Row(
      modifier = Modifier.padding(top = 4.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Text(
        book.noteCountLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
      )
      Text(
        book.replyCountLabel,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
      )
    }
  }
}

@Composable
private fun DiscoverReflection(
  card: QuoteCardUiModel,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier.padding(horizontal = 24.dp)) {
    HorizontalDivider(color = Color(0xFFE7E7E9))
    Row(
      modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(vertical = 14.dp),
      horizontalArrangement = Arrangement.spacedBy(13.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier.size(width = 52.dp, height = 74.dp)
          .discoverReflectionBookShadow().clip(RoundedCornerShape(2.dp)),
      ) {
        LocalRemoteBookCover.current(card.coverId, "${card.bookTitle} 표지", Modifier.fillMaxSize())
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(
          card.bookTitle,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          card.quoteText,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 20.15.sp),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            card.authorName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            card.timeLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
            maxLines = 1,
          )
        }
      }
      Box(Modifier.size(width = 24.dp, height = 44.dp), contentAlignment = Alignment.Center) {
        Text("›", fontSize = 17.sp, fontFamily = ChaekIconFontFamily())
      }
    }
  }
}

@Composable
private fun SearchField(
  query: String,
  onQueryChange: (String) -> Unit,
  onSearch: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(15.dp)
  BasicTextField(
    value = query,
    onValueChange = onQueryChange,
    modifier =
      modifier
        .height(54.dp)
        .background(Color(0xFFF3F3F5), shape)
        .padding(start = 15.dp),
    textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp),
    singleLine = true,
    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
    decorationBox = { field ->
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          painter = painterResource(Res.drawable.ic_search),
          contentDescription = null,
          modifier = Modifier.size(20.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f)) {
          if (query.isEmpty()) {
            Text(
              "책 제목이나 작가를 검색해요",
              style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
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
              painter = painterResource(Res.drawable.ic_close),
              contentDescription = "검색어 지우기",
              modifier = Modifier.size(18.dp),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          Box(modifier = Modifier.size(42.dp), contentAlignment = Alignment.Center) {
            Text(
              "→",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurface,
              fontFamily = ChaekIconFontFamily(),
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
  totalCount: Int,
  nextPage: Int?,
  sort: BookSearchSort,
  registeredBookIds: Set<String>,
  onRegister: (BookSearchResult) -> Unit,
  onLoadMore: () -> Unit,
  onSortSelect: (BookSearchSort) -> Unit,
  onBookClick: (BookDetailTarget) -> Unit,
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  val focusManager = LocalFocusManager.current
  LaunchedEffect(listState.isScrollInProgress) {
    if (listState.isScrollInProgress) focusManager.clearFocus()
  }
  Column(modifier = modifier.fillMaxWidth()) {
    SearchResultHeader(totalCount, sort, onSortSelect)
    LazyColumn(modifier = Modifier.weight(1f), state = listState) {
      itemsIndexed(results) { index, book ->
        SearchResultRow(
          book = book,
          isReading = book.registrationId() in registeredBookIds,
          onRegister = { onRegister(book) },
          onClick = { onBookClick(book.toBookDetailTarget()) },
          modifier = Modifier.analyticsImpression(
            contentType = "book",
            contentId = analyticsBookKey(book.isbn13, null) ?: "position:${index + 1}",
            bookKey = analyticsBookKey(book.isbn13, null),
            listId = "search_results",
            position = index + 1,
          ),
        )
        HorizontalDivider(color = ChaekBand)
      }
      if (nextPage != null) {
        item(key = "next-page-$nextPage") {
          LaunchedEffect(nextPage) { onLoadMore() }
        }
      }
    }
  }
}

@Composable
private fun SearchResultHeader(
  count: Int,
  sort: BookSearchSort,
  onSortSelect: (BookSearchSort) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Text(
        "ARCHIVE SEARCH",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
        color = ChaekAccentInk,
      )
      Text(
        "검색 결과 ${count}건",
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Normal),
        color = ChaekAccentInk.copy(alpha = 0.7f),
      )
    }
    Box {
      Row(
        modifier = Modifier.clickable(enabled = count > 0, role = Role.Button) { expanded = true },
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(sort.label, style = MaterialTheme.typography.bodySmall)
        Icon(
          painter = painterResource(Res.drawable.ic_chevron_down),
          contentDescription = "검색 결과 정렬",
          modifier = Modifier.size(20.dp),
        )
      }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        BookSearchSort.entries.forEach { option ->
          DropdownMenuItem(
            text = { Text(option.label) },
            onClick = {
              expanded = false
              onSortSelect(option)
            },
          )
        }
      }
    }
  }
  HorizontalDivider(color = ChaekBand)
}

private val BookSearchSort.label: String
  get() = when (this) {
    BookSearchSort.LATEST -> "최신순"
    BookSearchSort.COMMENT -> "감상 많은순"
  }

@Composable
private fun SearchResultRow(
  book: BookSearchResult,
  isReading: Boolean,
  onRegister: () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp),
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
        LocalRemoteBookCover.current(book.coverUrl, "${book.title} 표지", Modifier.fillMaxSize())
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
      Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(book.publisher, book.year).filter(String::isNotBlank).forEachIndexed { index, metadata ->
          Text(
            metadata,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (index == 0) 1f else 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
      Surface(
        modifier =
          Modifier
            .align(Alignment.End)
            .height(32.dp)
            .clickable(enabled = !isReading, role = Role.Button, onClick = onRegister),
        shape = RoundedCornerShape(6.dp),
        color = if (isReading) ChaekSurfaceMuted else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isReading) ChaekBorder else MaterialTheme.colorScheme.onSurface),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 10.dp),
          horizontalArrangement = Arrangement.spacedBy(4.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (!isReading) Text(
            "+",
            style = MaterialTheme.typography.labelMedium,
            fontFamily = ChaekIconFontFamily(),
          )
          Text(
            if (isReading) "읽는 중" else "읽는 중 시작",
            style = MaterialTheme.typography.labelMedium,
            color = if (isReading) ChaekInkSecondary else MaterialTheme.colorScheme.onSurface,
          )
        }
      }
    }
  }
}

@Composable
private fun SearchLoading(modifier: Modifier = Modifier) {
  var visible by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    delay(500)
    visible = true
  }
  if (visible) {
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

internal fun BookSearchResult.registrationId(): String =
  isbn13.ifBlank { listOf(title, creator, publisher, year).joinToString("|") }

private fun BookSearchResult.toBookDetailTarget() = BookDetailTarget(
  id = registrationId(),
  isbn13 = isbn13,
  title = title,
  creator = creator,
  publisher = publisher,
  year = year,
  category = category,
  totalPages = totalPages,
  coverUrl = coverUrl,
)

private fun TrendingBookUiModel.toBookDetailTarget() = BookDetailTarget(
  id = isbn13.ifBlank { bookId.value },
  isbn13 = isbn13,
  bookId = bookId.value.toLongOrNull(),
  title = title,
  coverId = coverId,
  coverUrl = coverId.takeIf { it.startsWith("https://") }.orEmpty(),
)

private fun QuoteCardUiModel.toBookDetailTarget() = BookDetailTarget(
  id = isbn13.ifBlank { bookId.value },
  isbn13 = isbn13,
  bookId = bookId.value.toLongOrNull(),
  title = bookTitle,
  coverId = coverId,
  coverUrl = coverId.takeIf { it.startsWith("https://") }.orEmpty(),
)
