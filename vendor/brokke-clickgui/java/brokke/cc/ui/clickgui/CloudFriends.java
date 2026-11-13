/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  com.mojang.serialization.DynamicOps
 *  com.mojang.serialization.JsonOps
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ServerData
 *  net.minecraft.core.registries.BuiltInRegistries
 *  net.minecraft.network.chat.Component
 *  net.minecraft.resources.Identifier
 *  net.minecraft.resources.RegistryOps
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 */
package brokke.cc.ui.clickgui;

import brokke.cc.core.cosmetics.system.LocalCosmetics;
import brokke.cc.core.setting.ConfigManager;
import brokke.cc.system.scripting.api.ScriptGame;
import brokke.cc.ui.clickgui.globals.GlobalChat;
import brokke.cc.ui.clickgui.globals.Profiles;
import brokke.cc.ui.hud.legacy.notification.NotificationManager;
import brokke.cc.ui.hud.legacy.notification.NotificationType;
import brokke.cc.util.Log;
import brokke.cc.util.sound.ClientSound;
import brokke.cc.utils.User;
import brokke.cc.utils.player.AnarchyUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public final class CloudFriends {
    public static final int INVENTORY_SLOTS = 41;
    private static final int MAX_ITEM_JSON = 2500;
    private static final Pattern TAB_ANARCHY = Pattern.compile("#\\s*\\d{1,3}(?!\\d)");
    private static final String API = "http://31.76.28.223:3790";
    private static final String CFG_API = "http://31.76.28.223:3850";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
    private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Brokke-CloudFriends");
        t.setDaemon(true);
        return t;
    });
    private static volatile Snapshot snapshot = new Snapshot(List.of(), List.of(), List.of());
    private static volatile boolean connected = false;
    private static volatile boolean synced = false;
    private static volatile String error = "";
    private static volatile long errorTime = 0L;
    private static volatile long successTime = 0L;
    private static volatile boolean shareInventory = true;
    private static volatile boolean shareLocation = true;
    private static volatile String marked;
    private static volatile String watched;
    private static volatile long watchedAt;
    private static volatile FriendInventory watchedInv;
    private static String lastUpload;
    private static long lastUploadTime;
    private static String token;
    private static boolean started;

    private CloudFriends() {
    }

    public static synchronized void init() {
        if (started) {
            return;
        }
        started = true;
        token = CloudFriends.loadToken();
        GlobalChat.init();
        EXEC.scheduleWithFixedDelay(() -> CloudFriends.call("/sync", null), 2L, 5L, TimeUnit.SECONDS);
        EXEC.scheduleWithFixedDelay(CloudFriends::uploadInventory, 4L, 3L, TimeUnit.SECONDS);
        EXEC.scheduleWithFixedDelay(CloudFriends::fetchWatched, 3L, 2L, TimeUnit.SECONDS);
    }

    public static synchronized String token() {
        if (token == null) {
            token = CloudFriends.loadToken();
        }
        return token;
    }

    public static String nick() {
        return CloudFriends.currentNick();
    }

    public static void refresh() {
        EXEC.execute(() -> CloudFriends.call("/sync", null));
    }

    public static void request(String nick) {
        EXEC.execute(() -> CloudFriends.call("/request", nick));
    }

    public static void accept(String nick) {
        EXEC.execute(() -> CloudFriends.call("/accept", nick));
    }

    public static void decline(String nick) {
        EXEC.execute(() -> CloudFriends.call("/decline", nick));
    }

    public static void cancel(String nick) {
        EXEC.execute(() -> CloudFriends.call("/cancel", nick));
    }

    public static void remove(String nick) {
        EXEC.execute(() -> CloudFriends.call("/remove", nick));
    }

    public static void invite(String nick) {
        EXEC.execute(() -> {
            try {
                JsonObject extra = new JsonObject();
                extra.addProperty("target", nick);
                JsonObject json = CloudFriends.post("/invite", extra);
                if (CloudFriends.ok(json)) {
                    successTime = System.currentTimeMillis();
                    CloudFriends.notifyUser("\u041f\u0440\u0438\u0433\u043b\u0430\u0448\u0435\u043d\u0438\u0435 \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u043e " + nick, NotificationType.ENABLE);
                } else if (json != null) {
                    String code = json.has("error") ? json.get("error").getAsString() : "bad_request";
                    CloudFriends.fail(code.equals("slow_down") ? "rate_limited" : code);
                }
            }
            catch (Exception e) {
                connected = false;
                CloudFriends.fail("offline");
            }
        });
    }

    public static List<Entry> friends() {
        return snapshot.friends();
    }

    public static List<Entry> incoming() {
        return snapshot.incoming();
    }

    public static List<Entry> outgoing() {
        return snapshot.outgoing();
    }

    public static boolean isConnected() {
        return connected;
    }

    public static boolean isSynced() {
        return synced;
    }

    public static String error() {
        return error;
    }

    public static long errorTime() {
        return errorTime;
    }

    public static long successTime() {
        return successTime;
    }

    public static boolean isFriend(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (Entry e : snapshot.friends()) {
            if (!e.mc().equalsIgnoreCase(name)) continue;
            return true;
        }
        return false;
    }

    public static List<String> mcNicks() {
        ArrayList<String> list = new ArrayList<String>();
        for (Entry e : snapshot.friends()) {
            if (e.mc().isEmpty()) continue;
            list.add(e.mc());
        }
        return list;
    }

    public static boolean isValidName(String name) {
        return name != null && name.matches("[\\p{L}\\p{N}_.\\-]{2,24}");
    }

    public static JsonObject api(String path, JsonObject extra) throws Exception {
        return CloudFriends.post(path.startsWith("/cfg/") ? CFG_API : API, path, extra);
    }

    public static int[] cosmeticsOf(String mcName) {
        if (mcName == null || mcName.isEmpty()) {
            return null;
        }
        for (Entry e : snapshot.friends()) {
            if (!e.online() || !e.mc().equalsIgnoreCase(mcName)) continue;
            return e.cos();
        }
        return null;
    }

    private static void addCosmetics(JsonObject body) {
        try {
            JsonArray arr = new JsonArray();
            for (int index : LocalCosmetics.selectedIndices()) {
                arr.add((Number)LocalCosmetics.resourceIndex(index));
            }
            body.add("cos", (JsonElement)arr);
        }
        catch (Throwable ignored) {
            Log.swallow(ignored);
        }
    }

    private static JsonObject post(String path, JsonObject extra) throws Exception {
        return CloudFriends.post(API, path, extra);
    }

    private static JsonObject post(String base, String path, JsonObject extra) throws Exception {
        String nick = CloudFriends.currentNick();
        if (nick == null) {
            return null;
        }
        JsonObject body = extra != null ? extra : new JsonObject();
        body.addProperty("token", token);
        body.addProperty("nick", nick);
        body.addProperty("mc", CloudFriends.currentMcNick());
        body.addProperty("server", CloudFriends.currentServer());
        CloudFriends.addPresence(body);
        if ("/sync".equals(path)) {
            CloudFriends.addCosmetics(body);
        }
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(8L)).header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8)).build();
        HttpResponse<String> resp = HTTP.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        JsonObject json = JsonParser.parseString((String)resp.body()).getAsJsonObject();
        connected = true;
        return json;
    }

    private static void addPresence(JsonObject body) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null || mc.player == null || mc.level == null) {
                return;
            }
            String an = CloudFriends.anarchyLabel();
            if (!an.isEmpty()) {
                body.addProperty("an", an);
            }
            if (!shareLocation) {
                return;
            }
            JsonObject pos = new JsonObject();
            pos.addProperty("x", (Number)((int)Math.floor(mc.player.getX())));
            pos.addProperty("y", (Number)((int)Math.floor(mc.player.getY())));
            pos.addProperty("z", (Number)((int)Math.floor(mc.player.getZ())));
            pos.addProperty("dim", mc.level.dimension().identifier().getPath());
            body.add("pos", (JsonElement)pos);
        }
        catch (Throwable ignored) {
            Log.swallow(ignored);
        }
    }

    private static String anarchyLabel() {
        try {
            String header = new ScriptGame.TabList().header();
            if (header != null) {
                for (String line : header.split("\n")) {
                    String clean = line.replaceAll("\u00a7.", "").trim();
                    if (!TAB_ANARCHY.matcher(clean).find()) continue;
                    return clean.length() > 32 ? clean.substring(0, 32) : clean;
                }
            }
        }
        catch (Throwable ignored) {
            Log.swallow(ignored);
        }
        int n = AnarchyUtil.getCurrentAnarchy();
        return n > 0 ? "\u0410\u043d\u0430\u0440\u0445\u0438\u044f " + n : "";
    }

    private static boolean ok(JsonObject json) {
        return json != null && json.has("ok") && json.get("ok").getAsBoolean();
    }

    public static void watch(String nick) {
        watchedAt = System.currentTimeMillis();
        if (nick != null && !nick.equalsIgnoreCase(watched)) {
            watched = nick;
            EXEC.execute(CloudFriends::fetchWatched);
        }
    }

    public static FriendInventory watchedInventory() {
        FriendInventory inv = watchedInv;
        String w = watched;
        return inv != null && w != null && inv.nick().equalsIgnoreCase(w) ? inv : null;
    }

    private static void fetchWatched() {
        String nick = watched;
        if (nick == null || System.currentTimeMillis() - watchedAt > 4000L) {
            return;
        }
        try {
            JsonObject extra = new JsonObject();
            extra.addProperty("target", nick);
            JsonObject json = CloudFriends.post("/inventory/get", extra);
            if (!CloudFriends.ok(json)) {
                watchedInv = new FriendInventory(nick, null, 0, 0L, false);
                return;
            }
            JsonArray items = json.has("inv") && json.get("inv").isJsonArray() ? json.getAsJsonArray("inv") : null;
            int sel = json.has("sel") ? json.get("sel").getAsInt() : 0;
            long at = json.has("at") ? json.get("at").getAsLong() : 0L;
            boolean hidden = json.has("hidden") && json.get("hidden").getAsBoolean();
            FriendInventory prev = watchedInv;
            if (prev != null && prev.nick().equalsIgnoreCase(nick) && prev.at() == at && prev.hidden() == hidden && (items != null || hidden)) {
                return;
            }
            watchedInv = new FriendInventory(nick, hidden ? null : items, sel, at, hidden);
        }
        catch (Exception e) {
            connected = false;
        }
    }

    private static void uploadInventory() {
        JsonObject payload;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || snapshot.friends().isEmpty()) {
            return;
        }
        if (!shareInventory) {
            if ("hidden".equals(lastUpload)) {
                return;
            }
            try {
                JsonObject hidden = new JsonObject();
                hidden.addProperty("hidden", Boolean.valueOf(true));
                if (CloudFriends.ok(CloudFriends.post("/inventory", hidden))) {
                    lastUpload = "hidden";
                }
            }
            catch (Exception e) {
                connected = false;
            }
            return;
        }
        if (mc.player == null || mc.level == null) {
            return;
        }
        try {
            payload = (JsonObject)mc.submit(CloudFriends::captureInventory).get(2L, TimeUnit.SECONDS);
        }
        catch (Exception e) {
            return;
        }
        if (payload == null) {
            return;
        }
        String key = payload.toString();
        long now = System.currentTimeMillis();
        if (key.equals(lastUpload) && now - lastUploadTime < 20000L) {
            return;
        }
        try {
            if (CloudFriends.ok(CloudFriends.post("/inventory", payload))) {
                lastUpload = key;
                lastUploadTime = now;
            }
        }
        catch (Exception e) {
            connected = false;
        }
    }

    private static JsonObject captureInventory() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return null;
        }
        RegistryOps<JsonElement> ops = mc.level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        Inventory inv = mc.player.getInventory();
        JsonArray arr = new JsonArray();
        int size = Math.min(41, inv.getContainerSize());
        for (int i = 0; i < size; ++i) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            JsonElement encoded = ItemStack.CODEC.encodeStart(ops, stack).result().orElse(null);
            if (encoded == null || encoded.toString().length() > 2500) {
                encoded = ItemStack.CODEC.encodeStart(ops, new ItemStack((ItemLike)stack.getItem(), stack.getCount())).result().orElse(null);
            }
            if (encoded == null) continue;
            JsonObject o = new JsonObject();
            o.addProperty("s", (Number)i);
            o.add("i", encoded);
            arr.add((JsonElement)o);
        }
        JsonObject out = new JsonObject();
        out.add("inv", (JsonElement)arr);
        out.addProperty("sel", (Number)inv.getSelectedSlot());
        return out;
    }

    public static ItemStack[] decodeInventory(JsonArray items) {
        ItemStack[] stacks = new ItemStack[41];
        Arrays.fill(stacks, ItemStack.EMPTY);
        Minecraft mc = Minecraft.getInstance();
        if (items == null || mc.level == null) {
            return stacks;
        }
        RegistryOps<JsonElement> ops = mc.level.registryAccess().createSerializationContext(JsonOps.INSTANCE);
        for (JsonElement el : items) {
            try {
                JsonObject o = el.getAsJsonObject();
                int slot = o.get("s").getAsInt();
                if (slot < 0 || slot >= 41) continue;
                JsonElement data = o.get("i");
                ItemStack stack = ItemStack.CODEC.parse(ops, data).result().orElse(null);
                if (stack == null && data.isJsonObject() && data.getAsJsonObject().has("id")) {
                    JsonObject d = data.getAsJsonObject();
                    Identifier id = Identifier.tryParse((String)d.get("id").getAsString());
                    int count = d.has("count") ? d.get("count").getAsInt() : 1;
                    ItemStack itemStack = stack = id == null ? null : (ItemStack)BuiltInRegistries.ITEM.getOptional(id).map(item -> new ItemStack((ItemLike)item, count)).orElse(null);
                }
                if (stack == null) continue;
                stacks[slot] = stack;
            }
            catch (Exception ignored) {
                Log.swallow(ignored);
            }
        }
        return stacks;
    }

    private static void call(String path, String target) {
        block8: {
            JsonObject extra = new JsonObject();
            if (target != null) {
                extra.addProperty("target", target);
            }
            try {
                JsonObject json = CloudFriends.post(path, extra);
                if (json == null) {
                    return;
                }
                if (CloudFriends.ok(json)) {
                    Snapshot next;
                    Snapshot prev = snapshot;
                    snapshot = next = new Snapshot(CloudFriends.parse(json, "friends"), CloudFriends.parse(json, "incoming"), CloudFriends.parse(json, "outgoing"));
                    CloudFriends.announce(prev, next, !synced);
                    Profiles.onSync(json);
                    CloudFriends.handleExtras(json);
                    synced = true;
                    if (target != null) {
                        successTime = System.currentTimeMillis();
                    }
                } else if (target != null) {
                    CloudFriends.fail(json.has("error") ? json.get("error").getAsString() : "bad_request");
                }
            }
            catch (Exception e) {
                connected = false;
                if (target == null) break block8;
                CloudFriends.fail("offline");
            }
        }
    }

    private static void announce(Snapshot prev, Snapshot next, boolean first) {
        ArrayList<String> fresh = new ArrayList<String>();
        for (Entry e : next.incoming()) {
            if (!first && CloudFriends.contains(prev.incoming(), e.nick())) continue;
            fresh.add(e.nick());
        }
        if (first && !fresh.isEmpty()) {
            CloudFriends.notifyUser(fresh.size() == 1 ? (String)fresh.get(0) + " \u0445\u043e\u0447\u0435\u0442 \u0434\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0432\u0430\u0441 \u0432 \u0434\u0440\u0443\u0437\u044c\u044f" : "\u0423 \u0432\u0430\u0441 " + fresh.size() + " \u0437\u0430\u044f\u0432\u043e\u043a \u0432 \u0434\u0440\u0443\u0437\u044c\u044f", NotificationType.INFO);
        } else {
            for (String nick : fresh) {
                CloudFriends.notifyUser(nick + " \u0445\u043e\u0447\u0435\u0442 \u0434\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0432\u0430\u0441 \u0432 \u0434\u0440\u0443\u0437\u044c\u044f", NotificationType.INFO);
            }
        }
        if (first) {
            return;
        }
        for (Entry e : next.friends()) {
            if (!e.online()) continue;
            for (Entry old : prev.friends()) {
                if (!old.nick().equalsIgnoreCase(e.nick()) || old.online()) continue;
                CloudFriends.notifyUser(e.nick() + " \u0437\u0430\u0448\u0451\u043b \u0432 \u0438\u0433\u0440\u0443" + (String)(e.location().isEmpty() ? "" : " (" + e.location() + ")"), NotificationType.INFO);
            }
        }
        for (Entry e : next.friends()) {
            if (CloudFriends.contains(prev.friends(), e.nick()) || !CloudFriends.contains(prev.outgoing(), e.nick())) continue;
            CloudFriends.notifyUser(e.nick() + " \u043f\u0440\u0438\u043d\u044f\u043b \u0432\u0430\u0448\u0443 \u0437\u0430\u044f\u0432\u043a\u0443 \u0432 \u0434\u0440\u0443\u0437\u044c\u044f", NotificationType.ENABLE);
        }
    }

    private static void handleExtras(JsonObject json) {
        if (json.has("achNew") && json.get("achNew").isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray("achNew")) {
                CloudFriends.notifyUser("\u0414\u043e\u0441\u0442\u0438\u0436\u0435\u043d\u0438\u0435: " + el.getAsString() + " (+25 \u043e\u0447\u043a\u043e\u0432)", NotificationType.ENABLE);
            }
        }
        if (!json.has("invites") || !json.get("invites").isJsonArray()) {
            return;
        }
        for (JsonElement el : json.getAsJsonArray("invites")) {
            if (!el.isJsonObject()) continue;
            JsonObject inv = el.getAsJsonObject();
            String from = inv.get("from").getAsString();
            String where = inv.has("an") && !inv.get("an").getAsString().isEmpty() ? inv.get("an").getAsString() : (inv.has("server") ? inv.get("server").getAsString() : "");
            Object coords = "";
            if (inv.has("pos") && inv.get("pos").isJsonObject()) {
                JsonObject p = inv.getAsJsonObject("pos");
                coords = p.get("x").getAsInt() + " " + p.get("y").getAsInt() + " " + p.get("z").getAsInt();
            }
            String text = from + " \u0437\u043e\u0432\u0451\u0442 \u0442\u0435\u0431\u044f" + (String)(where.isEmpty() ? "" : " \u043d\u0430 " + where) + (String)(((String)coords).isEmpty() ? "" : " (" + (String)coords + ")");
            CloudFriends.notifyUser(text, NotificationType.INFO);
            String chatText = text;
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.player != null) {
                    mc.player.sendSystemMessage((Component)Component.literal((String)("\u00a77[\u00a7b\u0414\u0440\u0443\u0437\u044c\u044f\u00a77] \u00a7f" + chatText)));
                }
            });
            if (((String)coords).isEmpty() || CloudFriends.isMarked(from)) continue;
            marked = from;
        }
    }

    private static boolean contains(List<Entry> list, String nick) {
        for (Entry e : list) {
            if (!e.nick().equalsIgnoreCase(nick)) continue;
            return true;
        }
        return false;
    }

    private static void notifyUser(String message, NotificationType type) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        mc.execute(() -> {
            try {
                if (NotificationManager.isEnabled()) {
                    NotificationManager.notify("\u0414\u0440\u0443\u0437\u044c\u044f", message, type, 5000L);
                } else if (mc.player != null) {
                    mc.player.sendSystemMessage((Component)Component.literal((String)("\u00a77[\u00a7b\u0414\u0440\u0443\u0437\u044c\u044f\u00a77] \u00a7f" + message)));
                }
                ClientSound.checkbox(true);
            }
            catch (Throwable ignored) {
                Log.swallow(ignored);
            }
        });
    }

    private static void fail(String code) {
        error = switch (code) {
            case "not_found" -> "\u0418\u0433\u0440\u043e\u043a \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d \u0438\u043b\u0438 \u043d\u0435 \u0437\u0430\u0445\u043e\u0434\u0438\u043b \u0441 \u043a\u043b\u0438\u0435\u043d\u0442\u043e\u043c";
            case "self" -> "\u041d\u0435\u043b\u044c\u0437\u044f \u0434\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0441\u0430\u043c\u043e\u0433\u043e \u0441\u0435\u0431\u044f";
            case "already_friends" -> "\u0423\u0436\u0435 \u0432 \u0434\u0440\u0443\u0437\u044c\u044f\u0445";
            case "already_sent" -> "\u0417\u0430\u044f\u0432\u043a\u0430 \u0443\u0436\u0435 \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0430";
            case "limit" -> "\u0414\u043e\u0441\u0442\u0438\u0433\u043d\u0443\u0442 \u043b\u0438\u043c\u0438\u0442 \u0434\u0440\u0443\u0437\u0435\u0439";
            case "rate_limited" -> "\u0421\u043b\u0438\u0448\u043a\u043e\u043c \u043c\u043d\u043e\u0433\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432";
            case "bad_nick" -> "\u041d\u0435\u043a\u043e\u0440\u0440\u0435\u043a\u0442\u043d\u044b\u0439 \u043d\u0438\u043a";
            case "offline" -> "\u0421\u0435\u0440\u0432\u0435\u0440 \u0434\u0440\u0443\u0437\u0435\u0439 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d";
            default -> "\u041e\u0448\u0438\u0431\u043a\u0430: " + code;
        };
        errorTime = System.currentTimeMillis();
    }

    private static List<Entry> parse(JsonObject json, String key) {
        ArrayList<Entry> list = new ArrayList<Entry>();
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return list;
        }
        JsonArray arr = json.getAsJsonArray(key);
        for (JsonElement el : arr) {
            JsonObject o;
            if (!el.isJsonObject() || !(o = el.getAsJsonObject()).has("nick")) continue;
            boolean online = o.has("online") && o.get("online").getAsBoolean();
            String server = o.has("server") && !o.get("server").isJsonNull() ? o.get("server").getAsString() : "";
            String mc = o.has("mc") && !o.get("mc").isJsonNull() ? o.get("mc").getAsString() : "";
            String an = o.has("an") && o.get("an").isJsonPrimitive() ? o.get("an").getAsString() : "";
            int[] pos = null;
            String dim = "";
            if (o.has("pos") && o.get("pos").isJsonObject()) {
                JsonObject p = o.getAsJsonObject("pos");
                pos = new int[]{p.get("x").getAsInt(), p.get("y").getAsInt(), p.get("z").getAsInt()};
                dim = p.has("dim") ? p.get("dim").getAsString() : "";
            }
            int[] cos = new int[]{};
            if (o.has("cos") && o.get("cos").isJsonArray()) {
                JsonArray c = o.getAsJsonArray("cos");
                cos = new int[c.size()];
                for (int i = 0; i < cos.length; ++i) {
                    cos[i] = c.get(i).getAsInt();
                }
            }
            String status = o.has("status") && o.get("status").isJsonPrimitive() ? o.get("status").getAsString() : "";
            list.add(new Entry(o.get("nick").getAsString(), mc, online, server, an, pos, dim, cos, status));
        }
        return List.copyOf(list);
    }

    private static String currentNick() {
        String name = User.getInstance().getUsername();
        return CloudFriends.isValidName(name) ? name : null;
    }

    private static String currentMcNick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getUser() == null) {
            return "";
        }
        String name = mc.getUser().getName();
        return name != null && name.matches("[A-Za-z0-9_]{3,16}") ? name : "";
    }

    private static String currentServer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getConnection() == null) {
            return "";
        }
        ServerData data = mc.getCurrentServer();
        if (data == null) {
            return "\u041e\u0434\u0438\u043d\u043e\u0447\u043d\u0430\u044f \u0438\u0433\u0440\u0430";
        }
        return data.ip == null ? "" : data.ip;
    }

    private static String loadToken() {
        String t;
        String t2;
        JsonObject data = ConfigManager.loadJson("cloud.json");
        if (data.has("shareInventory")) {
            shareInventory = data.get("shareInventory").getAsBoolean();
        }
        if (data.has("shareLocation")) {
            shareLocation = data.get("shareLocation").getAsBoolean();
        }
        if (data.has("token") && (t2 = data.get("token").getAsString()).matches("[a-f0-9]{64}")) {
            return t2;
        }
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        token = t = HexFormat.of().formatHex(bytes);
        CloudFriends.saveState();
        return t;
    }

    private static void saveState() {
        JsonObject out = new JsonObject();
        out.addProperty("token", token);
        out.addProperty("shareInventory", Boolean.valueOf(shareInventory));
        out.addProperty("shareLocation", Boolean.valueOf(shareLocation));
        ConfigManager.saveJson("cloud.json", out);
    }

    public static boolean isShareInventory() {
        return shareInventory;
    }

    public static boolean isShareLocation() {
        return shareLocation;
    }

    public static void toggleShareInventory() {
        shareInventory = !shareInventory;
        CloudFriends.saveState();
        lastUpload = "";
        EXEC.execute(CloudFriends::uploadInventory);
    }

    public static void toggleShareLocation() {
        shareLocation = !shareLocation;
        CloudFriends.saveState();
        CloudFriends.refresh();
    }

    public static String markedNick() {
        return marked;
    }

    public static boolean isMarked(String nick) {
        String m = marked;
        return m != null && m.equalsIgnoreCase(nick);
    }

    public static Entry markedEntry() {
        String m = marked;
        if (m == null) {
            return null;
        }
        for (Entry e : snapshot.friends()) {
            if (!e.nick().equalsIgnoreCase(m)) continue;
            return e;
        }
        return null;
    }

    public static void toggleMark(String nick) {
        if (nick == null) {
            return;
        }
        if (CloudFriends.isMarked(nick)) {
            marked = null;
            CloudFriends.notifyUser("\u041c\u0435\u0442\u043a\u0430 \u043d\u0430 " + nick + " \u0443\u0431\u0440\u0430\u043d\u0430", NotificationType.DISABLE);
            return;
        }
        marked = nick;
        Entry e = CloudFriends.markedEntry();
        if (e != null && !e.hasPos()) {
            CloudFriends.notifyUser("\u041c\u0435\u0442\u043a\u0430 \u043d\u0430 " + nick + " \u043f\u043e\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0430, \u043d\u043e \u043e\u043d \u0441\u043a\u0440\u044b\u043b \u043a\u043e\u043e\u0440\u0434\u0438\u043d\u0430\u0442\u044b", NotificationType.WARNING);
        } else {
            CloudFriends.notifyUser("\u041c\u0435\u0442\u043a\u0430 \u043f\u043e\u0441\u0442\u0430\u0432\u043b\u0435\u043d\u0430 \u043d\u0430 " + nick, NotificationType.ENABLE);
        }
    }

    public static void clearMark(String message) {
        marked = null;
        if (message != null) {
            CloudFriends.notifyUser(message, NotificationType.INFO);
        }
    }

    static {
        watchedAt = 0L;
        lastUpload = "";
        lastUploadTime = 0L;
        started = false;
    }

    private record Snapshot(List<Entry> friends, List<Entry> incoming, List<Entry> outgoing) {
    }

    public record Entry(String nick, String mc, boolean online, String server, String anarchy, int[] pos, String dim, int[] cos, String status) {
        public boolean hasPos() {
            return this.pos != null;
        }

        public boolean hasAnarchy() {
            return !this.anarchy.isEmpty();
        }

        public String location() {
            return this.hasAnarchy() ? this.anarchy : this.server;
        }

        public String coords() {
            if (this.pos == null) {
                return "";
            }
            String base = this.pos[0] + ", " + this.pos[1] + ", " + this.pos[2];
            String d = switch (this.dim) {
                case "the_nether" -> "\u0410\u0434";
                case "the_end" -> "\u042d\u043d\u0434";
                case "overworld", "" -> "";
                default -> this.dim;
            };
            return d.isEmpty() ? base : base + " \u00b7 " + d;
        }
    }

    public record FriendInventory(String nick, JsonArray items, int selected, long at, boolean hidden) {
    }
}

