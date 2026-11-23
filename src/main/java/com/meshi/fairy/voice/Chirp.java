package com.meshi.fairy.voice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import de.maxhenkel.voicechat.api.opus.OpusEncoder;

// chirpy voice: linear resample up (higher pitch + a bit faster), reframed to 960 samples for opus
public final class Chirp {
    private static final int FRAME = 960;
    private static final double RATE = 1.3;

    private final OpusEncoder enc;
    private final OpusDecoder dec;
    private final short[] buf = new short[FRAME * 4];
    private int len;
    private double pos;
    private short prev;

    public Chirp(OpusEncoder enc, OpusDecoder dec) {
        this.enc = enc;
        this.dec = dec;
    }

    public List<byte[]> process(byte[] opus) {
        List<byte[]> out = new ArrayList<>();
        short[] in = dec.decode(opus);
        int n = in.length;
        if (n < 2) return out;

        while (pos < n - 1 && len < buf.length) {
            int i = (int) Math.floor(pos);
            double f = pos - i;
            short a = i < 0 ? prev : in[i];
            short b = in[i + 1];
            buf[len++] = (short) (a + (b - a) * f);
            pos += RATE;
        }
        pos -= n;
        prev = in[n - 1];
        drain(out);
        return out;
    }

    // end of speech: push whatever is left (zero padded) and reset
    public List<byte[]> flush() {
        List<byte[]> out = new ArrayList<>();
        if (len > 0) {
            Arrays.fill(buf, len, FRAME, (short) 0);
            len = FRAME;
            drain(out);
        }
        len = 0;
        pos = 0;
        prev = 0;
        dec.resetState();
        enc.resetState();
        return out;
    }

    private void drain(List<byte[]> out) {
        while (len >= FRAME) {
            out.add(enc.encode(Arrays.copyOf(buf, FRAME)));
            System.arraycopy(buf, FRAME, buf, 0, len - FRAME);
            len -= FRAME;
        }
    }
}
