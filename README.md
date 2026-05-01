# scan4java

Simple Java library for document scanning on Windows via **TWAIN** (64-bit) and **WIA** (Windows Image Acquisition).

## Requirements

| Requirement | Version |
|---|---|
| Java | 25+ |
| Maven | 3.9+ |
| OS | Windows (64-bit for TWAIN, any version for WIA) |

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.sijareca</groupId>
    <artifactId>scan4java</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

> Until the artifact is published to Maven Central, clone the repository and run `mvn install` to install it in your local repository.

## Quick Start

### List available scanners

```java
List<Scanner> scanners = ScanManager.instance().getScanners();
scanners.forEach(s -> System.out.println(s.getName() + " [" + s.getProtocol() + "]"));
```

### Scan with default settings

```java
Scanner scanner = ScanManager.instance().getScanners().get(0);
List<BufferedImage> pages = scanner.scan();
```

### Scan with custom configuration

```java
ScanConfig config = ScanConfig.defaults()
    .dpi(300)
    .color(ColorMode.COLOR)
    .adf(true)
    .duplex(true);

List<BufferedImage> pages = scanner.scan(config);
```

### Filter by protocol

```java
// Only TWAIN scanners
List<Scanner> twainScanners = ScanManager.instance().getScanners(Protocol.TWAIN);

// Only WIA scanners
List<Scanner> wiaScanners = ScanManager.instance().getScanners(Protocol.WIA);
```

## Documentation

*   [Integration Guide (integracion.md)](docs/integracion.md): Detailed guide on how to use scan4java in your Java application.
*   [Technical Design (scan4java.md)](docs/scan4java.md): Internal design decisions and implementation details.

### `ScanManager`

Singleton entry point. Queries both TWAIN and WIA providers and deduplicates results — when the same physical device appears in both protocols, TWAIN takes precedence.

| Method | Description |
|---|---|
| `ScanManager.instance()` | Returns the singleton instance |
| `getScanners()` | Returns all available scanners |
| `getScanners(Protocol)` | Returns scanners filtered by protocol |

### `Scanner`

| Method | Description |
|---|---|
| `getId()` | Protocol-specific device identifier |
| `getName()` | Human-readable device name |
| `getProtocol()` | `Protocol.TWAIN` or `Protocol.WIA` |
| `scan()` | Scan with default settings |
| `scan(ScanConfig)` | Scan with custom configuration |

### `ScanConfig`

Fluent builder. Defaults: 200 DPI, grayscale, ADF off, duplex off.

| Method | Default |
|---|---|
| `dpi(int)` | `200` |
| `color(ColorMode)` | `ColorMode.GRAYSCALE` |
| `adf(boolean)` | `false` |
| `duplex(boolean)` | `false` |

### `ColorMode`

`BW` · `GRAYSCALE` · `COLOR`

## Protocol Notes

**TWAIN**
- Requires Windows 64-bit and `TWAINDSM.DLL` installed.
- Supports multi-page ADF scanning (returns one `BufferedImage` per page).
- Uses the standard TWAIN state machine with a Windows message loop.

**WIA**
- Works on any Windows version via COM.
- Transfers each page to a temporary BMP file and reads it with `ImageIO`.

## Build

```bash
mvn clean package          # Compile and build JAR
mvn test                   # Run tests
mvn verify                 # Compile + test + package
```

## License

[Apache License 2.0](LICENSE)
