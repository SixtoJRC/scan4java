# Guía de Integración de scan4java

`scan4java` es una librería diseñada para simplificar la integración de escáneres documentales en aplicaciones Java sobre Windows, unificando el acceso a través de los protocolos **TWAIN** y **WIA**.
---
*Nota: TWAIN requiere driver Windows de 64 bits para funcionar con esta librería. WIA funciona en cualquier versión de Windows.*

## 1. Conceptos Fundamentales

Para integrar la librería, es necesario comprender cuatro componentes principales:

*   **`ScanManager`**: Es el punto de entrada único (Singleton). Se encarga de descubrir los escáneres conectados al sistema.
*   **`Scanner`**: Representa un dispositivo físico de escaneo. Abstrae las diferencias entre TWAIN y WIA.
*   **`ScanConfig`**: Objeto **inmutable** que contiene la configuración del escaneo (DPI, modo de color, uso de alimentador, etc.).
*   **`ScanException`**: Excepción única que lanza la librería ante errores de comunicación o configuración de hardware.

## 2. Instalación

Añade la dependencia a tu proyecto Maven:

```xml
<dependency>
    <groupId>com.sijareca</groupId>
    <artifactId>scan4java</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 3. Flujo de Trabajo Típico

El proceso de integración sigue siempre tres pasos: **Descubrimiento**, **Configuración** y **Ejecución**.

### Paso 1: Descubrimiento de dispositivos
El `ScanManager` utiliza internamente el mecanismo SPI de Java para cargar los drivers.

```java
// Obtener todos los escáneres disponibles (TWAIN + WIA deduplicados)
List<Scanner> scanners = ScanManager.instance().getScanners();

// Filtrar por un protocolo específico si se desea
List<Scanner> onlyTwain = ScanManager.instance().getScanners(Protocol.TWAIN);
```

### Paso 2: Configuración (Fluent API)
El objeto `ScanConfig` es inmutable. Cada método devuelve una nueva instancia.

```java
ScanConfig config = ScanConfig.defaults()
    .dpi(300)                           // Resolución en puntos por pulgada
    .color(ColorMode.GRAYSCALE)         // BW, GRAYSCALE o COLOR
    .adf(true)                          // Usar alimentador automático (si existe)
    .duplex(true);                      // Escaneo a doble cara (si el ADF lo soporta)
```

### Paso 3: Ejecución
El método `scan()` devuelve una lista de imágenes en memoria (`BufferedImage`), lo que da total libertad a la aplicación para guardarlas, procesarlas o mostrarlas.

```java
try {
    Scanner scanner = scanners.get(0);
    List<BufferedImage> images = scanner.scan(config);
    
    System.out.println("Se han escaneado " + images.size() + " páginas.");
} catch (ScanException e) {
    System.err.println("Error durante el escaneo: " + e.getMessage());
}
```

## 4. Referencia de la API

### Interfaz `Scanner`
| Método | Descripción |
|---|---|
| `getId()` | Identificador único del dispositivo en el sistema. |
| `getName()` | Nombre descriptivo (modelo del escáner). |
| `getProtocol()` | Protocolo utilizado (`TWAIN` o `WIA`). |
| `scan(ScanConfig)` | Inicia el proceso de captura y devuelve `List<BufferedImage>`. |

### Enumerados
*   **`ColorMode`**: `BW` (Blanco y negro), `GRAYSCALE` (256 niveles de gris), `COLOR` (24-bit RGB).
*   **`Protocol`**: `TWAIN`, `WIA`.

## 5. Gestión de Errores

La librería encapsula todos los errores nativos (HRESULT de Windows, códigos de retorno TWAIN) en una `ScanException`. Los casos comunes incluyen:

*   **Dispositivo no encontrado**: El escáner se ha desconectado tras el descubrimiento.
*   **Error de hardware**: Atasco de papel o tapa abierta.
*   **DSM no disponible**: (TWAIN) El gestor de fuentes de datos no está instalado en el sistema.
*   **Falta de permisos**: La aplicación no tiene acceso para crear archivos temporales (requerido por WIA).

## 6. Ejemplo Completo

```java
import com.sijareca.scan4java.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class ScanExample {
    public static void main(String[] args) {
        // 1. Buscar escáneres
        List<Scanner> scanners = ScanManager.instance().getScanners();
        if (scanners.isEmpty()) {
            System.out.println("No se detectaron escáneres.");
            return;
        }

        // 2. Seleccionar el primero y configurar
        Scanner scanner = scanners.get(0);
        ScanConfig config = ScanConfig.defaults()
                                .dpi(300)
                                .color(ColorMode.COLOR);

        // 3. Escanear
        try {
            List<BufferedImage> pages = scanner.scan(config);
            // Aquí la aplicación puede, por ejemplo, guardar en PDF o JPG
            saveToDisk(pages); 
        } catch (ScanException e) {
            e.printStackTrace();
        }
    }
}
```

