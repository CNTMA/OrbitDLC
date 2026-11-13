/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 */
package brokke.cc.ui.clickgui.globals;

import brokke.cc.ui.clickgui.CloudFriends;
import brokke.cc.ui.clickgui.globals.GlobalChat;
import brokke.cc.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

public final class VoiceMessages {
    public static final float RATE = 12000.0f;
    public static final float MAX_SECONDS = 15.0f;
    private static final int WAVE_BARS = 40;
    private static final ExecutorService NET = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Brokke-Voice");
        t.setDaemon(true);
        return t;
    });
    private static final Map<String, byte[]> CACHE = new LinkedHashMap<String, byte[]>(16, 0.75f, true){

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
            return this.size() > 30;
        }
    };
    private static volatile boolean recording = false;
    private static volatile long recordStart = 0L;
    private static volatile float level = 0.0f;
    private static volatile boolean cancelled = false;
    private static volatile String error = "";
    private static volatile long errorTime = 0L;
    private static volatile boolean uploading = false;
    private static volatile String playingId = null;
    private static volatile long playStart = 0L;
    private static volatile float playDuration = 0.0f;
    private static volatile String loadingId = null;
    private static volatile Thread playThread;

    private VoiceMessages() {
    }

    public static boolean isRecording() {
        return recording;
    }

    public static boolean isUploading() {
        return uploading;
    }

    public static float recordSeconds() {
        return recording ? (float)(System.currentTimeMillis() - recordStart) / 1000.0f : 0.0f;
    }

    public static float level() {
        return level;
    }

    public static String error() {
        return error;
    }

    public static long errorTime() {
        return errorTime;
    }

    private static void fail(String text) {
        error = text;
        errorTime = System.currentTimeMillis();
    }

    public static void startRecording() {
        TargetDataLine line;
        if (recording || uploading) {
            return;
        }
        AudioFormat format = new AudioFormat(12000.0f, 16, 1, true, false);
        try {
            line = (TargetDataLine)AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, format));
            line.open(format);
        }
        catch (Exception e) {
            VoiceMessages.fail("\u041c\u0438\u043a\u0440\u043e\u0444\u043e\u043d \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d");
            return;
        }
        recording = true;
        cancelled = false;
        recordStart = System.currentTimeMillis();
        Thread t = new Thread(() -> VoiceMessages.capture(line), "Brokke-VoiceRecord");
        t.setDaemon(true);
        t.start();
    }

    public static void stopRecording() {
        recording = false;
    }

    public static void cancelRecording() {
        cancelled = true;
        recording = false;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void capture(TargetDataLine line) {
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        byte[] buf = new byte[1200];
        try {
            line.start();
            int maxBytes = 360000;
            while (recording && pcm.size() < maxBytes) {
                int n = line.read(buf, 0, buf.length);
                if (n <= 0) continue;
                pcm.write(buf, 0, n);
                int peak = 0;
                int i = 0;
                while (i + 1 < n) {
                    int v = Math.abs((short)(buf[i] & 0xFF | buf[i + 1] << 8));
                    if (v > peak) {
                        peak = v;
                    }
                    i += 2;
                }
                level = Math.min(1.0f, (float)peak / 12000.0f);
            }
        }
        catch (Exception e) {
            VoiceMessages.fail("\u041e\u0448\u0438\u0431\u043a\u0430 \u0437\u0430\u043f\u0438\u0441\u0438");
        }
        finally {
            recording = false;
            level = 0.0f;
            try {
                line.stop();
                line.close();
            }
            catch (Exception ignored) {
                Log.swallow(ignored);
            }
        }
        if (cancelled) {
            return;
        }
        byte[] data = pcm.toByteArray();
        float seconds = (float)data.length / 2.0f / 12000.0f;
        if (seconds < 0.5f) {
            VoiceMessages.fail("\u0421\u043b\u0438\u0448\u043a\u043e\u043c \u043a\u043e\u0440\u043e\u0442\u043a\u043e\u0435");
            return;
        }
        VoiceMessages.upload(data, seconds);
    }

    private static void upload(byte[] pcm, float seconds) {
        int samples = pcm.length / 2;
        byte[] ulaw = new byte[samples];
        int[] wave = new int[40];
        int per = Math.max(1, samples / 40);
        int maxPeak = 1;
        int[] peaks = new int[40];
        for (int i = 0; i < samples; ++i) {
            int v = (short)(pcm[i * 2] & 0xFF | pcm[i * 2 + 1] << 8);
            ulaw[i] = VoiceMessages.linearToUlaw((short)v);
            int bar = Math.min(39, i / per);
            int abs = Math.abs(v);
            if (abs <= peaks[bar]) continue;
            peaks[bar] = abs;
        }
        for (int p : peaks) {
            maxPeak = Math.max(maxPeak, p);
        }
        JsonArray waveJson = new JsonArray();
        for (int i = 0; i < 40; ++i) {
            wave[i] = Math.round((float)peaks[i] * 15.0f / (float)maxPeak);
            waveJson.add((Number)wave[i]);
        }
        uploading = true;
        NET.execute(() -> {
            block10: {
                try {
                    JsonObject body = new JsonObject();
                    body.addProperty("data", Base64.getEncoder().encodeToString(ulaw));
                    body.addProperty("dur", (Number)Float.valueOf(seconds));
                    body.add("wave", (JsonElement)waveJson);
                    JsonObject json = CloudFriends.api("/chat/voice", body);
                    if (json == null) {
                        VoiceMessages.fail("\u041d\u0443\u0436\u043d\u043e \u0432\u043e\u0439\u0442\u0438 \u0432 \u0430\u043a\u043a\u0430\u0443\u043d\u0442 Brokke");
                        break block10;
                    }
                    if (!json.has("ok") || !json.get("ok").getAsBoolean()) {
                        String code;
                        String string = code = json.has("error") ? json.get("error").getAsString() : "";
                        VoiceMessages.fail((String)(code.equals("slow_down") ? "\u041d\u0435 \u0442\u0430\u043a \u0431\u044b\u0441\u0442\u0440\u043e" : (code.equals("too_large") ? "\u0421\u043b\u0438\u0448\u043a\u043e\u043c \u0434\u043b\u0438\u043d\u043d\u043e\u0435" : "\u041e\u0448\u0438\u0431\u043a\u0430: " + code)));
                        break block10;
                    }
                    String id = json.getAsJsonObject("msg").getAsJsonObject("voice").get("id").getAsString();
                    Map<String, byte[]> map = CACHE;
                    synchronized (map) {
                        CACHE.put(id, ulaw);
                    }
                    GlobalChat.refreshNow();
                }
                catch (Exception e) {
                    VoiceMessages.fail("\u0421\u0435\u0440\u0432\u0435\u0440 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d");
                }
                finally {
                    uploading = false;
                }
            }
        });
    }

    public static boolean isPlaying(String id) {
        return id != null && id.equals(playingId);
    }

    public static boolean isLoading(String id) {
        return id != null && id.equals(loadingId);
    }

    public static float progress(String id) {
        if (!VoiceMessages.isPlaying(id) || playDuration <= 0.0f) {
            return 0.0f;
        }
        return Math.clamp((float)(System.currentTimeMillis() - playStart) / 1000.0f / playDuration, 0.0f, 1.0f);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void toggle(String id) {
        byte[] cached;
        if (VoiceMessages.isPlaying(id)) {
            VoiceMessages.stopPlayback();
            return;
        }
        VoiceMessages.stopPlayback();
        Map<String, byte[]> map = CACHE;
        synchronized (map) {
            cached = CACHE.get(id);
        }
        if (cached != null) {
            VoiceMessages.play(id, cached);
            return;
        }
        loadingId = id;
        NET.execute(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("id", id);
                JsonObject json = CloudFriends.api("/chat/voice/get", body);
                if (json == null || !json.has("ok") || !json.get("ok").getAsBoolean()) {
                    VoiceMessages.fail("\u0413\u043e\u043b\u043e\u0441\u043e\u0432\u043e\u0435 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u043d\u043e");
                    return;
                }
                byte[] data = Base64.getDecoder().decode(json.get("data").getAsString());
                Map<String, byte[]> map2 = CACHE;
                synchronized (map2) {
                    CACHE.put(id, data);
                }
                if (id.equals(loadingId)) {
                    VoiceMessages.play(id, data);
                }
            }
            catch (Exception e) {
                VoiceMessages.fail("\u0421\u0435\u0440\u0432\u0435\u0440 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d");
            }
            finally {
                if (id.equals(loadingId)) {
                    loadingId = null;
                }
            }
        });
    }

    public static void stopPlayback() {
        playingId = null;
        loadingId = null;
        Thread t = playThread;
        if (t != null) {
            t.interrupt();
        }
    }

    private static void play(String id, byte[] ulaw) {
        playingId = id;
        playDuration = (float)ulaw.length / 12000.0f;
        playStart = System.currentTimeMillis();
        Thread t = new Thread(() -> {
            AudioFormat format = new AudioFormat(12000.0f, 16, 1, true, false);
            try (SourceDataLine line = (SourceDataLine)AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, format));){
                int n;
                line.open(format);
                line.start();
                byte[] out = new byte[2400];
                for (int i = 0; i < ulaw.length && id.equals(playingId); i += n) {
                    n = Math.min(out.length / 2, ulaw.length - i);
                    for (int k = 0; k < n; ++k) {
                        short v = VoiceMessages.ulawToLinear(ulaw[i + k]);
                        out[k * 2] = (byte)v;
                        out[k * 2 + 1] = (byte)(v >> 8);
                    }
                    line.write(out, 0, n * 2);
                }
                if (id.equals(playingId)) {
                    line.drain();
                } else {
                    line.flush();
                }
            }
            catch (Exception e) {
                VoiceMessages.fail("\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0432\u043e\u0441\u043f\u0440\u043e\u0438\u0437\u0432\u0435\u0441\u0442\u0438");
            }
            finally {
                if (id.equals(playingId)) {
                    playingId = null;
                }
            }
        }, "Brokke-VoicePlay");
        t.setDaemon(true);
        playThread = t;
        t.start();
    }

    private static byte linearToUlaw(short sample) {
        int pcm = sample;
        int sign = pcm >> 8 & 0x80;
        if (sign != 0) {
            pcm = -pcm;
        }
        if (pcm > 32635) {
            pcm = 32635;
        }
        pcm += 132;
        int exponent = 7;
        int mask = 16384;
        while ((pcm & mask) == 0 && exponent > 0) {
            --exponent;
            mask >>= 1;
        }
        int mantissa = pcm >> exponent + 3 & 0xF;
        return (byte)(~(sign | exponent << 4 | mantissa));
    }

    private static short ulawToLinear(byte b) {
        int u = ~b & 0xFF;
        int sign = u & 0x80;
        int exponent = u >> 4 & 7;
        int mantissa = u & 0xF;
        int sample = (mantissa << 3) + 132 << exponent;
        return (short)(sign != 0 ? -sample : (sample -= 132));
    }
}

