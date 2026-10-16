package com.chaekchaek.app.ui.search

class RecentSearchStorage(
    val read: () -> List<String> = { emptyList() },
    val write: (List<String>) -> Unit = {},
)
