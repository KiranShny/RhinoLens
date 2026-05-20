import Foundation
import Translation
import Shared

@available(iOS 17.4, *)
@MainActor
final class AppleTranslationModelManager: ModelPackManager {

    private let stream = AsyncStream<[DownloadedPack]>.makeStream()

    init() {
        Task { await refresh() }
    }

    func observePacks() -> AsyncStream<[DownloadedPack]> {
        stream.stream
    }

    func download(lang: LanguageCode) -> AsyncStream<DownloadProgress> {
        AsyncStream { continuation in
            Task {
                continuation.yield(.InProgress(lang: lang, percent: 0))
                let config = TranslationSession.Configuration(target: Locale.Language(identifier: lang.value))
                let session = TranslationSession(installedSource: nil, target: config.target)
                do {
                    try await session.prepareTranslation()
                    continuation.yield(.Done(lang: lang))
                    await refresh()
                } catch {
                    continuation.yield(.Failed(lang: lang, error: RhinoError.ModelDownloadFailed(lang: lang, cause: error.localizedDescription)))
                }
                continuation.finish()
            }
        }
    }

    func delete(lang: LanguageCode) async throws {}

    func isReady(source: LanguageCode?, target: LanguageCode) async throws -> Bool {
        let availability = LanguageAvailability()
        let sourceLanguage = source.map { Locale.Language(identifier: $0.value) }
        let targetLanguage = Locale.Language(identifier: target.value)
        let status = await availability.status(from: sourceLanguage, to: targetLanguage)
        return status == .installed
    }

    private func refresh() async {
        stream.continuation.yield([])
    }
}
