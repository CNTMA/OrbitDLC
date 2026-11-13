/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.tab;

import brokke.cc.ui.clickgui.CloudFriends;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CloudFriendsView
extends GuiComponent {
    public CloudFriendsView(ClickGuiContext ctx) {
        super(ctx);
    }

    public float renderCloudFriends(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom, long now) {
        String status = null;
        Color statusColor = Color.rgb(255, 95, 95);
        if (now - CloudFriends.errorTime() < 3500L && CloudFriends.errorTime() >= CloudFriends.successTime()) {
            status = CloudFriends.error();
        } else if (!CloudFriends.isConnected() && CloudFriends.isSynced()) {
            status = "\u041d\u0435\u0442 \u0441\u043e\u0435\u0434\u0438\u043d\u0435\u043d\u0438\u044f \u0441 \u0441\u0435\u0440\u0432\u0435\u0440\u043e\u043c \u0434\u0440\u0443\u0437\u0435\u0439";
        } else if (now - CloudFriends.successTime() < 2500L) {
            status = "\u0413\u043e\u0442\u043e\u0432\u043e";
            statusColor = Color.rgb(90, 200, 130);
        }
        if (status != null) {
            Text.of(Fonts.MEDIUM, status, x + 2.0f, y + 2.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(statusColor.withAlpha(a)).render(graphics);
            y += 9.0f;
        }
        y = this.renderCloudToggle(graphics, "cloud-share-inv", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u043c\u043e\u0439 \u0438\u043d\u0432\u0435\u043d\u0442\u0430\u0440\u044c \u0434\u0440\u0443\u0437\u044c\u044f\u043c", CloudFriends.isShareInventory(), CloudFriends::toggleShareInventory, x, y, w, a, accent, top, bottom);
        y = this.renderCloudToggle(graphics, "cloud-share-loc", "\u0420\u0430\u0437\u0440\u0435\u0448\u0430\u0442\u044c \u043c\u0435\u0442\u043a\u0438 \u043d\u0430 \u043c\u0435\u043d\u044f (\u043a\u043e\u043e\u0440\u0434\u0438\u043d\u0430\u0442\u044b)", CloudFriends.isShareLocation(), CloudFriends::toggleShareLocation, x, y, w, a, accent, top, bottom);
        y += 4.0f;
        List<CloudFriends.Entry> incoming = CloudFriends.incoming();
        List<CloudFriends.Entry> friends = CloudFriends.friends();
        List<CloudFriends.Entry> outgoing = CloudFriends.outgoing();
        if (incoming.isEmpty() && friends.isEmpty() && outgoing.isEmpty()) {
            String empty = !CloudFriends.isSynced() ? (CloudFriends.isConnected() ? "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430..." : "\u0421\u0435\u0440\u0432\u0435\u0440 \u0434\u0440\u0443\u0437\u0435\u0439 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d") : "\u0414\u0440\u0443\u0437\u0435\u0439 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442 \u2014 \u043e\u0442\u043f\u0440\u0430\u0432\u044c \u0437\u0430\u044f\u0432\u043a\u0443 \u043f\u043e \u043d\u0438\u043a\u0443";
            Text.of(Fonts.MEDIUM, empty, x + w * 0.5f, y + 16.0f, 5.4f).centered().color(Color.rgb(115, 120, 132).withAlpha(a * GuiDraw.easeOut((float)(now - this.ctx.friends.friendsSwitchTime) / 300.0f))).render(graphics);
            return y + 32.0f;
        }
        float rowH = 18.0f;
        float rowGap = 3.0f;
        int idx = 0;
        List<List<CloudFriends.Entry>> groups = List.of(incoming, friends, outgoing);
        String[] titles = new String[]{"\u0417\u0430\u044f\u0432\u043a\u0438 \u0432 \u0434\u0440\u0443\u0437\u044c\u044f", "\u0414\u0440\u0443\u0437\u044c\u044f", "\u0418\u0441\u0445\u043e\u0434\u044f\u0449\u0438\u0435 \u0437\u0430\u044f\u0432\u043a\u0438"};
        for (int kind = 0; kind < 3; ++kind) {
            List<CloudFriends.Entry> list = groups.get(kind);
            if (list.isEmpty()) continue;
            String title = titles[kind];
            float tw = Text.of(Fonts.MEDIUM, title).size(4.8f).width();
            Text.of(Fonts.MEDIUM, title, x + 2.0f, y + 4.0f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(130, 135, 146).withAlpha(a)).render(graphics);
            Text.of(Fonts.MEDIUM, String.valueOf(list.size()), x + 6.0f + tw, y + 4.0f, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(accent.withAlpha(a)).render(graphics);
            y += 10.0f;
            for (CloudFriends.Entry entry : list) {
                float t = GuiDraw.easeOut((float)(now - this.ctx.friends.friendsSwitchTime - (long)idx * 25L) / 320.0f);
                this.renderCloudRow(graphics, entry, kind, x, y + (1.0f - t) * 6.0f, w, rowH, a * t, accent, top, bottom);
                y += rowH + rowGap;
                ++idx;
            }
            y += 3.0f;
        }
        return y;
    }

    private float renderCloudToggle(GuiGraphicsExtractor graphics, String key, String label, boolean value, Runnable toggle, float x, float y, float w, float a, Color accent, float top, float bottom) {
        float h = 13.0f;
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        float t = this.anim(key + "-on", value, 6.0f);
        RoundedRect.of(x, y, w, h).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round((5.0f + 4.0f * hv) * a))).render(graphics);
        float s = 7.0f;
        GuiDraw.renderCheckBox(graphics, x + 4.0f, y + (h - s) * 0.5f, s, t, accent, a);
        Text.of(Fonts.MEDIUM, label, x + 4.0f + s + 5.0f, y + h * 0.5f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(150, 155, 165), Color.rgb(232, 235, 242), Math.max(t, hv * 0.5f)).withAlpha(a)).render(graphics);
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, () -> {
                toggle.run();
                ClientSound.checkbox(!value);
            }, null);
        }
        return y + h + 2.0f;
    }

    private void renderCloudRow(GuiGraphicsExtractor graphics, CloudFriends.Entry entry, int kind, float x, float y, float w, float h, float a, Color accent, float top, float bottom) {
        Object sub;
        String name = entry.nick();
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        String key = "cloud-row-" + kind + "-" + name;
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hv) * a))).render(graphics);
        float av = 12.0f;
        float ax = x + 4.0f;
        float ay = y + (h - av) * 0.5f;
        RoundedRect.of(ax, ay, av, av).radius(av * 0.5f).horizontalGradient(accent.withAlpha(0.85f * a), GuiDraw.getAccent2().withAlpha(0.85f * a)).render(graphics);
        Text.of(Fonts.MEDIUM, name.substring(0, 1).toUpperCase(Locale.ROOT), ax + av * 0.5f, ay + av * 0.5f, 5.4f).centered().color(Color.rgb(255, 255, 255).withAlpha(a)).render(graphics);
        if (kind == 1) {
            float dot = 3.5f;
            RoundedRect.of(ax + av - dot + 0.5f, ay + av - dot + 0.5f, dot, dot).radius(dot * 0.5f).color((entry.online() ? Color.rgb(70, 215, 120) : Color.rgb(95, 100, 110)).withAlpha(a)).render(graphics);
        }
        Color subColor = Color.rgb(110, 115, 126);
        if (kind == 0) {
            sub = "\u0425\u043e\u0447\u0435\u0442 \u0434\u043e\u0431\u0430\u0432\u0438\u0442\u044c \u0432\u0430\u0441 \u0432 \u0434\u0440\u0443\u0437\u044c\u044f";
            subColor = accent;
        } else if (kind == 2) {
            sub = "\u041e\u0436\u0438\u0434\u0430\u0435\u0442 \u043f\u043e\u0434\u0442\u0432\u0435\u0440\u0436\u0434\u0435\u043d\u0438\u044f";
        } else if (entry.online()) {
            sub = "\u0412 \u0441\u0435\u0442\u0438";
            if (!entry.status().isEmpty()) {
                sub = (String)sub + "  |  " + entry.status();
            }
            if (!entry.mc().isEmpty() && !entry.mc().equalsIgnoreCase(name)) {
                sub = (String)sub + "  |  " + entry.mc();
            }
            if (entry.hasAnarchy()) {
                sub = (String)sub + "  |  " + entry.anarchy();
            }
            if (!entry.server().isEmpty()) {
                sub = (String)sub + "  |  " + entry.server();
            }
            if (entry.hasPos()) {
                sub = (String)sub + "  |  " + entry.coords();
            }
            subColor = Color.rgb(90, 200, 130);
        } else {
            sub = entry.status().isEmpty() ? "\u041d\u0435 \u0432 \u0441\u0435\u0442\u0438" : "\u041d\u0435 \u0432 \u0441\u0435\u0442\u0438  |  " + entry.status();
        }
        float nx = ax + av + 5.0f;
        boolean marked = kind == 1 && CloudFriends.isMarked(name);
        Text.of(Fonts.MEDIUM, (String)(marked ? name + "  \u00b7 \u043c\u0435\u0442\u043a\u0430" : name), nx, y + h * 0.5f - 2.5f, 5.6f).verticalAlign(VerticalAlign.MIDDLE).color((marked ? accent : Color.rgb(232, 235, 242)).withAlpha(a)).render(graphics);
        Text.of(Fonts.MEDIUM, (String)sub, nx, y + h * 0.5f + 3.5f, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(subColor.withAlpha(a)).render(graphics);
        float bs = 12.0f;
        float bx = x + w - bs - 4.0f;
        float by = y + (h - bs) * 0.5f;
        Runnable close = switch (kind) {
            case 0 -> () -> {
                CloudFriends.decline(name);
                ClientSound.checkbox(false);
            };
            case 2 -> () -> {
                CloudFriends.cancel(name);
                ClientSound.checkbox(false);
            };
            default -> () -> {
                CloudFriends.remove(name);
                ClientSound.checkbox(false);
            };
        };
        if (kind == 1 && GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, () -> {
                CloudFriends.toggleMark(name);
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
        }
        this.renderCloudButton(graphics, String.valueOf(key) + "-x", bx, by, bs, a, hv, Color.rgb(255, 85, 85), false, close, y, h, top, bottom);
        if (kind == 1) {
            float iw = 34.0f;
            float ix = bx - iw - 3.0f;
            boolean iHover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(ix, by, iw, bs);
            float ih = this.anim(String.valueOf(key) + "-inv", iHover, 8.0f);
            RoundedRect.of(ix, by, iw, bs).radius(3.5f).horizontalGradient(accent.withAlpha((0.25f + 0.5f * ih) * a * Math.max(0.5f, hv)), GuiDraw.getAccent2().withAlpha((0.25f + 0.5f * ih) * a * Math.max(0.5f, hv))).render(graphics);
            Text.of(Fonts.MEDIUM, "\u041f\u043e\u0437\u0432\u0430\u0442\u044c", ix + iw * 0.5f, by + bs * 0.5f, 4.6f).centered().color(Color.rgb(255, 255, 255).withAlpha(a * Math.max(0.6f, hv))).render(graphics);
            if (GuiDraw.canHit(y, h, top, bottom)) {
                this.hit(ix, by, iw, bs, () -> {
                    CloudFriends.invite(name);
                    ClientSound.checkbox(true);
                }, null);
            }
            float pw = 34.0f;
            float px = ix - pw - 3.0f;
            boolean pHover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(px, by, pw, bs);
            float ph = this.anim(String.valueOf(key) + "-prof", pHover, 8.0f);
            RoundedRect.of(px, by, pw, bs).radius(3.5f).color(Color.rgba(255, 255, 255, Math.round((10.0f + 12.0f * ph) * a * Math.max(0.5f, hv)))).render(graphics);
            Text.of(Fonts.MEDIUM, "\u041f\u0440\u043e\u0444\u0438\u043b\u044c", px + pw * 0.5f, by + bs * 0.5f, 4.6f).centered().color(Color.rgb(225, 228, 236).withAlpha(a * Math.max(0.6f, hv))).render(graphics);
            if (GuiDraw.canHit(y, h, top, bottom)) {
                this.hit(px, by, pw, bs, () -> {
                    this.ctx.globals.tab = 0;
                    this.ctx.globals.profile.open(name);
                }, null);
            }
        }
        if (kind == 0) {
            this.renderCloudButton(graphics, String.valueOf(key) + "-ok", bx - bs - 3.0f, by, bs, a, hv, accent, true, () -> {
                CloudFriends.accept(name);
                ClientSound.checkbox(true);
            }, y, h, top, bottom);
        }
    }

    private void renderCloudButton(GuiGraphicsExtractor graphics, Object key, float bx, float by, float bs, float a, float rowHover, Color color, boolean plus, Runnable action, float rowY, float rowH, float top, float bottom) {
        boolean bHover = GuiDraw.canHit(rowY, rowH, top, bottom) && this.hovered(bx, by, bs, bs);
        float bhv = this.anim(key, bHover, 8.0f);
        float vis = plus ? 1.0f : Math.max(0.35f, rowHover);
        RoundedRect.of(bx, by, bs, bs).radius(3.5f).color(color.withAlpha((0.1f + (plus ? 0.12f : 0.0f) + 0.22f * bhv) * a * vis)).render(graphics);
        Color glyph = GuiDraw.mix(Color.rgb(150, 155, 165), color, plus ? Math.max(0.6f, bhv) : bhv).withAlpha(a * Math.max(0.45f, plus ? 1.0f : rowHover));
        float c = bs * 0.5f;
        if (plus) {
            GuiDraw.stroke(graphics, bx + c - 2.6f, by + c, bx + c + 2.6f, by + c, 0.9f, glyph);
            GuiDraw.stroke(graphics, bx + c, by + c - 2.6f, bx + c, by + c + 2.6f, 0.9f, glyph);
        } else {
            GuiDraw.stroke(graphics, bx + c - 2.2f, by + c - 2.2f, bx + c + 2.2f, by + c + 2.2f, 0.9f, glyph);
            GuiDraw.stroke(graphics, bx + c - 2.2f, by + c + 2.2f, bx + c + 2.2f, by + c - 2.2f, 0.9f, glyph);
        }
        if (GuiDraw.canHit(rowY, rowH, top, bottom)) {
            this.hit(bx - 2.0f, by - 2.0f, bs + 4.0f, bs + 4.0f, action, null);
        }
    }
}

