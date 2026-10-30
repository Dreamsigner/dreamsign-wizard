<!-- Generated: 2026-10-23 | Updated: 2026-10-27 | Files scanned: 265 at baseline; focused studio update | Token estimate: ~500 (characters/4 heuristic) -->

# Dependencies and integrations

Build versions are catalogued in `gradle/libs.versions.toml`; app-specific runtime dependencies are declared in `composeApp/build.gradle.kts`. The project uses Kotlin Multiplatform, Compose Multiplatform, coroutines and kotlinx.serialization. Desktop adds PDFBox for PDF rendering, JLaTeXMath for math typesetting, a WebView implementation for video, and Compose desktop packaging. Platform-specific Android and iOS APIs implement storage and media.

External integrations visible in source/config:

- GitHub Releases: Windows updater and published installer artifacts.
- MIT OCW, OSSU and France Éducation international, plus linked PDFs/videos and YouTube: learning sources; some require Internet.
- Optional local Windows video download: `yt-dlp`, FFmpeg/ffprobe and a supported JavaScript runtime, installed separately by the user.
- CI: GitHub Actions, Java 17, Gradle 8.11.1, and Python 3.12 with SymPy for curriculum validation.

No analytics, advertising, payment, authentication, social platform or cloud-storage SDK was found in the inspected dependency declarations. The app has no cross-device sync service. README directs users to the published `1.11.0` installer. Gradle and CI describe candidate `1.11.2`, documented as unpublished after native video CI failures; these are different delivery states, not proof of a stale installer link. Verify the live release before a launch campaign.

Studio IA (2026-10-26) adds an outbound OpenAI-compatible Chat Completions provider selected by the user. No provider SDK is installed: Java 17 `java.net.http` is included explicitly in the packaged runtime. Tests use the installed `jdk.httpserver` module for loopback fixtures. No paid/external inference is performed by deterministic tests. Other platform targets expose an unavailable studio adapter.
