package com.iskra.game;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

import java.util.ArrayList;
import java.util.List;

/**
 * Синтезатор звука через AudioTrack: осцилляторы (sine/square/saw/triangle),
 * экспоненциальное затухание, glissando — порт WebAudio-кода оригинала.
 */
public class Sound {
    static final int SR = 22050;
    static boolean mute = false;
    static float vol = 0.5f;

    static class Tone {
        double f, d, slide, v;
        int type;
        long at;
        Tone(double f, double d, int type, double v, double slide) {
            this.f = f; this.d = d; this.type = type; this.v = v; this.slide = slide;
            this.at = System.nanoTime();
        }
    }

    static final List<Tone> queue = new ArrayList<>();
    static Mixer mixer;

    static void snd(double f, double d, int type, double v, double slide) {
        if (mute) return;
        synchronized (queue) {
            queue.add(new Tone(f, d, type, v, slide));
            if (mixer == null || !mixer.alive) {
                mixer = new Mixer();
                mixer.start();
            }
        }
    }

    static void setVol(float v) { vol = v; }
    static void setMute(boolean m) { mute = m; }

    static class Mixer extends Thread {
        boolean alive = true;
        Mixer() {
            setDaemon(true);
            setPriority(Thread.MAX_PRIORITY - 1);
        }
        @Override
        public void run() {
            int bufN = SR / 20;
            AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(SR)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setBufferSizeInBytes(bufN * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build();
            track.play();
            short[] buf = new short[bufN];
            List<Tone> live = new ArrayList<>();
            while (alive) {
                synchronized (queue) {
                    live.addAll(queue);
                    queue.clear();
                }
                for (int i = 0; i < bufN; i++) {
                    double t = (double) i / SR;
                    double s = 0;
                    for (int j = live.size() - 1; j >= 0; j--) {
                        Tone tn = live.get(j);
                        double age = (System.nanoTime() - tn.at) / 1e9 + t;
                        if (age >= tn.d) { live.remove(j); continue; }
                        double env = Math.pow(0.0001, age / tn.d);
                        double f = tn.f + tn.slide * age;
                        double ph = age * f;
                        double w;
                        switch (tn.type) {
                            case 1: w = (ph % 1 < 0.5) ? 1 : -1; break;
                            case 2: w = 2 * (ph % 1) - 1; break;
                            case 3: w = 2 * Math.abs(2 * (ph % 1) - 1) - 1; break;
                            default: w = Math.sin(2 * Math.PI * ph);
                        }
                        s += w * env * tn.v;
                    }
                    s *= vol * 0.25;
                    if (s > 1) s = 1; if (s < -1) s = -1;
                    buf[i] = (short) (s * 32767);
                }
                track.write(buf, 0, bufN);
                if (live.isEmpty()) {
                    synchronized (queue) {
                        if (queue.isEmpty()) { alive = false; mixer = null; break; }
                    }
                }
            }
            track.stop();
            track.release();
        }
    }
}
