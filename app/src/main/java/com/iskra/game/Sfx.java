package com.iskra.game;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/**
 * Мини-синтезатор звука: замена WebAudio из оригинала.
 * Генерирует короткие тоны (sine/square/sawtooth) со скольжением частоты
 * и экспоненциальным затуханием, пишет в AudioTrack.
 */
public final class Sfx {

    private static final int SR = 22050;
    private static volatile float volume = 0.8f;
    private static volatile boolean muted = false;
    private static AudioTrack track;
    private static final Object lock = new Object();

    private Sfx() {}

    public static void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
    }

    public static void setMuted(boolean m) {
        muted = m;
    }

    public static boolean isMuted() {
        return muted;
    }

    /**
     * @param f   частота, Гц
     * @param d   длительность, секунды
     * @param ty  0 — sine, 1 — square, 2 — sawtooth, 3 — triangle
     * @param v   громкость 0..0.2
     * @param sl  скольжение частоты, Гц (0 — без)
     */
    public static void snd(final float f, final float d, final int ty, final float v, final float sl) {
        if (muted || volume <= 0f) return;
        final int n = Math.max(1, (int) (d * SR));
        final short[] pcm = new short[n];
        float freq = f;
        float phase = 0f;
        for (int i = 0; i < n; i++) {
            float t = (float) i / n;
            if (sl != 0f) freq = f + sl * t;
            phase += freq / SR;
            float w = phase % 1f;
            if (w < 0) w += 1f;
            float s;
            switch (ty) {
                case 1: s = w < 0.5f ? 1f : -1f; break;                 // square
                case 2: s = 2f * w - 1f; break;                          // sawtooth
                case 3: s = 1f - 4f * Math.abs(w - 0.5f); break;         // triangle
                default: s = (float) Math.sin(2 * Math.PI * w); break;   // sine
            }
            float env = (float) Math.exp(-4.5 * t);
            float amp = v * volume * 1.25f;
            if (amp > 0.9f) amp = 0.9f;
            pcm[i] = (short) (s * env * amp * Short.MAX_VALUE);
        }
        write(pcm);
    }

    private static void write(final short[] pcm) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (lock) {
                    try {
                        if (track == null) {
                            int min = AudioTrack.getMinBufferSize(SR,
                                    AudioFormat.CHANNEL_OUT_MONO,
                                    AudioFormat.ENCODING_PCM_16BIT);
                            track = new AudioTrack.Builder()
                                    .setAudioAttributes(new AudioAttributes.Builder()
                                            .setUsage(AudioAttributes.USAGE_GAME)
                                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                            .build())
                                    .setAudioFormat(new AudioFormat.Builder()
                                            .setSampleRate(SR)
                                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                            .build())
                                    .setBufferSizeInBytes(Math.max(min, SR))
                                    .setTransferMode(AudioTrack.MODE_STREAM)
                                    .build();
                            track.play();
                        }
                        track.write(pcm, 0, pcm.length);
                    } catch (Throwable ignored) {
                        // без звука игра работает
                    }
                }
            }
        }, "sfx").start();
    }

    public static void release() {
        synchronized (lock) {
            if (track != null) {
                try {
                    track.stop();
                    track.release();
                } catch (Throwable ignored) {}
                track = null;
            }
        }
    }
}
