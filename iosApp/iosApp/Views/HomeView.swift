import SwiftUI
import Shared

struct HomeView: View {
    @EnvironmentObject var container: AppContainer
    @Binding var path: NavigationPath

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Live translation")
                .font(.largeTitle)
                .bold()

            Text("Point your camera at foreign text and see it translated in place.")
                .foregroundStyle(.secondary)

            Button {
                path.append(Route.camera)
            } label: {
                HStack {
                    Image(systemName: "camera.fill")
                    Text("Start scanning")
                        .font(.headline)
                }
                .frame(maxWidth: .infinity, minHeight: 72)
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)

            Button {
                path.append(Route.importImage)
            } label: {
                HStack {
                    Image(systemName: "photo")
                    Text("Import image")
                }
                .frame(maxWidth: .infinity, minHeight: 56)
            }
            .buttonStyle(.bordered)

            Button("View captures") {
                path.append(Route.library)
            }
            .frame(maxWidth: .infinity)

            Spacer()
        }
        .padding(24)
        .navigationTitle("RhinoLens")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button {
                    path.append(Route.settings)
                } label: {
                    Image(systemName: "gearshape")
                }
            }
        }
    }
}
