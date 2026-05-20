import Foundation
import Shared

@MainActor
final class UserDefaultsSettingsRepository: SettingsRepository {

    private let store: UserDefaults
    private let kTargetLang = "k_target_lang"
    private let kTheme = "k_theme"
    private let kDynamicColor = "k_dynamic_color"

    private let targetStream = AsyncStream<Language>.makeStream()
    private let themeStream = AsyncStream<ThemeMode>.makeStream()
    private let dynamicColorStream = AsyncStream<Bool>.makeStream()

    init(store: UserDefaults = .standard) {
        self.store = store
        emitCurrent()
    }

    var targetLanguage: AsyncStream<Language> { targetStream.stream }
    var theme: AsyncStream<ThemeMode> { themeStream.stream }
    var dynamicColor: AsyncStream<Bool> { dynamicColorStream.stream }

    func setTargetLanguage(language: Language) async throws {
        store.set(language.code.value, forKey: kTargetLang)
        targetStream.continuation.yield(language)
    }

    func setTheme(theme: ThemeMode) async throws {
        store.set(theme.name, forKey: kTheme)
        themeStream.continuation.yield(theme)
    }

    func setDynamicColor(enabled: Bool) async throws {
        store.set(enabled, forKey: kDynamicColor)
        dynamicColorStream.continuation.yield(enabled)
    }

    private func emitCurrent() {
        let lang = readTarget()
        targetStream.continuation.yield(lang)
        themeStream.continuation.yield(readTheme())
        dynamicColorStream.continuation.yield(store.object(forKey: kDynamicColor) as? Bool ?? true)
    }

    private func readTarget() -> Language {
        if let code = store.string(forKey: kTargetLang),
           let language = Languages.shared.byCode(code: LanguageCode(value: code)) {
            return language
        }
        let tag = Locale.current.language.languageCode?.identifier ?? "en"
        return Languages.shared.byTag(tag: tag) ?? Languages.shared.default_
    }

    private func readTheme() -> ThemeMode {
        if let name = store.string(forKey: kTheme), let mode = ThemeMode.allCases.first(where: { $0.name == name }) {
            return mode
        }
        return .system
    }
}
