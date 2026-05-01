# Technical Debt — Fix Checklist (Round 2)

Segunda pasada de análisis de deuda técnica tras completar fix.md.  
Misma mecánica: checkbox por ítem, `mvn test` en verde al cerrar cada onda.

## Resumen ejecutivo

| Área | Alto | Medio | Bajo | Total |
|---|---|---|---|---|
| Thread Safety | 1 | — | — | 1 |
| Valores retorno nativos | 2 | 1 | — | 3 |
| Error Handling | 1 | 1 | — | 2 |
| Código muerto | — | — | 5 | 5 |
| Test Gaps | — | 1 | 1 | 2 |
| **Total** | **4** | **3** | **6** | **13** |

---

## Onda 6 — Limpieza de código muerto

> Sin cambio de comportamiento. Commits atómicos y limpios.  
> Verificación: `mvn compile` sin warnings nuevos.

- [x] **[BAJO] `TwainLib.java:5` — Import `Memory` sin uso**  
  Verificado: `Memory` sí se usa en la sobrecarga `DSM_Entry(..., Memory data)`. El import era correcto; no se eliminó. ✓

- [x] **[BAJO] `TwainLib.java:10-11` — Imports comentados**  
  Eliminadas las líneas `//import java.util.Arrays;` y `//import java.util.List;`. ✓

- [x] **[BAJO] `WiaLib.java:5,8-12` — Imports comentados**  
  Eliminados los 6 imports comentados residuales. ✓

- [x] **[BAJO] `WiaSession.java:4` — Import comentado**  
  Eliminado `//import com.sun.jna.platform.win32.COM.COMUtils;`. ✓

- [x] **[BAJO] `TwainUtils.java:18-19` — `copyString()` sin null check**  
  Añadido `if (value == null) throw new NullPointerException(...)` antes de `getBytes()`. ✓

---

## Onda 7 — Consistencia WIA: fallos silenciosos

> Alinea el comportamiento de `configureItem` con `configureDevice` y añade visibilidad a fallos de escritura de propiedades.  
> Verificación: `mvn test` en verde; los tests de onda 10 deben pasar tras esta onda.

- [x] **[MEDIO] `WiaScanner.java:107` — `configureItem()` retorna silenciosamente en error**  
  `configureDevice()` ya lanza `ScanException` cuando `queryPropertyStorage` falla. `configureItem()` retorna silenciosamente ante el mismo error — el dispositivo queda sin configurar el formato de transferencia.  
  **Fix:** alinear con `configureDevice`:
  ```java
  if (hr.intValue() != WiaLib.S_OK)
      throw new ScanException("Cannot access item properties (hr=0x"
                              + Integer.toHexString(hr.intValue()) + ")");
  ```

- [x] **[ALTO] `WiaScanner.java:178` — HRESULT de `WriteMultiple` ignorado**  
  `props.WriteMultiple(1, specs, vars, 2)` devuelve `HRESULT` que se descarta. Un fallo de escritura de propiedad (DPI, color, formato) pasa desapercibido; el escaneo continúa con la configuración por defecto del dispositivo.  
  **Fix:** cambiar `writeIntProperty` para capturar y propagar el fallo:
  ```java
  private void writeIntProperty(WiaLib.IWiaPropertyStorage props,
                                int propId, int value) throws ScanException {
      WiaLib.PROPSPEC[]    specs = { new WiaLib.PROPSPEC(propId) };
      WiaLib.PROPVARIANT[] vars  = { new WiaLib.PROPVARIANT() };
      vars[0].vt   = WiaLib.PROPVARIANT.VT_I4;
      vars[0].data = Pointer.createConstant(value);
      vars[0].write();
      HRESULT hr = props.WriteMultiple(1, specs, vars, 2);
      if (hr.intValue() != WiaLib.S_OK)
          throw new ScanException("Failed to write WIA property " + propId
                                  + " (hr=0x" + Integer.toHexString(hr.intValue()) + ")");
  }
  ```
  Actualizar la firma de `configureDevice` y `configureItem` para relanzar `ScanException` (ya la tienen).

---

## Onda 8 — Valores de retorno nativos TWAIN

> Los cleanup paths en finally no pueden lanzar excepciones (enmascararían la excepción original). La estrategia es loggear, no lanzar.  
> Añadir logger a `TwainScanner` igual que se hizo en `ScanManager`.  
> Verificación: `mvn test` en verde.

- [ ] **[ALTO] `TwainScanner.java:195-196` — `GlobalUnlock`/`GlobalFree` sin comprobación**  
  `GlobalUnlock` devuelve `boolean` (false = fallo). `GlobalFree` devuelve `Pointer` (null = éxito, no-null = el handle no se liberó).  
  **Fix:**
  ```java
  // Añadir logger a TwainScanner:
  private static final System.Logger logger =
      System.getLogger(TwainScanner.class.getName());

  // En el finally de transferPages:
  boolean unlocked = Kernel32Heap.INSTANCE.GlobalUnlock(hGlobal);
  if (!unlocked) logger.log(WARNING, "GlobalUnlock failed for handle {0}", hGlobal);
  Pointer freed = Kernel32Heap.INSTANCE.GlobalFree(hGlobal);
  if (freed != null) logger.log(WARNING, "GlobalFree failed for handle {0}", hGlobal);
  ```

- [ ] **[MEDIO] `TwainScanner.java:140` — `DestroyWindow` return ignorado**  
  `DestroyWindow` devuelve `boolean`; si falla, el HWND queda huérfano y TWAIN puede rechazar la siguiente sesión.  
  **Fix:**
  ```java
  } finally {
      boolean destroyed = User32.INSTANCE.DestroyWindow(hwnd);
      if (!destroyed) logger.log(WARNING, "Failed to destroy TWAIN window");
  }
  ```

- [ ] **[ALTO] `TwainScanner.java:123,129,135` — Cleanup `DSM_Entry` sin comprobación**  
  Las tres llamadas de teardown (`MSG_DISABLEDS`, `MSG_CLOSEDS`, `MSG_CLOSEDSM`) en bloques finally ignoran el código de retorno. Un fallo deja el DSM en estado inconsistente.  
  **Fix:** capturar y loggear (nunca lanzar en finally):
  ```java
  // MSG_DISABLEDS
  int rcDisable = lib.DSM_Entry(appId, dsId, DG_CONTROL, DAT_USERINTERFACE, MSG_DISABLEDS, ui);
  if (rcDisable != TWRC_SUCCESS)
      logger.log(WARNING, "MSG_DISABLEDS failed (rc={0})", rcDisable);

  // MSG_CLOSEDS (en finally)
  int rcCloseDs = lib.DSM_Entry(appId, null, DG_CONTROL, DAT_IDENTITY, MSG_CLOSEDS, dsId);
  if (rcCloseDs != TWRC_SUCCESS)
      logger.log(WARNING, "MSG_CLOSEDS failed (rc={0})", rcCloseDs);

  // MSG_CLOSEDSM (en finally)
  int rcCloseDsm = lib.DSM_Entry(appId, null, DG_CONTROL, DAT_PARENT, MSG_CLOSEDSM, hwndMem);
  if (rcCloseDsm != TWRC_SUCCESS)
      logger.log(WARNING, "MSG_CLOSEDSM failed (rc={0})", rcCloseDsm);
  ```

---

## Onda 9 — Thread safety de Scanner

> Los objetos `Scanner` contienen un campo `ScanConfig config` mutable que los setters modifican directamente. Si dos hilos llaman `setDpi`/`scan()` sobre la misma instancia concurrentemente, la configuración es impredecible.  
> El fix mínimo y sin rotura de API es: (a) snapshot defensivo en `scan()` sin argumento, y (b) documentar la restricción.

- [ ] **[ALTO] `TwainScanner.java:29-36` y `WiaScanner.java:32-39` — Setters mutan config sin sincronización**  
  **Fix en ambas clases:** hacer que `scan()` capture un snapshot inmutable de la config antes de delegar, de forma que una modificación concurrente no afecte al escaneo ya en curso:
  ```java
  @Override
  public List<BufferedImage> scan() throws ScanException {
      ScanConfig snapshot = new ScanConfig()
          .dpi(config.getDpi())
          .color(config.getColor())
          .adf(config.isAdf())
          .duplex(config.isDuplex());
      return scan(snapshot);
  }
  ```
  **Fix en `Scanner.java`:** añadir Javadoc que documente la restricción:
  ```java
  /**
   * Las instancias de Scanner NO son thread-safe.
   * Los métodos setter y scan() no deben llamarse concurrentemente
   * desde múltiples hilos sobre la misma instancia.
   */
  public interface Scanner { ... }
  ```

---

## Onda 10 — Tests adicionales

> Implementar tras completar ondas 7 y 9.  
> Verificación: `mvn verify`; cobertura JaCoCo de `DibConverter` y `WiaScanner` debe subir.

- [ ] **[BAJO] `DibConverterTest.java` — Sin test de DIB top-down (altura negativa)**  
  La rama `topDown = biHeight < 0` en `DibConverter:23` existe pero ningún test la ejecuta.  
  **Fix:** añadir test con `biHeight = -2` y verificar que los píxeles se ordenan correctamente (fila 0 del DIB es la fila 0 de la imagen, no la última):
  ```java
  @Test
  void convert24bitDibTopDown() {
      // biHeight negativo = top-down: fila 0 en memoria = fila 0 en imagen
      Memory mem = buildMinimal24bitDib(-2 /* top-down */);
      // fijar fila 0 = rojo, fila 1 = azul
      // assert img.getRGB(0,0) == rojo, img.getRGB(0,1) == azul
  }
  ```

- [ ] **[MEDIO] `WiaScannerTest.java` — Sin test de fallo de `configureItem`**  
  Tras onda 7, `configureItem()` lanza `ScanException`. Añadir test que verifique que `scan()` propaga la excepción cuando `queryPropertyStorage` falla en el item.

---

## Archivos a modificar (en orden de onda)

```
Onda 6:
  src/main/java/com/sijareca/scan4java/internal/twain/TwainLib.java
  src/main/java/com/sijareca/scan4java/internal/twain/TwainUtils.java
  src/main/java/com/sijareca/scan4java/internal/wia/WiaLib.java
  src/main/java/com/sijareca/scan4java/internal/wia/WiaSession.java

Onda 7:
  src/main/java/com/sijareca/scan4java/internal/wia/WiaScanner.java

Onda 8:
  src/main/java/com/sijareca/scan4java/internal/twain/TwainScanner.java

Onda 9:
  src/main/java/com/sijareca/scan4java/Scanner.java            (Javadoc)
  src/main/java/com/sijareca/scan4java/internal/twain/TwainScanner.java
  src/main/java/com/sijareca/scan4java/internal/wia/WiaScanner.java

Onda 10:
  src/test/java/com/sijareca/scan4java/internal/twain/DibConverterTest.java
  src/test/java/com/sijareca/scan4java/internal/wia/WiaScannerTest.java
```
