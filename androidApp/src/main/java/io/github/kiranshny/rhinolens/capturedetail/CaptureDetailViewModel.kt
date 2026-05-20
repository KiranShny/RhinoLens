package io.github.kiranshny.rhinolens.capturedetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.kiranshny.rhinolens.AppContainer
import io.github.kiranshny.rhinolens.shared.domain.Capture
import io.github.kiranshny.rhinolens.shared.port.CaptureRepository
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CaptureDetailViewModel(
    private val captureId: String,
    private val captureRepository: CaptureRepository,
    val filesRoot: File,
) : ViewModel() {

    private val _capture = MutableStateFlow<Capture?>(null)
    val capture: StateFlow<Capture?> = _capture.asStateFlow()

    init {
        viewModelScope.launch {
            _capture.value = captureRepository.get(captureId)
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            captureRepository.delete(captureId)
            onDone()
        }
    }

    companion object {
        fun factory(
            captureId: String,
            container: AppContainer,
            filesRoot: File,
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    CaptureDetailViewModel(
                        captureId = captureId,
                        captureRepository = container.captureRepository,
                        filesRoot = filesRoot,
                    ) as T
            }
    }
}
