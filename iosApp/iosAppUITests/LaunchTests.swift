import XCTest

final class LaunchTests: XCTestCase {
    func testSharedHomeRendersOnDevice() {
        let app = XCUIApplication()
        app.launchEnvironment["COMPOSE_UI_TESTING"] = "1"
        app.launch()
        defer {
            let hierarchy = XCTAttachment(string: app.debugDescription)
            hierarchy.name = "Shared accessibility hierarchy"
            hierarchy.lifetime = .keepAlways
            add(hierarchy)
            print("Shared accessibility hierarchy:\n\(app.debugDescription)")
            let screenshot = XCTAttachment(screenshot: app.screenshot())
            screenshot.name = "Shared home"
            screenshot.lifetime = .keepAlways
            add(screenshot)
        }
        XCTAssertTrue(app.descendants(matching: .any)["Cours"].firstMatch.waitForExistence(timeout: 30), "Shared Compose home must render")
        XCTAssertTrue(app.descendants(matching: .any)["Profil"].firstMatch.exists, "Shared navigation must render")
    }
}
