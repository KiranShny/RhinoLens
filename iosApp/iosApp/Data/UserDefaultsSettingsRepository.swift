import Foundation
import Shared

@MainActor
final class UserDefaultsSettingsRepository: SettingsRepository {

    private let store: UserDefaults
    private let kTargetLang = "k_target_lang"
    private let kTheme = "k_theme"
    private let kDynamicColor = "k_dynamic_color"

    private let targetState: SwiftMutableStateFlow<Language>
    private let themeState: SwiftMutableStateFlow<ThemeMode>
    private let dynamicColorState: SwiftMutableStateFlow<KotlinBoolean>

    init(store: UserDefaults = .standard) {
        self.store = store
        let initialTarget = Self.readTarget(store: store)
        let initialTheme = Self.readTheme(store: store)
        let initialDynamic = (store.object(forKey: kDynamicColor) as? Bool) ?? true
        targetState = SwiftMutableStateFlow<Language>(initial: initialTarget)
        themeState = SwiftMutableStateFlow<ThemeMode>(initial: initialTheme)
        dynamicColorState = SwiftMutableStateFlow<KotlinBoolean>(initial: KotlinBoolean(bool: initialDynamic))
    }

    var targetLanguage: any Kotlinx_coroutines_coreFlow { targetState.flow }
    var theme: any Kotlinx_coroutines_coreFlow { themeState.flow }
    var dynamicColor: any Kotlinx_coroutines_coreFlow { dynamicColorState.flow }

    func setTargetLanguage(language: Language) async throws {
        store.set(language.code.value, forKey: kTargetLang)
        targetState.set(value: language)
    }

    func setTheme(theme: ThemeMode) async throws {
        store.set(theme.name, forKey: kTheme)
        themeState.set(value: theme)
    }

    func setDynamicColor(enabled: Bool) async throws {
        store.set(enabled, forKey: kDynamicColor)
        dynamicColorState.set(value: KotlinBoolean(bool: enabled))
    }

    private static func readTarget(store: UserDefaults) -> Language {
        if let code = store.string(forKey: "k_target_lang"),
           let language = Languages.shared.byCode(code: LanguageCode(value: code)) {
            return language
        }
        let tag = Locale.current.language.languageCode?.identifier ?? "en"
        return Languages.shared.byTag(tag: tag) ?? Languages.shared.default_
    }

    private static func readTheme(store: UserDefaults) -> ThemeMode {
        guard let name = store.string(forKey: "k_theme") else { return .system }
        switch name {
        case ThemeMode.light.name: return .light
        case ThemeMode.dark.name: return .dark
        default: return .system
        }
    }
}
