import Foundation
import SwiftData
import Shared

@Model
final class CaptureEntity {
    @Attribute(.unique) var id: String
    var createdAt: Date
    var imagePath: String
    var thumbnailPath: String
    var sourceLangCode: String?
    var detectedLangCode: String
    var targetLangCode: String
    var blocksJson: String

    init(
        id: String,
        createdAt: Date,
        imagePath: String,
        thumbnailPath: String,
        sourceLangCode: String?,
        detectedLangCode: String,
        targetLangCode: String,
        blocksJson: String
    ) {
        self.id = id
        self.createdAt = createdAt
        self.imagePath = imagePath
        self.thumbnailPath = thumbnailPath
        self.sourceLangCode = sourceLangCode
        self.detectedLangCode = detectedLangCode
        self.targetLangCode = targetLangCode
        self.blocksJson = blocksJson
    }
}

@MainActor
final class SwiftDataCaptureRepository: CaptureRepository {

    private let container: ModelContainer
    private let context: ModelContext
    private let subject = AsyncStream<[Capture]>.makeStream()
    private let encoder = JSONEncoder()
    private let decoder = JSONDecoder()

    init() {
        let schema = Schema([CaptureEntity.self])
        self.container = try! ModelContainer(for: schema)
        self.context = ModelContext(container)
        encoder.dateEncodingStrategy = .millisecondsSince1970
        decoder.dateDecodingStrategy = .millisecondsSince1970
        Task { await refresh() }
    }

    func observe() -> AsyncStream<[Capture]> {
        subject.stream
    }

    func get(id: String) async throws -> Capture? {
        let predicate = #Predicate<CaptureEntity> { $0.id == id }
        let descriptor = FetchDescriptor<CaptureEntity>(predicate: predicate)
        return try context.fetch(descriptor).first.map(toDomain)
    }

    func save(c: Capture) async throws {
        let entity = try toEntity(c)
        let predicate = #Predicate<CaptureEntity> { $0.id == c.id }
        let existing = try context.fetch(FetchDescriptor<CaptureEntity>(predicate: predicate)).first
        if let existing = existing {
            context.delete(existing)
        }
        context.insert(entity)
        try context.save()
        await refresh()
    }

    func delete(id: String) async throws {
        let predicate = #Predicate<CaptureEntity> { $0.id == id }
        let descriptor = FetchDescriptor<CaptureEntity>(predicate: predicate)
        if let entity = try context.fetch(descriptor).first {
            context.delete(entity)
            try context.save()
        }
        await refresh()
    }

    func clear() async throws {
        try context.delete(model: CaptureEntity.self)
        try context.save()
        await refresh()
    }

    private func refresh() async {
        let descriptor = FetchDescriptor<CaptureEntity>(
            sortBy: [SortDescriptor(\.createdAt, order: .reverse)]
        )
        let entities = (try? context.fetch(descriptor)) ?? []
        subject.continuation.yield(entities.map(toDomain))
    }

    private func toDomain(_ entity: CaptureEntity) -> Capture {
        let target = Languages.shared.byCode(code: LanguageCode(value: entity.targetLangCode))
            ?? Languages.shared.default_
        let source = entity.sourceLangCode.flatMap { Languages.shared.byCode(code: LanguageCode(value: $0)) }
        let detected = Languages.shared.byCode(code: LanguageCode(value: entity.detectedLangCode))
            ?? Languages.shared.default_
        let blocks: [TranslatedBlock] = (try? decoder.decode([TranslatedBlockDTO].self, from: Data(entity.blocksJson.utf8)))?
            .compactMap { $0.toDomain() } ?? []
        return Capture(
            id: entity.id,
            createdAt: KotlinxDatetimeInstant.companion.fromEpochMilliseconds(epochMilliseconds: Int64(entity.createdAt.timeIntervalSince1970 * 1000)),
            imagePath: entity.imagePath,
            thumbnailPath: entity.thumbnailPath,
            pair: LanguagePair(source: source, target: target),
            detectedSource: detected,
            blocks: blocks
        )
    }

    private func toEntity(_ capture: Capture) throws -> CaptureEntity {
        let dtoList = capture.blocks.map { TranslatedBlockDTO(from: $0) }
        let data = try encoder.encode(dtoList)
        let blocksJson = String(data: data, encoding: .utf8) ?? "[]"
        return CaptureEntity(
            id: capture.id,
            createdAt: Date(timeIntervalSince1970: Double(capture.createdAt.toEpochMilliseconds()) / 1000),
            imagePath: capture.imagePath,
            thumbnailPath: capture.thumbnailPath,
            sourceLangCode: capture.pair.source?.code.value,
            detectedLangCode: capture.detectedSource.code.value,
            targetLangCode: capture.pair.target.code.value,
            blocksJson: blocksJson
        )
    }
}

private struct TranslatedBlockDTO: Codable {
    let id: String
    let text: String
    let left: Float
    let top: Float
    let right: Float
    let bottom: Float
    let rotationDeg: Float
    let confidence: Float
    let translated: String
    let detectedCode: String
    let targetCode: String

    init(from block: TranslatedBlock) {
        self.id = block.source.id
        self.text = block.source.text
        self.left = block.source.bbox.left
        self.top = block.source.bbox.top
        self.right = block.source.bbox.right
        self.bottom = block.source.bbox.bottom
        self.rotationDeg = block.source.rotationDeg
        self.confidence = block.source.confidence
        self.translated = block.translated
        self.detectedCode = block.detectedSource.code.value
        self.targetCode = block.target.code.value
    }

    func toDomain() -> TranslatedBlock? {
        let detected = Languages.shared.byCode(code: LanguageCode(value: detectedCode)) ?? Languages.shared.default_
        let target = Languages.shared.byCode(code: LanguageCode(value: targetCode)) ?? Languages.shared.default_
        let source = TextBlock(
            id: id,
            text: text,
            bbox: NormalizedRect(left: left, top: top, right: right, bottom: bottom),
            rotationDeg: rotationDeg,
            confidence: confidence
        )
        return TranslatedBlock(
            source: source,
            translated: translated,
            detectedSource: detected,
            target: target
        )
    }
}
