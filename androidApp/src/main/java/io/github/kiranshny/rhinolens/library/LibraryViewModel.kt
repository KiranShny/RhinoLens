package io.github.kiranshny.rhinolens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.kiranshny.rhinolens.AppContainer
import io.github.kiranshny.rhinolens.shared.domain.Capture
import io.github.kiranshny.rhinolens.shared.port.CaptureRepository
import java.io.File
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class LibraryViewModel(
    private val captureRepository: CaptureRepository,
    val filesRoot: File,
) : ViewModel() {

    val captures: StateFlow<List<Capture>> = captureRepository.observe()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = emptyList(),
        )

    companion object {
        fun factory(container: AppContainer, filesRoot: File): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    LibraryViewModel(
                        captureRepository = container.captureRepository,
                        filesRoot = filesRoot,
                    ) as T
            }
    }
}
