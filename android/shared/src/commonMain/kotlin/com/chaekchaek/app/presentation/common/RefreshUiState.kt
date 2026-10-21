package com.chaekchaek.app.presentation.common

/** 조회 결과와 별도로 보관해 갱신 중에도 기존 화면과 입력을 유지한다. */
data class RefreshUiState(
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val enabled: Boolean = true,
)
