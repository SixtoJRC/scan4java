package com.sijareca.scan4java.internal.twain;

import com.sun.jna.Pointer;

import java.awt.image.BufferedImage;

class DibConverter {

    /**
     * Convierte un DIB (Device Independent Bitmap) de Windows
     * apuntado por p en un BufferedImage Java.
     * El DIB empieza con BITMAPINFOHEADER seguido de la paleta (si existe)
     * y los datos de pixels.
     */
    static BufferedImage toBufferedImage(Pointer p) {
        if (p == null)
            throw new IllegalArgumentException("DIB pointer is null");

        // Leer BITMAPINFOHEADER
        int  biSize         = p.getInt(0);
        if (biSize < 40)
            throw new IllegalArgumentException("Invalid BITMAPINFOHEADER size: " + biSize);

        int  biWidth        = p.getInt(4);
        int  biHeight       = p.getInt(8);   // negativo = top-down
        short biBitCount    = p.getShort(14);
        int  biClrUsed      = p.getInt(32);

        boolean topDown = biHeight < 0;
        int width       = biWidth;
        int height      = topDown ? -biHeight : biHeight;

        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Invalid DIB dimensions: " + width + "x" + height);

        return switch (biBitCount) {
            case 8  -> read8bit(p, biSize, width, height, biClrUsed, topDown);
            case 24 -> read24bit(p, biSize, width, height, topDown);
            case 32 -> read32bit(p, biSize, width, height, topDown);
            default -> throw new IllegalArgumentException(
                "Unsupported bits per pixel: " + biBitCount);
        };
    }

    private static BufferedImage read8bit(Pointer p, int headerSize,
                                          int width, int height,
                                          int clrUsed, boolean topDown) {
        int numColors = (clrUsed > 0) ? clrUsed : 256;
        int[] palette = p.getIntArray(headerSize, numColors);

        int padBytes = (4 - width % 4) % 4;
        int rowBytes = width + padBytes;
        byte[] bitmap = p.getByteArray(
            (long) headerSize + numColors * 4L,
            height * rowBytes);

        BufferedImage img = new BufferedImage(
            width, height, BufferedImage.TYPE_INT_RGB);

        for (int row = 0; row < height; row++) {
            int srcRow = topDown ? row : (height - row - 1);
            for (int col = 0; col < width; col++) {
                int palIdx = bitmap[rowBytes * srcRow + col] & 0xFF;
                if (palIdx >= palette.length)
                    throw new IllegalArgumentException("Palette index out of bounds: " + palIdx);
                img.setRGB(col, row, 0xFF000000 | palette[palIdx]);
            }
        }
        return img;
    }

    private static BufferedImage read24bit(Pointer p, int headerSize,
                                           int width, int height,
                                           boolean topDown) {
        int padBytes = (4 - (width * 3) % 4) % 4;
        int rowBytes = width * 3 + padBytes;
        byte[] bitmap = p.getByteArray(headerSize, height * rowBytes);

        BufferedImage img = new BufferedImage(
            width, height, BufferedImage.TYPE_INT_RGB);

        for (int row = 0; row < height; row++) {
            int srcRow = topDown ? row : (height - row - 1);
            for (int col = 0; col < width; col++) {
                int idx = rowBytes * srcRow + col * 3;
                int rgb = 0xFF000000
                        | ((bitmap[idx + 2] & 0xFF) << 16)
                        | ((bitmap[idx + 1] & 0xFF) << 8)
                        |  (bitmap[idx]     & 0xFF);
                img.setRGB(col, row, rgb);
            }
        }
        return img;
    }

    private static BufferedImage read32bit(Pointer p, int headerSize,
                                           int width, int height,
                                           boolean topDown) {
        int rowBytes = width * 4;
        byte[] bitmap = p.getByteArray(headerSize, height * rowBytes);

        BufferedImage img = new BufferedImage(
            width, height, BufferedImage.TYPE_INT_ARGB);

        for (int row = 0; row < height; row++) {
            int srcRow = topDown ? row : (height - row - 1);
            for (int col = 0; col < width; col++) {
                int idx = rowBytes * srcRow + col * 4;
                int argb = ((bitmap[idx + 3] & 0xFF) << 24)
                         | ((bitmap[idx + 2] & 0xFF) << 16)
                         | ((bitmap[idx + 1] & 0xFF) << 8)
                         |  (bitmap[idx]     & 0xFF);
                img.setRGB(col, row, argb);
            }
        }
        return img;
    }
}