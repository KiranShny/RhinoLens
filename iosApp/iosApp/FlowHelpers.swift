import Foundation
import Shared

final class FlowSink: Kotlinx_coroutines_coreFlowCollector {
    private let handler: (Any?) -> Void
    init(_ handler: @escaping (Any?) -> Void) { self.handler = handler }
    func emit(value: Any?) async throws { handler(value) }
}

@discardableResult
func observeFlow(_ flow: any Kotlinx_coroutines_coreFlow, handler: @escaping (Any?) -> Void) -> Task<Void, Never> {
    Task {
        try? await flow.collect(collector: FlowSink(handler))
    }
}

func firstFlowValue(_ flow: any Kotlinx_coroutines_coreFlow) async -> Any? {
    await withCheckedContinuation { continuation in
        var resumed = false
        let sink = FlowSink { value in
            if resumed { return }
            resumed = true
            continuation.resume(returning: value)
        }
        Task {
            try? await flow.collect(collector: sink)
            if !resumed {
                resumed = true
                continuation.resume(returning: nil)
            }
        }
    }
}
