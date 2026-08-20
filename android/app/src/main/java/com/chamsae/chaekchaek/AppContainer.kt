package com.chamsae.chaekchaek

import android.content.Context
import com.chamsae.chaekchaek.auth.AuthSession
import com.chamsae.chaekchaek.data.BookRatingStore
import com.chamsae.chaekchaek.data.LibraryRepository
import com.chamsae.chaekchaek.data.ServerLibraryRepository
import com.chaekchaek.app.data.remote.BookDetailRemoteRepository
import com.chaekchaek.app.data.remote.BookSearchRemoteRepository
import com.chaekchaek.app.data.remote.MobileAuthRemoteRepository
import com.chaekchaek.app.domain.book.BookSearchRepository

class AppContainer(context: Context) {
  val bookSearchRepository: BookSearchRepository = BookSearchRemoteRepository()
  val bookDetailRepository = BookDetailRemoteRepository()
  val mobileAuthRepository = MobileAuthRemoteRepository()
  val authSession = AuthSession()
  val bookRatingStore = BookRatingStore(context.applicationContext)
  val libraryRepository: LibraryRepository = ServerLibraryRepository(context.applicationContext, authSession)
}
