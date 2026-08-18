package com.chamsae.chaekchaek

import android.content.Context
import com.chamsae.chaekchaek.data.AladinBookSearchRepository
import com.chamsae.chaekchaek.data.BookSearchRepository
import com.chamsae.chaekchaek.data.LibraryRepository
import com.chamsae.chaekchaek.data.PreferencesLibraryRepository

class AppContainer(context: Context) {
  val bookSearchRepository: BookSearchRepository = AladinBookSearchRepository()
  val libraryRepository: LibraryRepository = PreferencesLibraryRepository(context.applicationContext)
}
