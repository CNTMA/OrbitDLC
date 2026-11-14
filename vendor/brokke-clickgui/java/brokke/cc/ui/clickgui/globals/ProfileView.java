/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.ui.clickgui.globals;

import brokke.cc.core.cosmetics.system.LocalCosmetics;
import brokke.cc.ui.clickgui.CloudFriends;
import brokke.cc.ui.clickgui.globals.Profiles;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.color.Color;
import brokke.cc.util.discord.ProfileAvatar;
import brokke.cc.util.render.display.image.Image;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class ProfileView
extends GuiComponent {
    private static final SimpleDateFormat DATE = new SimpleDateFormat("dd.MM.yyyy");
    private long openTime = 0L;
    private String toast = null;
    private long toastTime = 0L;

    public ProfileView(ClickGuiContext ctx) {
        super(ctx);
    }

    public void open(String nick) {
        Profiles.view(nick);
        this.openTime = System.currentTimeMillis();
        ClientSound.ui(ClientSound.Ui.CLICK);
    }

    public void openOwn() {
        Profiles.refreshOwn();
    }

    public boolean isOpen() {
        return Profiles.viewedNick() != null;
    }

    public void close() {
        Profiles.close();
    }

    public void commitField(String id, String value) {
        Profiles.Profile own = Profiles.own();
        if (id.equals("profile:status")) {
            if (own != null && own.status().equals(value)) {
                return;
            }
            Profiles.setProfile(value, null, ok -> this.showToast(ok != false ? "\u0421\u0442\u0430\u0442\u0443\u0441 \u043e\u0431\u043d\u043e\u0432\u043b\u0451\u043d" : "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c"));
        } else if (id.equals("profile:bio")) {
            if (own != null && own.bio().equals(value)) {
                return;
            }
            Profiles.setProfile(null, value, ok -> this.showToast(ok != false ? "\u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435 \u043e\u0431\u043d\u043e\u0432\u043b\u0435\u043d\u043e" : "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c"));
        }
    }

    private void showToast(String text) {
        this.toast = text;
        this.toastTime = System.currentTimeMillis();
    }

    static String clean(String s) {
        return s.replaceAll("[\\x{1F000}-\\x{1FFFF}\\x{2600}-\\x{27BF}\\x{FE0F}]", "").trim();
    }

    private static int nextReward(int streak) {
        int s = streak + 1;
        return 10 + Math.min(s, 7) * 5 + (s % 7 == 0 ? 50 : 0);
    }

    public float renderOwn(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        float startY = y;
        Profiles.Profile p = Profiles.own();
        if (p == null) {
            String s = CloudFriends.nick() == null ? "\u0412\u043e\u0439\u0434\u0438 \u0432 \u0430\u043a\u043a\u0430\u0443\u043d\u0442 Brokke, \u0447\u0442\u043e\u0431\u044b \u043e\u0442\u043a\u0440\u044b\u0442\u044c \u043f\u0440\u043e\u0444\u0438\u043b\u044c" : "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430 \u043f\u0440\u043e\u0444\u0438\u043b\u044f...";
            Text.of(Fonts.MEDIUM, s, x + w * 0.5f, y + 30.0f, 5.4f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
            if (now - this.toastTime > 3000L) {
                this.toastTime = now;
                Profiles.refreshOwn();
            }
            return 60.0f;
        }
        Color white = Color.rgb(235, 238, 245);
        Color grey = Color.rgb(115, 120, 132);
        float headH = 32.0f;
        RoundedRect.of(x, y, w, headH).radius(6.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        RoundedRect.of(x, y, w, headH).radius(6.0f).horizontalGradient(accent.withAlpha(0.07f * a), Color.rgba(0, 0, 0, 0)).render(graphics);
        float av = 20.0f;
        float avx = x + 7.0f;
        float avy = y + (headH - av) * 0.5f;
        Outline.of(avx - 1.5f, avy - 1.5f, av + 3.0f, av + 3.0f).radius((av + 3.0f) * 0.5f).thickness(0.7f).color(accent.withAlpha(0.8f * a)).render(graphics);
        this.avatar(graphics, p.nick(), avx, avy, av, a, accent);
        Text.of(Fonts.MEDIUM, p.nick(), avx + av + 7.0f, y + headH * 0.5f - 3.0f, 6.6f).verticalAlign(VerticalAlign.MIDDLE).color(white.withAlpha(a)).render(graphics);
        String sub = (String)(p.mc().isEmpty() ? "" : p.mc() + "  |  ") + "\u0441 " + DATE.format(new Date(p.created()));
        Text.of(Fonts.MEDIUM, sub, avx + av + 7.0f, y + headH * 0.5f + 5.0f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(grey.withAlpha(a)).render(graphics);
        String pts = String.valueOf(p.points());
        float nw = Text.of(Fonts.MEDIUM, pts).size(5.6f).width();
        float lw = Text.of(Fonts.MEDIUM, "\u043e\u0447\u043a\u043e\u0432").size(4.4f).width();
        float pillW = nw + lw + 14.0f;
        float pillX = x + w - pillW - 7.0f;
        float pillY = y + (headH - 13.0f) * 0.5f;
        RoundedRect.of(pillX, pillY, pillW, 13.0f).radius(6.5f).color(accent.withAlpha(0.14f * a)).render(graphics);
        Text.of(Fonts.MEDIUM, pts, pillX + 6.0f, pillY + 6.5f, 5.6f).verticalAlign(VerticalAlign.MIDDLE).color(white.withAlpha(a)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u043e\u0447\u043a\u043e\u0432", pillX + 8.0f + nw, pillY + 6.7f, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(accent.brighter(0.25f).withAlpha(a)).render(graphics);
        y += headH + 5.0f;
        y = this.field(graphics, "profile:status", "\u0421\u0442\u0430\u0442\u0443\u0441", p.status(), x, y, w, a, accent, top, bottom, now);
        y = this.field(graphics, "profile:bio", "\u041e \u0441\u0435\u0431\u0435", p.bio(), x, y, w, a, accent, top, bottom, now);
        float tt = 1.0f - GuiDraw.easeOut(Math.max(0.0f, (float)(now - this.toastTime - 2000L) / 500.0f));
        if (this.toast != null && tt > 0.01f) {
            Text.of(Fonts.MEDIUM, this.toast, x + 2.0f, y + 3.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(90, 210, 140).withAlpha(a * tt)).render(graphics);
        }
        y += 8.0f;
        y = this.renderDaily(graphics, x, y, w, a, accent, top, bottom, now);
        String[][] stats = new String[][]{{String.valueOf(p.streak()), "\u0441\u0442\u0440\u0438\u043a"}, {String.valueOf(p.best()), "\u0440\u0435\u043a\u043e\u0440\u0434"}, {String.valueOf(p.msgs()), "\u0441\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0439"}, {String.valueOf(p.friends()), "\u0434\u0440\u0443\u0437\u0435\u0439"}, {String.valueOf(p.downloads()), "\u0441\u043a\u0430\u0447\u0438\u0432\u0430\u043d\u0438\u0439"}, {p.unlocked() + "/" + p.ach().size(), "\u0434\u043e\u0441\u0442\u0438\u0436\u0435\u043d\u0438\u0439"}};
        float statH = 26.0f;
        RoundedRect.of(x, y, w, statH).radius(6.0f).color(Color.rgba(255, 255, 255, Math.round(5.0f * a))).render(graphics);
        float colW = w / (float)stats.length;
        for (int i = 0; i < stats.length; ++i) {
            float sx = x + (float)i * colW;
            if (i > 0) {
                RoundedRect.of(sx, y + 7.0f, 0.5f, statH - 14.0f).color(Color.rgba(255, 255, 255, Math.round(12.0f * a))).render(graphics);
            }
            Text.of(Fonts.MEDIUM, stats[i][0], sx + colW * 0.5f, y + 10.0f, 6.6f).centered().color(white.withAlpha(a)).render(graphics);
            Text.of(Fonts.MEDIUM, stats[i][1], sx + colW * 0.5f, y + 18.5f, 4.2f).centered().color(grey.withAlpha(a)).render(graphics);
        }
        y += statH + 9.0f;
        y = GuiDraw.sectionTitle(graphics, "\u0414\u043e\u0441\u0442\u0438\u0436\u0435\u043d\u0438\u044f  " + p.unlocked() + "/" + p.ach().size(), x, y, a);
        ArrayList<Profiles.Ach> achs = new ArrayList<Profiles.Ach>(p.ach());
        achs.sort((l, r) -> Boolean.compare(r.unlocked(), l.unlocked()));
        float aw = (w - 4.0f) * 0.5f;
        float ah = 18.0f;
        for (int i = 0; i < achs.size(); ++i) {
            Profiles.Ach ach = (Profiles.Ach)achs.get(i);
            float ax = x + (float)(i % 2) * (aw + 4.0f);
            float ay = y + (float)(i / 2) * (ah + 3.0f);
            float ua = ach.unlocked() ? 1.0f : 0.55f;
            String key = "ach-" + ach.id();
            float hv = this.anim(key, this.hov(key, GuiDraw.canHit(ay, ah, top, bottom) && this.hovered(ax, ay, aw, ah)), 7.0f);
            RoundedRect.of(ax, ay, aw, ah).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round((4.0f + 4.0f * hv) * a))).render(graphics);
            float dx = ax + 7.0f;
            float dy = ay + ah * 0.5f;
            if (ach.unlocked()) {
                RoundedRect.of(dx - 3.0f, dy - 3.0f, 6.0f, 6.0f).radius(3.0f).color(accent.withAlpha(0.25f * a)).render(graphics);
                RoundedRect.of(dx - 1.5f, dy - 1.5f, 3.0f, 3.0f).radius(1.5f).color(accent.withAlpha(a)).render(graphics);
            } else {
                RoundedRect.of(dx - 1.5f, dy - 1.5f, 3.0f, 3.0f).radius(1.5f).color(Color.rgba(255, 255, 255, Math.round(30.0f * a))).render(graphics);
            }
            Text.of(Fonts.MEDIUM, ach.title(), ax + 15.0f, ay + ah * 0.5f - 2.5f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(white.withAlpha(a * ua)).render(graphics);
            Text.of(Fonts.MEDIUM, GuiDraw.fit(ach.desc(), 4.2f, aw - 20.0f), ax + 15.0f, ay + ah * 0.5f + 3.5f, 4.2f).verticalAlign(VerticalAlign.MIDDLE).color(grey.withAlpha(a * ua)).render(graphics);
        }
        y += (float)((achs.size() + 1) / 2) * (ah + 3.0f) + 8.0f;
        y = GuiDraw.sectionTitle(graphics, "\u0422\u043e\u043f", x, y, a);
        Profiles.Top top10 = Profiles.top();
        String[] titles = new String[]{"\u041e\u0447\u043a\u0438", "\u0421\u0442\u0440\u0438\u043a", "\u0421\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u044f"};
        List<List<Profiles.TopEntry>> lists = List.of(top10.points(), top10.streak(), top10.msgs());
        float cw = (w - 16.0f) / 3.0f;
        float maxH = 0.0f;
        for (int c = 0; c < 3; ++c) {
            float cx = x + (float)c * (cw + 8.0f);
            List<Profiles.TopEntry> list = lists.get(c);
            Text.of(Fonts.MEDIUM, titles[c], cx + 2.0f, y + 3.0f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(grey.withAlpha(a)).render(graphics);
            float ch = 10.0f + (float)Math.max(1, list.size()) * 9.0f;
            maxH = Math.max(maxH, ch);
            if (list.isEmpty()) {
                Text.of(Fonts.MEDIUM, "-", cx + 2.0f, y + 13.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(grey.withAlpha(a)).render(graphics);
            }
            for (int i = 0; i < list.size(); ++i) {
                Profiles.TopEntry e = list.get(i);
                float ry = y + 9.0f + (float)i * 9.0f;
                boolean me = e.nick().equalsIgnoreCase(p.nick());
                String nick = e.nick();
                boolean hv = GuiDraw.canHit(ry, 9.0f, top, bottom) && this.hovered(cx, ry, cw, 9.0f);
                Text.of(Fonts.MEDIUM, "" + (i + 1), cx + 2.0f, ry + 4.5f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(grey.withAlpha(a)).render(graphics);
                Text.of(Fonts.MEDIUM, GuiDraw.fit(nick, 4.8f, cw - 30.0f), cx + 10.0f, ry + 4.5f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color((me ? accent : (hv ? Color.rgb(255, 255, 255) : Color.rgb(200, 204, 214))).withAlpha(a)).render(graphics);
                Text.of(Fonts.MEDIUM, String.valueOf(e.value()), cx + cw - 2.0f, ry + 4.5f, 4.8f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(grey.withAlpha(a)).render(graphics);
                if (!GuiDraw.canHit(ry, 9.0f, top, bottom)) continue;
                this.hit(cx, ry, cw, 9.0f, () -> {
                    this.ctx.globals.tab = 0;
                    this.open(nick);
                }, null);
            }
        }
        return (y += maxH + 6.0f) - startY;
    }

    private float renderDaily(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        float h = 20.0f;
        boolean can = Profiles.canClaim();
        int streak = Profiles.streak();
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        if (can) {
            RoundedRect.of(x + 1.5f, y + 5.0f, 1.2f, h - 10.0f).radius(0.6f).verticalGradient(accent.withAlpha(a), GuiDraw.getAccent2().withAlpha(a)).render(graphics);
        }
        Text.of(Fonts.MEDIUM, "\u0415\u0436\u0435\u0434\u043d\u0435\u0432\u043d\u0430\u044f \u043d\u0430\u0433\u0440\u0430\u0434\u0430", x + 7.0f, y + h * 0.5f - 2.5f, 5.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(232, 235, 242).withAlpha(a)).render(graphics);
        String sub = "\u0441\u0442\u0440\u0438\u043a " + streak + "  |  " + (can ? "\u0441\u0435\u0433\u043e\u0434\u043d\u044f" : "\u0437\u0430\u0432\u0442\u0440\u0430") + " +" + ProfileView.nextReward(streak);
        Text.of(Fonts.MEDIUM, sub, x + 7.0f, y + h * 0.5f + 3.5f, 4.3f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(110, 115, 126).withAlpha(a)).render(graphics);
        int week = streak == 0 ? 0 : (can ? streak % 7 : (streak - 1) % 7 + 1);
        float bw = 42.0f;
        float bh = 12.0f;
        float bx = x + w - bw - 4.0f;
        float by = y + (h - bh) * 0.5f;
        float dotsX = bx - 35.0f - 6.0f;
        for (int i = 0; i < 7; ++i) {
            RoundedRect.of(dotsX + (float)i * 5.0f, y + h * 0.5f - 1.5f, 3.0f, 3.0f).radius(1.5f).color(i < week ? accent.withAlpha(a) : Color.rgba(255, 255, 255, Math.round(22.0f * a))).render(graphics);
        }
        boolean hover = can && GuiDraw.canHit(by, bh, top, bottom) && this.hovered(bx, by, bw, bh);
        float hv = this.anim("daily-btn", this.hov("daily-btn", hover), 8.0f);
        if (can) {
            RoundedRect.of(bx, by, bw, bh).radius(3.5f).horizontalGradient(accent.withAlpha((0.55f + 0.35f * hv) * a), GuiDraw.getAccent2().withAlpha((0.55f + 0.35f * hv) * a)).render(graphics);
        }
        Text.of(Fonts.MEDIUM, can ? "\u0417\u0430\u0431\u0440\u0430\u0442\u044c" : "\u041f\u043e\u043b\u0443\u0447\u0435\u043d\u043e", bx + bw * 0.5f, by + bh * 0.5f, 4.8f).centered().color((can ? Color.rgb(255, 255, 255) : Color.rgb(110, 115, 126)).withAlpha(a)).render(graphics);
        if (can && GuiDraw.canHit(by, bh, top, bottom)) {
            this.hit(bx, by, bw, bh, () -> Profiles.claim(msg -> {
                this.showToast((String)msg);
                ClientSound.checkbox(true);
            }), null);
        }
        return y + h + 8.0f;
    }

    private float field(GuiGraphicsExtractor graphics, String id, String placeholder, String value, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        float h = 14.0f;
        boolean focused = id.equals(this.ctx.focus.abField);
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        float hv = this.anim(id, this.hov(id, hover), 7.0f);
        float fc = this.anim(id + "-focus", focused, 6.0f);
        RoundedRect.of(x, y, w, h).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round((7.0f + 4.0f * hv + 6.0f * fc) * a))).render(graphics);
        if (fc > 0.01f) {
            Outline.of(x, y, w, h).radius(4.0f).thickness(0.7f).color(accent.withAlpha(0.6f * fc * a)).render(graphics);
        }
        String text = focused ? this.ctx.focus.abInput.toString() : value;
        float cy = y + h * 0.5f;
        if (text.isEmpty()) {
            Text.of(Fonts.MEDIUM, placeholder, x + 5.0f, cy, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
        } else {
            Text.of(Fonts.MEDIUM, GuiDraw.fit(ProfileView.clean(text), 5.0f, w - 12.0f), x + 5.0f, cy, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(232, 235, 242).withAlpha(a)).render(graphics);
        }
        if (focused) {
            float blink = 0.5f + 0.5f * (float)Math.cos((double)now / 170.0);
            float cx = x + 5.0f + Math.min(w - 12.0f, text.isEmpty() ? 0.0f : Text.of(Fonts.MEDIUM, ProfileView.clean(text)).size(5.0f).width() + 0.5f);
            RoundedRect.of(cx, y + 3.5f, 0.6f, h - 7.0f).color(Color.rgb(235, 238, 245).withAlpha(a * blink)).render(graphics);
        }
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, () -> {
                this.ctx.focus.setAbField(id, value);
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
        }
        return y + h + 3.0f;
    }

    private void avatar(GuiGraphicsExtractor graphics, String nick, float x, float y, float s, float a, Color accent) {
        Identifier tex;
        boolean self = nick.equalsIgnoreCase(CloudFriends.nick() == null ? "" : CloudFriends.nick());
        Object object = tex = self ? ProfileAvatar.getTexture() : null;
        if (tex != null) {
            Image.of(tex, x, y, s, s).circle().alpha(a).render(graphics);
            return;
        }
        RoundedRect.of(x, y, s, s).radius(s * 0.5f).horizontalGradient(accent.withAlpha(0.9f * a), GuiDraw.getAccent2().withAlpha(0.9f * a)).render(graphics);
        Text.of(Fonts.SEMIBOLD, nick.substring(0, 1).toUpperCase(Locale.ROOT), x + s * 0.5f, y + s * 0.5f, s * 0.45f).centered().color(Color.rgb(255, 255, 255).withAlpha(a)).render(graphics);
    }

    public void renderOverlay(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float a, Color accent, long now) {
        String nick = Profiles.viewedNick();
        if (nick == null) {
            return;
        }
        float t = GuiDraw.easeOut((float)(now - this.openTime) / 220.0f);
        float oa = a * t;
        RoundedRect.of(x, y, w, h).radius(6.0f).color(Color.rgba(0, 0, 0, Math.round(150.0f * oa))).render(graphics);
        this.hit(x, y, w, h, this::close, this::close);
        float cw = Math.min(210.0f, w - 20.0f);
        Profiles.Profile p = Profiles.viewed();
        float ch = p == null ? 50.0f : 128.0f;
        float cx = x + (w - cw) * 0.5f;
        float cy = y + (h - ch) * 0.5f + (1.0f - t) * 8.0f;
        this.hit(cx, cy, cw, ch, () -> {}, null);
        RoundedRect.of(cx, cy, cw, ch).radius(6.0f).color(Color.rgba(24, 25, 31, Math.round(250.0f * oa))).render(graphics);
        Outline.of(cx, cy, cw, ch).radius(6.0f).thickness(0.5f).color(Color.rgba(255, 255, 255, Math.round(14.0f * oa))).render(graphics);
        float bs = 11.0f;
        float bx = cx + cw - bs - 5.0f;
        float by = cy + 5.0f;
        float xh = this.anim("profile-x", this.hov("profile-x", this.hovered(bx, by, bs, bs)), 9.0f);
        Color xg = GuiDraw.mix(Color.rgb(200, 204, 214), Color.rgb(255, 120, 120), xh).withAlpha(oa);
        GuiDraw.stroke(graphics, bx + 3.0f, by + 3.0f, bx + bs - 3.0f, by + bs - 3.0f, 0.9f, xg);
        GuiDraw.stroke(graphics, bx + 3.0f, by + bs - 3.0f, bx + bs - 3.0f, by + 3.0f, 0.9f, xg);
        this.hit(bx, by, bs, bs, this::close, null);
        if (p == null) {
            Object s = Profiles.viewedFailed() ? "\u0418\u0433\u0440\u043e\u043a \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d \u0432 Brokke" : "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430 \u043f\u0440\u043e\u0444\u0438\u043b\u044f " + nick + "...";
            Text.of(Fonts.MEDIUM, (String)s, cx + cw * 0.5f, cy + ch * 0.5f + 5.0f, 5.4f).centered().color(Color.rgb(170, 175, 186).withAlpha(oa)).render(graphics);
            return;
        }
        float av = 20.0f;
        this.avatar(graphics, p.nick(), cx + 8.0f, cy + 8.0f, av, oa, accent);
        RoundedRect.of(cx + 8.0f + av - 5.0f, cy + 8.0f + av - 5.0f, 5.0f, 5.0f).radius(2.5f).color((p.online() ? Color.rgb(70, 215, 120) : Color.rgb(95, 100, 110)).withAlpha(oa)).render(graphics);
        float tx = cx + 34.0f;
        Text.of(Fonts.MEDIUM, p.nick(), tx, cy + 13.5f, 6.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(oa)).render(graphics);
        String online = p.online() ? "\u0432 \u0441\u0435\u0442\u0438" + (String)(p.server().isEmpty() ? "" : "  |  " + p.server()) : "\u0431\u044b\u043b " + DATE.format(new Date(p.lastSeen()));
        Text.of(Fonts.MEDIUM, online, tx, cy + 22.0f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 132).withAlpha(oa)).render(graphics);
        float ly = cy + 34.0f;
        if (!p.status().isEmpty()) {
            Text.of(Fonts.MEDIUM, GuiDraw.fit(ProfileView.clean(p.status()), 5.2f, cw - 16.0f), cx + 8.0f, ly + 4.0f, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(220, 224, 232).withAlpha(oa)).render(graphics);
            ly += 10.0f;
        }
        if (!p.bio().isEmpty()) {
            for (String line : ProfileView.wrapLines(ProfileView.clean(p.bio()), 4.8f, cw - 16.0f, 2)) {
                Text.of(Fonts.MEDIUM, line, cx + 8.0f, ly + 4.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(195, 199, 208).withAlpha(oa)).render(graphics);
                ly += 8.0f;
            }
        }
        ly += 3.0f;
        String[][] stats = new String[][]{{String.valueOf(p.points()), "\u043e\u0447\u043a\u043e\u0432"}, {String.valueOf(p.streak()), "\u0441\u0442\u0440\u0438\u043a"}, {String.valueOf(p.friends()), "\u0434\u0440\u0443\u0437\u0435\u0439"}, {p.unlocked() + "/" + p.ach().size(), "\u0430\u0447\u0438\u0432\u043e\u043a"}};
        float sw = (cw - 16.0f - 9.0f) / 4.0f;
        for (int i = 0; i < 4; ++i) {
            float sx = cx + 8.0f + (float)i * (sw + 3.0f);
            Text.of(Fonts.MEDIUM, stats[i][0], sx, ly + 4.0f, 6.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(oa)).render(graphics);
            Text.of(Fonts.MEDIUM, stats[i][1], sx, ly + 12.0f, 4.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(115, 120, 132).withAlpha(oa)).render(graphics);
        }
        ly += 21.0f;
        float chipX = cx + 8.0f;
        for (Profiles.Ach ach : p.ach()) {
            if (!ach.unlocked()) continue;
            float tw = Text.of(Fonts.MEDIUM, ach.title()).size(4.4f).width() + 8.0f;
            if (chipX + tw > cx + cw - 8.0f) break;
            RoundedRect.of(chipX, ly, tw, 9.0f).radius(3.0f).color(Color.rgba(255, 255, 255, Math.round(8.0f * oa))).render(graphics);
            Text.of(Fonts.MEDIUM, ach.title(), chipX + tw * 0.5f, ly + 4.5f, 4.4f).centered().color(Color.rgb(200, 204, 214).withAlpha(oa)).render(graphics);
            chipX += tw + 3.0f;
        }
        if (chipX > cx + 8.0f) {
            ly += 12.0f;
        }
        if (p.cos().length > 0) {
            ArrayList<String> names = new ArrayList<String>();
            for (int res : p.cos()) {
                int idx = LocalCosmetics.indexOfResource(res);
                if (idx < 0) continue;
                names.add(LocalCosmetics.name(idx));
            }
            if (!names.isEmpty()) {
                Text.of(Fonts.MEDIUM, GuiDraw.fit("\u041a\u043e\u0441\u043c\u0435\u0442\u0438\u043a\u0430: " + String.join((CharSequence)", ", names), 4.6f, cw - 16.0f), cx + 8.0f, ly + 4.0f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(150, 155, 165).withAlpha(oa)).render(graphics);
            }
        }
        float bh = 14.0f;
        float f = cy + ch - bh - 7.0f;
        if (p.self()) {
            this.button(graphics, "pv-own", "\u041c\u043e\u0439 \u043f\u0440\u043e\u0444\u0438\u043b\u044c", cx + 8.0f, f, cw - 16.0f, bh, oa, accent, true, () -> {
                this.close();
                this.ctx.globals.tab = 2;
                this.openOwn();
            });
        } else if (p.friend()) {
            float half = (cw - 19.0f) * 0.5f;
            this.button(graphics, "pv-invite", "\u041f\u043e\u0437\u0432\u0430\u0442\u044c \u043a \u0441\u0435\u0431\u0435", cx + 8.0f, f, half, bh, oa, accent, true, () -> CloudFriends.invite(p.nick()));
            this.button(graphics, "pv-mark", CloudFriends.isMarked(p.nick()) ? "\u0423\u0431\u0440\u0430\u0442\u044c \u043c\u0435\u0442\u043a\u0443" : "\u041c\u0435\u0442\u043a\u0430 \u043d\u0430 \u0434\u0440\u0443\u0433\u0430", cx + 11.0f + half, f, half, bh, oa, accent, false, () -> CloudFriends.toggleMark(p.nick()));
        } else {
            String label = p.pending() ? "\u0417\u0430\u044f\u0432\u043a\u0430 \u043e\u0442\u043f\u0440\u0430\u0432\u043b\u0435\u043d\u0430" : "\u0414\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0432 \u0434\u0440\u0443\u0437\u044c\u044f";
            this.button(graphics, "pv-add", label, cx + 8.0f, f, cw - 16.0f, bh, oa, accent, !p.pending(), () -> {
                if (!p.pending()) {
                    CloudFriends.request(p.nick());
                    Profiles.view(p.nick());
                }
            });
        }
    }

    private static List<String> wrapLines(String text, float size, float maxW, int maxLines) {
        ArrayList<String> lines = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            String next;
            String string = next = cur.isEmpty() ? word : String.valueOf(cur) + " " + word;
            if (Text.of(Fonts.MEDIUM, next).size(size).width() > maxW && !cur.isEmpty()) {
                lines.add(cur.toString());
                cur.setLength(0);
                cur.append(word);
                if (lines.size() != maxLines) continue;
                break;
            }
            cur.setLength(0);
            cur.append(next);
        }
        if (lines.size() < maxLines && !cur.isEmpty()) {
            lines.add(GuiDraw.fit(cur.toString(), size, maxW));
        }
        return lines;
    }

    private void button(GuiGraphicsExtractor graphics, String key, String label, float x, float y, float w, float h, float a, Color accent, boolean primary, Runnable action) {
        float hv = this.anim(key, this.hov(key, this.hovered(x, y, w, h)), 8.0f);
        if (primary) {
            RoundedRect.of(x, y, w, h).radius(4.0f).horizontalGradient(accent.withAlpha((0.6f + 0.3f * hv) * a), GuiDraw.getAccent2().withAlpha((0.6f + 0.3f * hv) * a)).render(graphics);
        } else {
            RoundedRect.of(x, y, w, h).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round((10.0f + 10.0f * hv) * a))).render(graphics);
        }
        Text.of(Fonts.MEDIUM, label, x + w * 0.5f, y + h * 0.5f, 4.8f).centered().color(Color.rgb(255, 255, 255).withAlpha(a)).render(graphics);
        this.hit(x, y, w, h, () -> {
            action.run();
            ClientSound.ui(ClientSound.Ui.CLICK);
        }, null);
    }
}

