import Foundation
import Translation
import Shared

@available(iOS 18.0, *)
@MainActor
final class AppleTranslationModelManager: ModelPackManager {

    private let packsState = SwiftMutableStateFlow<NSArray>(initial: NSArray())

    func observePacks() -> any Kotlinx_coroutines_coreFlow {
        packsState.flow
    }

    func download(lang: LanguageCode) -> any Kotlinx_coroutines_coreFlow {
        let progress = SwiftMutableStateFlow<DownloadProgress>(initial: DownloadProgressDone(lang: lang))
        return progress.flow
    }

    func delete(lang: LanguageCode) async throws {}

    func isReady(source: LanguageCode?, target: LanguageCode) async throws -> KotlinBoolean {
        let availability = LanguageAvailability()
        let targetLanguage = Locale.Language(identifier: target.value)
        let sourceLanguage = Locale.Language(identifier: source?.value ?? "en")
        let status = await availability.status(from: sourceLanguage, to: targetLanguage)
        return KotlinBoolean(bool: status == .installed)
    }
}
