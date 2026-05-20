import UIKit
import Vision
import Shared

enum OneShotVisionOcr {

    static func recognize(image: UIImage) async -> [TextBlock] {
        guard let cgImage = image.cgImage else { return [] }
        let width = CGFloat(cgImage.width)
        let height = CGFloat(cgImage.height)
        return await withCheckedContinuation { continuation in
            let request = VNRecognizeTextRequest { request, _ in
                let observations = request.results as? [VNRecognizedTextObservation] ?? []
                let blocks: [TextBlock] = observations.compactMap { obs in
                    let candidate = obs.topCandidates(1).first
                    guard let candidate = candidate else { return nil }
                    let bbox = obs.boundingBox
                    return TextBlock(
                        id: "\(Int(bbox.origin.x * 64)):\(Int(bbox.origin.y * 64)):\(candidate.string.hashValue)",
                        text: candidate.string,
                        bbox: NormalizedRect(
                            left: Float(bbox.origin.x),
                            top: Float(1.0 - bbox.origin.y - bbox.size.height),
                            right: Float(bbox.origin.x + bbox.size.width),
                            bottom: Float(1.0 - bbox.origin.y)
                        ),
                        rotationDeg: 0,
                        confidence: candidate.confidence
                    )
                }
                continuation.resume(returning: blocks)
            }
            request.recognitionLevel = .accurate
            request.usesLanguageCorrection = true

            let handler = VNImageRequestHandler(cgImage: cgImage, options: [:])
            do {
                try handler.perform([request])
            } catch {
                continuation.resume(returning: [])
            }
            _ = width
            _ = height
        }
    }
}
