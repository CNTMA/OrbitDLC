/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  net.minecraft.client.Minecraft
 */
package brokke.cc.ui.clickgui.globals;

import brokke.cc.core.setting.ConfigManager;
import brokke.cc.ui.clickgui.CloudFriends;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

public final class CloudConfigs {
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Brokke-CloudConfigs");
        t.setDaemon(true);
        return t;
    });
    private static volatile List<Config> own = List.of();
    private static volatile List<Config> shared = List.of();
    private static final Map<String, List<Code>> CODES = new HashMap<String, List<Code>>();
    private static volatile boolean loaded = false;
    private static volatile boolean busy = false;
    private static volatile boolean connected = true;
    private static volatile long loadedAt = 0L;

    private CloudConfigs() {
    }

    public static List<Config> own() {
        return own;
    }

    public static List<Config> shared() {
        return shared;
    }

    public static synchronized List<Code> codes(String id) {
        return CODES.getOrDefault(id, List.of());
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static boolean isBusy() {
        return busy;
    }

    public static boolean isConnected() {
        return connected;
    }

    public static void refreshIfStale() {
        if (!busy && System.currentTimeMillis() - loadedAt > 30000L) {
            CloudConfigs.refresh();
        }
    }

    public static void refresh() {
        loadedAt = System.currentTimeMillis();
        CloudConfigs.run("/cfg/list", new JsonObject(), null, null);
    }

    public static void upload(String localName, Consumer<Result> done) {
        File file = new File(ConfigManager.getConfigsDir(), localName + ".json");
        EXEC.execute(() -> {
            String data;
            try {
                data = Files.readString(file.toPath(), StandardCharsets.UTF_8);
            }
            catch (Exception e) {
                CloudConfigs.finish(done, new Result(false, "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0440\u043e\u0447\u0438\u0442\u0430\u0442\u044c \u043a\u043e\u043d\u0444\u0438\u0433"));
                return;
            }
            JsonObject body = new JsonObject();
            body.addProperty("name", localName);
            body.addProperty("data", data);
            CloudConfigs.call("/cfg/upload", body, done, "\u041a\u043e\u043d\u0444\u0438\u0433 " + localName + " \u0432\u044b\u0433\u0440\u0443\u0436\u0435\u043d \u0432 \u043e\u0431\u043b\u0430\u043a\u043e");
        });
    }

    public static void apply(Config cfg, Consumer<Result> done) {
        EXEC.execute(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("id", cfg.id());
            try {
                busy = true;
                JsonObject json = CloudFriends.api("/cfg/get", body);
                if (!CloudConfigs.ok(json)) {
                    CloudConfigs.finish(done, new Result(false, CloudConfigs.error(json)));
                    return;
                }
                String name = CloudConfigs.safeName((String)(cfg.own() ? "cloud-" : "cloud-" + cfg.author() + "-") + cfg.name());
                File file = new File(ConfigManager.getConfigsDir(), name + ".json");
                Files.writeString(file.toPath(), (CharSequence)json.get("data").getAsString(), StandardCharsets.UTF_8, new OpenOption[0]);
                Minecraft.getInstance().execute(() -> {
                    boolean loadedOk = ConfigManager.loadConfig(name);
                    if (done != null) {
                        done.accept(new Result(loadedOk, (String)(loadedOk ? "\u041e\u0431\u043b\u0430\u0447\u043d\u044b\u0439 \u043a\u043e\u043d\u0444\u0438\u0433 " + cfg.name() + " \u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043d" : "\u041e\u0448\u0438\u0431\u043a\u0430 \u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0438 \u043a\u043e\u043d\u0444\u0438\u0433\u0430")));
                    }
                });
            }
            catch (Exception e) {
                connected = false;
                CloudConfigs.finish(done, new Result(false, "\u0421\u0435\u0440\u0432\u0435\u0440 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d"));
            }
            finally {
                busy = false;
            }
        });
    }

    public static void delete(Config cfg, Consumer<Result> done) {
        JsonObject body = new JsonObject();
        body.addProperty("id", cfg.id());
        CloudConfigs.run(cfg.own() ? "/cfg/delete" : "/cfg/forget", body, done, cfg.own() ? "\u041a\u043e\u043d\u0444\u0438\u0433 \u0443\u0434\u0430\u043b\u0451\u043d \u0438\u0437 \u043e\u0431\u043b\u0430\u043a\u0430" : "\u041a\u043e\u043d\u0444\u0438\u0433 \u0443\u0431\u0440\u0430\u043d \u0438\u0437 \u0441\u043f\u0438\u0441\u043a\u0430");
    }

    public static void redeem(String code, Consumer<Result> done) {
        JsonObject body = new JsonObject();
        body.addProperty("code", code);
        EXEC.execute(() -> {
            JsonObject json = CloudConfigs.call("/cfg/redeem", body, null, null);
            if (json == null) {
                CloudConfigs.finish(done, new Result(false, "\u0421\u0435\u0440\u0432\u0435\u0440 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d"));
            } else if (CloudConfigs.ok(json)) {
                String name = json.has("redeemed") ? json.getAsJsonObject("redeemed").get("name").getAsString() : "";
                CloudConfigs.finish(done, new Result(true, "\u041a\u043b\u044e\u0447 \u0430\u043a\u0442\u0438\u0432\u0438\u0440\u043e\u0432\u0430\u043d: " + name));
            } else {
                CloudConfigs.finish(done, new Result(false, CloudConfigs.error(json)));
            }
        });
    }

    public static void createCode(Config cfg, int uses, Consumer<Result> done) {
        EXEC.execute(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("id", cfg.id());
            body.addProperty("uses", (Number)uses);
            try {
                JsonObject json = CloudFriends.api("/cfg/code/create", body);
                if (!CloudConfigs.ok(json)) {
                    CloudConfigs.finish(done, new Result(false, CloudConfigs.error(json)));
                    return;
                }
                String code = json.get("code").getAsString();
                Minecraft.getInstance().execute(() -> Minecraft.getInstance().keyboardHandler.setClipboard(code));
                CloudConfigs.loadCodes(cfg.id());
                CloudConfigs.finish(done, new Result(true, "\u041a\u043b\u044e\u0447 " + code + " \u0441\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d"));
            }
            catch (Exception e) {
                connected = false;
                CloudConfigs.finish(done, new Result(false, "\u0421\u0435\u0440\u0432\u0435\u0440 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d"));
            }
        });
    }

    public static void deleteCode(String cfgId, String code, Consumer<Result> done) {
        EXEC.execute(() -> {
            JsonObject body = new JsonObject();
            body.addProperty("code", code);
            try {
                JsonObject json = CloudFriends.api("/cfg/code/delete", body);
                CloudConfigs.loadCodes(cfgId);
                CloudConfigs.finish(done, CloudConfigs.ok(json) ? new Result(true, "\u041a\u043b\u044e\u0447 \u0443\u0434\u0430\u043b\u0451\u043d") : new Result(false, CloudConfigs.error(json)));
            }
            catch (Exception e) {
                connected = false;
                CloudConfigs.finish(done, new Result(false, "\u0421\u0435\u0440\u0432\u0435\u0440 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d"));
            }
        });
    }

    public static void requestCodes(String cfgId) {
        EXEC.execute(() -> CloudConfigs.loadCodes(cfgId));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void loadCodes(String cfgId) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("id", cfgId);
            JsonObject json = CloudFriends.api("/cfg/code/list", body);
            if (!CloudConfigs.ok(json)) {
                return;
            }
            ArrayList<Code> list = new ArrayList<Code>();
            for (JsonElement el : json.getAsJsonArray("codes")) {
                JsonObject o = el.getAsJsonObject();
                list.add(new Code(o.get("code").getAsString(), o.get("left").getAsInt(), o.get("max").getAsInt()));
            }
            Class<CloudConfigs> clazz = CloudConfigs.class;
            synchronized (CloudConfigs.class) {
                CODES.put(cfgId, List.copyOf(list));
                // ** MonitorExit[var4_5] (shouldn't be in output)
            }
        }
        catch (Exception e) {
            connected = false;
        }
        {
            return;
        }
    }

    private static void run(String path, JsonObject body, Consumer<Result> done, String success) {
        EXEC.execute(() -> CloudConfigs.call(path, body, done, success));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static JsonObject call(String path, JsonObject body, Consumer<Result> done, String success) {
        busy = true;
        try {
            JsonObject json = CloudFriends.api(path, body);
            if (json == null) {
                CloudConfigs.finish(done, new Result(false, "\u041d\u0443\u0436\u043d\u043e \u0432\u043e\u0439\u0442\u0438 \u0432 \u0430\u043a\u043a\u0430\u0443\u043d\u0442 Brokke"));
                JsonObject jsonObject = null;
                return jsonObject;
            }
            connected = true;
            if (CloudConfigs.ok(json)) {
                if (json.has("own")) {
                    own = CloudConfigs.parse(json.getAsJsonArray("own"));
                    shared = CloudConfigs.parse(json.getAsJsonArray("shared"));
                    loaded = true;
                }
                if (success != null) {
                    CloudConfigs.finish(done, new Result(true, success));
                }
            } else {
                CloudConfigs.finish(done, new Result(false, CloudConfigs.error(json)));
            }
            JsonObject jsonObject = json;
            return jsonObject;
        }
        catch (Exception e) {
            connected = false;
            CloudConfigs.finish(done, new Result(false, "\u0421\u0435\u0440\u0432\u0435\u0440 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d"));
            JsonObject jsonObject = null;
            return jsonObject;
        }
        finally {
            busy = false;
        }
    }

    private static List<Config> parse(JsonArray arr) {
        ArrayList<Config> list = new ArrayList<Config>();
        if (arr == null) {
            return list;
        }
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            list.add(new Config(o.get("id").getAsString(), o.get("name").getAsString(), o.get("author").getAsString(), o.get("own").getAsBoolean(), o.get("size").getAsInt(), o.get("updated").getAsLong(), o.get("downloads").getAsInt()));
        }
        return List.copyOf(list);
    }

    private static boolean ok(JsonObject json) {
        return json != null && json.has("ok") && json.get("ok").getAsBoolean();
    }

    private static String error(JsonObject json) {
        String code;
        return switch (code = json != null && json.has("error") ? json.get("error").getAsString() : "bad_request") {
            case "bad_code" -> "\u041d\u0435\u0432\u0435\u0440\u043d\u044b\u0439 \u043a\u043b\u044e\u0447";
            case "code_used" -> "\u041a\u043b\u044e\u0447 \u0443\u0436\u0435 \u0438\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u043d";
            case "already_have" -> "\u042d\u0442\u043e\u0442 \u043a\u043e\u043d\u0444\u0438\u0433 \u0443 \u0442\u0435\u0431\u044f \u0443\u0436\u0435 \u0435\u0441\u0442\u044c";
            case "own_config" -> "\u042d\u0442\u043e \u0442\u0432\u043e\u0439 \u0441\u043e\u0431\u0441\u0442\u0432\u0435\u043d\u043d\u044b\u0439 \u043a\u043e\u043d\u0444\u0438\u0433";
            case "too_large" -> "\u041a\u043e\u043d\u0444\u0438\u0433 \u0441\u043b\u0438\u0448\u043a\u043e\u043c \u0431\u043e\u043b\u044c\u0448\u043e\u0439";
            case "limit" -> "\u0414\u043e\u0441\u0442\u0438\u0433\u043d\u0443\u0442 \u043b\u0438\u043c\u0438\u0442";
            case "bad_name" -> "\u041d\u0435\u043a\u043e\u0440\u0440\u0435\u043a\u0442\u043d\u043e\u0435 \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u0435";
            case "not_found" -> "\u041a\u043e\u043d\u0444\u0438\u0433 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d";
            case "rate_limited" -> "\u0421\u043b\u0438\u0448\u043a\u043e\u043c \u043c\u043d\u043e\u0433\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432";
            default -> "\u041e\u0448\u0438\u0431\u043a\u0430: " + code;
        };
    }

    private static String safeName(String name) {
        String s = name.replaceAll("[^\\p{L}\\p{N}_\\- ]", "_");
        return s.length() > 40 ? s.substring(0, 40) : s;
    }

    private static void finish(Consumer<Result> done, Result result) {
        if (done == null) {
            return;
        }
        Minecraft.getInstance().execute(() -> done.accept(result));
    }

    public record Config(String id, String name, String author, boolean own, int size, long updated, int downloads) {
    }

    public record Code(String code, int left, int max) {
    }

    public record Result(boolean ok, String message) {
    }
}

