import SwiftUI
import Shared

@main
struct ArchiveTuneApp: App {
    init() {
        KoinInitializerKt.doInitKoinIos()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
