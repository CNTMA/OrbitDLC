/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  net.minecraft.client.Minecraft
 *  net.minecraft.network.chat.Component
 */
package brokke.cc.ui.clickgui.globals;

import brokke.cc.core.setting.ConfigManager;
import brokke.cc.ui.clickgui.CloudFriends;
import brokke.cc.ui.clickgui.globals.Emoji;
import brokke.cc.ui.hud.legacy.notification.NotificationManager;
import brokke.cc.ui.hud.legacy.notification.NotificationType;
import brokke.cc.util.Log;
import brokke.cc.util.sound.ClientSound;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class GlobalChat {
    public static final int[] REACTIONS = new int[]{128077, 10084, 128514, 128558, 128546, 128293, 128128, 129313};
    private static final int KEEP = 200;
    private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Brokke-GlobalChat");
        t.setDaemon(true);
        return t;
    });
    private static volatile List<Message> messages = List.of();
    private static volatile boolean connected = false;
    private static volatile boolean loaded = false;
    private static volatile int online = 0;
    private static volatile String error = "";
    private static volatile long errorTime = 0L;
    private static volatile long activeUntil = 0L;
    private static volatile int unread = 0;
    private static volatile boolean inGame = true;
    private static volatile boolean mentionSound = true;
    private static long rev = 0L;
    private static long lastPoll = 0L;
    private static final Object POLL_LOCK = new Object();
    private static final AtomicBoolean STARTED = new AtomicBoolean();

    private GlobalChat() {
    }

    public static void init() {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        JsonObject data = ConfigManager.loadJson("globals.json");
        if (data.has("ircInGame")) {
            inGame = data.get("ircInGame").getAsBoolean();
        }
        if (data.has("mentionSound")) {
            mentionSound = data.get("mentionSound").getAsBoolean();
        }
        EXEC.scheduleWithFixedDelay(GlobalChat::tick, 1L, 1L, TimeUnit.SECONDS);
    }

    private static void saveState() {
        JsonObject out = new JsonObject();
        out.addProperty("ircInGame", Boolean.valueOf(inGame));
        out.addProperty("mentionSound", Boolean.valueOf(mentionSound));
        ConfigManager.saveJson("globals.json", out);
    }

    public static boolean isInGame() {
        return inGame;
    }

    public static void toggleInGame() {
        inGame = !inGame;
        GlobalChat.saveState();
    }

    public static boolean isMentionSound() {
        return mentionSound;
    }

    public static void toggleMentionSound() {
        mentionSound = !mentionSound;
        GlobalChat.saveState();
    }

    public static void markActive() {
        GlobalChat.init();
        boolean wasIdle = System.currentTimeMillis() > activeUntil;
        activeUntil = System.currentTimeMillis() + 3000L;
        unread = 0;
        if (wasIdle) {
            EXEC.execute(GlobalChat::poll);
        }
    }

    private static boolean active() {
        return System.currentTimeMillis() < activeUntil;
    }

    private static void tick() {
        long interval;
        long now = System.currentTimeMillis();
        interval = GlobalChat.active() ? 1500L : inGame ? 4000L : 15000L;
        if (now - lastPoll >= interval) {
            GlobalChat.poll();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void poll() {
        Object object = POLL_LOCK;
        synchronized (object) {
            GlobalChat.pollLocked();
        }
    }

    private static void pollLocked() {
        lastPoll = System.currentTimeMillis();
        try {
            JsonObject body = new JsonObject();
            body.addProperty("rev", (Number)rev);
            JsonObject json = CloudFriends.api("/chat/poll", body);
            if (json == null || !json.has("ok") || !json.get("ok").getAsBoolean()) {
                return;
            }
            connected = true;
            online = json.has("online") ? json.get("online").getAsInt() : 0;
            ArrayList<Message> fresh = new ArrayList<Message>();
            for (JsonElement el : json.getAsJsonArray("messages")) {
                fresh.add(GlobalChat.parse(el.getAsJsonObject()));
            }
            if (!fresh.isEmpty()) {
                GlobalChat.merge(fresh, loaded);
            }
            if (json.has("rev")) {
                rev = json.get("rev").getAsLong();
            }
            loaded = true;
        }
        catch (Exception e) {
            connected = false;
        }
    }

    private static Message parse(JsonObject o) {
        Reply reply = null;
        if (o.has("reply") && o.get("reply").isJsonObject()) {
            JsonObject r = o.getAsJsonObject("reply");
            reply = new Reply(r.get("id").getAsLong(), r.get("nick").getAsString(), r.get("text").getAsString());
        }
        LinkedHashMap reactions = new LinkedHashMap();
        if (o.has("r") && o.get("r").isJsonObject()) {
            for (Map.Entry e : o.getAsJsonObject("r").entrySet()) {
                if (((String)e.getKey()).isEmpty() || !((JsonElement)e.getValue()).isJsonArray()) continue;
                ArrayList<String> nicks = new ArrayList<String>();
                for (JsonElement n : (JsonArray)e.getValue()) {
                    nicks.add(n.getAsString());
                }
                if (nicks.isEmpty()) continue;
                reactions.put(((String)e.getKey()).codePointAt(0), List.copyOf(nicks));
            }
        }
        Voice voice = null;
        if (o.has("voice") && o.get("voice").isJsonObject()) {
            JsonObject v = o.getAsJsonObject("voice");
            int[] wave = new int[]{};
            if (v.has("wave") && v.get("wave").isJsonArray()) {
                JsonArray w = v.getAsJsonArray("wave");
                wave = new int[w.size()];
                for (int i = 0; i < wave.length; ++i) {
                    wave[i] = w.get(i).getAsInt();
                }
            }
            voice = new Voice(v.get("id").getAsString(), v.get("dur").getAsFloat(), wave);
        }
        return new Message(o.get("id").getAsLong(), o.get("nick").getAsString(), o.has("mc") ? o.get("mc").getAsString() : "", o.get("text").getAsString(), o.get("at").getAsLong(), System.currentTimeMillis(), reply, Map.copyOf(reactions), voice);
    }

    private static void merge(List<Message> fresh, boolean notify) {
        ArrayList<Message> next = new ArrayList<Message>(messages);
        String self = CloudFriends.nick();
        for (Message m : fresh) {
            boolean mine;
            int pos;
            int idx = -1;
            for (int i = next.size() - 1; i >= 0; --i) {
                if (((Message)next.get(i)).id() != m.id()) continue;
                idx = i;
                break;
            }
            if (idx >= 0) {
                Message old = (Message)next.get(idx);
                next.set(idx, new Message(m.id(), m.nick(), m.mc(), m.text(), m.at(), old.received(), m.reply(), m.reactions(), m.voice()));
                continue;
            }
            for (pos = next.size(); pos > 0 && ((Message)next.get(pos - 1)).id() > m.id(); --pos) {
            }
            next.add(pos, m);
            boolean bl = mine = self != null && self.equalsIgnoreCase(m.nick());
            if (!notify || mine) continue;
            GlobalChat.onIncoming(m, self);
        }
        if (next.size() > 200) {
            next = new ArrayList(next.subList(next.size() - 200, next.size()));
        }
        messages = List.copyOf(next);
    }

    private static void onIncoming(Message m, String self) {
        boolean mention;
        boolean bl = mention = m.mentions(self) || m.reply() != null && self != null && m.reply().nick().equalsIgnoreCase(self);
        if (GlobalChat.active()) {
            return;
        }
        ++unread;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        mc.execute(() -> {
            try {
                if (inGame && mc.player != null) {
                    String prefix = mention ? "\u00a7d[IRC] \u00a7e" : "\u00a7d[IRC] \u00a7b";
                    String body = m.voice() != null ? "\u00a77[\u0433\u043e\u043b\u043e\u0441\u043e\u0432\u043e\u0435 " + Math.round(m.voice().dur()) + " \u0441\u0435\u043a]" : m.text();
                    mc.player.sendSystemMessage((Component)Component.literal((String)(prefix + m.nick() + "\u00a77: \u00a7f" + body)));
                }
                if (mention) {
                    if (NotificationManager.isEnabled()) {
                        NotificationManager.notify("IRC", m.nick() + " \u0443\u043f\u043e\u043c\u044f\u043d\u0443\u043b \u0442\u0435\u0431\u044f: " + m.text(), NotificationType.INFO, 5000L);
                    }
                    if (mentionSound) {
                        ClientSound.checkbox(true);
                    }
                }
            }
            catch (Throwable ignored) {
                Log.swallow(ignored);
            }
        });
    }

    public static void send(String text) {
        GlobalChat.send(text, 0L);
    }

    public static void send(String text, long replyTo) {
        String clean;
        String string = clean = text == null ? "" : text.replace("\u00a7", "").trim();
        if (clean.isEmpty()) {
            return;
        }
        GlobalChat.init();
        EXEC.execute(() -> {
            try {
                JsonObject json;
                JsonObject body = new JsonObject();
                body.addProperty("text", clean);
                if (replyTo > 0L) {
                    body.addProperty("reply", (Number)replyTo);
                }
                if ((json = CloudFriends.api("/chat/send", body)) == null) {
                    GlobalChat.fail("no_account");
                    return;
                }
                connected = true;
                if (json.has("ok") && json.get("ok").getAsBoolean()) {
                    GlobalChat.poll();
                } else {
                    GlobalChat.fail(json.has("error") ? json.get("error").getAsString() : "bad_request");
                }
            }
            catch (Exception e) {
                connected = false;
                GlobalChat.fail("offline");
            }
        });
    }

    public static void refreshNow() {
        EXEC.execute(GlobalChat::poll);
    }

    public static void react(long id, int emoji) {
        EXEC.execute(() -> {
            try {
                JsonObject body = new JsonObject();
                body.addProperty("id", (Number)id);
                body.addProperty("emoji", Emoji.string(emoji));
                JsonObject json = CloudFriends.api("/chat/react", body);
                if (json != null && json.has("ok") && json.get("ok").getAsBoolean()) {
                    GlobalChat.poll();
                } else if (json != null) {
                    GlobalChat.fail(json.has("error") ? json.get("error").getAsString() : "bad_request");
                }
            }
            catch (Exception e) {
                connected = false;
                GlobalChat.fail("offline");
            }
        });
    }

    public static Message byId(long id) {
        for (Message m : messages) {
            if (m.id() != id) continue;
            return m;
        }
        return null;
    }

    private static void fail(String code) {
        error = switch (code) {
            case "slow_down" -> "\u041d\u0435 \u0442\u0430\u043a \u0431\u044b\u0441\u0442\u0440\u043e";
            case "empty" -> "\u041f\u0443\u0441\u0442\u043e\u0435 \u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435";
            case "rate_limited" -> "\u0421\u043b\u0438\u0448\u043a\u043e\u043c \u043c\u043d\u043e\u0433\u043e \u0437\u0430\u043f\u0440\u043e\u0441\u043e\u0432";
            case "no_account" -> "\u041d\u0443\u0436\u043d\u043e \u0432\u043e\u0439\u0442\u0438 \u0432 \u0430\u043a\u043a\u0430\u0443\u043d\u0442 Brokke";
            case "offline" -> "\u0421\u0435\u0440\u0432\u0435\u0440 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d";
            default -> "\u041e\u0448\u0438\u0431\u043a\u0430: " + code;
        };
        errorTime = System.currentTimeMillis();
    }

    public static List<Message> messages() {
        return messages;
    }

    public static boolean isConnected() {
        return connected;
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static int online() {
        return online;
    }

    public static int unread() {
        return unread;
    }

    public static String error() {
        return error;
    }

    public static long errorTime() {
        return errorTime;
    }

    public record Message(long id, String nick, String mc, String text, long at, long received, Reply reply, Map<Integer, List<String>> reactions, Voice voice) {
        public boolean mentions(String nick) {
            return nick != null && this.text.toLowerCase(Locale.ROOT).contains("@" + nick.toLowerCase(Locale.ROOT));
        }

        public boolean reacted(int emoji, String nick) {
            List<String> list = this.reactions.get(emoji);
            if (list == null || nick == null) {
                return false;
            }
            for (String n : list) {
                if (!n.equalsIgnoreCase(nick)) continue;
                return true;
            }
            return false;
        }
    }

    public record Reply(long id, String nick, String text) {
    }

    public record Voice(String id, float dur, int[] wave) {
    }
}

