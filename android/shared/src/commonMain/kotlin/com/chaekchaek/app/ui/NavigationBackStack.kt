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
