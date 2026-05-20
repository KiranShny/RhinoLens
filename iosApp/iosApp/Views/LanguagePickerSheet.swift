import SwiftUI
import Shared

struct LanguagePickerSheet: View {
    let title: String
    let allowAuto: Bool
    let selected: Language?
    let onSelect: (Language?) -> Void

    @State private var query: String = ""

    private var filtered: [Language] {
        let all = Languages.shared.all
        if query.isEmpty { return all }
        let q = query.lowercased()
        return all.filter {
            $0.displayName.lowercased().contains(q)
            || $0.nativeName.lowercased().contains(q)
            || $0.code.value.lowercased().contains(q)
        }
    }

    var body: some View {
        NavigationStack {
            List {
                if allowAuto && query.isEmpty {
                    Button {
                        onSelect(nil)
                    } label: {
                        HStack {
                            Text("Auto-detect")
                            Spacer()
                        }
                    }
                }
                ForEach(filtered, id: \.code.value) { lang in
                    Button {
                        onSelect(lang)
                    } label: {
                        HStack {
                            VStack(alignment: .leading) {
                                Text(lang.displayName)
                                Text(lang.nativeName)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            if selected?.code.value == lang.code.value {
                                Image(systemName: "checkmark")
                            }
                        }
                    }
                }
            }
            .searchable(text: $query)
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}
