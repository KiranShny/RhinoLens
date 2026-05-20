import SwiftUI
import Shared

struct SettingsView: View {
    @EnvironmentObject var container: AppContainer
    @Binding var path: NavigationPath

    @State private var target: Language = Languages.shared.default_
    @State private var theme: ThemeMode = .system
    @State private var packs: [DownloadedPack] = []
    @State private var showClearConfirm = false
    @State private var showTargetPicker = false

    var body: some View {
        Form {
            Section("Languages") {
                Button {
                    showTargetPicker = true
                } label: {
                    HStack {
                        Text("Default target language")
                        Spacer()
                        Text("\(target.displayName) (\(target.nativeName))")
                            .foregroundStyle(.secondary)
                    }
                }
            }

            Section("Appearance") {
                Picker("Theme", selection: $theme) {
                    Text("System").tag(ThemeMode.system)
                    Text("Light").tag(ThemeMode.light)
                    Text("Dark").tag(ThemeMode.dark)
                }
                .onChange(of: theme) { _, new in
                    Task { try? await container.settingsRepository.setTheme(theme: new) }
                }
            }

            Section("Language packs") {
                if packs.isEmpty {
                    Text("No packs downloaded yet")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(packs, id: \.lang.code.value) { pack in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(pack.lang.displayName)
                                Text("~\(pack.sizeBytes / 1_000_000) MB")
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Button(role: .destructive) {
                                Task { try? await container.modelPackManager.delete(lang: pack.lang.code) }
                            } label: {
                                Image(systemName: "trash")
                            }
                        }
                    }
                }
            }

            Section("Storage") {
                Button(role: .destructive) {
                    showClearConfirm = true
                } label: {
                    Text("Clear capture history")
                }
            }
        }
        .navigationTitle("Settings")
        .task {
            for await lang in container.settingsRepository.targetLanguage {
                target = lang
            }
        }
        .task {
            for await mode in container.settingsRepository.theme {
                theme = mode
            }
        }
        .task {
            for await list in container.modelPackManager.observePacks() {
                packs = list
            }
        }
        .sheet(isPresented: $showTargetPicker) {
            LanguagePickerSheet(
                title: "Default target language",
                allowAuto: false,
                selected: target
            ) { lang in
                if let lang = lang {
                    Task { try? await container.settingsRepository.setTargetLanguage(language: lang) }
                }
                showTargetPicker = false
            }
        }
        .alert("Clear capture history?", isPresented: $showClearConfirm) {
            Button("Clear", role: .destructive) {
                Task { try? await container.captureRepository.clear() }
            }
            Button("Cancel", role: .cancel) { }
        } message: {
            Text("This permanently deletes every saved capture and its image. This cannot be undone.")
        }
    }
}
