/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.multiplayer.PlayerInfo
 */
package brokke.cc.ui.clickgui.tab;

import brokke.cc.ui.clickgui.CloudFriends;
import brokke.cc.ui.clickgui.FriendsManager;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;

public final class FriendsTab
extends GuiComponent {
    public int friendsTab = 0;
    public long friendsSwitchTime = 0L;
    private long friendErrorTime = 0L;
    public final StringBuilder friendInput = new StringBuilder();
    public final List<Long> friendCharTimes = new ArrayList<Long>();

    public FriendsTab(ClickGuiContext ctx) {
        super(ctx);
    }

    private List<String> serverPlayers() {
        ArrayList<String> names = new ArrayList<String>();
        if (this.ctx.minecraft() == null || this.ctx.minecraft().getConnection() == null) {
            return names;
        }
        String self = this.ctx.minecraft().getUser() != null ? this.ctx.minecraft().getUser().getName() : "";
        for (PlayerInfo info : this.ctx.minecraft().getConnection().getOnlinePlayers()) {
            String name;
            if (info == null || info.getProfile() == null || (name = info.getProfile().name()) == null || name.isBlank() || name.equalsIgnoreCase(self) || !name.matches("[a-zA-Z0-9_]{3,16}")) continue;
            names.add(name);
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    private boolean isOnline(String name, List<String> online) {
        for (String n : online) {
            if (!n.equalsIgnoreCase(name)) continue;
            return true;
        }
        return false;
    }

    public float renderFriends(GuiGraphicsExtractor graphics, float x, float y, float w, float prog, float top, float bottom, long now) {
        float startY = y;
        float appear = GuiDraw.easeOut((float)(now - this.ctx.switchTime) / 380.0f);
        float a = prog * appear;
        y += (1.0f - appear) * 8.0f;
        Color accent = GuiDraw.getAccent();
        List<String> online = this.serverPlayers();
        List<FriendsManager.Friend> friends = FriendsManager.getFriends();
        float tabH = 14.0f;
        float tabW = (w - 4.0f) * 0.5f;
        RoundedRect.of(x, y, w, tabH + 4.0f).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        float tabPos = this.anim("friends-tab", this.friendsTab == 1, 6.0f);
        RoundedRect.of(x + 2.0f + tabPos * tabW, y + 2.0f, tabW, tabH).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round(14.0f * a))).render(graphics);
        String[] tabs = new String[]{"\u041b\u043e\u043a\u0430\u043b\u044c\u043d\u044b\u0435", "\u041d\u0430 \u0441\u0435\u0440\u0432\u0435\u0440\u0435"};
        int[] counts = new int[]{friends.size(), CloudFriends.friends().size()};
        for (int i = 0; i < 2; ++i) {
            int idx = i;
            float tx = x + 2.0f + (float)i * tabW;
            boolean hover = GuiDraw.canHit(y, tabH, top, bottom) && this.hovered(tx, y + 2.0f, tabW, tabH);
            String key = "friends-tab-" + i;
            float hv = this.anim(key, this.hov(key, hover), 7.0f);
            float act = i == 1 ? tabPos : 1.0f - tabPos;
            String label = tabs[i];
            float lw = Text.of(Fonts.MEDIUM, label).size(5.4f).width();
            String count = String.valueOf(counts[i]);
            float cw = Text.of(Fonts.MEDIUM, count).size(4.6f).width();
            float total = lw + 4.0f + cw;
            float lx = tx + (tabW - total) * 0.5f;
            Color c = GuiDraw.mix(Color.rgb(135, 140, 150), Color.rgb(240, 242, 248), Math.max(act, hv * 0.5f)).withAlpha(a);
            Text.of(Fonts.MEDIUM, label, lx, y + 2.0f + tabH * 0.5f, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(c).render(graphics);
            Text.of(Fonts.MEDIUM, count, lx + lw + 4.0f, y + 2.0f + tabH * 0.5f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(105, 110, 120), accent, act).withAlpha(a)).render(graphics);
            if (!GuiDraw.canHit(y, tabH + 4.0f, top, bottom)) continue;
            this.hit(tx, y + 2.0f, tabW, tabH, () -> {
                if (this.friendsTab != idx) {
                    this.friendsTab = idx;
                    this.friendsSwitchTime = System.currentTimeMillis();
                    if (idx == 1) {
                        CloudFriends.refresh();
                    }
                    ClientSound.ui(ClientSound.Ui.CLICK);
                }
            }, null);
        }
        y += tabH + 4.0f + 6.0f;
        y = this.renderFriendInput(graphics, x, y, w, a, accent, top, bottom, now);
        if (this.friendsTab == 1) {
            y = this.ctx.cloudFriends.renderCloudFriends(graphics, x, y, w, a, accent, top, bottom, now);
            return y - startY;
        }
        float rowH = 18.0f;
        float rowGap = 3.0f;
        ArrayList<String> rows = new ArrayList<String>();
        for (FriendsManager.Friend f : friends) {
            rows.add(f.name());
        }
        if (rows.isEmpty()) {
            String empty = "\u0421\u043f\u0438\u0441\u043e\u043a \u0434\u0440\u0443\u0437\u0435\u0439 \u043f\u0443\u0441\u0442";
            Text.of(Fonts.MEDIUM, empty, x + w * 0.5f, y + 16.0f, 5.4f).centered().color(Color.rgb(115, 120, 132).withAlpha(a * GuiDraw.easeOut((float)(now - this.friendsSwitchTime) / 300.0f))).render(graphics);
            y += 32.0f;
        }
        for (int i = 0; i < rows.size(); ++i) {
            String name = (String)rows.get(i);
            float ra = a * GuiDraw.easeOut((float)(now - this.friendsSwitchTime - (long)i * 25L) / 320.0f);
            float ry = y + (1.0f - GuiDraw.easeOut((float)(now - this.friendsSwitchTime - (long)i * 25L) / 320.0f)) * 6.0f;
            this.renderFriendRow(graphics, name, x, ry, w, rowH, ra, accent, this.isOnline(name, online), top, bottom);
            y += rowH + rowGap;
        }
        return y - startY;
    }

    private float renderFriendInput(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        long errAt;
        float err;
        float h = 15.0f;
        float btnW = 52.0f;
        float fw = w - btnW - 4.0f;
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, fw, h);
        float hv = this.anim("friend-input", this.hov("friend-input", hover), 7.0f);
        float fc = this.anim("friend-input-focus", this.ctx.focus.friendFocused, 6.0f);
        RoundedRect.of(x, y, fw, h).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round((8.0f + 4.0f * hv + 6.0f * fc) * a))).render(graphics);
        if (fc > 0.01f) {
            Outline.of(x, y, fw, h).radius(4.0f).thickness(0.7f).color(accent.withAlpha(0.6f * fc * a)).render(graphics);
        }
        Text.of(Fonts.ICONS, "D", x + 8.0f, y + h * 0.5f, 6.0f).centered().color(GuiDraw.mix(Color.rgb(130, 135, 146), Color.rgb(225, 228, 236), fc).withAlpha(a)).render(graphics);
        String text = this.friendInput.toString();
        float tx = x + 16.0f;
        float cy = y + h * 0.5f;
        if (text.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u041d\u0438\u043a \u0438\u0433\u0440\u043e\u043a\u0430...", tx, cy, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(120, 125, 136).withAlpha(a)).render(graphics);
        } else {
            for (int i = 0; i < text.length(); ++i) {
                float cx = tx + Text.of(Fonts.MEDIUM, text.substring(0, i)).size(5.4f).width();
                long born = i < this.friendCharTimes.size() ? this.friendCharTimes.get(i) : 0L;
                float t = GuiDraw.easeOut((float)(now - born) / 220.0f);
                Text.of(Fonts.MEDIUM, String.valueOf(text.charAt(i)), cx, cy + (1.0f - t) * 4.0f, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(a * t)).render(graphics);
            }
        }
        if (this.ctx.focus.friendFocused) {
            float blink = 0.5f + 0.5f * (float)Math.cos((double)now / 170.0);
            float caretX = tx + (text.isEmpty() ? 0.0f : Text.of(Fonts.MEDIUM, text).size(5.4f).width() + 0.5f);
            RoundedRect.of(caretX, y + 4.0f, 0.6f, h - 8.0f).color(Color.rgb(235, 238, 245).withAlpha(a * blink)).render(graphics);
        }
        if ((err = 1.0f - GuiDraw.easeOut((float)(now - (errAt = this.friendsTab == 1 ? Math.max(this.friendErrorTime, CloudFriends.errorTime()) : this.friendErrorTime)) / 600.0f)) > 0.01f) {
            RoundedRect.of(x, y, fw, h).radius(4.0f).color(Color.rgba(255, 70, 70, Math.round(40.0f * err * a))).render(graphics);
        }
        float bx = x + fw + 4.0f;
        boolean bHover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(bx, y, btnW, h);
        float bh = this.anim("friend-add", this.hov("friend-add", bHover), 7.0f);
        RoundedRect.of(bx, y, btnW, h).radius(4.0f).horizontalGradient(accent.withAlpha((0.55f + 0.35f * bh) * a), GuiDraw.getAccent2().withAlpha((0.55f + 0.35f * bh) * a)).render(graphics);
        Text.of(Fonts.MEDIUM, this.friendsTab == 1 ? "\u041e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c" : "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c", bx + btnW * 0.5f, cy, 5.2f).centered().color(Color.rgb(255, 255, 255).withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, fw, h, () -> {
                this.ctx.focus.setFriendFocus(true);
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, () -> {
                this.friendInput.setLength(0);
                this.friendCharTimes.clear();
            });
            this.hit(bx, y, btnW, h, this::submitFriend, null);
        }
        return y + h + 6.0f;
    }

    public void submitFriend() {
        String name = this.friendInput.toString().trim();
        if (this.friendsTab == 1) {
            if (CloudFriends.isValidName(name)) {
                CloudFriends.request(name);
                this.friendInput.setLength(0);
                this.friendCharTimes.clear();
                ClientSound.checkbox(true);
            } else {
                this.friendErrorTime = System.currentTimeMillis();
                ClientSound.checkbox(false);
            }
            return;
        }
        if (FriendsManager.addFriend(name)) {
            this.friendInput.setLength(0);
            this.friendCharTimes.clear();
            this.friendsSwitchTime = System.currentTimeMillis() - 2000L;
            ClientSound.checkbox(true);
        } else {
            this.friendErrorTime = System.currentTimeMillis();
            ClientSound.checkbox(false);
        }
    }

    private void renderFriendRow(GuiGraphicsExtractor graphics, String name, float x, float y, float w, float h, float a, Color accent, boolean online, float top, float bottom) {
        boolean friend = FriendsManager.isFriend(name);
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        String key = "friend-row-" + this.friendsTab + "-" + name;
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        float fr = this.anim(String.valueOf(key) + "-friend", friend, 6.0f);
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hv) * a))).render(graphics);
        float av = 12.0f;
        float ax = x + 4.0f;
        float ay = y + (h - av) * 0.5f;
        RoundedRect.of(ax, ay, av, av).radius(av * 0.5f).horizontalGradient(accent.withAlpha(0.85f * a), GuiDraw.getAccent2().withAlpha(0.85f * a)).render(graphics);
        Text.of(Fonts.MEDIUM, name.substring(0, 1).toUpperCase(Locale.ROOT), ax + av * 0.5f, ay + av * 0.5f, 5.4f).centered().color(Color.rgb(255, 255, 255).withAlpha(a)).render(graphics);
        float dot = 3.5f;
        RoundedRect.of(ax + av - dot + 0.5f, ay + av - dot + 0.5f, dot, dot).radius(dot * 0.5f).color((online ? Color.rgb(70, 215, 120) : Color.rgb(95, 100, 110)).withAlpha(a)).render(graphics);
        float nx = ax + av + 5.0f;
        Text.of(Fonts.MEDIUM, name, nx, y + h * 0.5f - 2.5f, 5.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(232, 235, 242).withAlpha(a)).render(graphics);
        Text.of(Fonts.MEDIUM, online ? "\u0412 \u0441\u0435\u0442\u0438" : "\u041d\u0435 \u0432 \u0441\u0435\u0442\u0438", nx, y + h * 0.5f + 3.5f, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color((online ? Color.rgb(90, 200, 130) : Color.rgb(110, 115, 126)).withAlpha(a)).render(graphics);
        float bs = 12.0f;
        float bx = x + w - bs - 4.0f;
        float by = y + (h - bs) * 0.5f;
        boolean bHover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(bx, by, bs, bs);
        float bhv = this.anim(String.valueOf(key) + "-btn", bHover, 8.0f);
        boolean removeMode = this.friendsTab == 0 || friend;
        Color btnColor = removeMode ? Color.rgb(255, 85, 85) : accent;
        RoundedRect.of(bx, by, bs, bs).radius(3.5f).color(btnColor.withAlpha((0.1f + 0.22f * bhv) * a * Math.max(0.35f, hv))).render(graphics);
        Color glyph = GuiDraw.mix(Color.rgb(150, 155, 165), btnColor, Math.max(bhv, this.friendsTab == 1 ? fr : 0.0f)).withAlpha(a * Math.max(0.45f, hv));
        float c = bs * 0.5f;
        if (removeMode && this.friendsTab == 1) {
            Text.of(Fonts.ICONS, "Q", bx + c, by + c, 6.0f).centered().color(GuiDraw.mix(accent, Color.rgb(255, 85, 85), bhv).withAlpha(a)).render(graphics);
        } else if (removeMode) {
            GuiDraw.stroke(graphics, bx + c - 2.2f, by + c - 2.2f, bx + c + 2.2f, by + c + 2.2f, 0.9f, glyph);
            GuiDraw.stroke(graphics, bx + c - 2.2f, by + c + 2.2f, bx + c + 2.2f, by + c - 2.2f, 0.9f, glyph);
        } else {
            GuiDraw.stroke(graphics, bx + c - 2.6f, by + c, bx + c + 2.6f, by + c, 0.9f, glyph);
            GuiDraw.stroke(graphics, bx + c, by + c - 2.6f, bx + c, by + c + 2.6f, 0.9f, glyph);
        }
        if (GuiDraw.canHit(y, h, top, bottom)) {
            Runnable toggle = () -> {
                if (FriendsManager.isFriend(name)) {
                    FriendsManager.removeFriend(name);
                    ClientSound.checkbox(false);
                } else if (FriendsManager.addFriend(name)) {
                    ClientSound.checkbox(true);
                }
            };
            this.hit(bx - 2.0f, by - 2.0f, bs + 4.0f, bs + 4.0f, toggle, null);
            if (this.friendsTab == 1) {
                this.hit(x, y, w, h, toggle, null);
            }
        }
    }
}

