package com.meshi.fairy.skin;

// builds the per-owner fairy texture (128x128 argb ints). pure java
// head faces + hat layer come from the skin, skin-colored pixels of the fairy get recolored
public final class SkinCompose {
    public static final int SIZE = 128;
    // median skin pixel of the fairy texture, colors get scaled relative to this
    private static final int[] PEACH = { 253, 217, 178 };

    // dst x,y of each 10x10 face (top-left of the rect), faces: north east south west up down
    private static final int[][] BASE_DST = { { 28, 18 }, { 24, 28 }, { 34, 28 }, { 38, 0 }, { 38, 10 }, { 24, 38 } };
    private static final int[][] HAT_DST = { { 70, 0 }, { 80, 0 }, { 90, 0 }, { 100, 0 }, { 70, 10 }, { 80, 10 } };
    // src x,y of each 8x8 face in the skin (base layer)
    private static final int[][] SRC = { { 8, 8 }, { 0, 8 }, { 24, 8 }, { 16, 8 }, { 8, 0 }, { 16, 0 } };

    private SkinCompose() {}

    public static int[] compose(int[] base, int[] skin, int rgb) {
        int[] out = base.clone();
        recolor(out, rgb);
        if (skin != null && skin.length >= 64 * 64) {
            for (int f = 0; f < 6; f++) {
                boolean flip = f >= 4; // up/down are rotated 180 in the geo
                paste(out, skin, SRC[f][0], SRC[f][1], BASE_DST[f][0], BASE_DST[f][1], flip, true);
                paste(out, skin, SRC[f][0] + 32, SRC[f][1], HAT_DST[f][0], HAT_DST[f][1], flip, false);
            }
        }
        return out;
    }

    private static void paste(int[] out, int[] skin, int sx, int sy, int dx, int dy, boolean rot, boolean opaque) {
        for (int y = 0; y < 10; y++)
            for (int x = 0; x < 10; x++) {
                int u = x * 8 / 10, v = y * 8 / 10;
                if (rot) { u = 7 - u; v = 7 - v; }
                int c = skin[(sy + v) * 64 + sx + u];
                if (opaque) c |= 0xFF000000;
                out[(dy + y) * SIZE + dx + x] = c;
            }
    }

    private static void recolor(int[] px, int rgb) {
        int tr = (rgb >> 16) & 255, tg = (rgb >> 8) & 255, tb = rgb & 255;
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            if ((c >>> 24) == 0) continue;
            float[] h = SkinColor.hsv(c & 0xFFFFFF);
            if (h[0] < 8f || h[0] > 38f || h[2] <= 0.8f || h[1] <= 0.1f) continue;
            int r = clamp(tr * ((c >> 16) & 255) / (float) PEACH[0]);
            int g = clamp(tg * ((c >> 8) & 255) / (float) PEACH[1]);
            int b = clamp(tb * (c & 255) / (float) PEACH[2]);
            px[i] = (c & 0xFF000000) | (r << 16) | (g << 8) | b;
        }
    }

    private static int clamp(float v) { return Math.max(0, Math.min(255, Math.round(v))); }
}
