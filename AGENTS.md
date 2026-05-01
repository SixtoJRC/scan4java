# scan4java Agent Guidance

## Build & Test
- Compile: `mvn clean compile`
- Run tests: `mvn test` (requires Java 25+; `--enable-native-access=ALL-UNNAMED` already configured in Surefire)
- Single test class: `mvn -Dtest=ClassName test`
- Single test method: `mvn -Dtest=ClassName#methodName test`
- Package: `mvn package`
- Verify: `mvn clean verify` (compile + test + package)
- Coverage: `mvn jacoco:report`

## Architecture
- Uses SPI pattern: `ScanManager` (singleton) → `ScannerProvider` implementations (`TwainScannerProvider`, `WiaScannerProvider`) → `Scanner` implementations
- Public API: `ScanManager.instance()`, `Scanner`, `ScanConfig`, `Protocol` enum, `ColorMode` enum, `ScanException`
- SPI interface: `com.sijareca.scan4java.spi.ScannerProvider` (`isAvailable()`, `getScanners()`)
- TWAIN implementation (`internal/twain`): Requires Windows 64-bit + `TWAINDSM.DLL`; uses JNA for `TWAINDSM.DLL` bindings
- WIA implementation (`internal/wia`): Works on any Windows version via COM; transfers scans to temp BMP files
- Deduplication: `ScanManager.getScanners()` prefers TWAIN over WIA when same device appears in both protocols

## Key Constraints
- TWAIN requires Windows 64-bit; checked at runtime in `TwainScannerProvider.isAvailable()`
- WIA works on any Windows version but requires COM initialization
- Both `TwainSession` and `WiaSession` implement `AutoCloseable`; always use try-with-resources
- Tests require `--enable-native-access=ALL-UNNAMED` for JNA reflection access (pre-configured in pom.xml Surefire plugin)
- Adding new protocol: implement `ScannerProvider` + `Scanner`, register in `ScanManager`'s provider list
- Providers must fail gracefully: return empty list (not null) and don't propagate `ScanException` from `getScanners()`

## Project Structure
- Main entry point: `ScanManager.instance()` (singleton)
- Source: `src/main/java/com/sijareca/scan4java`
  - API: root package (`ScanManager`, `Scanner`, `ScanConfig`, etc.)
  - SPI: `spi` package (`ScannerProvider`)
  - TWAIN: `internal/twain` package
  - WIA: `internal/wia` package
- Tests: `src/test/java/com/sijareca/scan4java`
  - Follow same package structure as main
  - Use JUnit 5 (`org.junit.jupiter`)
  - Native access already configured via Surefire plugin

## Developer Notes
- TWAIN scanning uses Windows message loop; scans return one `BufferedImage` per page
- WIA scanning transfers each page to temp BMP file then reads with `ImageIO`
- `ScanConfig` is immutable; use `ScanConfig.defaults()` as starting point for fluent configuration
- All `Scanner` setter methods return `this` for chaining
- Physical scanner required for actual scanning; tests run without hardware and expect `ScanException`