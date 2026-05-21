import Foundation
import AVFoundation
import Combine
import SwiftUI
import UIKit
import Shared

@MainActor
final class CameraViewModel: NSObject, ObservableObject, AVCapturePhotoCaptureDelegate {

    @Published var blocks: [TranslatedBlock] = []
    @Published var source: Language?
    @Published var target: Language = Languages.shared.default_
    @Published var showSourcePicker = false
    @Published var showTargetPicker = false
    @Published var isCapturing = false

    private var ocrSource: VisionOcrSource?
    private var container: AppContainer?
    private var pairTask: Task<Void, Never>?
    private var streamTask: Task<Void, Never>?
    private var pendingCapture: ((String) -> Void)?

    var session: AVCaptureSession {
        ocrSource?.session ?? AVCaptureSession()
    }

    func bind(container: AppContainer) {
        self.container = container
        let source = VisionOcrSource { [weak self] frame in
            self?.relayFrame(frame: frame)
        }
        self.ocrSource = source
        source.start()

        observeFlow(container.settingsRepository.targetLanguage) { [weak self] value in
            guard let self, let lang = value as? Language else { return }
            Task { @MainActor in self.target = lang }
        }
    }

    func unbind() {
        ocrSource?.stop()
        ocrSource = nil
        pairTask?.cancel()
        streamTask?.cancel()
    }

    func setSource(_ language: Language?) {
        source = language
    }

    func setTarget(_ language: Language) {
        Task { try? await container?.settingsRepository.setTargetLanguage(language: language) }
    }

    func swap() {
        guard let s = source else { return }
        let oldTarget = target
        source = oldTarget
        Task { try? await container?.settingsRepository.setTargetLanguage(language: s) }
    }

    func capture(onSaved: @escaping (String) -> Void) {
        guard let ocrSource = ocrSource, !isCapturing else { return }
        isCapturing = true
        pendingCapture = onSaved
        let settings = AVCapturePhotoSettings()
        ocrSource.photoOutput.capturePhoto(with: settings, delegate: self)
    }

    nonisolated func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        Task { @MainActor [weak self] in
            await self?.handleCaptured(photo: photo)
        }
    }

    private func handleCaptured(photo: AVCapturePhoto) async {
        defer { isCapturing = false }
        guard let container = container,
              let data = photo.fileDataRepresentation(),
              let image = UIImage(data: data) else { return }

        guard let root = FileManager.default
            .urls(for: .applicationSupportDirectory, in: .userDomainMask).first else { return }
        let id = UUID().uuidString
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

        let pair = LanguagePair(source: source, target: target)
        let detected = blocks.first?.detectedSource ?? source ?? Languages.shared.default_
        let capture = Capture(
            id: id,
            createdAt: Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
                epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1000)
            ),
            imagePath: "captures/\(id).jpg",
            thumbnailPath: "captures/thumbs/\(id).jpg",
            pair: pair,
            detectedSource: detected,
            blocks: blocks
        )
        try? await container.captureRepository.save(capture: capture)
        pendingCapture?(id)
        pendingCapture = nil
    }

    private func relayFrame(frame: OcrFrame) {
        guard let container = container else { return }
        Task { [weak self] in
            guard let self = self else { return }
            let pair = LanguagePair(source: self.source, target: self.target)
            let translated = await self.translate(frame: frame, pair: pair, container: container)
            await MainActor.run { self.blocks = translated }
        }
    }

    private func translate(frame: OcrFrame, pair: LanguagePair, container: AppContainer) async -> [TranslatedBlock] {
        var results: [TranslatedBlock] = []
        for textBlock in frame.blocks where textBlock.confidence >= 0.5 {
            let translatedText = (try? await container.translationEngine.translate(
                text: textBlock.text,
                source: pair.source?.code,
                target: pair.target.code
            )) ?? textBlock.text
            results.append(TranslatedBlock(
                source: textBlock,
                translated: translatedText,
                detectedSource: pair.source ?? Languages.shared.default_,
                target: pair.target
            ))
        }
        return results
    }
}
