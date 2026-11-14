/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonParser
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.events;

import brokke.cc.core.command.RCTCommand;
import brokke.cc.util.Log;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class EventsPanel {
    private static final String FT_API = "https://api.funtime.su/method/events-info";
    private static final String FT_TOKEN = "1477d51.2fa08175b7e6df549477e2d933cdf9cf";
    private static final String HW_API = "https://api.holyworld.me/v1/events";
    private static final int MAX_PER_REQ = 30;
    private static final long REFRESH_MS = 10000L;
    private static final float CARD_H = 28.0f;
    private static final float CARD_GAP = 3.0f;
    private static final int[] FT_SERVERS_121 = new int[]{100, 101, 102, 103, 104, 105, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209, 210, 300, 301, 302, 303, 400, 401, 500, 501, 600, 700, 800, 900, 904};
    private static final int[] FT_SERVERS_1165 = new int[]{1001, 1002, 1003, 1004, 1005, 2001, 2002, 2003, 2004, 2005, 3001, 3002, 3003, 4001, 4002, 5001, 5002, 6001, 6002, 6003, 6004};
    private static final Map<String, String> HW_SERVER_NAMES = EventsPanel.buildServerNames();
    private static final Map<String, String> NAME_SHORTENINGS = new HashMap<String, String>();
    private final List<EventCard> ftCards = new CopyOnWriteArrayList<EventCard>();
    private final List<EventCard> hwCards = new CopyOnWriteArrayList<EventCard>();
    private volatile long fetchTimestamp = 0L;
    private volatile boolean fetching = false;
    private volatile String ftStatus = "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430...";
    private volatile String hwStatus = "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430...";
    private long lastFetch = 0L;
    private float scrollOffset = 0.0f;
    private float scrollTarget = 0.0f;
    private float maxScroll = 0.0f;
    private final List<ClickBox> clickBoxes = new ArrayList<ClickBox>();
    private SortMode sortMode = SortMode.TIME;

    public List<EventCard> getFtCards() {
        return this.ftCards;
    }

    public List<EventCard> getHwCards() {
        return this.hwCards;
    }

    public String getFtStatus() {
        return this.ftStatus;
    }

    public String getHwStatus() {
        return this.hwStatus;
    }

    public boolean isFetching() {
        return this.fetching;
    }

    public long getFetchTimestamp() {
        return this.fetchTimestamp;
    }

    public SortMode getSortMode() {
        return this.sortMode;
    }

    public void cycleSortMode() {
        this.sortMode = SortMode.values()[(this.sortMode.ordinal() + 1) % SortMode.values().length];
        this.sortCards();
    }

    public void update(boolean active) {
        long now = System.currentTimeMillis();
        if (active && !this.fetching && now - this.lastFetch > 10000L) {
            this.triggerFetch();
        }
    }

    public void triggerFetch() {
        if (this.fetching) {
            return;
        }
        this.fetching = true;
        this.lastFetch = System.currentTimeMillis();
        Thread t = new Thread(this::fetchAll, "brokke-EventsFetch");
        t.setDaemon(true);
        t.start();
    }

    private void fetchAll() {
        ArrayList<EventCard> result;
        try {
            result = new ArrayList<EventCard>();
            this.fetchFtServers(FT_SERVERS_121, "1.21", result);
            this.fetchFtServers(FT_SERVERS_1165, "1.16.5", result);
            result.removeIf(c -> c.eventId == null);
            result.removeIf(c -> "OPENED".equals(c.phase) || "LOOTING".equals(c.phase));
            result.sort(Comparator.comparingInt(EventCard::serverNum).thenComparingInt(EventCard::secondsLeft));
            this.ftCards.clear();
            this.ftCards.addAll(result);
            this.ftStatus = this.ftCards.isEmpty() ? "\u041d\u0435\u0442 \u0430\u043a\u0442\u0438\u0432\u043d\u044b\u0445" : "";
        }
        catch (Exception e) {
            this.ftStatus = "FT \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d";
        }
        try {
            result = new ArrayList();
            this.fetchHwEvents(result);
            this.hwCards.clear();
            this.hwCards.addAll(result);
            this.hwStatus = this.hwCards.isEmpty() ? "\u041d\u0435\u0442 \u0430\u043a\u0442\u0438\u0432\u043d\u044b\u0445" : "";
        }
        catch (Exception e) {
            this.hwStatus = "HW \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d";
        }
        this.sortCards();
        this.fetchTimestamp = System.currentTimeMillis();
        this.fetching = false;
    }

    public void sortCards() {
        Comparator<EventCard> ftComp = switch (this.sortMode.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> Comparator.comparingInt(EventCard::secondsLeft).thenComparingInt(EventCard::serverNum);
            case 1 -> Comparator.comparingInt(EventCard::serverNum).thenComparingInt(EventCard::secondsLeft);
            case 2 -> Comparator.comparing(EventCard::displayName).thenComparingInt(EventCard::secondsLeft);
        };
        ArrayList<EventCard> ftSorted = new ArrayList<EventCard>(this.ftCards);
        ftSorted.sort(ftComp);
        this.ftCards.clear();
        this.ftCards.addAll(ftSorted);
        Comparator<EventCard> hwComp = switch (this.sortMode.ordinal()) {
            default -> throw new MatchException(null, null);
            case 0 -> Comparator.comparingInt(EventCard::secondsLeft).thenComparing(EventCard::displayName);
            case 1 -> Comparator.comparingInt(EventCard::serverNum).thenComparing(EventCard::displayName);
            case 2 -> Comparator.comparing(EventCard::displayName).thenComparingInt(EventCard::serverNum);
        };
        ArrayList<EventCard> hwSorted = new ArrayList<EventCard>(this.hwCards);
        hwSorted.sort(hwComp);
        this.hwCards.clear();
        this.hwCards.addAll(hwSorted);
    }

    private void fetchFtServers(int[] nums, String ver, List<EventCard> out) {
        for (int i = 0; i < nums.length; i += 30) {
            StringBuilder sb = new StringBuilder();
            for (int j = i; j < Math.min(i + 30, nums.length); ++j) {
                if (j > i) {
                    sb.append(',');
                }
                sb.append("anarchy").append(nums[j]);
            }
            try {
                this.fetchFtBatch(sb.toString(), ver, out);
                continue;
            }
            catch (Exception ignored) {
                Log.swallow(ignored);
            }
        }
    }

    private void fetchFtBatch(String servers, String ver, List<EventCard> out) throws Exception {
        JsonObject json;
        HttpURLConnection conn = (HttpURLConnection)URI.create("https://api.funtime.su/method/events-info?event-type=all&server-type=" + servers).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization-Token", FT_TOKEN);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        if (conn.getResponseCode() != 200) {
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(conn.getInputStream());){
            json = JsonParser.parseReader((Reader)reader).getAsJsonObject();
        }
        JsonArray resp = json.getAsJsonArray("response");
        if (resp == null) {
            return;
        }
        for (JsonElement el : resp) {
            int num;
            JsonObject sObj = el.getAsJsonObject();
            String sName = sObj.get("server").getAsString();
            try {
                num = Integer.parseInt(sName.replace("anarchy", ""));
            }
            catch (Exception e) {
                continue;
            }
            JsonArray evs = sObj.getAsJsonArray("events");
            if (evs == null) continue;
            for (JsonElement evEl : evs) {
                String evType;
                String loot;
                JsonObject ev = evEl.getAsJsonObject();
                int timeLeft = ev.has("time-seconds-left") ? ev.get("time-seconds-left").getAsInt() : 0;
                String id = ev.has("id") && !ev.get("id").isJsonNull() ? ev.get("id").getAsString() : null;
                String phase = ev.has("phase") && !ev.get("phase").isJsonNull() ? ev.get("phase").getAsString() : null;
                String string = loot = ev.has("loot") && !ev.get("loot").isJsonNull() ? ev.get("loot").getAsString() : null;
                if ("null".equals(loot)) {
                    loot = null;
                }
                String string2 = evType = ev.has("event-type") ? ev.get("event-type").getAsString() : "system";
                if (EventsPanel.isHiddenEvent(id) || id == null && timeLeft <= 0) continue;
                out.add(new EventCard(sName, num, ver, id != null ? id : "\u043e\u0436\u0438\u0434\u0430\u043d\u0438\u0435", phase, timeLeft, loot, evType));
            }
        }
    }

    private void fetchHwEvents(List<EventCard> out) {
        block11: {
            try {
                HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4L)).followRedirects(HttpClient.Redirect.NORMAL).build();
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(HW_API)).header("User-Agent", "Mozilla/5.0").header("Accept", "application/json").timeout(Duration.ofSeconds(5L)).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() != 200) break block11;
                JsonObject root = JsonParser.parseString((String)response.body()).getAsJsonObject();
                for (String serverId : root.keySet()) {
                    JsonElement el = root.get(serverId);
                    if (el == null || !el.isJsonArray()) continue;
                    JsonArray arr = el.getAsJsonArray();
                    String serverName = HW_SERVER_NAMES.getOrDefault(serverId, EventsPanel.fallbackServerName(serverId));
                    int num = 0;
                    try {
                        num = Integer.parseInt(serverId.replaceAll("[^0-9]", ""));
                    }
                    catch (NumberFormatException numberFormatException) {
                        // empty catch block
                    }
                    for (JsonElement item : arr) {
                        if (!item.isJsonObject()) continue;
                        JsonObject ev = item.getAsJsonObject();
                        String displayName = "\u0418\u0432\u0435\u043d\u0442";
                        String rare = "";
                        if (ev.has("metadata") && ev.get("metadata").isJsonObject()) {
                            JsonObject meta = ev.getAsJsonObject("metadata");
                            if (meta.has("displayName") && !meta.get("displayName").isJsonNull()) {
                                displayName = meta.get("displayName").getAsString();
                            }
                            if (meta.has("rare") && !meta.get("rare").isJsonNull()) {
                                rare = meta.get("rare").getAsString();
                            }
                        }
                        if (displayName.isBlank() && ev.has("id") && !ev.get("id").isJsonNull()) {
                            displayName = ev.get("id").getAsString();
                        }
                        out.add(new EventCard(serverName, num, "HW", EventsPanel.capitalizeFirst(EventsPanel.shortenName(displayName)), (String)(rare.isBlank() ? "HOLYWORLD_ACTIVE" : "HOLYWORLD_ACTIVE:" + rare), 0, null, "hw"));
                    }
                }
            }
            catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("HW \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d", interrupted);
            }
            catch (Exception error) {
                throw new IllegalStateException("HW \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d", error);
            }
        }
    }

    private static boolean isHiddenEvent(String id) {
        if (id == null) {
            return false;
        }
        String s = id.toLowerCase();
        return s.contains("airdrop") || s.contains("myst_beacon") || s.contains("myst");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void render(GuiGraphicsExtractor graphics, float contentX, float contentTop, float contentWidth, float contentHeight, float mouseX, float mouseY, Color accent) {
        this.update(true);
        this.clickBoxes.clear();
        float barY = contentTop;
        float barH = 15.0f;
        String sortText = "\u0421\u043e\u0440\u0442\u0438\u0440\u043e\u0432\u043a\u0430: " + this.sortMode.getLabel();
        float sortW = Text.of(Fonts.SEMIBOLD, sortText).size(6.0f).width() + 10.0f;
        boolean sortHover = mouseX >= contentX && mouseX <= contentX + sortW && mouseY >= barY && mouseY <= barY + barH;
        RoundedRect.of(contentX, barY, sortW, barH).radius(3.5f).color(sortHover ? Color.rgba(255, 255, 255, 14) : Color.rgba(255, 255, 255, 7)).render(graphics);
        Outline.of(contentX, barY, sortW, barH).radius(3.5f).thickness(0.6f).color(sortHover ? accent.withAlpha(120) : Color.rgba(255, 255, 255, 12)).render(graphics);
        Text.of(Fonts.SEMIBOLD, sortText, contentX + 5.0f, barY + barH * 0.5f, 6.0f).verticalAlign(VerticalAlign.MIDDLE).color(sortHover ? Color.WHITE : Color.rgb(205, 210, 222)).render(graphics);
        this.clickBoxes.add(new ClickBox(contentX, barY, sortW, barH, () -> {
            this.sortMode = SortMode.values()[(this.sortMode.ordinal() + 1) % SortMode.values().length];
            this.sortCards();
        }));
        String refreshText = this.fetching ? "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430..." : "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c";
        float refW = Text.of(Fonts.SEMIBOLD, refreshText).size(6.0f).width() + 10.0f;
        float refX = contentX + contentWidth - refW;
        boolean refHover = !this.fetching && mouseX >= refX && mouseX <= refX + refW && mouseY >= barY && mouseY <= barY + barH;
        RoundedRect.of(refX, barY, refW, barH).radius(3.5f).color(refHover ? accent.withAlpha(50) : Color.rgba(255, 255, 255, 7)).render(graphics);
        Outline.of(refX, barY, refW, barH).radius(3.5f).thickness(0.6f).color(refHover ? accent.withAlpha(140) : Color.rgba(255, 255, 255, 12)).render(graphics);
        Text.of(Fonts.SEMIBOLD, refreshText, refX + refW * 0.5f, barY + barH * 0.5f, 6.0f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(this.fetching ? Color.rgb(140, 145, 160) : (refHover ? Color.WHITE : Color.rgb(205, 210, 222))).render(graphics);
        if (!this.fetching) {
            this.clickBoxes.add(new ClickBox(refX, barY, refW, barH, this::triggerFetch));
        }
        float colGap = 7.0f;
        float colW = (contentWidth - colGap) / 2.0f;
        float col1X = contentX;
        float col2X = contentX + colW + colGap;
        float colHeaderY = barY + barH + 4.0f;
        float colHeaderH = 13.0f;
        Text.of(Fonts.SEMIBOLD, "FunTime", col1X + 2.0f, colHeaderY + colHeaderH * 0.5f, 7.2f).verticalAlign(VerticalAlign.MIDDLE).color(accent).render(graphics);
        String ftCountStr = String.valueOf(this.ftCards.size());
        float ftBadgeW = Text.of(Fonts.SEMIBOLD, ftCountStr).size(5.5f).width() + 6.0f;
        float ftBadgeX = col1X + colW - ftBadgeW - 2.0f;
        RoundedRect.of(ftBadgeX, colHeaderY + 2.0f, ftBadgeW, 9.5f).radius(2.5f).color(Color.rgba(255, 255, 255, 9)).render(graphics);
        Text.of(Fonts.SEMIBOLD, ftCountStr, ftBadgeX + ftBadgeW * 0.5f, colHeaderY + 6.75f, 5.5f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(150, 155, 170)).render(graphics);
        Text.of(Fonts.SEMIBOLD, "HolyWorld", col2X + 2.0f, colHeaderY + colHeaderH * 0.5f, 7.2f).verticalAlign(VerticalAlign.MIDDLE).color(accent).render(graphics);
        String hwCountStr = String.valueOf(this.hwCards.size());
        float hwBadgeW = Text.of(Fonts.SEMIBOLD, hwCountStr).size(5.5f).width() + 6.0f;
        float hwBadgeX = col2X + colW - hwBadgeW - 2.0f;
        RoundedRect.of(hwBadgeX, colHeaderY + 2.0f, hwBadgeW, 9.5f).radius(2.5f).color(Color.rgba(255, 255, 255, 9)).render(graphics);
        Text.of(Fonts.SEMIBOLD, hwCountStr, hwBadgeX + hwBadgeW * 0.5f, colHeaderY + 6.75f, 5.5f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(150, 155, 170)).render(graphics);
        RoundedRect.of(contentX + colW + colGap * 0.5f - 0.5f, colHeaderY + 2.0f, 1.0f, contentHeight - (colHeaderY - contentTop) - 4.0f).color(Color.rgba(255, 255, 255, 6)).render(graphics);
        float listY = colHeaderY + colHeaderH + 2.0f;
        float listVisibleH = contentHeight - (listY - contentTop);
        if (this.scrollTarget < 0.0f) {
            this.scrollTarget += (0.0f - this.scrollTarget) * 0.2f;
        } else if (this.scrollTarget > this.maxScroll && this.maxScroll > 0.0f) {
            this.scrollTarget += (this.maxScroll - this.scrollTarget) * 0.2f;
        }
        this.scrollOffset = Math.abs(this.scrollTarget - this.scrollOffset) > 0.05f ? (this.scrollOffset += (this.scrollTarget - this.scrollOffset) * 0.25f) : this.scrollTarget;
        long elapsed = this.fetchTimestamp == 0L ? 0L : (System.currentTimeMillis() - this.fetchTimestamp) / 1000L;
        Scissor.push(graphics, contentX, listY, contentWidth, listVisibleH);
        try {
            float ftY = listY - this.scrollOffset;
            if (this.ftCards.isEmpty()) {
                Text.of(Fonts.SEMIBOLD, this.ftStatus, col1X + colW * 0.5f, listY + 30.0f, 6.5f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 135)).render(graphics);
            } else {
                for (EventCard eventCard : this.ftCards) {
                    if (ftY + 28.0f >= listY && ftY <= listY + listVisibleH) {
                        this.renderFunTimeCard(graphics, eventCard, col1X, ftY, colW, mouseX, mouseY, elapsed, accent);
                    }
                    ftY += 31.0f;
                }
            }
            float hwY = listY - this.scrollOffset;
            if (this.hwCards.isEmpty()) {
                Text.of(Fonts.SEMIBOLD, this.hwStatus, col2X + colW * 0.5f, listY + 30.0f, 6.5f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 135)).render(graphics);
            } else {
                for (EventCard card : this.hwCards) {
                    if (hwY + 28.0f >= listY && hwY <= listY + listVisibleH) {
                        this.renderHolyWorldCard(graphics, card, col2X, hwY, colW, mouseX, mouseY, accent);
                    }
                    hwY += 31.0f;
                }
            }
            float f = this.ftCards.isEmpty() ? 0.0f : (float)this.ftCards.size() * 31.0f;
            float hwTotalH = this.hwCards.isEmpty() ? 0.0f : (float)this.hwCards.size() * 31.0f;
            float maxContentH = Math.max(f, hwTotalH);
            this.maxScroll = Math.max(0.0f, maxContentH - listVisibleH + 8.0f);
        }
        finally {
            Scissor.pop(graphics);
        }
    }

    private void renderFunTimeCard(GuiGraphicsExtractor graphics, EventCard card, float x, float y, float w, float mouseX, float mouseY, long elapsed, Color accent) {
        boolean cardHover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 28.0f;
        RoundedRect.of(x, y, w, 28.0f).radius(4.0f).color(cardHover ? Color.rgba(32, 35, 46, 175) : Color.rgba(24, 26, 34, 130)).render(graphics);
        Outline.of(x, y, w, 28.0f).radius(4.0f).thickness(0.6f).color(cardHover ? accent.withAlpha(60) : Color.rgba(255, 255, 255, 10)).render(graphics);
        float pad = 5.0f;
        Text.of(Fonts.SEMIBOLD, card.displayName(), x + pad, y + 7.5f, 6.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(245, 245, 250)).render(graphics);
        String btnText = "/an" + card.serverNum();
        float btnW = Text.of(Fonts.SEMIBOLD, btnText).size(5.5f).width() + 8.0f;
        float btnH = 11.0f;
        float btnX = x + w - btnW - pad;
        float btnY = y + 2.5f;
        boolean btnHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        RoundedRect.of(btnX, btnY, btnW, btnH).radius(2.5f).color(btnHover ? accent.brighter(0.1f) : accent.withAlpha(190)).render(graphics);
        Text.of(Fonts.SEMIBOLD, btnText, btnX + btnW * 0.5f, btnY + btnH * 0.5f, 5.5f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
        this.clickBoxes.add(new ClickBox(btnX, btnY, btnW, btnH, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.player.connection != null) {
                mc.player.connection.sendCommand("an" + card.serverNum());
            }
            if (mc.gui.screen() != null) {
                mc.setScreenAndShow(null);
            }
        }));
        String info = "\u0410\u043d\u0430\u0440\u0445\u0438\u044f " + card.serverNum() + " \u2022 " + card.version();
        Text.of(Fonts.SEMIBOLD, info, x + pad, y + 19.5f, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(135, 140, 155)).render(graphics);
        String status = card.statusStr(elapsed);
        Text.of(Fonts.SEMIBOLD, status, x + w - pad, y + 19.5f, 5.2f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(accent).render(graphics);
    }

    private void renderHolyWorldCard(GuiGraphicsExtractor graphics, EventCard card, float x, float y, float w, float mouseX, float mouseY, Color accent) {
        boolean cardHover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + 28.0f;
        RoundedRect.of(x, y, w, 28.0f).radius(4.0f).color(cardHover ? Color.rgba(32, 35, 46, 175) : Color.rgba(24, 26, 34, 130)).render(graphics);
        Outline.of(x, y, w, 28.0f).radius(4.0f).thickness(0.6f).color(cardHover ? accent.withAlpha(60) : Color.rgba(255, 255, 255, 10)).render(graphics);
        float pad = 5.0f;
        Text.of(Fonts.SEMIBOLD, card.displayName(), x + pad, y + 7.5f, 6.5f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(245, 245, 250)).render(graphics);
        String btnText = card.serverNum() > 0 ? ".rct " + card.serverNum() : ".rct";
        float btnW = Text.of(Fonts.SEMIBOLD, btnText).size(5.5f).width() + 8.0f;
        float btnH = 11.0f;
        float btnX = x + w - btnW - pad;
        float btnY = y + 2.5f;
        boolean btnHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        RoundedRect.of(btnX, btnY, btnW, btnH).radius(2.5f).color(btnHover ? accent.brighter(0.1f) : accent.withAlpha(190)).render(graphics);
        Text.of(Fonts.SEMIBOLD, btnText, btnX + btnW * 0.5f, btnY + btnH * 0.5f, 5.5f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(graphics);
        this.clickBoxes.add(new ClickBox(btnX, btnY, btnW, btnH, () -> {
            Minecraft mc = Minecraft.getInstance();
            if (card.serverNum() > 0) {
                RCTCommand.execute(card.serverNum());
            } else {
                RCTCommand.executeCurrentOrLast();
            }
            if (mc.gui.screen() != null) {
                mc.setScreenAndShow(null);
            }
        }));
        Text.of(Fonts.SEMIBOLD, card.server(), x + pad, y + 19.5f, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(135, 140, 155)).render(graphics);
        if (card.phase() != null && card.phase().startsWith("HOLYWORLD_ACTIVE:")) {
            String rare = EventsPanel.translateRarity(card.phase().substring("HOLYWORLD_ACTIVE:".length()));
            Color rareCol = EventsPanel.getRarityColor(rare);
            Text.of(Fonts.SEMIBOLD, rare, x + w - pad, y + 19.5f, 5.2f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(rareCol).render(graphics);
        } else {
            Text.of(Fonts.SEMIBOLD, "\u0410\u043a\u0442\u0438\u0432\u0435\u043d", x + w - pad, y + 19.5f, 5.2f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(87, 227, 137)).render(graphics);
        }
    }

    public boolean mouseClicked(float mx, float my, int button) {
        if (button == 0) {
            for (ClickBox b : this.clickBoxes) {
                if (!(mx >= b.x) || !(mx <= b.x + b.w) || !(my >= b.y) || !(my <= b.y + b.h)) continue;
                if (b.action != null) {
                    b.action.run();
                }
                return true;
            }
        }
        return false;
    }

    public boolean mouseScrolled(double verticalAmount) {
        if (this.maxScroll <= 0.0f) {
            this.scrollTarget = Math.clamp(this.scrollTarget - (float)verticalAmount * 8.0f, -8.0f, 8.0f);
            return true;
        }
        this.scrollTarget = Math.clamp(this.scrollTarget - (float)verticalAmount * 24.0f, -10.0f, this.maxScroll + 10.0f);
        return true;
    }

    public void resetScroll() {
        this.scrollOffset = 0.0f;
        this.scrollTarget = 0.0f;
    }

    private static String capitalizeFirst(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static String shortenName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        String trim = name.trim();
        String custom = NAME_SHORTENINGS.get(trim);
        if (custom != null) {
            return custom;
        }
        int space = trim.indexOf(32);
        if (space > 0) {
            return trim.substring(space + 1);
        }
        return trim;
    }

    public static String translateRarity(String rare) {
        if (rare == null || rare.isBlank()) {
            return "";
        }
        String lower = rare.toLowerCase();
        if (lower.contains("legendary") || lower.contains("\u043b\u0435\u0433\u0435\u043d\u0434\u0430\u0440")) {
            return "\u041b\u0435\u0433\u0435\u043d\u0434\u0430\u0440\u043d\u044b\u0439";
        }
        if (lower.contains("mythic")) {
            return "\u041c\u0438\u0444\u0438\u0447\u0435\u0441\u043a\u0438\u0439";
        }
        if (lower.contains("epic")) {
            return "\u042d\u043f\u0438\u0447\u0435\u0441\u043a\u0438\u0439";
        }
        if (lower.contains("explosive") || lower.contains("\u0432\u0437\u0440\u044b\u0432")) {
            return "\u0412\u0437\u0440\u044b\u0432\u043d\u043e\u0439";
        }
        if (lower.contains("deadly") || lower.contains("\u0441\u043c\u0435\u0440\u0442\u0435\u043b\u044c\u043d")) {
            return "\u0421\u043c\u0435\u0440\u0442\u0435\u043b\u044c\u043d\u044b\u0439";
        }
        if (lower.contains("peaceful") || lower.contains("\u043c\u0438\u0440\u043d")) {
            return "\u041c\u0438\u0440\u043d\u044b\u0439";
        }
        if (lower.contains("roskoshni") || lower.contains("\u0440\u043e\u0441\u043a\u043e\u0448\u043d")) {
            return "\u0420\u043e\u0441\u043a\u043e\u0448\u043d\u044b\u0439";
        }
        if (lower.contains("rare") || lower.contains("\u0440\u0435\u0434\u043a")) {
            return "\u0420\u0435\u0434\u043a\u0438\u0439";
        }
        if (lower.contains("normal") || lower.contains("common") || lower.contains("default") || lower.contains("\u043e\u0431\u044b\u0447\u043d")) {
            return "\u041e\u0431\u044b\u0447\u043d\u044b\u0439";
        }
        return rare;
    }

    public static Color getRarityColor(String rare) {
        if (rare == null || rare.isBlank()) {
            return Color.rgb(180, 185, 195);
        }
        String lower = rare.toLowerCase();
        if (lower.contains("legendary") || lower.contains("\u043b\u0435\u0433\u0435\u043d\u0434\u0430\u0440") || lower.contains("roskoshni") || lower.contains("\u0440\u043e\u0441\u043a\u043e\u0448\u043d")) {
            return Color.rgb(255, 215, 0);
        }
        if (lower.contains("mythic") || lower.contains("\u043c\u0438\u0444\u0438\u0447")) {
            return Color.rgb(198, 120, 221);
        }
        if (lower.contains("epic") || lower.contains("\u044d\u043f\u0438\u0447")) {
            return Color.rgb(178, 58, 238);
        }
        if (lower.contains("explosive") || lower.contains("\u0432\u0437\u0440\u044b\u0432")) {
            return Color.rgb(255, 140, 66);
        }
        if (lower.contains("deadly") || lower.contains("\u0441\u043c\u0435\u0440\u0442\u0435\u043b\u044c\u043d")) {
            return Color.rgb(255, 85, 85);
        }
        if (lower.contains("peaceful") || lower.contains("\u043c\u0438\u0440\u043d")) {
            return Color.rgb(87, 227, 137);
        }
        if (lower.contains("rare") || lower.contains("\u0440\u0435\u0434\u043a")) {
            return Color.rgb(64, 166, 255);
        }
        return Color.rgb(160, 165, 175);
    }

    private static String fallbackServerName(String serverId) {
        if (serverId == null || serverId.isBlank()) {
            return serverId == null ? "?" : serverId;
        }
        if (serverId.startsWith("LITE_NEW_ANARCHY_")) {
            return "\u041b\u0430\u0439\u0442 (1.20) #" + serverId.substring("LITE_NEW_ANARCHY_".length());
        }
        if (serverId.startsWith("LITE_ANARCHY_")) {
            String num = serverId.substring("LITE_ANARCHY_".length());
            try {
                int n = Integer.parseInt(num);
                String prefix = n <= 9 ? "\u0421\u043e\u043b\u043e\u041b\u0430\u0439\u0442" : (n <= 19 ? "\u0414\u0443\u043e\u041b\u0430\u0439\u0442" : (n <= 29 ? "\u0422\u0440\u0438\u043e\u041b\u0430\u0439\u0442" : "\u041a\u043b\u0430\u043d\u041b\u0430\u0439\u0442"));
                return prefix + " #" + n;
            }
            catch (NumberFormatException ignored) {
                return "\u0410\u043d\u0430\u0440\u0445\u0438\u044f #" + num;
            }
        }
        return serverId;
    }

    private static Map<String, String> buildServerNames() {
        int i;
        HashMap<String, String> map = new HashMap<String, String>();
        for (i = 1; i <= 9; ++i) {
            map.put("LITE_ANARCHY_" + i, "\u0421\u043e\u043b\u043e\u041b\u0430\u0439\u0442 #" + i);
        }
        for (i = 10; i <= 19; ++i) {
            map.put("LITE_ANARCHY_" + i, "\u0414\u0443\u043e\u041b\u0430\u0439\u0442 #" + i);
        }
        for (i = 20; i <= 29; ++i) {
            map.put("LITE_ANARCHY_" + i, "\u0422\u0440\u0438\u043e\u041b\u0430\u0439\u0442 #" + i);
        }
        for (i = 30; i <= 36; ++i) {
            map.put("LITE_ANARCHY_" + i, "\u041a\u043b\u0430\u043d\u041b\u0430\u0439\u0442 #" + i);
        }
        map.put("LITE_NEW_ANARCHY_1", "\u041b\u0430\u0439\u0442 (1.20) #1");
        map.put("LITE_NEW_ANARCHY_2", "\u041b\u0430\u0439\u0442 (1.20) #2");
        map.put("LITE_NEW_ANARCHY_3", "\u041b\u0430\u0439\u0442 (1.20) #3");
        return map;
    }

    static {
        NAME_SHORTENINGS.put("\u0426\u0435\u043d\u043d\u044b\u0439 \u0433\u0440\u0443\u0437", "\u0426\u043d \u0433\u0440\u0443\u0437");
    }

    public static enum SortMode {
        TIME("\u041f\u043e \u0432\u0440\u0435\u043c\u0435\u043d\u0438"),
        SERVER("\u041f\u043e \u0441\u0435\u0440\u0432\u0435\u0440\u0443"),
        NAME("\u041f\u043e \u043d\u0430\u0437\u0432\u0430\u043d\u0438\u044e");

        private final String label;

        private SortMode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return this.label;
        }
    }

    public record EventCard(String server, int serverNum, String version, String eventId, String phase, int secondsLeft, String loot, String eventType) {
        public String displayName() {
            return switch (this.eventId) {
                case "vulkan" -> "\u0412\u0443\u043b\u043a\u0430\u043d";
                case "beacon" -> "\u041c\u0430\u044f\u043a";
                case "hellm" -> "\u0410\u0434\u0441\u043a\u0438\u0439 \u043c\u0430\u044f\u043a";
                case "airdrop" -> "\u0410\u0438\u0440\u0434\u0440\u043e\u043f";
                case "myst_beacon" -> "\u041c\u0438\u0441\u0442. \u043c\u0430\u044f\u043a";
                case "meteor_rain" -> "\u041c\u0435\u0442\u0435\u043e\u0440\u0438\u0442";
                case "\u043e\u0436\u0438\u0434\u0430\u043d\u0438\u0435" -> "\u041e\u0436\u0438\u0434\u0430\u043d\u0438\u0435";
                case "supply_drop" -> "\u0421\u0431\u0440\u043e\u0441 \u043f\u0440\u0438\u043f\u0430\u0441\u043e\u0432";
                case "boss" -> "\u0411\u043e\u0441\u0441";
                case "treasure" -> "\u0421\u043e\u043a\u0440\u043e\u0432\u0438\u0449\u0435";
                case "raid" -> "\u0420\u0435\u0439\u0434";
                default -> this.eventId;
            };
        }

        public String statusStr(long elapsedSec) {
            if (this.phase != null && this.phase.startsWith("HOLYWORLD_ACTIVE")) {
                return "\u0410\u043a\u0442\u0438\u0432\u0435\u043d";
            }
            int remaining = "LOOTING".equals(this.phase) ? Math.max(0, 60 - (int)elapsedSec) : Math.max(0, this.secondsLeft - (int)elapsedSec);
            String time = EventCard.formatTime(remaining);
            if (this.phase == null) {
                return "\u0421\u043f\u0430\u0432\u043d: " + time;
            }
            return switch (this.phase) {
                case "ACTIVATING" -> "\u0427\u0435\u0440\u0435\u0437: " + time;
                case "RUNNING" -> "\u0410\u043a\u0442: " + time;
                case "OPENED" -> "\u041e\u0442\u043a\u0440: " + time;
                case "LOOTING" -> "\u0417\u0430\u043a\u0440: " + time;
                case "WAITING" -> "\u0421\u043f\u0430\u0432\u043d: " + time;
                default -> time;
            };
        }

        private static String formatTime(int sec) {
            int m = sec / 60;
            int s = sec % 60;
            return m > 0 ? m + "\u043c " + s + "\u0441" : s + "\u0441";
        }
    }

    private record ClickBox(float x, float y, float w, float h, Runnable action) {
    }
}

