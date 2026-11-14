/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.ui.clickgui.globals;

import brokke.cc.ui.clickgui.CloudFriends;
import brokke.cc.ui.clickgui.globals.Emoji;
import brokke.cc.ui.clickgui.globals.GlobalChat;
import brokke.cc.ui.clickgui.globals.ProfileView;
import brokke.cc.ui.clickgui.globals.Profiles;
import brokke.cc.ui.clickgui.globals.Translator;
import brokke.cc.ui.clickgui.globals.VoiceMessages;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.animation.Animation;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.image.Image;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public final class GlobalsTab
extends GuiComponent {
    public static final String CHAT_FIELD = "globals:chat";
    private static final SimpleDateFormat TIME = new SimpleDateFormat("HH:mm");
    private static final float TEXT_SIZE = 6.4f;
    private static final float EMOJI_SIZE = 14.0f;
    private static final float LINE_H = 10.5f;
    public int tab = 0;
    public long tabSwitchTime = 0L;
    public String draft = "";
    public float chatScroll = 0.0f;
    private float chatScrollValue = 0.0f;
    private float chatMaxScroll = 0.0f;
    private final float[] messagesBox = new float[4];
    private boolean pickerOpen = false;
    private int pickerGroup = 0;
    private long pickerTime = 0L;
    private GlobalChat.Message replyTo = null;
    private long reactFor = -1L;
    private float reactX;
    private float reactY;
    private long reactTime = 0L;
    public final ProfileView profile;
    private final Map<Long, List<List<Piece>>> wrapCache = new HashMap<Long, List<List<Piece>>>();
    private final Set<Long> translated = new HashSet<Long>();
    private final Map<Long, List<List<Piece>>> transCache = new HashMap<Long, List<List<Piece>>>();
    private static final float VOICE_H = 18.0f;
    private float wrapCacheW = -1.0f;

    public GlobalsTab(ClickGuiContext ctx) {
        super(ctx);
        this.profile = new ProfileView(ctx);
    }

    public float renderGlobals(GuiGraphicsExtractor graphics, float x, float y, float w, float prog, float top, float bottom, long now) {
        float startY = y;
        float appear = GuiDraw.easeOut((float)(now - this.ctx.switchTime) / 380.0f);
        float a = prog * appear;
        Color accent = GuiDraw.getAccent();
        GlobalChat.markActive();
        y = this.renderTabs(graphics, x, y + (1.0f - appear) * 8.0f, w, a, accent, top, bottom);
        if (this.tab == 1) {
            float h = this.ctx.friends.renderFriends(graphics, x, y, w, prog, top, bottom, now);
            return y - startY + h;
        }
        if (this.tab == 2) {
            float h = this.profile.renderOwn(graphics, x, y, w, a, accent, top, bottom, now);
            return y - startY + h;
        }
        float chatH = Math.max(80.0f, bottom - y - 1.0f);
        this.renderChat(graphics, x, y, w, chatH, a, accent, now);
        this.profile.renderOverlay(graphics, x, y, w, chatH, a, accent, now);
        return y - startY + chatH;
    }

    private float renderTabs(GuiGraphicsExtractor graphics, float x, float y, float w, float a, Color accent, float top, float bottom) {
        float tabH = 14.0f;
        float tabW = (w - 4.0f) / 3.0f;
        RoundedRect.of(x, y, w, tabH + 4.0f).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        String posKey = "globals-tab-pos";
        float target = this.tab;
        Animation posAnim = this.ctx.fx.computeIfAbsent(posKey, k -> new Animation(target));
        posAnim.animate(target, 1.0f, 6.0f);
        float tabPos = posAnim.value();
        RoundedRect.of(x + 2.0f + tabPos * tabW, y + 2.0f, tabW, tabH).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round(14.0f * a))).render(graphics);
        String[] labels = new String[]{"IRC \u0447\u0430\u0442", "\u0414\u0440\u0443\u0437\u044c\u044f", "\u041f\u0440\u043e\u0444\u0438\u043b\u044c"};
        String[] icons = new String[]{"G", "D", "H"};
        int incoming = CloudFriends.incoming().size();
        for (int i = 0; i < 3; ++i) {
            int idx = i;
            float tx = x + 2.0f + (float)i * tabW;
            boolean hover = GuiDraw.canHit(y, tabH, top, bottom) && this.hovered(tx, y + 2.0f, tabW, tabH);
            String key = "globals-tab-" + i;
            float hv = this.anim(key, this.hov(key, hover), 7.0f);
            float act = Math.clamp(1.0f - Math.abs(tabPos - (float)i), 0.0f, 1.0f);
            Color c = GuiDraw.mix(Color.rgb(135, 140, 150), Color.rgb(240, 242, 248), Math.max(act, hv * 0.5f)).withAlpha(a);
            String badge = i == 0 ? (GlobalChat.online() > 0 ? String.valueOf(GlobalChat.online()) : "") : (i == 1 ? (incoming > 0 ? "+" + incoming : String.valueOf(CloudFriends.friends().size())) : (Profiles.canClaim() ? "!" : ""));
            float lw = Text.of(Fonts.MEDIUM, labels[i]).size(5.4f).width();
            float bw = badge.isEmpty() ? 0.0f : Text.of(Fonts.MEDIUM, badge).size(4.4f).width() + 4.0f;
            float total = 9.0f + lw + bw;
            float lx = tx + (tabW - total) * 0.5f;
            float cy = y + 2.0f + tabH * 0.5f;
            Text.of(Fonts.ICONS, icons[i], lx + 3.0f, cy, 5.6f).centered().color(c).render(graphics);
            Text.of(Fonts.MEDIUM, labels[i], lx + 9.0f, cy, 5.4f).verticalAlign(VerticalAlign.MIDDLE).color(c).render(graphics);
            if (!badge.isEmpty()) {
                Color bc = i == 1 && incoming > 0 || i == 2 ? accent : GuiDraw.mix(Color.rgb(105, 110, 120), accent, act);
                Text.of(Fonts.MEDIUM, badge, lx + 9.0f + lw + 4.0f, cy, 4.4f).verticalAlign(VerticalAlign.MIDDLE).color(bc.withAlpha(a)).render(graphics);
            }
            if (!GuiDraw.canHit(y, tabH + 4.0f, top, bottom)) continue;
            this.hit(tx, y + 2.0f, tabW, tabH, () -> {
                if (this.tab != idx) {
                    this.tab = idx;
                    this.ctx.friends.friendsSwitchTime = this.tabSwitchTime = System.currentTimeMillis();
                    this.pickerOpen = false;
                    this.reactFor = -1L;
                    if (idx == 1) {
                        CloudFriends.refresh();
                    }
                    if (idx == 2) {
                        this.profile.openOwn();
                    }
                    ClientSound.ui(ClientSound.Ui.CLICK);
                }
            }, null);
        }
        return y + tabH + 4.0f + 5.0f;
    }

    private void renderChat(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float a, Color accent, long now) {
        RoundedRect.of(x, y, w, h).radius(6.0f).color(Color.rgba(0, 0, 0, Math.round(28.0f * a))).render(graphics);
        float headH = 20.0f;
        float av = 12.0f;
        RoundedRect.of(x + 5.0f, y + (headH - av) * 0.5f, av, av).radius(av * 0.5f).horizontalGradient(accent.withAlpha(0.85f * a), GuiDraw.getAccent2().withAlpha(0.85f * a)).render(graphics);
        Text.of(Fonts.ICONS, "G", x + 5.0f + av * 0.5f, y + headH * 0.5f, 5.6f).centered().color(Color.rgb(255, 255, 255).withAlpha(a)).render(graphics);
        Text.of(Fonts.MEDIUM, "\u041e\u0431\u0449\u0438\u0439 \u0447\u0430\u0442", x + 22.0f, y + headH * 0.5f - 2.5f, 5.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(a)).render(graphics);
        Object sub = !GlobalChat.isConnected() && GlobalChat.isLoaded() ? "\u041d\u0435\u0442 \u0441\u043e\u0435\u0434\u0438\u043d\u0435\u043d\u0438\u044f" : (!GlobalChat.isLoaded() ? "\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435..." : GlobalChat.online() + " \u0432 \u0441\u0435\u0442\u0438");
        Text.of(Fonts.MEDIUM, (String)sub, x + 22.0f, y + headH * 0.5f + 3.5f, 4.3f).verticalAlign(VerticalAlign.MIDDLE).color((GlobalChat.isConnected() ? Color.rgb(110, 115, 126) : Color.rgb(235, 100, 100)).withAlpha(a)).render(graphics);
        RoundedRect.of(x + 4.0f, y + headH, w - 8.0f, 0.5f).color(Color.rgba(255, 255, 255, Math.round(10.0f * a))).render(graphics);
        float tgW = 62.0f;
        this.pill(graphics, "globals-ingame", GlobalChat.isInGame() ? "\u0412 \u0438\u0433\u0440\u0435: \u0432\u043a\u043b" : "\u0412 \u0438\u0433\u0440\u0435: \u0432\u044b\u043a\u043b", x + w - tgW - 5.0f, y + 4.5f, tgW, 11.0f, GlobalChat.isInGame(), a, accent, GlobalChat::toggleInGame);
        Text.of(Fonts.MEDIUM, "# \u0432 \u0447\u0430\u0442\u0435 \u0438\u0433\u0440\u044b = IRC", x + w - tgW - 9.0f, y + headH * 0.5f, 4.2f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(100, 105, 116).withAlpha(a)).render(graphics);
        float inputH = 17.0f;
        float replyH = this.replyTo != null ? 13.0f : 0.0f;
        float msgTop = y + headH + 2.0f;
        float msgBottom = y + h - inputH - 8.0f - replyH;
        this.renderMessages(graphics, x + 4.0f, msgTop, w - 8.0f, msgBottom - msgTop, a, accent, now);
        float iy = y + h - inputH - 4.0f;
        if (this.replyTo != null) {
            this.renderReplyBar(graphics, x + 4.0f, iy - replyH, w - 8.0f, replyH - 2.0f, a, accent);
        }
        if (this.pickerOpen || this.reactFor >= 0L) {
            this.hit(x, y, w, h, () -> {
                this.pickerOpen = false;
                this.reactFor = -1L;
            }, null);
        }
        this.renderInput(graphics, x + 4.0f, iy, w - 8.0f, inputH, a, accent, now);
        if (this.reactFor >= 0L) {
            this.renderReactStrip(graphics, a, accent, now);
        }
        if (this.pickerOpen) {
            this.renderPicker(graphics, x + 4.0f, iy - 4.0f, w - 8.0f, a, accent, now);
        }
    }

    private List<List<Piece>> translation(GlobalChat.Message m, float maxW) {
        if (!this.translated.contains(m.id())) {
            return null;
        }
        Translator.Result res = Translator.cached(m.text());
        if (res == null) {
            return List.of(List.of(new Piece(Translator.isPending(m.text()) ? "\u041f\u0435\u0440\u0435\u0432\u043e\u0436\u0443..." : "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043f\u0435\u0440\u0435\u0432\u0435\u0441\u0442\u0438", -1, 0.0f)));
        }
        if (res.same()) {
            return List.of(List.of(new Piece("\u041f\u0435\u0440\u0435\u0432\u043e\u0434 \u043d\u0435 \u043d\u0443\u0436\u0435\u043d", -1, 0.0f)));
        }
        if (this.wrapCacheW != maxW) {
            this.transCache.clear();
        }
        return this.transCache.computeIfAbsent(m.id(), k -> this.wrap(res.text(), maxW));
    }

    private void toggleTranslate(GlobalChat.Message m) {
        if (!this.translated.remove(m.id())) {
            this.translated.add(m.id());
            this.transCache.remove(m.id());
            Translator.translate(m.text(), "ru", res -> this.transCache.remove(m.id()));
        }
        ClientSound.ui(ClientSound.Ui.CLICK);
    }

    private List<List<Piece>> wrapCached(GlobalChat.Message m, float maxW) {
        if (this.wrapCacheW != maxW || this.wrapCache.size() > 400) {
            this.wrapCache.clear();
            this.wrapCacheW = maxW;
        }
        return this.wrapCache.computeIfAbsent(m.id(), k -> this.wrap(m.text(), maxW));
    }

    private List<List<Piece>> wrap(String text, float maxW) {
        ArrayList<List<Piece>> lines = new ArrayList<List<Piece>>();
        ArrayList<Piece> line = new ArrayList<Piece>();
        float cx = 0.0f;
        for (Emoji.Segment seg : Emoji.split(text)) {
            if (seg.isEmoji()) {
                if (cx + 14.0f > maxW && !line.isEmpty()) {
                    lines.add(line);
                    line = new ArrayList();
                    cx = 0.0f;
                }
                line.add(new Piece("", seg.emoji(), cx));
                cx += 14.8f;
                continue;
            }
            String s = seg.text();
            int i = 0;
            while (i < s.length()) {
                String word;
                float ww;
                int j = i;
                if (s.charAt(i) == ' ') {
                    for (j = i; j < s.length() && s.charAt(j) == ' '; ++j) {
                    }
                } else {
                    while (j < s.length() && s.charAt(j) != ' ') {
                        ++j;
                    }
                }
                if (cx + (ww = GlobalsTab.width(word = s.substring(i, j))) > maxW && !line.isEmpty() && word.charAt(0) != ' ') {
                    lines.add(line);
                    line = new ArrayList();
                    cx = 0.0f;
                }
                if (ww > maxW) {
                    StringBuilder part = new StringBuilder();
                    for (char c : word.toCharArray()) {
                        if (cx + GlobalsTab.width(part.toString() + c) > maxW && !part.isEmpty()) {
                            line.add(new Piece(part.toString(), -1, cx));
                            lines.add(line);
                            line = new ArrayList();
                            cx = 0.0f;
                            part.setLength(0);
                        }
                        part.append(c);
                    }
                    line.add(new Piece(part.toString(), -1, cx));
                    cx += GlobalsTab.width(part.toString());
                } else if (!line.isEmpty() || word.charAt(0) != ' ') {
                    line.add(new Piece(word, -1, cx));
                    cx += ww;
                }
                i = j;
            }
        }
        if (!line.isEmpty() || lines.isEmpty()) {
            lines.add(line);
        }
        return lines;
    }

    private static float lineHeight(List<Piece> line) {
        for (Piece p : line) {
            if (p.emoji() < 0) continue;
            return Math.max(10.5f, 15.5f);
        }
        return 10.5f;
    }

    private static float linesHeight(List<List<Piece>> lines) {
        float h = 0.0f;
        for (List<Piece> line : lines) {
            h += GlobalsTab.lineHeight(line);
        }
        return h;
    }

    private static float width(String s) {
        return s.isEmpty() ? 0.0f : Text.of(Fonts.MEDIUM, s).size(6.4f).width();
    }

    private void renderMessages(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float a, Color accent, long now) {
        this.messagesBox[0] = x;
        this.messagesBox[1] = y;
        this.messagesBox[2] = w;
        this.messagesBox[3] = h;
        List<GlobalChat.Message> messages = GlobalChat.messages();
        if (messages.isEmpty()) {
            String empty = !GlobalChat.isLoaded() ? (GlobalChat.isConnected() ? "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430..." : "\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u0438\u0435 \u043a \u0441\u0435\u0440\u0432\u0435\u0440\u0443...") : "\u0421\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0439 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442, \u043d\u0430\u043f\u0438\u0448\u0438 \u043f\u0435\u0440\u0432\u044b\u043c!";
            Text.of(Fonts.MEDIUM, empty, x + w * 0.5f, y + h * 0.5f, 5.2f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
            this.chatMaxScroll = 0.0f;
            return;
        }
        String self = CloudFriends.nick();
        float textX = 19.0f;
        float maxW = w - textX - 6.0f;
        float total = 0.0f;
        ArrayList<List<List<Piece>>> wrapped = new ArrayList<List<List<Piece>>>(messages.size());
        float[] heights = new float[messages.size()];
        boolean[] groupedFlags = new boolean[messages.size()];
        for (int i = 0; i < messages.size(); ++i) {
            boolean grouped;
            GlobalChat.Message m = messages.get(i);
            List<List<Piece>> lines = this.wrapCached(m, maxW);
            wrapped.add(lines);
            groupedFlags[i] = grouped = i > 0 && messages.get(i - 1).nick().equals(m.nick()) && m.at() - messages.get(i - 1).at() < 120000L;
            List<List<Piece>> tr = m.voice() == null ? this.translation(m, maxW) : null;
            heights[i] = (grouped ? 0.0f : 10.0f) + (m.reply() != null ? 10.0f : 0.0f) + (m.voice() != null ? 18.0f : GlobalsTab.linesHeight(lines)) + (tr != null ? GlobalsTab.linesHeight(tr) + 2.0f : 0.0f) + (m.reactions().isEmpty() ? 0.0f : 14.0f) + (grouped ? 1.0f : 4.0f);
            total += heights[i];
        }
        this.chatMaxScroll = Math.max(0.0f, total - h + 4.0f);
        this.chatScroll = Math.clamp(this.chatScroll, 0.0f, this.chatMaxScroll);
        this.chatScrollValue = this.smooth(this.chatScrollValue, this.chatScroll, 14.0f);
        Scissor.push(graphics, x, y, w, h);
        float cy = y + h - 2.0f + this.chatScrollValue;
        for (int i = messages.size() - 1; i >= 0 && cy > y - 40.0f; --i) {
            Identifier tex;
            boolean rowHover;
            float top;
            GlobalChat.Message m = messages.get(i);
            float mh = heights[i];
            cy = top = cy - mh;
            if (top > y + h) continue;
            boolean grouped = groupedFlags[i];
            boolean mine = self != null && self.equalsIgnoreCase(m.nick());
            float t = GuiDraw.easeOut((float)(now - m.received()) / 300.0f);
            float ma = a * t;
            float oy = (1.0f - t) * 4.0f;
            float ly = top + oy;
            boolean mention = !mine && (m.mentions(self) || m.reply() != null && self != null && m.reply().nick().equalsIgnoreCase(self));
            boolean bl = rowHover = !this.pickerOpen && this.reactFor < 0L && !this.profile.isOpen() && this.hovered(x, Math.max(y, top), w, Math.min(mh, y + h - top)) && top + mh > y;
            if (mention || rowHover || this.reactFor == m.id()) {
                Color bg = mention ? accent.withAlpha(0.1f * ma) : Color.rgba(255, 255, 255, Math.round(6.0f * ma));
                RoundedRect.of(x, top, w, mh - 1.0f).radius(4.0f).color(bg).render(graphics);
                if (mention) {
                    RoundedRect.of(x, top + 1.0f, 1.2f, mh - 3.0f).radius(0.6f).color(accent.withAlpha(ma)).render(graphics);
                }
            }
            if (!grouped) {
                String nick = m.nick();
                float nwHit = Text.of(Fonts.MEDIUM, nick).size(5.8f).width();
                this.hit(x, ly, textX + nwHit + 2.0f, 14.0f, () -> this.profile.open(nick), null);
                float f = 13.0f;
                Color ac = mine ? accent : GlobalsTab.nickColor(m.nick());
                RoundedRect.of(x + 2.0f, ly + 1.0f, f, f).radius(f * 0.5f).color(ac.withAlpha(0.8f * ma)).render(graphics);
                Text.of(Fonts.MEDIUM, m.nick().substring(0, 1).toUpperCase(Locale.ROOT), x + 2.0f + f * 0.5f, ly + 1.0f + f * 0.5f, 6.0f).centered().color(Color.rgb(255, 255, 255).withAlpha(ma)).render(graphics);
                Text.of(Fonts.MEDIUM, m.nick(), x + textX, ly + 4.5f, 5.8f).verticalAlign(VerticalAlign.MIDDLE).color((mine ? accent : GlobalsTab.nickColor(m.nick()).brighter(0.15f)).withAlpha(ma)).render(graphics);
                float nw = Text.of(Fonts.MEDIUM, m.nick()).size(5.8f).width();
                String meta = TIME.format(new Date(m.at()));
                Text.of(Fonts.MEDIUM, meta, x + textX + nw + 4.0f, ly + 4.7f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(105, 110, 120).withAlpha(ma)).render(graphics);
                ly += 10.0f;
            }
            if (m.reply() != null) {
                GlobalChat.Reply rp = m.reply();
                RoundedRect.of(x + textX, ly + 1.0f, 1.0f, 7.0f).color(GlobalsTab.nickColor(rp.nick()).withAlpha(ma)).render(graphics);
                String quote = rp.nick() + ": " + rp.text().replaceAll("[\\x{1F000}-\\x{1FFFF}\\x{2600}-\\x{27BF}]", "").trim();
                Text.of(Fonts.MEDIUM, GuiDraw.fit(quote, 4.8f, w - textX - 12.0f), x + textX + 3.5f, ly + 4.5f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(130, 135, 146).withAlpha(ma)).render(graphics);
                ly += 10.0f;
            }
            if (m.voice() != null) {
                this.renderVoice(graphics, m.voice(), x + textX, ly, w - textX - 8.0f, ma, accent, mine);
                ly += 18.0f;
            }
            List<List<Piece>> tr = m.voice() == null ? this.translation(m, maxW) : null;
            for (List<Piece> list : m.voice() != null ? List.<List<Piece>>of() : wrapped.get(i)) {
                float lh = GlobalsTab.lineHeight(list);
                for (Piece p : list) {
                    if (p.emoji() >= 0) {
                        tex = Emoji.texture(p.emoji());
                        if (tex == null) continue;
                        Image.of(tex, x + textX + p.x(), ly + (lh - 14.0f) * 0.5f, 14.0f, 14.0f).alpha(ma).render(graphics);
                        continue;
                    }
                    Text.of(Fonts.MEDIUM, p.text(), x + textX + p.x(), ly + lh * 0.5f, 6.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(222, 225, 232).withAlpha(ma)).render(graphics);
                }
                ly += lh;
            }
            if (tr != null) {
                RoundedRect.of(x + textX, (ly += 1.0f) + 1.5f, 1.0f, GlobalsTab.linesHeight(tr) - 3.0f).color(accent.withAlpha(0.6f * ma)).render(graphics);
                for (List<Piece> list : tr) {
                    float lh = GlobalsTab.lineHeight(list);
                    for (Piece p : list) {
                        if (p.emoji() >= 0) {
                            tex = Emoji.texture(p.emoji());
                            if (tex == null) continue;
                            Image.of(tex, x + textX + 4.0f + p.x(), ly + (lh - 14.0f) * 0.5f, 14.0f, 14.0f).alpha(ma).render(graphics);
                            continue;
                        }
                        Text.of(Fonts.MEDIUM, p.text(), x + textX + 4.0f + p.x(), ly + lh * 0.5f, 6.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(160, 165, 176).withAlpha(ma)).render(graphics);
                    }
                    ly += lh;
                }
                ly += 1.0f;
            }
            if (!m.reactions().isEmpty()) {
                float rx = x + textX;
                for (Map.Entry<Integer, List<String>> e : m.reactions().entrySet()) {
                    Identifier tex2;
                    int code = e.getKey();
                    boolean minePick = m.reacted(code, self);
                    String count = String.valueOf(e.getValue().size());
                    float cw = 11.0f + Text.of(Fonts.MEDIUM, count).size(4.8f).width() + 5.0f;
                    String key = "globals-rc-" + m.id() + "-" + code;
                    boolean hv = this.hovered(rx, ly + 1.0f, cw, 11.0f);
                    float hva = this.anim(key, this.hov(key, hv), 9.0f);
                    RoundedRect.of(rx, ly + 1.0f, cw, 11.0f).radius(5.5f).color(minePick ? accent.withAlpha((0.3f + 0.15f * hva) * ma) : Color.rgba(255, 255, 255, Math.round((10.0f + 8.0f * hva) * ma))).render(graphics);
                    if (minePick) {
                        Outline.of(rx, ly + 1.0f, cw, 11.0f).radius(5.5f).thickness(0.5f).color(accent.withAlpha(0.8f * ma)).render(graphics);
                    }
                    if ((tex2 = Emoji.texture(code)) != null) {
                        Image.of(tex2, rx + 2.5f, ly + 2.5f, 8.0f, 8.0f).alpha(ma).render(graphics);
                    }
                    Text.of(Fonts.MEDIUM, count, rx + 12.5f, ly + 6.5f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(225, 228, 236).withAlpha(ma)).render(graphics);
                    long id = m.id();
                    this.hit(rx, ly + 1.0f, cw, 11.0f, () -> {
                        GlobalChat.react(id, code);
                        ClientSound.ui(ClientSound.Ui.CLICK);
                    }, null);
                    rx += cw + 2.5f;
                }
            }
            if (!rowHover) continue;
            float bs = 11.0f;
            boolean bl2 = m.voice() == null && !m.text().isBlank();
            int count = bl2 ? 3 : 2;
            float bx = x + w - bs * (float)count - 2.0f * (float)(count - 1) - 4.0f;
            float by = Math.max(y + 1.0f, top + 1.0f);
            RoundedRect.of(bx - 2.0f, by - 1.0f, bs * (float)count + 2.0f * (float)(count - 1) + 4.0f, bs + 2.0f).radius(4.0f).color(Color.rgba(20, 21, 26, Math.round(230.0f * ma))).render(graphics);
            GlobalChat.Message msg = m;
            if (bl2) {
                float tbx = bx + (bs + 2.0f) * 2.0f;
                String tk = "globals-act-tr";
                float th = this.anim(tk, this.hov(tk, this.hovered(tbx, by, bs, bs)), 9.0f);
                boolean on = this.translated.contains(m.id());
                RoundedRect.of(tbx, by, bs, bs).radius(3.0f).color(on ? accent.withAlpha(0.35f * ma) : Color.rgba(255, 255, 255, Math.round(20.0f * th * ma))).render(graphics);
                Text.of(Fonts.MEDIUM, "\u0410\u0430", tbx + bs * 0.5f, by + bs * 0.5f, 4.4f).centered().color(GuiDraw.mix(Color.rgb(160, 165, 175), Color.rgb(255, 255, 255), Math.max(th, on ? 1.0f : 0.0f)).withAlpha(ma)).render(graphics);
                this.hit(tbx, by, bs, bs, () -> this.toggleTranslate(msg), null);
            }
            float rbx = bx;
            String rk = "globals-act-reply";
            float rh = this.anim(rk, this.hov(rk, this.hovered(rbx, by, bs, bs)), 9.0f);
            RoundedRect.of(rbx, by, bs, bs).radius(3.0f).color(Color.rgba(255, 255, 255, Math.round(20.0f * rh * ma))).render(graphics);
            Color g = GuiDraw.mix(Color.rgb(160, 165, 175), Color.rgb(255, 255, 255), rh).withAlpha(ma);
            float c = bs * 0.5f;
            GuiDraw.stroke(graphics, rbx + c + 2.5f, by + c + 2.0f, rbx + c + 2.5f, by + c, 0.8f, g);
            GuiDraw.stroke(graphics, rbx + c + 2.5f, by + c, rbx + c - 2.5f, by + c, 0.8f, g);
            GuiDraw.stroke(graphics, rbx + c - 2.5f, by + c, rbx + c - 0.8f, by + c - 1.7f, 0.8f, g);
            GuiDraw.stroke(graphics, rbx + c - 2.5f, by + c, rbx + c - 0.8f, by + c + 1.7f, 0.8f, g);
            this.hit(rbx, by, bs, bs, () -> {
                this.replyTo = msg;
                this.focusInput();
            }, null);
            float ebx = bx + bs + 2.0f;
            String ek = "globals-act-react";
            float eh = this.anim(ek, this.hov(ek, this.hovered(ebx, by, bs, bs)), 9.0f);
            RoundedRect.of(ebx, by, bs, bs).radius(3.0f).color(Color.rgba(255, 255, 255, Math.round(20.0f * eh * ma))).render(graphics);
            Identifier tex3 = Emoji.texture(128578);
            if (tex3 != null) {
                Image.of(tex3, ebx + 1.5f, by + 1.5f, bs - 3.0f, bs - 3.0f).alpha(ma).render(graphics);
            }
            float stripY = by;
            this.hit(ebx, by, bs, bs, () -> {
                this.reactFor = msg.id();
                this.reactX = ebx + bs;
                this.reactY = stripY;
                this.reactTime = System.currentTimeMillis();
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
        }
        Scissor.pop(graphics);
        if (this.chatScroll > 4.0f) {
            float bs = 12.0f;
            float bx = x + w - bs - 3.0f;
            float by = y + h - bs - 3.0f;
            boolean hover = this.hovered(bx, by, bs, bs);
            float hv = this.anim("globals-down", this.hov("globals-down", hover), 8.0f);
            RoundedRect.of(bx, by, bs, bs).radius(bs * 0.5f).color(accent.withAlpha((0.55f + 0.35f * hv) * a)).render(graphics);
            Color g = Color.rgb(255, 255, 255).withAlpha(a);
            float c = bs * 0.5f;
            GuiDraw.stroke(graphics, bx + c - 2.5f, by + c - 1.0f, bx + c, by + c + 1.5f, 0.9f, g);
            GuiDraw.stroke(graphics, bx + c, by + c + 1.5f, bx + c + 2.5f, by + c - 1.0f, 0.9f, g);
            this.hit(bx, by, bs, bs, () -> {
                this.chatScroll = 0.0f;
            }, null);
        }
    }

    private void renderReactStrip(GuiGraphicsExtractor graphics, float a, Color accent, long now) {
        float t = GuiDraw.easeOut((float)(now - this.reactTime) / 180.0f);
        int n = GlobalChat.REACTIONS.length;
        float cell = 14.0f;
        float sw = (float)n * cell + 6.0f;
        float sh = cell + 4.0f;
        float sx = this.reactX - sw;
        float sy = this.reactY - sh - 2.0f + (1.0f - t) * 4.0f;
        if (sy < this.messagesBox[1]) {
            sy = this.reactY + 14.0f;
        }
        float pa = a * t;
        this.hit(sx, sy, sw, sh, () -> {}, null);
        RoundedRect.of(sx, sy, sw, sh).radius(sh * 0.5f).color(Color.rgba(18, 19, 24, Math.round(245.0f * pa))).render(graphics);
        Outline.of(sx, sy, sw, sh).radius(sh * 0.5f).thickness(0.5f).color(Color.rgba(255, 255, 255, Math.round(20.0f * pa))).render(graphics);
        GlobalChat.Message m = GlobalChat.byId(this.reactFor);
        String self = CloudFriends.nick();
        for (int i = 0; i < n; ++i) {
            int code = GlobalChat.REACTIONS[i];
            float cx = sx + 3.0f + (float)i * cell;
            float cy = sy + 2.0f;
            String key = "globals-rs-" + code;
            float hv = this.anim(key, this.hov(key, this.hovered(cx, cy, cell, cell)), 10.0f);
            if (m != null && m.reacted(code, self)) {
                RoundedRect.of(cx, cy, cell, cell).radius(cell * 0.5f).color(accent.withAlpha(0.35f * pa)).render(graphics);
            }
            float s = 10.0f + 2.5f * hv;
            Identifier tex = Emoji.texture(code);
            if (tex != null) {
                Image.of(tex, cx + (cell - s) * 0.5f, cy + (cell - s) * 0.5f - hv, s, s).alpha(pa).render(graphics);
            }
            long id = this.reactFor;
            this.hit(cx, cy, cell, cell, () -> {
                GlobalChat.react(id, code);
                this.reactFor = -1L;
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
        }
    }

    private void renderReplyBar(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float a, Color accent) {
        RoundedRect.of(x, y, w, h).radius(3.5f).color(accent.withAlpha(0.12f * a)).render(graphics);
        RoundedRect.of(x, y + 1.5f, 1.2f, h - 3.0f).radius(0.6f).color(accent.withAlpha(a)).render(graphics);
        String text = "\u041e\u0442\u0432\u0435\u0442 " + this.replyTo.nick() + ": " + ProfileView.clean(this.replyTo.text());
        Text.of(Fonts.MEDIUM, GuiDraw.fit(text, 4.8f, w - 22.0f), x + 5.0f, y + h * 0.5f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(200, 204, 214).withAlpha(a)).render(graphics);
        float bs = h - 2.0f;
        float bx = x + w - bs - 2.0f;
        float by = y + 1.0f;
        float hv = this.anim("globals-reply-x", this.hov("globals-reply-x", this.hovered(bx, by, bs, bs)), 9.0f);
        Color g = GuiDraw.mix(Color.rgb(150, 155, 165), Color.rgb(255, 110, 110), hv).withAlpha(a);
        float c = bs * 0.5f;
        GuiDraw.stroke(graphics, bx + c - 2.0f, by + c - 2.0f, bx + c + 2.0f, by + c + 2.0f, 0.8f, g);
        GuiDraw.stroke(graphics, bx + c - 2.0f, by + c + 2.0f, bx + c + 2.0f, by + c - 2.0f, 0.8f, g);
        this.hit(bx, by, bs, bs, () -> {
            this.replyTo = null;
        }, null);
    }

    private void pill(GuiGraphicsExtractor graphics, String key, String label, float x, float y, float w, float h, boolean on, float a, Color accent, Runnable action) {
        float hv = this.anim(key, this.hov(key, this.hovered(x, y, w, h)), 8.0f);
        float t = this.anim(key + "-on", on, 6.0f);
        RoundedRect.of(x, y, w, h).radius(h * 0.5f).color(GuiDraw.mix(Color.rgb(255, 255, 255), accent, t).withAlpha((0.06f + 0.06f * hv + 0.28f * t) * a)).render(graphics);
        Text.of(Fonts.MEDIUM, label, x + w * 0.5f, y + h * 0.5f, 4.6f).centered().color(GuiDraw.mix(Color.rgb(160, 165, 175), Color.rgb(255, 255, 255), Math.max(t, hv)).withAlpha(a)).render(graphics);
        this.hit(x, y, w, h, () -> {
            action.run();
            ClientSound.checkbox(!on);
        }, null);
    }

    private void renderVoice(GuiGraphicsExtractor graphics, GlobalChat.Voice voice, float x, float y, float maxW, float a, Color accent, boolean mine) {
        float bs = 14.0f;
        float by = y + (18.0f - bs) * 0.5f;
        boolean playing = VoiceMessages.isPlaying(voice.id());
        boolean loading = VoiceMessages.isLoading(voice.id());
        String key = "voice-" + voice.id();
        float hv = this.anim(key, this.hov(key, this.hovered(x, by, bs, bs)), 9.0f);
        RoundedRect.of(x, by, bs, bs).radius(bs * 0.5f).horizontalGradient(accent.withAlpha((0.65f + 0.3f * hv) * a), GuiDraw.getAccent2().withAlpha((0.65f + 0.3f * hv) * a)).render(graphics);
        float cx = x + bs * 0.5f;
        float cy = by + bs * 0.5f;
        Color g = Color.rgb(255, 255, 255).withAlpha(a);
        if (playing) {
            RoundedRect.of(cx - 2.2f, cy - 2.5f, 1.4f, 5.0f).radius(0.4f).color(g).render(graphics);
            RoundedRect.of(cx + 0.8f, cy - 2.5f, 1.4f, 5.0f).radius(0.4f).color(g).render(graphics);
        } else if (loading) {
            float t = (float)(System.currentTimeMillis() % 900L) / 900.0f;
            for (int i = 0; i < 3; ++i) {
                float al = 0.3f + 0.7f * (float)Math.max(0.0, Math.sin((double)(t - (float)i * 0.2f) * Math.PI * 2.0));
                RoundedRect.of(cx - 3.0f + (float)i * 2.5f, cy - 0.6f, 1.2f, 1.2f).radius(0.6f).color(g.withAlpha(a * al)).render(graphics);
            }
        } else {
            GuiDraw.stroke(graphics, cx - 1.2f, cy - 2.6f, cx - 1.2f, cy + 2.6f, 0.9f, g);
            GuiDraw.stroke(graphics, cx - 1.2f, cy - 2.6f, cx + 2.4f, cy, 0.9f, g);
            GuiDraw.stroke(graphics, cx - 1.2f, cy + 2.6f, cx + 2.4f, cy, 0.9f, g);
        }
        String id = voice.id();
        this.hit(x, by, bs, bs, () -> VoiceMessages.toggle(id), null);
        int[] wave = voice.wave();
        int bars = wave.length == 0 ? 30 : wave.length;
        float barW = 1.6f;
        float gap = 1.0f;
        float waveW = Math.min(maxW - bs - 40.0f, (float)bars * (barW + gap));
        float step = waveW / (float)bars;
        float wx = x + bs + 5.0f;
        float progress = VoiceMessages.progress(id);
        for (int i = 0; i < bars; ++i) {
            int v = wave.length == 0 ? 3 : wave[i];
            float bh = 2.0f + (float)v * 0.6f;
            boolean done = playing && ((float)i + 0.5f) / (float)bars <= progress;
            RoundedRect.of(wx + (float)i * step, cy - bh * 0.5f, Math.max(1.0f, step - gap), bh).radius(0.6f).color(done ? accent.withAlpha(a) : Color.rgba(255, 255, 255, Math.round(70.0f * a))).render(graphics);
        }
        float shown = playing ? voice.dur() * progress : voice.dur();
        int sec = Math.round(shown);
        String dur = sec / 60 + ":" + String.format("%02d", sec % 60);
        Text.of(Fonts.MEDIUM, dur, wx + waveW + 5.0f, cy, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(150, 155, 165).withAlpha(a)).render(graphics);
    }

    private void renderMic(GuiGraphicsExtractor graphics, float cx, float cy, Color g) {
        RoundedRect.of(cx - 1.4f, cy - 3.2f, 2.8f, 4.4f).radius(1.4f).color(g).render(graphics);
        GuiDraw.stroke(graphics, cx - 2.6f, cy - 0.4f, cx - 2.6f, cy + 0.4f, 0.7f, g);
        GuiDraw.stroke(graphics, cx + 2.6f, cy - 0.4f, cx + 2.6f, cy + 0.4f, 0.7f, g);
        GuiDraw.stroke(graphics, cx - 2.6f, cy + 0.4f, cx, cy + 2.4f, 0.7f, g);
        GuiDraw.stroke(graphics, cx + 2.6f, cy + 0.4f, cx, cy + 2.4f, 0.7f, g);
        GuiDraw.stroke(graphics, cx, cy + 2.4f, cx, cy + 3.6f, 0.7f, g);
    }

    private static Color nickColor(String nick) {
        int hash = nick.toLowerCase(Locale.ROOT).hashCode();
        return Color.hsb((float)(hash & 0xFFFF) / 65535.0f, 0.45f, 0.85f);
    }

    private void renderInput(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float a, Color accent, long now) {
        boolean canSend;
        List<List<Piece>> lines;
        boolean focused = CHAT_FIELD.equals(this.ctx.focus.abField);
        float eb = h;
        float sendW = h;
        float fx = x + eb + 3.0f;
        float fw = w - eb - sendW - 6.0f;
        boolean eHover = this.hovered(x, y, eb, eb);
        float ehv = this.anim("globals-emoji-btn", this.hov("globals-emoji-btn", eHover), 8.0f);
        float eo = this.anim("globals-emoji-open", this.pickerOpen, 7.0f);
        RoundedRect.of(x, y, eb, eb).radius(4.0f).color(GuiDraw.mix(Color.rgb(255, 255, 255), accent, eo).withAlpha((0.05f + 0.06f * ehv + 0.25f * eo) * a)).render(graphics);
        Identifier smile = Emoji.texture(128522);
        float es = 9.0f + ehv * 1.0f;
        if (smile != null) {
            Image.of(smile, x + (eb - es) * 0.5f, y + (eb - es) * 0.5f, es, es).alpha(a).render(graphics);
        }
        this.hit(x, y, eb, eb, () -> {
            this.pickerOpen = !this.pickerOpen;
            this.pickerTime = System.currentTimeMillis();
            ClientSound.ui(ClientSound.Ui.CLICK);
        }, null);
        boolean recording = VoiceMessages.isRecording();
        boolean uploading = VoiceMessages.isUploading();
        if (recording || uploading) {
            RoundedRect.of(fx, y, fw, h).radius(4.0f).color(Color.rgba(255, 70, 70, Math.round(18.0f * a))).render(graphics);
            float cyy = y + h * 0.5f;
            float pulse = 0.5f + 0.5f * (float)Math.sin((double)now / 180.0);
            RoundedRect.of(fx + 6.0f, cyy - 2.0f, 4.0f, 4.0f).radius(2.0f).color(Color.rgb(255, 80, 80).withAlpha(a * (recording ? 0.5f + 0.5f * pulse : 0.4f))).render(graphics);
            float secs = VoiceMessages.recordSeconds();
            String label = uploading ? "\u041e\u0442\u043f\u0440\u0430\u0432\u043a\u0430..." : "\u0417\u0430\u043f\u0438\u0441\u044c  " + (int)secs / 60 + ":" + String.format("%02d", (int)secs % 60) + "  /  0:" + String.format("%02d", 15);
            Text.of(Fonts.MEDIUM, label, fx + 14.0f, cyy, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(a)).render(graphics);
            if (recording) {
                float lvlX = fx + 14.0f + Text.of(Fonts.MEDIUM, label).size(5.0f).width() + 8.0f;
                float lvlW = Math.max(10.0f, fx + fw - 40.0f - lvlX);
                RoundedRect.of(lvlX, cyy - 1.0f, lvlW, 2.0f).radius(1.0f).color(Color.rgba(255, 255, 255, Math.round(20.0f * a))).render(graphics);
                RoundedRect.of(lvlX, cyy - 1.0f, lvlW * VoiceMessages.level(), 2.0f).radius(1.0f).color(accent.withAlpha(a)).render(graphics);
                float cxw = 30.0f;
                float cxx = fx + fw - cxw - 3.0f;
                float chv = this.anim("voice-cancel", this.hov("voice-cancel", this.hovered(cxx, y + 2.0f, cxw, h - 4.0f)), 8.0f);
                Text.of(Fonts.MEDIUM, "\u041e\u0442\u043c\u0435\u043d\u0430", cxx + cxw * 0.5f, cyy, 4.8f).centered().color(GuiDraw.mix(Color.rgb(170, 175, 186), Color.rgb(255, 120, 120), chv).withAlpha(a)).render(graphics);
                this.hit(cxx, y, cxw, h, VoiceMessages::cancelRecording, null);
            }
            float sx = fx + fw + 3.0f;
            float shv = this.anim("globals-send", this.hov("globals-send", this.hovered(sx, y, sendW, h)), 8.0f);
            RoundedRect.of(sx, y, sendW, h).radius(4.0f).horizontalGradient(accent.withAlpha((0.6f + 0.3f * shv) * a), GuiDraw.getAccent2().withAlpha((0.6f + 0.3f * shv) * a)).render(graphics);
            Color sg = Color.rgb(255, 255, 255).withAlpha(a);
            float sc = sx + sendW * 0.5f;
            GuiDraw.stroke(graphics, sc - 2.5f, cyy, sc + 2.5f, cyy, 0.9f, sg);
            GuiDraw.stroke(graphics, sc + 0.3f, cyy - 2.2f, sc + 2.5f, cyy, 0.9f, sg);
            GuiDraw.stroke(graphics, sc + 0.3f, cyy + 2.2f, sc + 2.5f, cyy, 0.9f, sg);
            if (recording) {
                this.hit(sx, y, sendW, h, VoiceMessages::stopRecording, null);
            }
            return;
        }
        boolean hover = this.hovered(fx, y, fw, h);
        float hv = this.anim("globals-input", this.hov("globals-input", hover), 7.0f);
        float fc = this.anim("globals-input-focus", focused, 6.0f);
        RoundedRect.of(fx, y, fw, h).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round((8.0f + 4.0f * hv + 6.0f * fc) * a))).render(graphics);
        if (fc > 0.01f) {
            Outline.of(fx, y, fw, h).radius(4.0f).thickness(0.7f).color(accent.withAlpha(0.6f * fc * a)).render(graphics);
        }
        String text = focused ? this.ctx.focus.abInput.toString() : this.draft;
        float tx = fx + 5.0f;
        float cy = y + h * 0.5f;
        float maxW = fw - 10.0f;
        if (text.isEmpty()) {
            Text.of(Fonts.MEDIUM, CloudFriends.nick() == null ? "\u0412\u043e\u0439\u0434\u0438 \u0432 \u0430\u043a\u043a\u0430\u0443\u043d\u0442 Brokke, \u0447\u0442\u043e\u0431\u044b \u043f\u0438\u0441\u0430\u0442\u044c" : "\u0421\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435...", tx, cy, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(120, 125, 136).withAlpha(a)).render(graphics);
        }
        List<Piece> pieces = (lines = this.wrap(text, 100000.0f)).isEmpty() ? List.of() : lines.get(0);
        float full = 0.0f;
        if (!pieces.isEmpty()) {
            Piece last = pieces.get(pieces.size() - 1);
            full = last.x() + (last.emoji() >= 0 ? 14.0f : GlobalsTab.width(last.text()));
        }
        if (text.endsWith(" ")) {
            full = Math.max(full, GlobalsTab.width(text.substring(text.stripTrailing().length())) + full);
        }
        float shift = Math.max(0.0f, full - maxW);
        if (focused && this.ctx.focus.abSelectedAll && !text.isEmpty()) {
            RoundedRect.of(tx - 1.0f, y + 3.0f, Math.min(full, maxW) + 2.0f, h - 6.0f).radius(1.5f).color(accent.withAlpha(0.3f * a)).render(graphics);
        }
        Scissor.push(graphics, tx - 1.0f, y, maxW + 2.0f, h);
        for (Piece p : pieces) {
            float px = tx + p.x() - shift;
            if (p.emoji() >= 0) {
                Identifier tex = Emoji.texture(p.emoji());
                if (tex == null) continue;
                Image.of(tex, px, cy - 7.0f, 14.0f, 14.0f).alpha(a).render(graphics);
                continue;
            }
            Text.of(Fonts.MEDIUM, p.text(), px, cy, 6.4f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(235, 238, 245).withAlpha(a)).render(graphics);
        }
        Scissor.pop(graphics);
        if (focused) {
            float blink = 0.5f + 0.5f * (float)Math.cos((double)now / 170.0);
            RoundedRect.of(tx + Math.min(full, maxW) + 0.5f, y + 4.0f, 0.6f, h - 8.0f).color(Color.rgb(235, 238, 245).withAlpha(a * blink)).render(graphics);
        }
        boolean voiceErr = VoiceMessages.errorTime() > GlobalChat.errorTime();
        float err = 1.0f - GuiDraw.easeOut((float)(now - Math.max(GlobalChat.errorTime(), VoiceMessages.errorTime())) / 1800.0f);
        if (err > 0.01f) {
            RoundedRect.of(fx, y, fw, h).radius(4.0f).color(Color.rgba(255, 70, 70, Math.round(35.0f * err * a))).render(graphics);
            Text.of(Fonts.MEDIUM, voiceErr ? VoiceMessages.error() : GlobalChat.error(), fx + fw - 4.0f, cy, 4.6f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(255, 140, 140).withAlpha(a * err)).render(graphics);
        }
        this.hit(fx, y, fw, h, this::focusInput, () -> {
            if (focused) {
                this.ctx.focus.clearAbText();
            }
            this.draft = "";
        });
        float sx = fx + fw + 3.0f;
        boolean sHover = this.hovered(sx, y, sendW, h);
        float shv = this.anim("globals-send", this.hov("globals-send", sHover), 8.0f);
        boolean bl = canSend = !text.isBlank();
        if (!canSend) {
            RoundedRect.of(sx, y, sendW, h).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round((10.0f + 14.0f * shv) * a))).render(graphics);
            this.renderMic(graphics, sx + sendW * 0.5f, cy, GuiDraw.mix(Color.rgb(170, 175, 186), Color.rgb(255, 255, 255), shv).withAlpha(a));
            this.hit(sx, y, sendW, h, () -> {
                VoiceMessages.startRecording();
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
            return;
        }
        RoundedRect.of(sx, y, sendW, h).radius(4.0f).horizontalGradient(accent.withAlpha((0.6f + 0.3f * shv) * a), GuiDraw.getAccent2().withAlpha((0.6f + 0.3f * shv) * a)).render(graphics);
        Color sg = Color.rgb(255, 255, 255).withAlpha(a);
        float sc = sx + sendW * 0.5f;
        GuiDraw.stroke(graphics, sc - 2.5f, cy, sc + 2.5f, cy, 0.9f, sg);
        GuiDraw.stroke(graphics, sc + 0.3f, cy - 2.2f, sc + 2.5f, cy, 0.9f, sg);
        GuiDraw.stroke(graphics, sc + 0.3f, cy + 2.2f, sc + 2.5f, cy, 0.9f, sg);
        this.hit(sx, y, sendW, h, this::sendChat, null);
    }

    private void renderPicker(GuiGraphicsExtractor graphics, float x, float bottomY, float w, float a, Color accent, long now) {
        float t = GuiDraw.easeOut((float)(now - this.pickerTime) / 220.0f);
        int cols = 9;
        float cell = 20.0f;
        float pad = 5.0f;
        float pw = (float)cols * cell + pad * 2.0f;
        float headH = 15.0f;
        Emoji.Group group = Emoji.GROUPS.get(this.pickerGroup);
        int rows = 5;
        float ph = headH + (float)rows * cell + pad * 2.0f;
        float px = x;
        float py = bottomY - ph + (1.0f - t) * 6.0f;
        float pa = a * t;
        this.hit(px, py, pw, ph, () -> {}, null);
        RoundedRect.of(px, py, pw, ph).radius(6.0f).color(Color.rgba(18, 19, 24, Math.round(240.0f * pa))).render(graphics);
        Outline.of(px, py, pw, ph).radius(6.0f).thickness(0.5f).color(Color.rgba(255, 255, 255, Math.round(18.0f * pa))).render(graphics);
        float gw = (pw - pad * 2.0f) / (float)Emoji.GROUPS.size();
        for (int i = 0; i < Emoji.GROUPS.size(); ++i) {
            Identifier tex;
            int idx = i;
            float gx = px + pad + (float)i * gw;
            float gy = py + pad - 1.0f;
            boolean sel = i == this.pickerGroup;
            boolean hover = this.hovered(gx, gy, gw, 12.0f);
            float hv = this.anim("globals-pg-" + i, this.hov("globals-pg-" + i, hover), 8.0f);
            if (sel || hv > 0.01f) {
                RoundedRect.of(gx + 1.0f, gy, gw - 2.0f, 12.0f).radius(3.5f).color(sel ? accent.withAlpha(0.35f * pa) : Color.rgba(255, 255, 255, Math.round(12.0f * hv * pa))).render(graphics);
            }
            if ((tex = Emoji.texture(Emoji.GROUPS.get(i).icon())) != null) {
                Image.of(tex, gx + gw * 0.5f - 4.0f, gy + 2.0f, 8.0f, 8.0f).alpha(pa).render(graphics);
            }
            this.hit(gx, gy, gw, 12.0f, () -> {
                this.pickerGroup = idx;
                ClientSound.ui(ClientSound.Ui.CLICK);
            }, null);
        }
        int[] codes = group.codes();
        float gy0 = py + headH + pad - 2.0f;
        for (int i = 0; i < codes.length && i < cols * rows; ++i) {
            int code = codes[i];
            float cx = px + pad + (float)(i % cols) * cell;
            float cy = gy0 + (float)(i / cols) * cell;
            boolean hover = this.hovered(cx, cy, cell, cell);
            float hv = this.anim("globals-e-" + code, this.hov("globals-e-" + code, hover), 10.0f);
            if (hv > 0.01f) {
                RoundedRect.of(cx, cy, cell, cell).radius(3.5f).color(Color.rgba(255, 255, 255, Math.round(18.0f * hv * pa))).render(graphics);
            }
            float s = 16.0f + 2.0f * hv;
            Identifier tex = Emoji.texture(code);
            if (tex != null) {
                Image.of(tex, cx + (cell - s) * 0.5f, cy + (cell - s) * 0.5f, s, s).alpha(pa).render(graphics);
            }
            this.hit(cx, cy, cell, cell, () -> this.insertEmoji(code), null);
        }
    }

    private void insertEmoji(int code) {
        this.focusInput();
        this.ctx.focus.appendAbText(Emoji.string(code));
    }

    public void focusInput() {
        if (!CHAT_FIELD.equals(this.ctx.focus.abField)) {
            this.ctx.focus.setAbField(CHAT_FIELD, this.draft);
            ClientSound.ui(ClientSound.Ui.CLICK);
        }
    }

    public void sendChat() {
        String text;
        boolean focused = CHAT_FIELD.equals(this.ctx.focus.abField);
        String string = text = focused ? this.ctx.focus.abInput.toString() : this.draft;
        if (text.isBlank()) {
            return;
        }
        GlobalChat.send(text, this.replyTo != null ? this.replyTo.id() : 0L);
        this.replyTo = null;
        this.draft = "";
        if (focused) {
            this.ctx.focus.clearAbText();
        }
        this.chatScroll = 0.0f;
        this.pickerOpen = false;
        ClientSound.ui(ClientSound.Ui.CLICK);
    }

    public boolean scrollChat(double amount) {
        if (this.tab != 0) {
            return false;
        }
        this.reactFor = -1L;
        float mx = this.getMouseX();
        float my = this.getMouseY();
        if (mx < this.messagesBox[0] || mx > this.messagesBox[0] + this.messagesBox[2] || my < this.messagesBox[1] || my > this.messagesBox[1] + this.messagesBox[3]) {
            return false;
        }
        this.chatScroll = Math.clamp(this.chatScroll + (float)amount * 20.0f, 0.0f, this.chatMaxScroll);
        return true;
    }

    private record Piece(String text, int emoji, float x) {
    }
}

