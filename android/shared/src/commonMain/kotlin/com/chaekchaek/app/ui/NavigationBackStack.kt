package com.chaekchaek.app.ui

internal fun <T : Any> MutableList<T>.navigateFromCurrent(
    current: T,
    destination: T,
): Boolean {
    if (lastOrNull() != current || destination == current) return false

    add(destination)
    return true
}

internal fun <T : Any> MutableList<T>.popTopDestination(): Boolean {
    if (size <= 1) return false

    removeAt(lastIndex)
    return true
}

internal fun <T : Any> MutableList<T>.popDestinationIfCurrent(destination: T): Boolean {
    if (lastOrNull() != destination) return false

    return popTopDestination()
}

internal fun <T : Any> List<T>.isCurrentDestination(destination: T): Boolean =
    lastOrNull() == destination

internal fun <T : Any> MutableList<T>.prepareRootExit(root: T): Boolean {
    if (isCurrentDestination(root)) return true

    popTopDestination()
    return false
}

/** 이미 열린 공개 서재는 기존 entry로 돌아가 필터와 목록 위치를 보존한다. */
internal fun <T : Any> MutableList<T>.navigateToExistingFromCurrent(
    current: T,
    destination: T,
    matches: (T) -> Boolean = { it == destination },
): Boolean {
    if (lastOrNull() != current) return false
    val existing = indexOfLast(matches)
    if (existing == lastIndex) return false
    if (existing >= 0) {
        while (lastIndex > existing) removeAt(lastIndex)
    } else add(destination)
    return true
}
