import Foundation
import Translation
import NaturalLanguage
import Shared

@available(iOS 17.4, *)
@MainActor
final class AppleTranslationEngine: TranslationEngine {

    private var sessions: [String: TranslationSession] = [:]

    func translate(text: String, source: LanguageCode?, target: LanguageCode) async throws -> String {
        let sourceTag = source?.value
        let targetTag = target.value
        let sessionKey = "\(sourceTag ?? "auto")>\(targetTag)"

        let configuration = TranslationSession.Configuration(
            source: sourceTag.map(Locale.Language.init(identifier:)),
            target: Locale.Language(identifier: targetTag)
        )
        let session = sessions[sessionKey] ?? TranslationSession(installedSource: configuration.source, target: configuration.target)
        sessions[sessionKey] = session

        let response = try await session.translate(text)
        return response.targetText
    }

    func detectLanguage(text: String) async throws -> LanguageCode? {
        let recognizer = NLLanguageRecognizer()
        recognizer.processString(text)
        guard let language = recognizer.dominantLanguage else { return nil }
        return LanguageCode(value: language.rawValue)
    }
}
