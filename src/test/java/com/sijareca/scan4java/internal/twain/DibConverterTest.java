package com.sijareca.scan4java.internal.twain;

import com.sun.jna.Memory;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;

class DibConverterTest {

    @Test
    void convert24bitDib() {
        int width  = 2;
        int height = 2;

        // Construir un BITMAPINFOHEADER mínimo + datos 24-bit
        // BITMAPINFOHEADER = 40 bytes
        // Cada fila: width*3 bytes + padding hasta múltiplo de 4
        int rowBytes = width * 3; // 6 bytes → no necesita padding (ya es múltiplo de 2, pero no de 4)
        int pad      = (4 - rowBytes % 4) % 4; // 2 bytes de padding
        int dataSize = (rowBytes + pad) * height;
        Memory mem   = new Memory(40 + dataSize);

        // BITMAPINFOHEADER
        mem.setInt(0,  40);      // biSize
        mem.setInt(4,  width);   // biWidth
        mem.setInt(8,  height);  // biHeight (positivo = bottom-up)
        mem.setShort(12, (short) 1);   // biPlanes
        mem.setShort(14, (short) 24);  // biBitCount
        mem.setInt(16, 0);       // biCompression = BI_RGB
        mem.setInt(20, 0);       // biSizeImage
        mem.setInt(24, 0);       // biXPelsPerMeter
        mem.setInt(28, 0);       // biYPelsPerMeter
        mem.setInt(32, 0);       // biClrUsed
        mem.setInt(36, 0);       // biClrImportant

        // Pixel data — fila 0 (bottom): rojo puro, fila 1 (top): azul puro
        // Formato BGR en memoria
        int base = 40;
        // Fila 0 (bottom-up → row 1 en imagen Java)
        mem.setByte(base,     (byte) 0x00); // B
        mem.setByte(base + 1, (byte) 0x00); // G
        mem.setByte(base + 2, (byte) 0xFF); // R → rojo
        mem.setByte(base + 3, (byte) 0x00);
        mem.setByte(base + 4, (byte) 0x00);
        mem.setByte(base + 5, (byte) 0xFF);
        // padding
        mem.setByte(base + 6, (byte) 0x00);
        mem.setByte(base + 7, (byte) 0x00);
        // Fila 1 (bottom-up → row 0 en imagen Java)
        int base2 = base + rowBytes + pad;
        mem.setByte(base2,     (byte) 0xFF); // B → azul
        mem.setByte(base2 + 1, (byte) 0x00); // G
        mem.setByte(base2 + 2, (byte) 0x00); // R
        mem.setByte(base2 + 3, (byte) 0xFF);
        mem.setByte(base2 + 4, (byte) 0x00);
        mem.setByte(base2 + 5, (byte) 0x00);

        BufferedImage img = DibConverter.toBufferedImage(mem);

        assertNotNull(img);
        assertEquals(width,  img.getWidth());
        assertEquals(height, img.getHeight());
        // Fila 0 de la imagen debe ser azul (viene de la fila 1 del DIB bottom-up)
        assertEquals(0xFF0000FF, img.getRGB(0, 0) | 0xFF000000);
        // Fila 1 de la imagen debe ser rojo (viene de la fila 0 del DIB)
        assertEquals(0xFFFF0000, img.getRGB(0, 1) | 0xFF000000);
    }

    @Test
    void unsupportedBitsPerPixelThrowsException() {
        Memory mem = new Memory(40);
        mem.setInt(0,  40);
        mem.setInt(4,  1);
        mem.setInt(8,  1);
        mem.setShort(12, (short) 1);
        mem.setShort(14, (short) 16); // 16-bit — no soportado
        mem.clear();

        assertThrows(IllegalArgumentException.class,
                     () -> DibConverter.toBufferedImage(mem));
    }

    @Test
    void nullPointerThrowsException() {
        assertThrows(IllegalArgumentException.class,
                     () -> DibConverter.toBufferedImage(null));
    }

    @Test
    void smallBiSizeThrowsException() {
        Memory mem = new Memory(40);
        mem.setInt(0, 20); // biSize = 20 < 40
        assertThrows(IllegalArgumentException.class,
                     () -> DibConverter.toBufferedImage(mem));
    }

    @Test
    void zeroDimensionsThrowsException() {
        Memory mem = new Memory(40);
        mem.setInt(0, 40);            // biSize OK
        mem.setInt(4, 0);             // biWidth = 0 — inválido
        mem.setInt(8, 1);
        mem.setShort(14, (short) 24);
        assertThrows(IllegalArgumentException.class,
                     () -> DibConverter.toBufferedImage(mem));
    }

    @Test
    void convert8bitDib() {
        int width = 1, height = 1, numColors = 2;
        int rowBytes = 4; // 1 byte píxel + 3 bytes padding
        Memory mem = new Memory(40 + numColors * 4 + height * rowBytes);

        mem.setInt(0,  40);            // biSize
        mem.setInt(4,  width);         // biWidth
        mem.setInt(8,  height);        // biHeight (bottom-up)
        mem.setShort(12, (short) 1);   // biPlanes
        mem.setShort(14, (short) 8);   // biBitCount = 8
        mem.setInt(32, numColors);     // biClrUsed = 2

        // RGBQUAD little-endian: B=0x00, G=0x00, R=0xFF → rojo
        mem.setInt(40, 0x00FF0000); // entrada 0: rojo
        mem.setInt(44, 0x0000FF00); // entrada 1: verde

        // Píxel único en índice 0 (rojo)
        mem.setByte(40 + numColors * 4, (byte) 0);

        BufferedImage img = DibConverter.toBufferedImage(mem);
        assertNotNull(img);
        assertEquals(1, img.getWidth());
        assertEquals(1, img.getHeight());
        // palette[0] = 0x00FF0000 → 0xFF000000 | 0x00FF0000 = 0xFFFF0000 (rojo ARGB)
        assertEquals(0xFFFF0000, img.getRGB(0, 0) | 0xFF000000);
    }

    @Test
    void paletteIndexOutOfBoundsThrowsException() {
        int width = 1, height = 1, numColors = 2;
        int rowBytes = 4;
        Memory mem = new Memory(40 + numColors * 4 + height * rowBytes);

        mem.setInt(0,  40);
        mem.setInt(4,  width);
        mem.setInt(8,  height);
        mem.setShort(12, (short) 1);
        mem.setShort(14, (short) 8);   // 8-bit
        mem.setInt(32, numColors);     // biClrUsed = 2 (solo 2 entradas)

        mem.setInt(40, 0x00000000); // entrada 0: negro
        mem.setInt(44, 0x00FFFFFF); // entrada 1: blanco

        // Índice 5 — fuera de rango para paleta de 2 entradas
        mem.setByte(40 + numColors * 4, (byte) 5);

        assertThrows(IllegalArgumentException.class,
                     () -> DibConverter.toBufferedImage(mem));
    }
}