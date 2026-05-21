import SwiftUI
import Shared

struct LibraryView: View {
    @EnvironmentObject var container: AppContainer
    @Binding var path: NavigationPath

    @State private var captures: [Capture] = []
    private let columns = [
        GridItem(.flexible(), spacing: 8),
        GridItem(.flexible(), spacing: 8),
        GridItem(.flexible(), spacing: 8),
    ]

    var body: some View {
        Group {
            if captures.isEmpty {
                Text("No captures yet")
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    LazyVGrid(columns: columns, spacing: 8) {
                        ForEach(captures, id: \.id) { capture in
                            Button {
                                path.append(Route.captureDetail(id: capture.id))
                            } label: {
                                CaptureCell(capture: capture)
                            }
                        }
                    }
                    .padding(8)
                }
            }
        }
        .navigationTitle("Library")
        .task {
            observeFlow(container.captureRepository.observe()) { value in
                if let array = value as? NSArray {
                    captures = array.compactMap { $0 as? Capture }
                }
            }
        }
    }
}

private struct CaptureCell: View {
    let capture: Capture

    var body: some View {
        let url = appPrivateURL(for: capture.imagePath)
        AsyncImage(url: url) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            Color.gray.opacity(0.2)
        }
        .frame(width: 120, height: 120)
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .contentShape(Rectangle())
    }
}

private func appPrivateURL(for relative: String) -> URL? {
    let root = FileManager.default
        .urls(for: .applicationSupportDirectory, in: .userDomainMask).first
    return root?.appendingPathComponent(relative)
}
