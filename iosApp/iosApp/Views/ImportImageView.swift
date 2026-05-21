import SwiftUI
import PhotosUI
import Shared

struct ImportImageView: View {
    @EnvironmentObject var container: AppContainer
    @Binding var path: NavigationPath

    @State private var selectedItem: PhotosPickerItem?
    @State private var selectedImage: UIImage?
    @State private var blocks: [TranslatedBlock] = []
    @State private var isProcessing = false
    @State private var savedId: String?

    var body: some View {
        VStack {
            if let image = selectedImage {
                ZStack {
                    Image(uiImage: image)
                        .resizable()
                        .scaledToFit()
                    AROverlayView(blocks: blocks)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(.black)

                HStack(spacing: 12) {
                    Button(role: .destructive) {
                        path.removeLast()
                    } label: {
                        Text("Discard").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)

                    Button {
                        save(image: image)
                    } label: {
                        Text("Save to library").frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding(16)
            } else {
                PhotosPicker(selection: $selectedItem, matching: .images) {
                    Text("Pick an image")
                }
                .padding(24)
            }
        }
        .navigationTitle("Import image")
        .onChange(of: selectedItem) { _, newItem in
            Task { await load(item: newItem) }
        }
        .onChange(of: savedId) { _, newId in
            if let id = newId {
                path.removeLast()
                path.append(Route.captureDetail(id: id))
            }
        }
    }

    private func load(item: PhotosPickerItem?) async {
        guard let item = item else { return }
        guard let data = try? await item.loadTransferable(type: Data.self) else { return }
        guard let image = UIImage(data: data) else { return }
        selectedImage = image
        await runOcrAndTranslate(image: image)
    }

    private func runOcrAndTranslate(image: UIImage) async {
        isProcessing = true
        let recognized = await OneShotVisionOcr.recognize(image: image)
        let target = (await firstFlowValue(container.settingsRepository.targetLanguage) as? Language) ?? Languages.shared.default_
        var translated: [TranslatedBlock] = []
        for textBlock in recognized {
            let result = (try? await container.translationEngine.translate(
                text: textBlock.text,
                source: nil,
                target: target.code
            )) ?? textBlock.text
            translated.append(TranslatedBlock(
                source: textBlock,
                translated: result,
                detectedSource: Languages.shared.default_,
                target: target
            ))
        }
        blocks = translated
        isProcessing = false
    }

    private func save(image: UIImage) {
        Task {
            let id = UUID().uuidString
            guard let root = FileManager.default
                .urls(for: .applicationSupportDirectory, in: .userDomainMask).first else { return }
            let capturesDir = root.appendingPathComponent("captures")
            let thumbsDir = capturesDir.appendingPathComponent("thumbs")
            try? FileManager.default.createDirectory(at: capturesDir, withIntermediateDirectories: true)
            try? FileManager.default.createDirectory(at: thumbsDir, withIntermediateDirectories: true)

            let imageUrl = capturesDir.appendingPathComponent("\(id).jpg")
            let thumbUrl = thumbsDir.appendingPathComponent("\(id).jpg")
            if let jpeg = image.jpegData(compressionQuality: 0.9) {
                try? jpeg.write(to: imageUrl)
            }
            if let thumb = image.scaled(toMaxEdge: 300).jpegData(compressionQuality: 0.8) {
                try? thumb.write(to: thumbUrl)
            }

            let target = (await firstFlowValue(container.settingsRepository.targetLanguage) as? Language) ?? Languages.shared.default_
            let capture = Capture(
                id: id,
                createdAt: Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
                    epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1000)
                ),
                imagePath: "captures/\(id).jpg",
                thumbnailPath: "captures/thumbs/\(id).jpg",
                pair: LanguagePair(source: nil, target: target),
                detectedSource: Languages.shared.default_,
                blocks: blocks
            )
            try? await container.captureRepository.save(capture: capture)
            savedId = id
        }
    }
}

extension UIImage {
    func scaled(toMaxEdge maxEdge: CGFloat) -> UIImage {
        let maxSide = max(size.width, size.height)
        guard maxSide > maxEdge else { return self }
        let ratio = maxEdge / maxSide
        let newSize = CGSize(width: size.width * ratio, height: size.height * ratio)
        let format = UIGraphicsImageRendererFormat.default()
        format.scale = 1
        let renderer = UIGraphicsImageRenderer(size: newSize, format: format)
        return renderer.image { _ in
            draw(in: CGRect(origin: .zero, size: newSize))
        }
    }
}
