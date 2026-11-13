/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 */
package brokke.cc.ui.clickgui;

import brokke.cc.core.setting.BooleanSetting;
import brokke.cc.core.setting.ConfigManager;
import brokke.cc.core.setting.StringSetting;
import brokke.cc.ui.clickgui.CloudFriends;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FriendsManager {
    private static final List<Friend> FRIENDS = new ArrayList<Friend>();
    private static final StringSetting FRIEND_INPUT = new StringSetting("FriendNick", "\u041d\u0438\u043a \u0438\u0433\u0440\u043e\u043a\u0430 \u0434\u043b\u044f \u0434\u043e\u0431\u0430\u0432\u043b\u0435\u043d\u0438\u044f \u0432 \u0434\u0440\u0443\u0437\u044c\u044f", "", 16);
    private static final BooleanSetting NO_FRIEND_DAMAGE = new BooleanSetting("NoFriendDamage", "\u041d\u0435 \u0430\u0442\u0430\u043a\u043e\u0432\u0430\u0442\u044c \u0434\u0440\u0443\u0437\u0435\u0439 \u0432 KillAura", true);
    private static final BooleanSetting FRIEND_GPS = new BooleanSetting("FriendGPS", "\u0417\u0435\u043b\u0451\u043d\u044b\u0439 \u0446\u0432\u0435\u0442 \u043d\u0438\u043a\u043d\u0435\u0439\u043c\u0430 \u0434\u0440\u0443\u0437\u0435\u0439", true);
    private static float noFriendDamageAnim = 1.0f;
    private static float friendGpsAnim = 1.0f;

    private FriendsManager() {
    }

    public static StringSetting getFriendInputSetting() {
        return FRIEND_INPUT;
    }

    public static void load() {
        JsonObject data = ConfigManager.loadJson("friends.json");
        if (data.has("friends")) {
            JsonArray friendsArray = data.getAsJsonArray("friends");
            FRIENDS.clear();
            for (JsonElement elem : friendsArray) {
                String name;
                if (!elem.isJsonPrimitive() || (name = elem.getAsString()).isEmpty()) continue;
                FRIENDS.add(new Friend(name, false));
            }
        } else {
            if (FRIENDS.isEmpty()) {
                FRIENDS.add(new Friend("profrulla7", true));
            }
            FriendsManager.save();
        }
        if (data.has("noFriendDamage")) {
            NO_FRIEND_DAMAGE.setValue(data.get("noFriendDamage").getAsBoolean());
        }
        if (data.has("friendGps")) {
            FRIEND_GPS.setValue(data.get("friendGps").getAsBoolean());
        }
        noFriendDamageAnim = (Boolean)NO_FRIEND_DAMAGE.value() != false ? 1.0f : 0.0f;
        friendGpsAnim = (Boolean)FRIEND_GPS.value() != false ? 1.0f : 0.0f;
    }

    public static void save() {
        JsonObject data = new JsonObject();
        JsonArray friendsArray = new JsonArray();
        for (Friend f : FRIENDS) {
            friendsArray.add(f.name());
        }
        data.add("friends", (JsonElement)friendsArray);
        data.addProperty("noFriendDamage", (Boolean)NO_FRIEND_DAMAGE.value());
        data.addProperty("friendGps", (Boolean)FRIEND_GPS.value());
        ConfigManager.saveJson("friends.json", data);
    }

    public static List<Friend> getFriends() {
        return Collections.unmodifiableList(FRIENDS);
    }

    public static boolean isFriend(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return FriendsManager.isLocalFriend(name) || CloudFriends.isFriend(name.trim());
    }

    public static boolean isLocalFriend(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        String clean = name.trim();
        for (Friend f : FRIENDS) {
            if (!f.name().equalsIgnoreCase(clean)) continue;
            return true;
        }
        return false;
    }

    public static boolean addFriend(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        String clean = name.trim();
        if (clean.length() < 3 || clean.length() > 16) {
            return false;
        }
        if (!clean.matches("[a-zA-Z0-9_]+")) {
            return false;
        }
        if (FriendsManager.isLocalFriend(clean)) {
            return false;
        }
        FRIENDS.add(new Friend(clean, false));
        FriendsManager.save();
        return true;
    }

    public static boolean removeFriend(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        String clean = name.trim();
        boolean removed = FRIENDS.removeIf(f -> f.name().equalsIgnoreCase(clean));
        if (removed) {
            FriendsManager.save();
        }
        return removed;
    }

    public static int count() {
        return FRIENDS.size();
    }

    public static int countOnline() {
        int count = 0;
        for (Friend f : FRIENDS) {
            if (!f.online()) continue;
            ++count;
        }
        return count;
    }

    public static boolean isNoFriendDamage() {
        return (Boolean)NO_FRIEND_DAMAGE.value();
    }

    public static void toggleNoFriendDamage() {
        NO_FRIEND_DAMAGE.toggle();
        FriendsManager.save();
    }

    public static boolean isFriendGps() {
        return (Boolean)FRIEND_GPS.value();
    }

    public static void toggleFriendGps() {
        FRIEND_GPS.toggle();
        FriendsManager.save();
    }

    public static float noFriendDamageAnim() {
        return noFriendDamageAnim;
    }

    public static float friendGpsAnim() {
        return friendGpsAnim;
    }

    public static void updateAnimations(float delta) {
        float speed = 12.0f * delta;
        float targetDamage = (Boolean)NO_FRIEND_DAMAGE.value() != false ? 1.0f : 0.0f;
        noFriendDamageAnim += (targetDamage - noFriendDamageAnim) * Math.min(1.0f, speed);
        float targetGps = (Boolean)FRIEND_GPS.value() != false ? 1.0f : 0.0f;
        friendGpsAnim += (targetGps - friendGpsAnim) * Math.min(1.0f, speed);
    }

    public record Friend(String name, boolean online) {
    }
}

