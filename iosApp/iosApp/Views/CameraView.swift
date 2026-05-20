import SwiftUI
import AVFoundation
import Vision
import Shared

struct CameraView: View {
    @EnvironmentObject var container: AppContainer
    @Binding var path: NavigationPath
    @StateObject private var viewModel = CameraViewModel()

    var body: some View {
        ZStack(alignment: .bottom) {
            CameraPreview(session: viewModel.session)
                .ignoresSafeArea()

            AROverlayView(blocks: viewModel.blocks)
                .ignoresSafeArea()

            CameraBottomToolbar(
                source: viewModel.source,
                target: viewModel.target,
                onSourceTap: { viewModel.showSourcePicker = true },
                onTargetTap: { viewModel.showTargetPicker = true },
                onSwap: viewModel.swap,
                onCapture: {
                    viewModel.capture { id in
                        path.append(Route.captureDetail(id: id))
                    }
                },
                onLibrary: { path.append(Route.library) }
            )
            .padding(.bottom, 24)
        }
        .background(.black)
        .navigationBarBackButtonHidden(false)
        .onAppear {
            viewModel.bind(container: container)
        }
        .onDisappear {
            viewModel.unbind()
        }
        .sheet(isPresented: $viewModel.showSourcePicker) {
            LanguagePickerSheet(
                title: "Source language",
                allowAuto: true,
                selected: viewModel.source
            ) { lang in
                viewModel.setSource(lang)
                viewModel.showSourcePicker = false
            }
        }
        .sheet(isPresented: $viewModel.showTargetPicker) {
            LanguagePickerSheet(
                title: "Target language",
                allowAuto: false,
                selected: viewModel.target
            ) { lang in
                if let lang = lang { viewModel.setTarget(lang) }
                viewModel.showTargetPicker = false
            }
        }
    }
}
