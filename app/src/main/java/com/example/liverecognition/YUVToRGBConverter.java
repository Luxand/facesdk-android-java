package com.example.liverecognition;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageProxy;

public class YUVToRGBConverter {

    private int width = -1;
    private int height = -1;

    private int chromaHeight;
    private int chromaWidth;
    private int yRowStride;
    private int uRowStride;
    private int vRowStride;
    private int uPixelStride;
    private int vPixelStride;
    private int outStride;
    private int ySize;

    private byte[] rgb;
    private byte[] nv21;
    private byte[] uLineBuffer;
    private byte[] vLineBuffer;

    private void checkBuffers(final ImageProxy image) {
        final var width = image.getWidth();
        final var height = image.getHeight();

        if (this.width == width && this.height == height)
            return;

        final var yPlane = image.getPlanes()[0];
        final var uPlane = image.getPlanes()[1];
        final var vPlane = image.getPlanes()[2];

        final var yBuffer = yPlane.getBuffer();
        yBuffer.rewind();

        this.width = width;
        this.height = height;

        chromaHeight = image.getHeight() / 2;
        chromaWidth = image.getWidth() / 2;

        yRowStride = yPlane.getRowStride();
        uRowStride = uPlane.getRowStride();
        vRowStride = vPlane.getRowStride();
        uPixelStride = uPlane.getPixelStride();
        vPixelStride = vPlane.getPixelStride();
        outStride = 3 * width;
        ySize = yBuffer.remaining();

        rgb = new byte[width * height * 3];
        nv21 = new byte[ySize + width * height / 2];
        uLineBuffer = new byte[uRowStride];
        vLineBuffer = new byte[vRowStride];
    }

    private static byte toByte(final int value) {
        return (byte)Math.min(255, Math.max(0, value));
    }

    private static int toUnsigned(final int value) {
        return (value + 256) % 256;
    }

    private static void fillRGBBytes(final byte[] rgb, final int r, final int g, final int b, final int y, final int index) {
        rgb[index + 2] = toByte(y + r);
        rgb[index + 1] = toByte(y - g);
        rgb[index    ] = toByte(y + b);
    }

    @NonNull
    public byte[] convert(final ImageProxy image) {
        checkBuffers(image);

        final var yPlane = image.getPlanes()[0];
        final var uPlane = image.getPlanes()[1];
        final var vPlane = image.getPlanes()[2];

        final var yBuffer = yPlane.getBuffer();
        final var uBuffer = uPlane.getBuffer();
        final var vBuffer = vPlane.getBuffer();

        yBuffer.rewind();
        uBuffer.rewind();
        vBuffer.rewind();

        var position = 0;
        for (int i = 0; i < height; ++i) {
            yBuffer.get(nv21, position, width);
            position += width;
            yBuffer.position(Math.min(ySize, yBuffer.position() - width + yRowStride));
        }

        for (int row = 0; row < chromaHeight; ++row) {
            vBuffer.get(vLineBuffer, 0, Math.min(vRowStride, vBuffer.remaining()));
            uBuffer.get(uLineBuffer, 0, Math.min(uRowStride, uBuffer.remaining()));

            var vLineBufferPosition = 0;
            var uLineBufferPosition = 0;
            for (int col = 0; col < chromaWidth; ++col) {
                nv21[position++] = vLineBuffer[vLineBufferPosition];
                nv21[position++] = uLineBuffer[uLineBufferPosition];

                vLineBufferPosition += vPixelStride;
                uLineBufferPosition += uPixelStride;
            }
        }

        int yIndex;
        int outIndex;
        int cIndex = height * width;

        for (int i = 0; i < height / 2; ++i) {
            yIndex = 2 * i * width;
            outIndex = 6 * i * width;

            for (int j = 0; j < width / 2; ++j) {
                final var u = toUnsigned(nv21[cIndex]);
                final var v = toUnsigned(nv21[cIndex + 1]);

                final var r = (91881 * v >> 16) - 179;
                final var g = ((22544 * u + 46793 * v) >> 16) - 135;
                final var b = (116129 * u >> 16) - 226;

                fillRGBBytes(rgb, r, g, b, toUnsigned(nv21[yIndex]), outIndex);
                fillRGBBytes(rgb, r, g, b, toUnsigned(nv21[yIndex + 1]), outIndex + 3);

                fillRGBBytes(rgb, r, g, b, toUnsigned(nv21[yIndex + width]), outIndex + outStride);
                fillRGBBytes(rgb, r, g, b, toUnsigned(nv21[yIndex + width + 1]), outIndex + outStride + 3);

                yIndex += 2;
                cIndex += 2;
                outIndex += 6;
            }
        }

        return rgb;
    }
}
