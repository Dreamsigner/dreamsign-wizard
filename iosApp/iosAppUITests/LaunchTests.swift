import XCTest

final class LaunchTests: XCTestCase {
    func testSharedHomeRendersOnDevice() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.descendants(matching: .any)["Cours"].firstMatch.waitForExistence(timeout: 30), "Shared Compose home must render")
        XCTAssertTrue(app.descendants(matching: .any)["Profil"].firstMatch.exists, "Shared navigation must render")
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "Shared home"
        screenshot.lifetime = .keepAlways
        add(screenshot)
    }
}
