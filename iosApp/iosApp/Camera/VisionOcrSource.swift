import Foundation
import AVFoundation
import Vision
import UIKit
import Shared

@MainActor
final class VisionOcrSource: NSObject, AVCaptureVideoDataOutputSampleBufferDelegate {

    let session = AVCaptureSession()
    private let queue = DispatchQueue(label: "rhinolens.camera.queue")
    private let videoOutput = AVCaptureVideoDataOutput()
    private(set) var photoOutput = AVCapturePhotoOutput()
    private var lastProcessedMs: Int64 = 0
    private let frameIntervalMs: Int64 = 1000 / 12
    private var frameCounter: Int64 = 0
    private var recognitionLanguages: [String] = ["en-US"]

    private let onFrame: (OcrFrame) -> Void

    init(onFrame: @escaping (OcrFrame) -> Void) {
        self.onFrame = onFrame
        super.init()
        configureSession()
    }

    func setRecognitionLanguages(_ languages: [String]) {
        if !languages.isEmpty {
            recognitionLanguages = languages
        }
    }

    func start() {
        if !session.isRunning {
            DispatchQueue.global(qos: .userInitiated).async { [weak self] in
                self?.session.startRunning()
            }
        }
    }

    func stop() {
        if session.isRunning {
            DispatchQueue.global(qos: .userInitiated).async { [weak self] in
                self?.session.stopRunning()
            }
        }
    }

    private func configureSession() {
        session.beginConfiguration()
        session.sessionPreset = .high

        if let device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back),
           let input = try? AVCaptureDeviceInput(device: device),
           session.canAddInput(input) {
            session.addInput(input)
        }

        videoOutput.alwaysDiscardsLateVideoFrames = true
        videoOutput.setSampleBufferDelegate(self, queue: queue)
        if session.canAddOutput(videoOutput) {
            session.addOutput(videoOutput)
        }
        if session.canAddOutput(photoOutput) {
            session.addOutput(photoOutput)
        }

        session.commitConfiguration()
    }

    nonisolated func captureOutput(
        _ output: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from connection: AVCaptureConnection
    ) {
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        Task { @MainActor [weak self] in
            guard let self = self else { return }
            if now - self.lastProcessedMs < self.frameIntervalMs { return }
            self.lastProcessedMs = now
        }

        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }

        let request = VNRecognizeTextRequest { [weak self] request, _ in
            guard let self = self else { return }
            let observations = request.results as? [VNRecognizedTextObservation] ?? []
            let blocks: [TextBlock] = observations.compactMap { obs in
                let candidate = obs.topCandidates(1).first
                guard let candidate = candidate else { return nil }
                let bbox = obs.boundingBox
                let rect = NormalizedRect(
                    left: Float(bbox.origin.x),
                    top: Float(1.0 - bbox.origin.y - bbox.size.height),
                    right: Float(bbox.origin.x + bbox.size.width),
                    bottom: Float(1.0 - bbox.origin.y)
                )
                return TextBlock(
                    id: "\(Int(bbox.origin.x * 64)):\(Int(bbox.origin.y * 64)):\(candidate.string.hashValue)",
                    text: candidate.string,
                    bbox: rect,
                    rotationDeg: 0,
                    confidence: candidate.confidence
                )
            }
            Task { @MainActor in
                self.frameCounter += 1
                self.onFrame(OcrFrame(
                    frameId: self.frameCounter,
                    timestampMs: now,
                    blocks: blocks
                ))
            }
        }
        request.recognitionLevel = .accurate
        request.usesLanguageCorrection = true
        request.recognitionLanguages = recognitionLanguages

        let handler = VNImageRequestHandler(cvPixelBuffer: pixelBuffer, orientation: .right, options: [:])
        try? handler.perform([request])
    }
}
