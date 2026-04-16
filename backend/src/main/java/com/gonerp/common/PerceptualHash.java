package com.gonerp.common;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * 64-bit DCT-based perceptual hash (pHash).
 *
 * Resize to 32×32 grayscale → 2D DCT → keep top-left 8×8 block (low frequencies)
 * → threshold against the median → 64 bits. Hamming distance between two hashes
 * is a cheap proxy for visual similarity. 0 = identical, &lt;10 = very close.
 */
public final class PerceptualHash {

    private PerceptualHash() {}

    private static final int SIZE = 32;
    private static final int SMALL = 8;

    public static Long compute(InputStream in) {
        if (in == null) return null;
        try {
            BufferedImage img = ImageIO.read(in);
            return compute(img);
        } catch (Exception e) {
            return null;
        }
    }

    public static Long compute(BufferedImage src) {
        if (src == null) return null;

        // 1. Resize to 32x32 grayscale
        BufferedImage gray = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(src, 0, 0, SIZE, SIZE, null);
        g.dispose();

        double[][] pixels = new double[SIZE][SIZE];
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                pixels[y][x] = gray.getRaster().getSample(x, y, 0);
            }
        }

        // 2. DCT
        double[][] dct = dct(pixels);

        // 3. Extract top-left 8x8 (skip DC at [0][0] when computing median for robustness)
        double[] block = new double[SMALL * SMALL];
        int k = 0;
        for (int y = 0; y < SMALL; y++) {
            for (int x = 0; x < SMALL; x++) {
                block[k++] = dct[y][x];
            }
        }

        // 4. Median of the 8x8 excluding the DC coefficient
        double median = median(block, 1);

        // 5. Build 64-bit hash: bit = 1 if coefficient > median
        long hash = 0L;
        for (int i = 0; i < block.length; i++) {
            if (block[i] > median) hash |= (1L << i);
        }
        return hash;
    }

    public static int hammingDistance(long a, long b) {
        return Long.bitCount(a ^ b);
    }

    // ── DCT-II on a 32x32 matrix ────────────────────────────────────

    private static final double[][] COS = buildCosTable();

    private static double[][] buildCosTable() {
        double[][] t = new double[SIZE][SIZE];
        for (int u = 0; u < SIZE; u++) {
            for (int x = 0; x < SIZE; x++) {
                t[u][x] = Math.cos(((2 * x + 1) * u * Math.PI) / (2.0 * SIZE));
            }
        }
        return t;
    }

    private static double[][] dct(double[][] pixels) {
        double[][] out = new double[SIZE][SIZE];
        for (int u = 0; u < SIZE; u++) {
            for (int v = 0; v < SIZE; v++) {
                double sum = 0;
                for (int x = 0; x < SIZE; x++) {
                    for (int y = 0; y < SIZE; y++) {
                        sum += pixels[y][x] * COS[u][x] * COS[v][y];
                    }
                }
                double cu = (u == 0) ? 1.0 / Math.sqrt(2) : 1.0;
                double cv = (v == 0) ? 1.0 / Math.sqrt(2) : 1.0;
                out[u][v] = 0.25 * cu * cv * sum;
            }
        }
        return out;
    }

    private static double median(double[] a, int fromIndex) {
        double[] copy = new double[a.length - fromIndex];
        System.arraycopy(a, fromIndex, copy, 0, copy.length);
        java.util.Arrays.sort(copy);
        int n = copy.length;
        if (n == 0) return 0;
        return (n % 2 == 1) ? copy[n / 2] : 0.5 * (copy[n / 2 - 1] + copy[n / 2]);
    }
}
