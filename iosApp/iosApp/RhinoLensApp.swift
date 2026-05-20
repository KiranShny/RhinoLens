import SwiftUI
import Shared

@main
struct RhinoLensApp: App {

    @StateObject private var container = AppContainer()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(container)
                .preferredColorScheme(container.preferredColorScheme)
        }
    }
}

struct RootView: View {
    @EnvironmentObject var container: AppContainer
    @State private var path = NavigationPath()

    var body: some View {
        NavigationStack(path: $path) {
            HomeView(path: $path)
                .navigationDestination(for: Route.self) { route in
                    switch route {
                    case .camera:
                        CameraView(path: $path)
                    case .library:
                        LibraryView(path: $path)
                    case .importImage:
                        ImportImageView(path: $path)
                    case .settings:
                        SettingsView(path: $path)
                    case .captureDetail(let id):
                        CaptureDetailView(captureId: id, path: $path)
                    }
                }
        }
    }
}

enum Route: Hashable {
    case camera
    case library
    case importImage
    case settings
    case captureDetail(id: String)
}
