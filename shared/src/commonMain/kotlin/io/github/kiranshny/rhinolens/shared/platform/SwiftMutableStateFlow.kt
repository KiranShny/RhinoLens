package io.github.kiranshny.rhinolens.shared.platform

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SwiftMutableStateFlow<T : Any>(initial: T) {

    private val state = MutableStateFlow(initial)
    val flow: Flow<T> = state

    fun get(): T = state.value
    fun set(value: T) {
        state.value = value
    }
}
