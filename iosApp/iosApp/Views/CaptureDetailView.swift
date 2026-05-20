import SwiftUI
import Shared

struct CaptureDetailView: View {
    @EnvironmentObject var container: AppContainer
    let captureId: String
    @Binding var path: NavigationPath

    @State private var capture: Capture?

    var body: some View {
        ZStack {
            if let capture = capture, let url = appPrivateURL(for: capture.imagePath) {
                AsyncImage(url: url) { image in
                    image.resizable().scaledToFit()
                } placeholder: {
                    ProgressView()
                }
                AROverlayView(blocks: capture.blocks)
            } else {
                ProgressView()
            }
        }
        .background(.black)
        .navigationTitle("Capture")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                if let capture = capture, let url = appPrivateURL(for: capture.imagePath) {
                    ShareLink(item: url)
                }
            }
            ToolbarItem(placement: .navigationBarTrailing) {
                if capture != nil {
                    Button(role: .destructive) {
                        Task {
                            try? await container.captureRepository.delete(id: captureId)
                            path.removeLast()
                        }
                    } label: {
                        Image(systemName: "trash")
                    }
                }
            }
        }
        .task {
            capture = try? await container.captureRepository.get(id: captureId)
        }
    }
}

private func appPrivateURL(for relative: String) -> URL? {
    let root = FileManager.default
        .urls(for: .applicationSupportDirectory, in: .userDomainMask).first
    return root?.appendingPathComponent(relative)
}
