package com.chamsae.chaekchaek

import android.content.Context
import com.chamsae.chaekchaek.data.LibraryRepository
import com.chamsae.chaekchaek.data.PreferencesLibraryRepository
import com.chaekchaek.app.data.remote.BookSearchRemoteRepository
import com.chaekchaek.app.domain.book.BookSearchRepository

class AppContainer(context: Context) {
  val bookSearchRepository: BookSearchRepository = BookSearchRemoteRepository()
  val libraryRepository: LibraryRepository = PreferencesLibraryRepository(context.applicationContext)
}
