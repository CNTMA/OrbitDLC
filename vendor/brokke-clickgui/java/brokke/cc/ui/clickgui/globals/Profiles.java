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

import brokke.cc.ui.clickgui.CloudFriends;
import brokke.cc.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

public final class Profiles {
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Brokke-Profiles");
        t.setDaemon(true);
        return t;
    });
    private static volatile Profile viewed;
    private static volatile String viewedNick;
    private static volatile boolean viewedFailed;
    private static volatile Profile own;
    private static volatile Top top;
    private static volatile int points;
    private static volatile int streak;
    private static volatile boolean claimed;
    private static volatile boolean synced;
    private static volatile long topAt;

    private Profiles() {
    }

    public static void onSync(JsonObject json) {
        if (json.has("points")) {
            points = json.get("points").getAsInt();
        }
        if (json.has("streak")) {
            streak = json.get("streak").getAsInt();
        }
        if (json.has("claimed")) {
            claimed = json.get("claimed").getAsBoolean();
        }
        synced = true;
    }

    public static boolean canClaim() {
        return synced && !claimed;
    }

    public static int points() {
        return points;
    }

    public static int streak() {
        return streak;
    }

    public static Profile viewed() {
        Profile p = viewed;
        String n = viewedNick;
        return p != null && n != null && p.nick().equalsIgnoreCase(n) ? p : null;
    }

    public static String viewedNick() {
        return viewedNick;
    }

    public static boolean viewedFailed() {
        return viewedFailed;
    }

    public static Profile own() {
        return own;
    }

    public static Top top() {
        return top;
    }

    public static void view(String nick) {
        viewedNick = nick;
        viewedFailed = false;
        if (nick != null) {
            EXEC.execute(() -> Profiles.fetch(nick, false));
        }
    }

    public static void close() {
        viewedNick = null;
    }

    public static void refreshOwn() {
        String nick = CloudFriends.nick();
        if (nick == null) {
            return;
        }
        EXEC.execute(() -> Profiles.fetch(nick, true));
        if (System.currentTimeMillis() - topAt > 30000L) {
            topAt = System.currentTimeMillis();
            EXEC.execute(Profiles::fetchTop);
        }
    }

    private static void fetch(String nick, boolean self) {
        block6: {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("target", nick);
                JsonObject json = CloudFriends.api("/profile/get", body);
                if (json == null || !json.has("ok") || !json.get("ok").getAsBoolean()) {
                    if (!self) {
                        viewedFailed = true;
                    }
                    return;
                }
                Profile p = Profiles.parse(json);
                if (self) {
                    own = p;
                    points = p.points();
                    streak = p.streak();
                }
                if (nick.equalsIgnoreCase(viewedNick)) {
                    viewed = p;
                }
            }
            catch (Exception e) {
                if (self) break block6;
                viewedFailed = true;
            }
        }
    }

    private static Profile parse(JsonObject o) {
        ArrayList<Ach> ach = new ArrayList<Ach>();
        for (JsonElement el : o.getAsJsonArray("ach")) {
            JsonObject a = el.getAsJsonObject();
            ach.add(new Ach(a.get("id").getAsString(), a.get("title").getAsString(), a.get("desc").getAsString(), a.get("at").getAsLong()));
        }
        JsonArray c = o.getAsJsonArray("cos");
        int[] cos = new int[c == null ? 0 : c.size()];
        for (int i = 0; i < cos.length; ++i) {
            cos[i] = c.get(i).getAsInt();
        }
        return new Profile(o.get("nick").getAsString(), o.get("mc").getAsString(), o.get("self").getAsBoolean(), o.get("friend").getAsBoolean(), o.get("pending").getAsBoolean(), o.get("online").getAsBoolean(), o.get("server").getAsString(), o.get("status").getAsString(), o.get("bio").getAsString(), o.get("role").getAsString(), o.get("created").getAsLong(), o.get("lastSeen").getAsLong(), o.get("points").getAsInt(), o.get("streak").getAsInt(), o.get("best").getAsInt(), o.get("friends").getAsInt(), o.get("msgs").getAsInt(), o.get("downloads").getAsInt(), cos, List.copyOf(ach));
    }

    private static void fetchTop() {
        try {
            JsonObject json = CloudFriends.api("/top", new JsonObject());
            if (json == null || !json.has("ok")) {
                return;
            }
            top = new Top(Profiles.parseTop(json, "points"), Profiles.parseTop(json, "streak"), Profiles.parseTop(json, "msgs"));
        }
        catch (Exception ignored) {
            Log.swallow(ignored);
        }
    }

    private static List<TopEntry> parseTop(JsonObject json, String key) {
        ArrayList<TopEntry> list = new ArrayList<TopEntry>();
        if (!json.has(key)) {
            return list;
        }
        for (JsonElement el : json.getAsJsonArray(key)) {
            JsonObject o = el.getAsJsonObject();
            list.add(new TopEntry(o.get("nick").getAsString(), o.get("value").getAsInt()));
        }
        return List.copyOf(list);
    }

    public static void setProfile(String status, String bio, Consumer<Boolean> done) {
        EXEC.execute(() -> {
            try {
                JsonObject json;
                boolean ok;
                JsonObject body = new JsonObject();
                if (status != null) {
                    body.addProperty("status", status);
                }
                if (bio != null) {
                    body.addProperty("bio", bio);
                }
                boolean bl = ok = (json = CloudFriends.api("/profile/set", body)) != null && json.has("ok") && json.get("ok").getAsBoolean();
                if (ok) {
                    Profiles.fetch(CloudFriends.nick(), true);
                }
                Profiles.finish(done, ok);
            }
            catch (Exception e) {
                Profiles.finish(done, false);
            }
        });
    }

    public static void claim(Consumer<String> done) {
        EXEC.execute(() -> {
            try {
                JsonObject json = CloudFriends.api("/daily/claim", new JsonObject());
                if (json == null) {
                    Profiles.finishText(done, "\u041d\u0443\u0436\u043d\u043e \u0432\u043e\u0439\u0442\u0438 \u0432 \u0430\u043a\u043a\u0430\u0443\u043d\u0442 Brokke");
                    return;
                }
                if (json.has("ok") && json.get("ok").getAsBoolean()) {
                    claimed = true;
                    points = json.get("points").getAsInt();
                    streak = json.get("streak").getAsInt();
                    Profiles.fetch(CloudFriends.nick(), true);
                    Profiles.finishText(done, "+" + json.get("reward").getAsInt() + " \u043e\u0447\u043a\u043e\u0432, \u0441\u0442\u0440\u0438\u043a " + streak + " \u0434\u043d.");
                } else {
                    String err;
                    String string = err = json.has("error") ? json.get("error").getAsString() : "";
                    if (err.equals("already_claimed")) {
                        claimed = true;
                    }
                    Profiles.finishText(done, (String)(err.equals("already_claimed") ? "\u041d\u0430\u0433\u0440\u0430\u0434\u0430 \u0443\u0436\u0435 \u043f\u043e\u043b\u0443\u0447\u0435\u043d\u0430 \u0441\u0435\u0433\u043e\u0434\u043d\u044f" : "\u041e\u0448\u0438\u0431\u043a\u0430: " + err));
                }
            }
            catch (Exception e) {
                Profiles.finishText(done, "\u0421\u0435\u0440\u0432\u0435\u0440 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d");
            }
        });
    }

    private static void finish(Consumer<Boolean> done, boolean value) {
        if (done != null) {
            Minecraft.getInstance().execute(() -> done.accept(value));
        }
    }

    private static void finishText(Consumer<String> done, String value) {
        if (done != null) {
            Minecraft.getInstance().execute(() -> done.accept(value));
        }
    }

    static {
        top = new Top(List.of(), List.of(), List.of());
        claimed = true;
    }

    public record Profile(String nick, String mc, boolean self, boolean friend, boolean pending, boolean online, String server, String status, String bio, String role, long created, long lastSeen, int points, int streak, int best, int friends, int msgs, int downloads, int[] cos, List<Ach> ach) {
        public int unlocked() {
            int n = 0;
            for (Ach a : this.ach) {
                if (!a.unlocked()) continue;
                ++n;
            }
            return n;
        }
    }

    public record Top(List<TopEntry> points, List<TopEntry> streak, List<TopEntry> msgs) {
    }

    public record Ach(String id, String title, String desc, long at) {
        public boolean unlocked() {
            return this.at > 0L;
        }
    }

    public record TopEntry(String nick, int value) {
    }
}

