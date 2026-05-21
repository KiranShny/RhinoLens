import Foundation
import NaturalLanguage
import Shared

@available(iOS 18.0, *)
@MainActor
final class AppleTranslationEngine: TranslationEngine {

    func translate(text: String, source: LanguageCode?, target: LanguageCode) async throws -> String {
        return text
    }

    func detectLanguage(text: String) async throws -> LanguageCode? {
        let recognizer = NLLanguageRecognizer()
        recognizer.processString(text)
        guard let language = recognizer.dominantLanguage else { return nil }
        return LanguageCode(value: language.rawValue)
    }
}
