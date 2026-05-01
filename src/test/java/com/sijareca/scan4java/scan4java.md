# Summary: scan4java — Diseño e Implementación

## Contexto y Objetivo

Proyecto iniciado para crear una librería Java open source que permita integrar
escáneres documentales en aplicaciones Java, combinando los protocolos
**TWAIN (64-bit)** y **WIA** en una única API simple. Inspirada en la simplicidad
de `twain4java` (DenisLAD), orientada a entornos de digitalización en la
Administración General del Estado (AGE).

---

## Decisiones de Diseño Clave

### Restricción de arquitectura

- **TWAIN**: solo drivers 64-bit — elimina la necesidad de proceso bridge
- **WIA**: cubre escáneres con driver de 32-bit y modernos — acceso via COM, nativo 64-bit
- Esta restricción simplifica radicalmente la arquitectura: **todo en un único proceso JVM 64-bit**, sin procesos auxiliares ni JREs adicionales

### API pública — filosofía de simplicidad máxima

Inspirada directamente en twain4java:

```java
List<Scanner> scanners = ScanManager.instance().getScanners();
Scanner scanner = scanners.get(0);
scanner.setDpi(300).setColor(ColorMode.GRAYSCALE);
List<BufferedImage> pages = scanner.scan();
```

### Decisiones técnicas

- **Sin `outputDir`** en `ScanConfig` — la librería devuelve `BufferedImage` en memoria, la aplicación decide qué hacer con las imágenes
- **Patrón Strategy** con interfaz `ScannerProvider` — permite añadir nuevos protocolos en el futuro
- **Deduplicación automática** — si un dispositivo aparece en TWAIN y WIA, TWAIN tiene preferencia (más control, mejor soporte ADF)
- **Persistencia de configuración WIA** — WIA no recuerda configuración entre sesiones; se resuelve en la capa de aplicación (JSON en `user.home`, Java Preferences API, o BBDD)
- **Licencia Apache 2.0** — compatible con uso en administración pública

---

## Stack Tecnológico

| Componente     | Versión | Motivo                                |
|----------------|---------|---------------------------------------|
| Java           | 25      | LTS más reciente                      |
| JNA            | 5.18.0  | Acceso nativo a DLLs Windows sin JNI  |
| JUnit Jupiter  | 6.0.3   | Tests unitarios                       |
| JaCoCo         | 0.8.14  | Cobertura de código                   |
| Maven Compiler | 3.15.0  | Build                                 |
| Maven Surefire | 3.5.5   | Ejecución de tests                    |

---

## Repositorio GitHub

| Campo          | Valor                                 |
|----------------|---------------------------------------|
| **URL**        | https://github.com/sijareca/scan4java |
| **Rama**       | `main`                                |
| **Licencia**   | Apache 2.0                            |
| **GroupId**    | `com.sijareca`                        |
| **ArtifactId** | `scan4java`                           |
| **Versión**    | `0.1.0-SNAPSHOT`                      |

### Estructura de paquetes

```
com.sijareca.scan4java/
├── ScanManager.java              ← punto de entrada (Singleton)
├── Scanner.java                  ← interfaz pública principal
├── ScanConfig.java               ← configuración fluent (DPI, color, ADF, duplex)
├── ColorMode.java                ← enum: BW, GRAYSCALE, COLOR
├── Protocol.java                 ← enum: TWAIN, WIA
├── ScanException.java            ← excepción única de la librería
├── internal/
│   ├── twain/
│   │   ├── TwainLib.java         ← JNA binding TWAINDSM.DLL
│   │   ├── TwainSession.java     ← ciclo de vida DSM (AutoCloseable)
│   │   ├── TwainScanner.java     ← implementación Scanner via TWAIN
│   │   ├── TwainScannerProvider.java
│   │   ├── DibConverter.java     ← Windows DIB → BufferedImage (8/24/32-bit)
│   │   └── Kernel32Heap.java     ← GlobalLock/GlobalUnlock/GlobalFree via JNA
│   └── wia/
│       ├── WiaLib.java           ← COM interfaces (IWiaDevMgr, IWiaItem, etc.)
│       ├── WiaSession.java       ← ciclo de vida COM (AutoCloseable)
│       ├── WiaScanner.java       ← implementación Scanner via WIA
│       ├── WiaScannerProvider.java
│       └── WiaDeviceInfo.java    ← DTO interno dispositivo WIA
└── spi/
    └── ScannerProvider.java      ← interfaz interna de providers
```

### Commits principales

| # | Commit | Contenido |
|---|--------|-----------|
| 1 | `feat: initial project structure` | API pública, esqueletos, tests base |
| 2 | `feat: TWAIN JNA binding and session management` | TwainLib, TwainSession |
| 3 | `feat: WIA COM binding and session management` | WiaLib, WiaSession |
| 4 | `feat: TWAIN scan implementation` | Máquina de estados TWAIN, DibConverter |
| 5 | `feat: WIA scan implementation` | Flujo COM completo, IWiaDataTransfer |
| 6 | `test: improve unit test coverage and build configuration` | Tests, JaCoCo, Surefire |

---

## Implementación TWAIN

Máquina de estados completa (estados 2-7 del protocolo TWAIN):

- Apertura/cierre DSM via `TWAINDSM.DLL` (64-bit)
- Bucle de mensajes Windows para eventos TWAIN
- Transferencia nativa de imagen (DIB en memoria via `GlobalLock`/`GlobalFree`)
- Soporte ADF multi-página via `DAT_PENDINGXFERS`
- Conversión DIB → `BufferedImage` para 8-bit, 24-bit y 32-bit

## Implementación WIA

Flujo COM completo via JNA:

- Enumeración de dispositivos via `IWiaDevMgr::EnumDeviceInfo`
- Apertura de dispositivo via `IWiaDevMgr::CreateDevice`
- Configuración de propiedades (DPI, color, formato) via `IWiaPropertyStorage`
- Transferencia a fichero BMP temporal via `IWiaDataTransfer::idtGetData`
- Conversión a `BufferedImage` via `ImageIO`
- Gestión correcta de `BSTR` via `OleAuto.SysAllocString/SysFreeString`

---

## Cobertura de Tests

| Paquete | Cobertura |
|---------|-----------|
| `com.sijareca.scan4java` (API pública) | ~80% |
| `com.sijareca.scan4java.internal.twain` | ~41% |
| `com.sijareca.scan4java.internal.wia` | ~33% |
| **Total** | **~41%** |

### Clases excluidas de JaCoCo

Requieren hardware real instalado y no son medibles en CI:

- `TwainSession` — necesita `TWAINDSM.DLL` instalada
- `WiaSession` — necesita servicio WIA activo

### Notas de configuración

- Surefire configurado con `--enable-native-access=ALL-UNNAMED` para compatibilidad con Java 25 y el acceso nativo de JNA
- `TwainScannerProvider` y `WiaScannerProvider` capturan `ScanException` internamente — nunca propagan errores de hardware a la capa superior

---

## Problemas Resueltos Durante el Desarrollo

| Problema | Solución |
|----------|----------|
| `java -d32` no existe en Java moderno | Flag eliminado en Java 9 — se usa ejecutable JRE 32-bit separado si se necesita bridge |
| `CLSCTX_LOCAL_SERVER` no encontrado en `Ole32` | Está en `WTypes.CLSCTX_LOCAL_SERVER` |
| `GlobalLock/GlobalUnlock/GlobalFree` no en `Kernel32` de JNA | Interfaz propia `Kernel32Heap` con binding directo |
| `WTypes.BSTR(String)` deprecado | Reemplazado por `OleAuto.INSTANCE.SysAllocString/SysFreeString` |
| `_invokeNativeObject` con acceso `protected` | Métodos wrapper añadidos dentro de las propias clases COM en `WiaLib` |
| `STGMEDIUM` inner class no-static | Declarada `static` en `WiaLib` |
| Warning Java 25 acceso nativo JNA | `--enable-native-access=ALL-UNNAMED` en Surefire |
| `GetMessage` devuelve `int` no `boolean` | Comparación cambiada a `> 0` |

---

## Pendiente / Próximos Pasos

- Crear proyecto de prueba separado que consuma el JAR y pruebe contra escáneres reales
- Validar en entorno AGE con hardware específico (Kyocera, HP, Xerox, Canon)
- Considerar publicación en Maven Central cuando la librería esté validada
- Evaluar soporte de configuración de escáner sin UI (`ShowUI=false`) para digitalización desatendida