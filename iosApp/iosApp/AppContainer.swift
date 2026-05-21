import Foundation
import SwiftUI
import Combine
import Shared

@MainActor
final class AppContainer: ObservableObject {

    @Published var theme: ThemeMode = .system
    @Published var preferredColorScheme: ColorScheme? = nil

    let captureRepository: CaptureRepository
    let settingsRepository: SettingsRepository
    let translationEngine: TranslationEngine
    let modelPackManager: ModelPackManager
    let orchestrator: TranslationOrchestrator

    private var cancellables = Set<AnyCancellable>()

    init() {
        let captures = SwiftDataCaptureRepository()
        let settings = UserDefaultsSettingsRepository()
        let translation = AppleTranslationEngine()
        let packs = AppleTranslationModelManager()

        self.captureRepository = captures
        self.settingsRepository = settings
        self.translationEngine = translation
        self.modelPackManager = packs
        self.orchestrator = TranslationOrchestrator(
            translator: translation,
            cache: LruCache(maxSize: 256),
            smoothing: BboxSmoother(alpha: 0.6),
            confidenceFloor: 0.5,
            maxInFlight: 4,
            autoDetectSampleLength: 200
        )

        observeTheme(settings: settings)
    }

    private func observeTheme(settings: SettingsRepository) {
        observeFlow(settings.theme) { [weak self] value in
            guard let self, let mode = value as? ThemeMode else { return }
            Task { @MainActor in
                self.theme = mode
                self.preferredColorScheme = Self.colorScheme(for: mode)
            }
        }
    }

    static func colorScheme(for mode: ThemeMode) -> ColorScheme? {
        switch mode {
        case .light: return .light
        case .dark: return .dark
        default: return nil
        }
    }
}
