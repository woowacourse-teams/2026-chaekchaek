package com.chamsae.chaekchaek

import androidx.navigation3.runtime.NavKey
import com.chamsae.chaekchaek.ui.bookdetail.BookDetailArgs
import kotlinx.serialization.Serializable

@Serializable data object Main : NavKey

@Serializable data class BookDetailKey(val book: BookDetailArgs) : NavKey
