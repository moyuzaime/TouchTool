package top.bogey.touch_tool.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * 流式 PNG 编码器：按横带渲染、逐行压进一个 deflate 流，再写成连续的 IDAT 块。
 * 内存只和「画布宽度 × 单带高度」有关，所以再大的图也能导出。
 */
public class PngStreamWriter {
    private static final byte[] SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
    // 单带占用的预算：位图与像素缓冲各一份，合计约 2 倍
    private static final long BAND_BUDGET = 32L * 1024 * 1024;
    private static final int MAX_BAND_HEIGHT = 2048;
    private static final int IDAT_FLUSH_SIZE = 256 * 1024;

    public interface BandPainter {
        // bandTop 是该带在整张图中的纵坐标，painter 负责把对应内容画进 canvas
        void paint(Canvas canvas, int bandTop, int bandHeight);
    }

    public static void write(File file, int width, int height, BandPainter painter) throws IOException {
        if (width <= 0 || height <= 0) throw new IOException("invalid size " + width + "x" + height);

        int bandHeight = (int) Math.min(MAX_BAND_HEIGHT,
                Math.max(1, BAND_BUDGET / ((long) width * 4)));
        bandHeight = Math.min(bandHeight, height);

        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
            outputStream.write(SIGNATURE);
            writeChunk(outputStream, "IHDR", ihdr(width, height));

            int rowBytes = width * 4;
            ByteArrayOutputStream pending = new ByteArrayOutputStream(IDAT_FLUSH_SIZE * 2);
            byte[] compressed = new byte[64 * 1024];

            for (int top = 0; top < height; top += bandHeight) {
                int currentHeight = Math.min(bandHeight, height - top);

                Bitmap band = Bitmap.createBitmap(width, currentHeight, Bitmap.Config.ARGB_8888);
                try {
                    Canvas canvas = new Canvas(band);
                    painter.paint(canvas, top, currentHeight);

                    ByteBuffer buffer = ByteBuffer.allocateDirect(rowBytes * currentHeight);
                    band.copyPixelsToBuffer(buffer);
                    buffer.rewind();

                    byte[] row = new byte[rowBytes + 1];
                    for (int y = 0; y < currentHeight; y++) {
                        row[0] = 0; // filter type: None
                        buffer.get(row, 1, rowBytes);
                        deflater.setInput(row);
                        while (!deflater.needsInput()) {
                            int count = deflater.deflate(compressed);
                            if (count > 0) pending.write(compressed, 0, count);
                            if (pending.size() >= IDAT_FLUSH_SIZE) {
                                writeChunk(outputStream, "IDAT", pending.toByteArray());
                                pending.reset();
                            }
                        }
                    }
                } finally {
                    band.recycle();
                }
            }

            deflater.finish();
            while (!deflater.finished()) {
                int count = deflater.deflate(compressed);
                if (count > 0) pending.write(compressed, 0, count);
            }
            if (pending.size() > 0) {
                writeChunk(outputStream, "IDAT", pending.toByteArray());
                pending.reset();
            }

            writeChunk(outputStream, "IEND", new byte[0]);
        } finally {
            deflater.end();
        }
    }

    private static byte[] ihdr(int width, int height) {
        byte[] data = new byte[13];
        putInt(data, 0, width);
        putInt(data, 4, height);
        data[8] = 8;  // bit depth
        data[9] = 6;  // color type: RGBA
        data[10] = 0; // compression
        data[11] = 0; // filter
        data[12] = 0; // interlace
        return data;
    }

    private static void putInt(byte[] target, int offset, int value) {
        target[offset] = (byte) (value >>> 24);
        target[offset + 1] = (byte) (value >>> 16);
        target[offset + 2] = (byte) (value >>> 8);
        target[offset + 3] = (byte) value;
    }

    private static void writeChunk(OutputStream outputStream, String type, byte[] data) throws IOException {
        byte[] typeBytes = type.getBytes("US-ASCII");
        byte[] length = new byte[4];
        putInt(length, 0, data.length);
        outputStream.write(length);
        outputStream.write(typeBytes);
        outputStream.write(data);

        CRC32 crc32 = new CRC32();
        crc32.update(typeBytes);
        crc32.update(data);
        byte[] crc = new byte[4];
        putInt(crc, 0, (int) crc32.getValue());
        outputStream.write(crc);
    }
}
