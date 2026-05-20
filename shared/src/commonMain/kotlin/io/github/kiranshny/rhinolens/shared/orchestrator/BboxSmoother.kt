package io.github.kiranshny.rhinolens.shared.orchestrator

import io.github.kiranshny.rhinolens.shared.domain.NormalizedRect

class BboxSmoother(private val alpha: Float = 0.6f) {

    private val state = mutableMapOf<String, NormalizedRect>()

    fun smooth(id: String, current: NormalizedRect): NormalizedRect {
        val previous = state[id]
        if (previous == null) {
            state[id] = current
            return current
        }
        val inverse = 1f - alpha
        val smoothed = NormalizedRect(
            left = previous.left * inverse + current.left * alpha,
            top = previous.top * inverse + current.top * alpha,
            right = previous.right * inverse + current.right * alpha,
            bottom = previous.bottom * inverse + current.bottom * alpha,
        )
        state[id] = smoothed
        return smoothed
    }

    fun forget(id: String) {
        state.remove(id)
    }

    fun reset() {
        state.clear()
    }
}
