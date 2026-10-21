# PDF reader

The shared UI exposes a platform reader through `PdfReaderProvider` and opens a
catalogued source with `PdfDocumentDialog`:

```kotlin
PdfReaderProvider(DesktopPdfDocumentReader()) {
    PdfDocumentDialog(resource = officialPdfResource, onClose = { showPdf = false })
}
```

Pass a `CourseResource` obtained from `CourseResources.forCourse`,
`CourseResources.forSession`, or `CourseResources.forExercise`. The common
component checks the resource id, URL, kind, and HTTPS scheme against the
catalogue before any platform reader is invoked. Do not pass arbitrary URLs or
user-entered addresses.

The Windows/Desktop reader downloads the original remote PDF in memory, then
uses PDFBox to render the currently selected page into a Compose image. It does
not copy the source PDF into the repository, a user folder, or a persistent
cache. The reader supports previous/next page, zoom, scrolling, loading and
error states, retry, close, and a browser fallback. The root desktop composition
should install `PdfReaderProvider(DesktopPdfDocumentReader())`; parent screens
can show `PdfDocumentDialog` for catalogued PDF resources. Common code's default
provider reports unsupported reading on platforms without a native reader, so
the mobile host can open the official URL in its browser.

Closing a session does not wait on a slow background page render or font lookup.
The rendering worker disposes its document when it releases the render lock;
disposal is idempotent. Test PDFs embed their font so a fresh Windows runner does
not enumerate installed fonts while creating fixtures. A latch-controlled test
checks nonblocking closure without timing a real font scan.

Download restrictions are 50 MiB maximum body size, a 30 second total download
deadline with connection/read timeouts, HTTPS only, same-host HTTPS redirects
only (up to four), PDF content-type/magic validation, and a 2,000-page document
limit. Page render width is bounded and the page bitmap is capped at eight
million pixels. Documents and page images remain in memory and are released
when the dialog closes or the requested page changes. PDFBox warns that a
crafted PDF can still consume disproportionate CPU or memory; byte and render
limits do not prove that every hostile file is harmless.

PDFBox 3.0.8 is the current stable 3.0 release verified against the official
Apache download page on 2026-10-20. Its security page lists CVE-2026-23907 and
CVE-2026-33929, which affect only the `ExtractEmbeddedFiles` example and are
fixed in 3.0.7 and 3.0.8 respectively. The reader does not use that example
module. See [PDFBox's security page](https://pdfbox.apache.org/security.html),
[3.0.8 getting started](https://pdfbox.apache.org/3.0/getting-started.html),
and [dependency guide](https://pdfbox.apache.org/3.0/dependencies.html).
Include [the PDFBox third-party notice](licences/PDFBOX-THIRD-PARTY-NOTICE.txt)
and [the Apache License 2.0 text](licences/APACHE-LICENSE-2.0.txt) in the
Windows package.
