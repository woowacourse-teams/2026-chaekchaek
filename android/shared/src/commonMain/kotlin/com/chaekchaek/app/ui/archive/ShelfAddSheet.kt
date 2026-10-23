package com.chaekchaek.app.ui.archive

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.foundation.text.input.TextFieldLineLimits
import com.chaekchaek.app.ui.input.rememberLimitedTextFieldState
import com.chaekchaek.app.presentation.common.withDelayedApiLoading
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.chaekchaek.app.data.remote.BookSearchRemoteRepository
import com.chaekchaek.app.data.remote.Bookshelf
import com.chaekchaek.app.domain.book.BookSearchResult
import com.chaekchaek.app.domain.book.BookSearchSort
import com.chaekchaek.app.presentation.common.ErrorOperation
import com.chaekchaek.app.presentation.common.ErrorRecoveryState
import com.chaekchaek.app.ui.bookdetail.BookDetailArgs
import com.chaekchaek.app.ui.common.ErrorRecoveryEffect
import com.chaekchaek.app.ui.input.TextInputPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun ShelfAddSheet(shelves: List<Bookshelf>, onDismiss: () -> Unit, onAdd: (BookDetailArgs) -> Unit) {
    var search by remember { mutableStateOf(false) }
    val inputPolicy = remember { TextInputPolicy(100) }
    val query = rememberLimitedTextFieldState(inputPolicy)
    var showLoading by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf("") }
    var results by remember { mutableStateOf(emptyList<BookSearchResult>()) }
    var nextPage by remember { mutableStateOf<Int?>(null) }
    var loading by remember { mutableStateOf(false) }
    val repository = remember { BookSearchRemoteRepository() }
    val errors = remember { ErrorRecoveryState() }
    val scope = rememberCoroutineScope()
    fun load(term: String, page: Int) {
        if (!loading && term.isNotBlank()) scope.launch {
            loading = true
            try {
                val result = withDelayedApiLoading({ showLoading = it }) { repository.search(term, BookSearchSort.LATEST, page) }
                results = if (page == 1) result.items else (results + result.items).distinctBy { it.isbn13 }
                submitted = term
                nextPage = result.nextPage
            } catch (e: CancellationException) { throw e }
            catch (e: Throwable) { errors.report(e, ErrorOperation.Search) { load(term, page) } }
            finally { loading = false }
        }
    }
    ErrorRecoveryEffect(errors)
    ShelfSheet("책 담기", onDismiss) {
        Row { TextButton(onClick = { search = false }) { Text("내 서재에서") }; TextButton(onClick = { search = true }) { Text("책 검색") } }
        if (search) {
            OutlinedTextField(query, inputTransformation = inputPolicy.inputTransformation, label = { Text("제목 · 저자 · ISBN") }, lineLimits = TextFieldLineLimits.SingleLine, modifier = Modifier.fillMaxWidth())
            Button(onClick = { load(query.text.toString(), 1) }, enabled = !loading) { Text("검색") }
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(results, key = { it.isbn13 }) { book ->
                    BookShelfRow(book.title, book.creator, book.coverUrl) {
                        onAdd(BookDetailArgs(book.isbn13, book.isbn13, title = book.title, creator = book.creator, publisher = book.publisher, year = book.year, category = book.category, coverUrl = book.coverUrl, totalPages = book.totalPages))
                    }
                }
                if (submitted.isNotEmpty() && results.isEmpty() && !loading) item { Text("검색 결과가 없어요") }
                if (nextPage != null) item { TextButton(onClick = { nextPage?.let { load(submitted, it) } }, enabled = !loading) { Text("더 보기") } }
            }
        } else {
            val books = shelves.flatMap { it.items }.distinctBy { it.bookId }
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                items(books, key = { it.bookId }) { book ->
                    BookShelfRow(book.title, "", "") { onAdd(book.asArchive().asArgs()) }
                }
                if (books.isEmpty()) item { Text("책 검색에서 첫 책을 골라 보세요") }
            }
        }
        if (showLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}
