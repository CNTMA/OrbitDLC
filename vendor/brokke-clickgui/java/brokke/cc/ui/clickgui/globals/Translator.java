/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 */
package brokke.cc.ui.clickgui.globals;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class Translator {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
    private static final ExecutorService EXEC = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "Brokke-Translator");
        t.setDaemon(true);
        return t;
    });
    private static final Map<String, Result> CACHE = new LinkedHashMap<String, Result>(64, 0.75f, true){

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Result> eldest) {
            return this.size() > 500;
        }
    };
    private static final Map<String, List<Consumer<Result>>> PENDING = new ConcurrentHashMap<String, List<Consumer<Result>>>();

    private Translator() {
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static Result cached(String text) {
        Map<String, Result> map = CACHE;
        synchronized (map) {
            return CACHE.get(text);
        }
    }

    public static boolean isPending(String text) {
        return PENDING.containsKey(text);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void translate(String text, String target, Consumer<Result> done) {
        if (text == null || text.isBlank()) {
            return;
        }
        Result hit = Translator.cached(text);
        if (hit != null) {
            if (done != null) {
                done.accept(hit);
            }
            return;
        }
        Map<String, List<Consumer<Result>>> map = PENDING;
        synchronized (map) {
            List<Consumer<Result>> waiting = PENDING.get(text);
            if (waiting != null) {
                if (done != null) {
                    waiting.add(done);
                }
                return;
            }
            waiting = new ArrayList<Consumer<Result>>();
            if (done != null) {
                waiting.add(done);
            }
            PENDING.put(text, waiting);
        }
        EXEC.execute(() -> {
            try {
                List<Consumer<Result>> waiting;
                Result result = Translator.chrome(text, target);
                if (result == null) {
                    result = Translator.google(text, target);
                }
                if (result == null) {
                    result = Translator.myMemory(text, target);
                }
                if (result == null) {
                    return;
                }
                Map<String, Result> map3 = CACHE;
                synchronized (map3) {
                    CACHE.put(text, result);
                }
                Map<String, List<Consumer<Result>>> map2 = PENDING;
                synchronized (map2) {
                    waiting = PENDING.remove(text);
                }
                if (waiting != null) {
                    for (Consumer<Result> c : waiting) {
                        c.accept(result);
                    }
                }
            }
            finally {
                Map<String, List<Consumer<Result>>> map4 = PENDING;
                synchronized (map4) {
                    PENDING.remove(text);
                }
            }
        });
    }

    private static String get(String url) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(8L)).header("User-Agent", "Mozilla/5.0").GET().build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return resp.statusCode() == 200 ? resp.body() : null;
        }
        catch (Exception e) {
            return null;
        }
    }

    private static Result make(String text, String translated, String source, String target) {
        if (translated == null || translated.isBlank()) {
            return null;
        }
        translated = translated.trim();
        return source.equalsIgnoreCase(target) || translated.equalsIgnoreCase(text.trim()) ? new Result(null, source) : new Result(translated, source);
    }

    private static Result chrome(String text, String target) {
        try {
            JsonArray inner = new JsonArray();
            JsonArray texts = new JsonArray();
            texts.add(Translator.escape(text));
            inner.add((JsonElement)texts);
            inner.add("auto");
            inner.add(target);
            JsonArray payload = new JsonArray();
            payload.add((JsonElement)inner);
            payload.add("wt_lib");
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://translate-pa.googleapis.com/v1/translateHtml")).timeout(Duration.ofSeconds(8L)).header("Content-Type", "application/json+protobuf").header("X-Goog-API-Key", "AIzaSyATBXajvzQLTDHEQbcpq0Ihe0vWDHmO520").POST(HttpRequest.BodyPublishers.ofString(payload.toString(), StandardCharsets.UTF_8)).build();
            HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                return null;
            }
            JsonArray root = JsonParser.parseString((String)resp.body()).getAsJsonArray();
            String translated = Translator.unescape(root.get(0).getAsJsonArray().get(0).getAsString());
            String source = root.size() > 1 && root.get(1).isJsonArray() && !root.get(1).getAsJsonArray().isEmpty() ? root.get(1).getAsJsonArray().get(0).getAsString() : "";
            return Translator.make(text, translated, source, target);
        }
        catch (Exception e) {
            return null;
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String unescape(String s) {
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&amp;", "&");
    }

    private static Result google(String text, String target) {
        String body = Translator.get("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=" + target + "&dt=t&q=" + URLEncoder.encode(text, StandardCharsets.UTF_8));
        if (body == null || !body.startsWith("[")) {
            return null;
        }
        try {
            JsonArray root = JsonParser.parseString((String)body).getAsJsonArray();
            StringBuilder out = new StringBuilder();
            if (root.get(0).isJsonArray()) {
                for (JsonElement part : root.get(0).getAsJsonArray()) {
                    if (!part.isJsonArray() || part.getAsJsonArray().get(0).isJsonNull()) continue;
                    out.append(part.getAsJsonArray().get(0).getAsString());
                }
            }
            String source = root.size() > 2 && root.get(2).isJsonPrimitive() ? root.get(2).getAsString() : "";
            return Translator.make(text, out.toString(), source, target);
        }
        catch (Exception e) {
            return null;
        }
    }

    private static Result myMemory(String text, String target) {
        String body = Translator.get("https://api.mymemory.translated.net/get?langpair=autodetect%7C" + target + "&q=" + URLEncoder.encode(text.length() > 480 ? text.substring(0, 480) : text, StandardCharsets.UTF_8));
        if (body == null) {
            return null;
        }
        try {
            JsonObject data = JsonParser.parseString((String)body).getAsJsonObject().getAsJsonObject("responseData");
            String source = data.has("detectedLanguage") && data.get("detectedLanguage").isJsonPrimitive() ? data.get("detectedLanguage").getAsString() : "";
            return Translator.make(text, data.get("translatedText").getAsString(), source, target);
        }
        catch (Exception e) {
            return null;
        }
    }

    public record Result(String text, String source) {
        public boolean same() {
            return this.text == null;
        }
    }
}

