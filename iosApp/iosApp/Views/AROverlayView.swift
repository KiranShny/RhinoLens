import SwiftUI
import Shared

struct AROverlayView: View {
    let blocks: [TranslatedBlock]

    var body: some View {
        GeometryReader { geo in
            Canvas { context, size in
                for block in blocks {
                    let bbox = block.source.bbox
                    let x = CGFloat(bbox.left) * size.width
                    let y = CGFloat(bbox.top) * size.height
                    let width = CGFloat(bbox.right - bbox.left) * size.width
                    let height = CGFloat(bbox.bottom - bbox.top) * size.height
                    guard width > 1, height > 1 else { continue }

                    let rect = CGRect(x: x, y: y, width: width, height: height)
                    let path = Path(roundedRect: rect, cornerRadius: 4)
                    context.fill(path, with: .color(.black.opacity(0.85)))

                    let displayText = block.translated.isEmpty ? block.source.text : block.translated
                    let fontSize = max(10, height * 0.55)
                    let resolved = context.resolve(
                        Text(displayText)
                            .font(.system(size: fontSize))
                            .foregroundColor(.white)
                    )

                    let textRect = CGRect(
                        x: rect.minX + 6,
                        y: rect.minY,
                        width: rect.width - 12,
                        height: rect.height
                    )
                    context.draw(resolved, in: textRect)
                }
            }
        }
    }
}
