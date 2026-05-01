# Technical Debt — Fix Checklist

Hallazgos del análisis de deuda técnica de scan4java.  
Cada ítem es un checkbox; marcar cuando el cambio esté commiteado y los tests en verde.

## Resumen ejecutivo

| Área | Alto | Medio | Bajo | Total |
|---|---|---|---|---|
| Thread Safety | 1 | 2 | — | 3 |
| Resource Leaks | 3 | 2 | — | 5 |
| Error Handling | — | 3 | 2 | 5 |
| API Design | 1 | 1 | 3 | 5 |
| Native / JNA | 3 | 2 | 1 | 6 |
| Test Gaps | 2 | 2 | 1 | 5 |
| Duplicación | — | 1 | 2 | 3 |
| Plataforma | 1 | 1 | 1 | 3 |
| Dependencias | — | — | 3 | 3 |
| **Total** | **11** | **14** | **13** | **38** |

---

## Onda 1 — Bugs reales

> Cambios quirúrgicos sin rotura de API. Commiteables de forma independiente.  
> Verificación: `mvn test` en verde tras cada cambio.

- [x] **[ALTO] `TwainSession.java:30` — `SupportedGroups` incorrecto**  
  `SupportedGroups = 2` debe ser `3` (`DG_CONTROL=1 | DG_IMAGE=2`).  
  `TwainScanner.java` ya usa `3`; la inconsistencia puede hacer que el DSM rechace la identidad de la aplicación.  
  **Fix:** cambiar `2` → `3` en `TwainSession.buildAppIdentity()`. ✓

- [x] **[ALTO] `ScanManager.java:12,21-23` — Singleton no thread-safe**  
  `if (INSTANCE == null) INSTANCE = new ScanManager()` es una condición de carrera: dos hilos pueden crear instancias distintas.  
  **Fix:** reemplazado por el patrón *Initialization-on-demand holder*. ✓

- [x] **[ALTO] `WiaScanner.java:54-76` — Fuga de IWiaItem en configureItem()**  
  Verificado en el código fuente: `configureItem()` ya está dentro del bloque `try-finally { scanItem.Release(); }` (líneas 58-74). El código ya era correcto; no requería cambio.

---

## Onda 2 — Refactoring compartido

> Elimina duplicación antes de que las ondas siguientes toquen los mismos archivos.  
> Verificación: `mvn test` + grep manual para confirmar que no queda código duplicado.

- [x] **[MEDIO] Duplicación `buildAppIdentity()` / `copyString()` entre TwainSession y TwainScanner**  
  Extraídos a `TwainUtils` (clase package-private nueva). `TwainSession` y `TwainScanner` usan `TwainUtils.buildAppIdentity()`. ✓

- [x] **[MEDIO] `WiaScanner.java:91-95` / `WiaLib.java` — Magic numbers de WIA**  
  Añadidas constantes `WIA_DPS_HORIZONTAL_RESOLUTION`, `WIA_DPS_VERTICAL_RESOLUTION` y `WIA_IPA_DATATYPE` en `WiaLib`. Sustituidos los literales en `WiaScanner.configureDevice()`. ✓

---

## Onda 3 — Robustez nativa (JNA / COM)

> Defensas contra datos corruptos o fallos del dispositivo.  
> Verificación: `mvn test` + prueba manual con DIB fabricado (ver `DibConverterTest`).

- [x] **[ALTO] `DibConverter.java:15-33` — Sin validación de puntero ni de `biSize`**  
  Añadidos guards: null check sobre `p` y validación `biSize >= 40` antes de leer el header. ✓

- [x] **[ALTO] `DibConverter.java:40-46` — Sin validación de dimensiones**  
  Añadida validación `width <= 0 || height <= 0` antes del switch. ✓

- [x] **[MEDIO] `DibConverter.java:52-55` — Índice de paleta fuera de límites**  
  Añadido bounds check `palIdx >= palette.length` en el bucle de pixels de 8-bit. ✓

- [x] **[MEDIO] `WiaLib.java:238-245` — `getBSTR()` sin validación de puntero**  
  Verificado en el código fuente: la guarda `if (vt != VT_BSTR || data == null) return ""` ya cubre el caso nulo. No requería cambio.

- [x] **[MEDIO] `WiaScanner.java:85-86` — HRESULT fallido retorna silenciosamente**  
  `configureDevice` lanza `ScanException` con el código HRESULT en hexadecimal. ✓

---

## Onda 4 — Validación de entrada en API pública

> Rechaza entradas inválidas en la frontera de la API, no dentro del código nativo.  
> Verificación: `mvn test` + confirmar que los nuevos tests de onda 5 pasan.

- [x] **[BAJO] `ScanConfig.java:10` — DPI negativo o cero aceptado**  
  `dpi()` lanza `IllegalArgumentException` si `dpi <= 0`. ✓

- [x] **[BAJO] `ScanConfig.java:11` — `color(null)` aceptado**  
  `color()` lanza `NullPointerException` si el argumento es nulo. ✓

- [x] **[BAJO] `ScanManager.java:30-34` — ScanException de proveedor se traga sin rastro**  
  Añadido `System.Logger` y llamada a `logger.log(WARNING, ...)` en el catch. ✓

---

## Onda 5 — Mejoras de tests

> No afectan código de producción. Implementar tras completar ondas 1-4.  
> Verificación: `mvn verify`; revisar cobertura JaCoCo en `DibConverter` y `ScanConfig`.

- [x] **[ALTO] `DibConverterTest.java` — Sin cobertura de casos límite**  
  Añadidos 5 tests: `nullPointerThrowsException`, `smallBiSizeThrowsException`, `zeroDimensionsThrowsException`, `convert8bitDib`, `paletteIndexOutOfBoundsThrowsException`. ✓

- [x] **[BAJO] `ScanConfigTest.java` — Sin tests de validación**  
  Añadidos `dpiNegativoLanzaExcepcion`, `dpiCeroLanzaExcepcion` y `colorNuloLanzaExcepcion`. ✓

- [x] **[BAJO] `ScanManagerTest.java` — `deduplicate()` probado vía reflexión**  
  `deduplicate()` cambiado a package-private en `ScanManager`. Los 5 tests de reflexión reemplazados por llamadas directas; eliminado `throws Exception` de cada firma. ✓

---

## Hallazgos excluidos (bajo impacto / no accionables ahora)

| Ítem | Razón de exclusión |
|---|---|
| TW_IDENTITY struct alignment | JNA usa `@FieldOrder` correctamente; no hay evidencia de fallo real |
| JNA Memory en `toWideString` | El Memory vive en el stack frame hasta que `idtGetData` retorna; no hay riesgo de GC prematuro en ese scope |
| `GlobalLock` sin check de null | Ya hay `if (p != null)` en `TwainScanner.java:191` |
| JNA 5.18.0 outdated | Actualización de dependencia menor; hacerlo en una PR separada de mantenimiento |
| `module-info.java` ausente | Trabajo mayor; valorar cuando se estabilice la API pública |
