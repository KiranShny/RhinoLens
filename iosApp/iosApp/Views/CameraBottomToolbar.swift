import SwiftUI
import Shared

struct CameraBottomToolbar: View {
    let source: Language?
    let target: Language
    let onSourceTap: () -> Void
    let onTargetTap: () -> Void
    let onSwap: () -> Void
    let onCapture: () -> Void
    let onLibrary: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            chip(label: source?.displayName ?? "Auto", action: onSourceTap)
            Button(action: onSwap) {
                Image(systemName: "arrow.left.arrow.right")
                    .foregroundStyle(.white)
            }
            .disabled(source == nil)
            chip(label: target.displayName, action: onTargetTap)
            Button(action: onCapture) {
                Image(systemName: "camera.fill")
                    .font(.system(size: 28))
                    .foregroundStyle(.black)
                    .padding(16)
                    .background(Circle().fill(Color.white))
            }
            Button(action: onLibrary) {
                Image(systemName: "square.grid.2x2.fill")
                    .foregroundStyle(.white)
                    .padding(12)
                    .background(RoundedRectangle(cornerRadius: 8).fill(.white.opacity(0.15)))
            }
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 16)
        .background(Color.black.opacity(0.6))
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .padding(.horizontal, 12)
    }

    private func chip(label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(label)
                .foregroundStyle(.white)
                .padding(.horizontal, 12)
                .padding(.vertical, 8)
                .background(
                    Capsule().fill(.white.opacity(0.15))
                )
                .overlay(
                    Capsule().stroke(.white.opacity(0.4))
                )
        }
    }
}
