package com.meshi.fairy.skin;

import java.util.ArrayList;
import java.util.List;

// picks the owner's skin color from a 64x64 skin (argb ints). pure java, no minecraft stuff
// face + hands + feet vote, hair colors get dropped, similar shades merge
public final class SkinColor {
    public static final int NONE = -1;
    // shown when no skin color can be found (not peach, so it doesn't look like a wrong skin tone)
    public static final int FAIL_COLOR = 0xC8B9EB;

    // tunables
    static final double MERGE_DIST = 24;
    static final double HAIR_DIST = 8;
    static final double W_FACE = 1.5, W_HAND = 1.0, W_FOOT = 0.5;

    private SkinColor() {}

    private static final class Cluster {
        double r, g, b, w;
        int count;
        Cluster(int c, double w) { add(c, w); }
        void add(int c, double wt) {
            r += ((c >> 16) & 255) * wt; g += ((c >> 8) & 255) * wt; b += (c & 255) * wt;
            w += wt; count++;
        }
        int rgb() { return ((int) Math.round(r / w) << 16) | ((int) Math.round(g / w) << 8) | (int) Math.round(b / w); }
    }

    static double dist(int a, int b) {
        int dr = ((a >> 16) & 255) - ((b >> 16) & 255);
        int dg = ((a >> 8) & 255) - ((b >> 8) & 255);
        int db = (a & 255) - (b & 255);
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }

    // hsv: h degrees, s, v 0..1
    static float[] hsv(int c) {
        float r = ((c >> 16) & 255) / 255f, g = ((c >> 8) & 255) / 255f, b = (c & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float h = 0;
        if (d > 0) {
            if (max == r) h = ((g - b) / d) % 6f;
            else if (max == g) h = (b - r) / d + 2f;
            else h = (r - g) / d + 4f;
            h *= 60f;
            if (h < 0) h += 360f;
        }
        return new float[] { h, max == 0 ? 0 : d / max, max };
    }

    static boolean ignored(int c) {
        float[] h = hsv(c);
        return h[2] < 0.25f || h[1] < 0.1f; // near black, white, grey
    }

    static boolean skinLooking(int c) {
        float[] h = hsv(c);
        return h[0] <= 40f && h[1] >= 0.1f && h[1] <= 0.7f;
    }

    private static int px(int[] skin, int x, int y) { return skin[y * 64 + x]; }

    // opaque pixels of a rect, minus ignored ones
    private static List<Integer> rect(int[] skin, int x0, int y0, int x1, int y1) {
        List<Integer> out = new ArrayList<>();
        for (int y = y0; y <= y1; y++)
            for (int x = x0; x <= x1; x++) {
                int c = px(skin, x, y);
                if ((c >>> 24) < 200) continue;
                c &= 0xFFFFFF;
                if (!ignored(c)) out.add(c);
            }
        return out;
    }

    // dominant merged color of a pixel list, or NONE
    private static int dominant(List<Integer> px) {
        List<Cluster> cl = new ArrayList<>();
        for (int c : px) add(cl, c, 1);
        Cluster best = null;
        for (Cluster k : cl) if (best == null || k.w > best.w) best = k;
        return best == null ? NONE : best.rgb();
    }

    private static void add(List<Cluster> cl, int c, double w) {
        Cluster best = null;
        double bd = MERGE_DIST;
        for (Cluster k : cl) {
            double d = dist(k.rgb(), c);
            if (d <= bd) { bd = d; best = k; }
        }
        if (best == null) cl.add(new Cluster(c, w));
        else best.add(c, w);
    }

    // every opaque color in the hair area: top/back/sides of the head base + the whole hat layer
    private static List<Integer> hair(int[] skin) {
        List<Integer> out = new ArrayList<>();
        int[][] r = {
                { 8, 0, 15, 7 }, { 0, 8, 7, 15 }, { 16, 8, 31, 15 },
                { 32, 0, 63, 15 } };
        for (int[] a : r)
            for (int y = a[1]; y <= a[3]; y++)
                for (int x = a[0]; x <= a[2]; x++) {
                    int c = px(skin, x, y);
                    if ((c >>> 24) >= 200) out.add(c & 0xFFFFFF);
                }
        return out;
    }

    private static List<Integer> dropHair(List<Integer> face, List<Integer> hair, int[] keep) {
        List<Integer> out = new ArrayList<>();
        for (int c : face) {
            boolean h = false;
            boolean kept = false;
            for (int k : keep) if (k != NONE && dist(c, k) <= MERGE_DIST) kept = true;
            if (kept) { out.add(c); continue; }
            for (int hc : hair) if (dist(c, hc) <= HAIR_DIST) { h = true; break; }
            if (!h) out.add(c);
        }
        return out;
    }

    // returns rgb (0xRRGGBB) or FAIL_COLOR
    public static int pick(int[] skin) {
        if (skin == null || skin.length < 64 * 64) return FAIL_COLOR;
        List<Integer> hair = hair(skin);

        // hands + feet: last pixel row of the limbs only (sleeves/pants cover the rest)
        int handR = dominant(rect(skin, 44, 31, 47, 31));
        int handL = dominant(rect(skin, 36, 63, 39, 63));
        int footR = dominant(rect(skin, 4, 31, 7, 31));
        int footL = dominant(rect(skin, 20, 63, 23, 63));
        // a face color that matches a hand is skin even if the hair area has it too
        int face = dominant(dropHair(rect(skin, 8, 10, 15, 15), hair, new int[] { handR, handL }));

        int[] cols = { face, handR, handL, footR, footL };
        double[] wts = { W_FACE, W_HAND, W_HAND, W_FOOT, W_FOOT };
        List<Cluster> votes = new ArrayList<>();
        for (int i = 0; i < cols.length; i++) if (cols[i] != NONE) add(votes, cols[i], wts[i]);

        // unusual colors only win when face and both hands agree
        Cluster best = null;
        for (Cluster k : votes) {
            boolean ok = skinLooking(k.rgb()) || (k.w >= W_FACE + 2 * W_HAND);
            if (!ok) continue;
            if (best == null || k.w > best.w + 0.001) best = k;
            else if (Math.abs(k.w - best.w) <= 0.001 && skinLooking(k.rgb()) && !skinLooking(best.rgb())) best = k;
        }
        if (best != null) return best.rgb();

        // fallbacks: face (not hair), a hand, arm, leg
        if (face != NONE) return face;
        if (handR != NONE) return handR;
        if (handL != NONE) return handL;
        int arm = dominant(rect(skin, 44, 20, 47, 29));
        if (arm != NONE) return arm;
        int leg = dominant(rect(skin, 4, 20, 7, 29));
        if (leg != NONE) return leg;
        return FAIL_COLOR;
    }
}
