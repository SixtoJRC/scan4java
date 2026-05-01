# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`scan4java` is a Java library (Java 25, Maven) that exposes a unified API for document scanning on Windows via two protocols: **TWAIN** (64-bit only) and **WIA** (Windows Image Acquisition, any Windows version).

## Build & Test Commands

```bash
mvn clean compile                  # Compile
mvn test                           # Run all tests
mvn -Dtest=ClassName test          # Run a single test class
mvn -Dtest=ClassName#methodName test  # Run a single test method
mvn package                        # Build JAR
mvn clean verify                   # Compile + test + package
mvn jacoco:report                  # Generate coverage report (target/site/jacoco/)
```

Tests require `--enable-native-access=ALL-UNNAMED` (already configured in `pom.xml` via Surefire plugin) for JNA to work under Java 25.

## Architecture

The library uses a **Service Provider Interface (SPI)** pattern:

```
ScanManager (singleton)
  └─ List<ScannerProvider>  [TwainScannerProvider, WiaScannerProvider]
       └─ List<Scanner>     [TwainScanner, WiaScanner]
```

**Public API** (`com.sijareca.scan4java`):
- `ScanManager` — singleton that queries all providers and deduplicates results (TWAIN preferred over WIA when both see the same device)
- `Scanner` — interface: `getId()`, `getName()`, `getProtocol()`, setters, `scan(ScanConfig)`
- `ScanConfig` — value object with dpi, color (`ColorMode`), adf, duplex; use `ScanConfig.defaults()` for a base instance
- `Protocol` enum (`TWAIN`, `WIA`), `ColorMode` enum (`BW`, `GRAYSCALE`, `COLOR`), `ScanException`

**SPI** (`com.sijareca.scan4java.spi`):
- `ScannerProvider` — interface: `isAvailable()`, `getScanners()`; providers fail gracefully (return empty list) so one broken provider never blocks the other

**TWAIN implementation** (`internal/twain`):
- `TwainLib` — JNA bindings to `TWAINDSM.DLL`; defines all structs (`TW_IDENTITY`, `TW_IMAGEINFO`, etc.) and message constants
- `TwainSession` — DSM lifecycle (open/close), source enumeration, HWND management
- `TwainScanner` — TWAIN state machine (State 2→5), Windows message loop, multi-page ADF transfer via `GlobalLock`/`GlobalUnlock`
- `DibConverter` — converts Windows DIB memory handles to `BufferedImage` (8-bit indexed, 24-bit RGB, 32-bit ARGB; handles scanline padding and bitmap orientation)
- `Kernel32Heap` — thin JNA interface for `GlobalLock`, `GlobalUnlock`, `GlobalFree`

**WIA implementation** (`internal/wia`):
- `WiaLib` — COM interface definitions (GUIDs, `PROPSPEC`/`PROPVARIANT`/`STGMEDIUM` structs, WIA property ID constants)
- `WiaSession` — COM init/uninit, device enumeration via `IWiaDevMgr`, `CoCreateInstance` for device open
- `WiaScanner` — sets device properties (DPI, color, format), transfers scan to a temp BMP file, reads it with `ImageIO`
- `WiaDeviceInfo` — simple DTO (id, name, type); `isScanner()` filters by WIA device type constant

## Key Constraints

- **TWAIN requires Windows 64-bit**; `TwainScannerProvider.isAvailable()` checks `os.arch` at runtime.
- **WIA works on any Windows version** but requires COM; `WiaSession` calls `CoInitializeEx`/`CoUninitialize`.
- Both `TwainSession` and `WiaSession` implement `AutoCloseable`; always use try-with-resources.
- Adding a new protocol means: implement `ScannerProvider` + `Scanner`, register in `ScanManager`'s provider list.
