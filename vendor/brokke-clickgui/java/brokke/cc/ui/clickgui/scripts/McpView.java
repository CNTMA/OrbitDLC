/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.scripts;

import brokke.cc.system.scripting.mcp.McpClients;
import brokke.cc.system.scripting.mcp.McpServer;
import brokke.cc.system.scripting.mcp.McpSetup;
import brokke.cc.system.scripting.runtime.ScriptConsole;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class McpView {
    private static final float LEFT_W = 122.0f;
    private static final float CARD_H = 27.0f;
    private static final Color GREEN = Color.rgb(46, 204, 113);
    private static final Color RED = Color.rgb(235, 87, 87);
    private static final Color GRAY = Color.rgb(105, 110, 125);
    private final Runnable onBack;
    private final List<float[]> boxes = new ArrayList<float[]>();
    private final List<Runnable> actions = new ArrayList<Runnable>();
    private final Map<McpSetup.Kind, State> states = new HashMap<McpSetup.Kind, State>();
    private final Map<McpSetup.Kind, String> messages = new HashMap<McpSetup.Kind, String>();
    private final Map<McpSetup.Kind, Long> messageTime = new HashMap<McpSetup.Kind, Long>();
    private long lastScan;
    private String lastUrl = "";
    private float mouseX;
    private float mouseY;
    private float scroll;
    private float scrollTarget;
    private float maxScroll;
    private float listX;
    private float listY;
    private float listW;
    private float listH;
    private boolean showAdvanced;
    private String banner = "";
    private boolean bannerOk;
    private long bannerTime;

    public McpView(Runnable onBack) {
        this.onBack = onBack;
    }

    public void reset() {
        this.scrollTarget = 0.0f;
        this.scroll = 0.0f;
        this.lastScan = 0L;
    }

    private void scan(boolean force) {
        String url = McpServer.get().url();
        long now = System.currentTimeMillis();
        if (!force && now - this.lastScan < 2000L && url.equals(this.lastUrl)) {
            return;
        }
        this.lastScan = now;
        this.lastUrl = url;
        for (McpClients.Client c : McpClients.ALL) {
            if (c.kind() == McpSetup.Kind.MANUAL) continue;
            State s = McpSetup.configured(c.kind(), url) ? State.CONNECTED : (McpSetup.detected(c.kind()) ? State.FOUND : State.MISSING);
            this.states.put(c.kind(), s);
        }
    }

    private boolean hover(float x, float y, float w, float h) {
        return this.mouseX >= x && this.mouseX <= x + w && this.mouseY >= y && this.mouseY <= y + h;
    }

    private void box(float x, float y, float w, float h, Runnable r) {
        this.boxes.add(new float[]{x, y, w, h});
        this.actions.add(r);
    }

    private static float width(String s, float size) {
        return Text.of(Fonts.MEDIUM, s).size(size).width();
    }

    private static String fit(String s, float size, float maxW) {
        if (McpView.width(s, size) <= maxW) {
            return s;
        }
        String cut = s;
        while (cut.length() > 1 && McpView.width(cut + "..", size) > maxW) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "..";
    }

    private static List<String> wrap(String text, float size, float maxW) {
        ArrayList<String> out = new ArrayList<String>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate;
            String string = candidate = line.isEmpty() ? word : String.valueOf(line) + " " + word;
            if (McpView.width(candidate, size) > maxW && !line.isEmpty()) {
                out.add(line.toString());
                line = new StringBuilder(word);
                continue;
            }
            line = new StringBuilder(candidate);
        }
        if (!line.isEmpty()) {
            out.add(line.toString());
        }
        return out;
    }

    private static void card(GuiGraphicsExtractor g, float x, float y, float w, float h) {
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(12, 12, 18, 90)).render(g);
        Outline.of(x, y, w, h).radius(5.0f).thickness(0.7f).color(Color.rgba(255, 255, 255, 10)).render(g);
    }

    private void button(GuiGraphicsExtractor g, String label, float x, float y, float w, float h, Color base, float size, Runnable action) {
        boolean hov = this.hover(x, y, w, h);
        RoundedRect.of(x, y, w, h).radius(2.5f).color(hov ? base.brighter(0.12f) : base).render(g);
        Text.of(Fonts.MEDIUM, label, x + w * 0.5f, y + h * 0.5f, size).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(g);
        if (action != null) {
            this.box(x, y, w, h, action);
        }
    }

    private static void copy(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }

    private void ensureRunning() {
        if (!McpServer.get().isRunning()) {
            McpServer.get().start(false);
        }
    }

    private void setup(McpClients.Client c) {
        this.ensureRunning();
        if (!McpServer.get().isRunning()) {
            this.message(c.kind(), "\u0421\u0435\u0440\u0432\u0435\u0440 \u043d\u0435 \u0437\u0430\u043f\u0443\u0441\u0442\u0438\u043b\u0441\u044f - \u043d\u0430\u0436\u043c\u0438 \u0421\u043c\u0435\u043d\u0438\u0442\u044c \u043f\u043e\u0440\u0442");
            return;
        }
        if (c.kind() == McpSetup.Kind.MANUAL) {
            McpView.copy(McpServer.get().url());
            this.message(c.kind(), "\u0421\u0441\u044b\u043b\u043a\u0430 \u0441\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u043d\u0430 - \u0432\u0441\u0442\u0430\u0432\u044c \u0435\u0451 \u0432 \u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 MCP");
            return;
        }
        McpSetup.Result r = McpSetup.configure(c.kind(), McpServer.get().url());
        this.message(c.kind(), (String)(r.ok() ? "\u0413\u043e\u0442\u043e\u0432\u043e! " + r.message() : r.message()));
        this.scan(true);
    }

    private void setupAll() {
        this.ensureRunning();
        if (!McpServer.get().isRunning()) {
            this.showBanner("\u0421\u0435\u0440\u0432\u0435\u0440 \u043d\u0435 \u0437\u0430\u043f\u0443\u0441\u0442\u0438\u043b\u0441\u044f - \u043d\u0430\u0436\u043c\u0438 \u0421\u043c\u0435\u043d\u0438\u0442\u044c \u043f\u043e\u0440\u0442", false);
            return;
        }
        this.scan(true);
        int done = 0;
        ArrayList<String> names = new ArrayList<String>();
        for (McpClients.Client c : McpClients.ALL) {
            McpSetup.Result r;
            if (c.kind() == McpSetup.Kind.MANUAL || this.states.get(c.kind()) == State.MISSING || !(r = McpSetup.configure(c.kind(), McpServer.get().url())).ok()) continue;
            ++done;
            names.add(c.name());
            this.message(c.kind(), "\u0413\u043e\u0442\u043e\u0432\u043e! " + r.message());
        }
        this.scan(true);
        if (done == 0) {
            this.showBanner("\u041d\u0435 \u043d\u0430\u0448\u0451\u043b \u043d\u0438 \u043e\u0434\u043d\u043e\u0439 \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u044b. \u0423\u0441\u0442\u0430\u043d\u043e\u0432\u0438 Codex, Claude Code \u0438\u043b\u0438 \u0434\u0440\u0443\u0433\u0443\u044e \u0438\u0437 \u0441\u043f\u0438\u0441\u043a\u0430", false);
        } else {
            this.showBanner("\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u043e: " + String.join(", ", names) + ". \u041f\u0435\u0440\u0435\u0437\u0430\u043f\u0443\u0441\u0442\u0438 \u044d\u0442\u0438 \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u044b", true);
            ScriptConsole.info("mcp", "\u0430\u0432\u0442\u043e\u043d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0430: " + String.join(", ", names));
        }
    }

    private void message(McpSetup.Kind kind, String text) {
        this.messages.put(kind, text);
        this.messageTime.put(kind, System.currentTimeMillis());
    }

    private void showBanner(String text, boolean ok) {
        this.banner = text;
        this.bannerOk = ok;
        this.bannerTime = System.currentTimeMillis();
    }

    public void render(GuiGraphicsExtractor g, float x, float y, float w, float h, float mx, float my, Color accent, float delta) {
        this.mouseX = mx;
        this.mouseY = my;
        this.boxes.clear();
        this.actions.clear();
        this.scan(false);
        McpServer mcp = McpServer.get();
        boolean running = mcp.isRunning();
        float headH = 20.0f;
        McpView.card(g, x, y, w, headH);
        RoundedRect.of(x + 6.0f, y + 6.0f, 2.0f, 8.0f).radius(1.0f).color(accent).render(g);
        Text.of(Fonts.MEDIUM, "\u0418\u0418 \u043f\u0438\u0448\u0435\u0442 \u0441\u043a\u0440\u0438\u043f\u0442\u044b \u0437\u0430 \u0442\u0435\u0431\u044f", x + 12.0f, y + headH * 0.5f, 7.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(g);
        this.button(g, "<- \u041d\u0430\u0437\u0430\u0434", x + w - 46.0f, y + 4.5f, 42.0f, 11.0f, accent, 5.3f, this.onBack);
        float bodyY = y + headH + 5.0f;
        float bodyH = h - headH - 5.0f;
        this.renderGuide(g, x, bodyY, 122.0f, bodyH, accent, running);
        this.renderClients(g, x + 122.0f + 5.0f, bodyY, w - 122.0f - 5.0f, bodyH, accent, delta);
    }

    private float step(GuiGraphicsExtractor g, int num, String title, String text, float x, float y, float w, Color accent, boolean done) {
        RoundedRect.of(x, y, 11.0f, 11.0f).radius(5.5f).color(done ? GREEN : accent).render(g);
        Text.of(Fonts.MEDIUM, done ? "v" : String.valueOf(num), x + 5.5f, y + 5.5f, 5.6f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(g);
        Text.of(Fonts.MEDIUM, title, x + 15.0f, y + 5.5f, 6.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(g);
        float cy = y + 12.5f;
        for (String line : McpView.wrap(text, 4.9f, w - 15.0f)) {
            Text.of(Fonts.MEDIUM, line, x + 15.0f, cy + 3.0f, 4.9f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(150, 155, 170)).render(g);
            cy += 6.2f;
        }
        return cy + 3.0f;
    }

    private void renderGuide(GuiGraphicsExtractor g, float x, float y, float w, float h, Color accent, boolean running) {
        List<String> lines;
        float bhh;
        McpView.card(g, x, y, w, h);
        float px = x + 7.0f;
        float pw = w - 14.0f;
        float cy = y + 7.0f;
        boolean anyConnected = this.states.containsValue(State.CONNECTED);
        boolean anyFound = anyConnected || this.states.containsValue(State.FOUND);
        cy = this.step(g, 1, "\u0423\u0441\u0442\u0430\u043d\u043e\u0432\u0438 \u0418\u0418-\u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0443", "\u041d\u0430\u043f\u0440\u0438\u043c\u0435\u0440 Codex \u0438\u043b\u0438 Claude Code. \u0421\u043f\u0440\u0430\u0432\u0430 \u0432\u0438\u0434\u043d\u043e, \u0447\u0442\u043e \u0443\u0436\u0435 \u0441\u0442\u043e\u0438\u0442 \u043d\u0430 \u041f\u041a.", px, cy, pw, accent, anyFound);
        cy = this.step(g, 2, "\u041f\u043e\u0434\u043a\u043b\u044e\u0447\u0438", "\u041d\u0430\u0436\u043c\u0438 \u041d\u0430\u0441\u0442\u0440\u043e\u0438\u0442\u044c \u0443 \u043d\u0443\u0436\u043d\u043e\u0439 \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u044b \u0441\u043f\u0440\u0430\u0432\u0430 \u0438\u043b\u0438 \u0441\u0440\u0430\u0437\u0443 \u0443 \u0432\u0441\u0435\u0445.", px, cy, pw, accent, anyConnected);
        float bh = 13.0f;
        boolean hov = this.hover(px, cy, pw, bh);
        RoundedRect.of(px, cy, pw, bh).radius(3.5f).horizontalGradient(hov ? accent.brighter(0.15f) : accent, accent.darker(0.15f)).render(g);
        Text.of(Fonts.MEDIUM, "\u041d\u0430\u0441\u0442\u0440\u043e\u0438\u0442\u044c \u0432\u0441\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043d\u044b\u0435", px + pw * 0.5f, cy + bh * 0.5f, 5.4f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(g);
        this.box(px, cy, pw, bh, this::setupAll);
        cy += bh + 3.0f;
        if (anyConnected) {
            this.button(g, "\u041e\u0442\u043a\u043b\u044e\u0447\u0438\u0442\u044c \u043e\u0442 \u0432\u0441\u0435\u0445 \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c", px, cy, pw, 10.0f, Color.rgba(235, 75, 75, 110), 5.0f, () -> {
                int n = McpSetup.removeAll();
                this.scan(true);
                this.showBanner((String)(n > 0 ? "\u041e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u043e \u043e\u0442 " + n + " \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c. \u041f\u0435\u0440\u0435\u0437\u0430\u043f\u0443\u0441\u0442\u0438 \u0438\u0445" : "\u041d\u0438\u0447\u0435\u0433\u043e \u043d\u0435 \u0431\u044b\u043b\u043e \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u043e"), n > 0);
            });
            cy += 13.0f;
        }
        cy += 2.0f;
        cy = this.step(g, 3, "\u041f\u043e\u043f\u0440\u043e\u0441\u0438 \u0418\u0418", "\u041f\u0435\u0440\u0435\u0437\u0430\u043f\u0443\u0441\u0442\u0438 \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0443 \u0438 \u043d\u0430\u043f\u0438\u0448\u0438 \u0432 \u0435\u0451 \u0447\u0430\u0442\u0435: \"\u0441\u0434\u0435\u043b\u0430\u0439 \u0441\u043a\u0440\u0438\u043f\u0442 \u0434\u043b\u044f brokke, \u043a\u043e\u0442\u043e\u0440\u044b\u0439...\". \u0421\u043a\u0440\u0438\u043f\u0442 \u0441\u0430\u043c \u043f\u043e\u044f\u0432\u0438\u0442\u0441\u044f \u0432 \u0441\u043f\u0438\u0441\u043a\u0435.", px, cy, pw, accent, false);
        if (!this.banner.isEmpty() && System.currentTimeMillis() - this.bannerTime < 8000L && cy + (bhh = (float)(lines = McpView.wrap(this.banner, 4.9f, pw - 8.0f)).size() * 6.2f + 6.0f) < y + h - 30.0f) {
            RoundedRect.of(px, cy, pw, bhh).radius(3.0f).color(this.bannerOk ? Color.rgba(46, 204, 113, 40) : Color.rgba(235, 87, 87, 40)).render(g);
            float ly = cy + 3.0f;
            for (String l : lines) {
                Text.of(Fonts.MEDIUM, l, px + 4.0f, ly + 3.0f, 4.9f).verticalAlign(VerticalAlign.MIDDLE).color(this.bannerOk ? GREEN.brighter(0.2f) : RED.brighter(0.2f)).render(g);
                ly += 6.2f;
            }
        }
        float footY = y + h - 26.0f;
        RoundedRect.of(px, footY, pw, 1.0f).color(Color.rgba(255, 255, 255, 8)).render(g);
        Color stateCol = running ? GREEN : RED;
        RoundedRect.of(px, footY + 6.0f, 4.0f, 4.0f).radius(2.0f).color(stateCol).render(g);
        Text.of(Fonts.MEDIUM, running ? "\u0421\u0435\u0440\u0432\u0435\u0440 \u0440\u0430\u0431\u043e\u0442\u0430\u0435\u0442" : "\u0421\u0435\u0440\u0432\u0435\u0440 \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d", px + 7.0f, footY + 8.0f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(170, 175, 190)).render(g);
        String more = this.showAdvanced ? "\u0441\u043a\u0440\u044b\u0442\u044c" : "\u0435\u0449\u0451";
        float moreW = McpView.width(more, 4.8f);
        boolean moreHov = this.hover(px + pw - moreW - 2.0f, footY + 4.0f, moreW + 4.0f, 8.0f);
        Text.of(Fonts.MEDIUM, more, px + pw, footY + 8.0f, 4.8f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(moreHov ? Color.WHITE : accent).render(g);
        this.box(px + pw - moreW - 2.0f, footY + 4.0f, moreW + 4.0f, 8.0f, () -> {
            this.showAdvanced = !this.showAdvanced;
        });
        float by = footY + 13.0f;
        float third = (pw - 6.0f) / 3.0f;
        if (this.showAdvanced) {
            this.button(g, running ? "\u0421\u0442\u043e\u043f" : "\u0421\u0442\u0430\u0440\u0442", px, by, third, 9.5f, running ? Color.rgba(235, 75, 75, 140) : Color.rgba(46, 170, 100, 170), 4.8f, () -> McpServer.get().toggle());
            this.button(g, "\u041d\u043e\u0432\u044b\u0439 \u0442\u043e\u043a\u0435\u043d", px + third + 3.0f, by, third, 9.5f, Color.rgba(255, 255, 255, 18), 4.8f, () -> {
                McpServer.get().regenerateToken();
                McpSetup.resyncAll(McpServer.get().url());
                this.scan(true);
                this.showBanner("\u0422\u043e\u043a\u0435\u043d \u043e\u0431\u043d\u043e\u0432\u043b\u0451\u043d, \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u044b \u043f\u0435\u0440\u0435\u043d\u0430\u0441\u0442\u0440\u043e\u0435\u043d\u044b", true);
            });
            this.button(g, "\u041f\u043e\u0440\u0442", px + 2.0f * (third + 3.0f), by, third, 9.5f, Color.rgba(255, 255, 255, 18), 4.8f, () -> {
                McpServer.get().changePort();
                McpSetup.resyncAll(McpServer.get().url());
                this.scan(true);
                this.showBanner("\u041f\u043e\u0440\u0442 \u0441\u043c\u0435\u043d\u0451\u043d: " + McpServer.get().port(), true);
            });
        } else {
            String client = McpServer.get().lastClient().isEmpty() ? "\u0435\u0449\u0451 \u043d\u0438\u043a\u0442\u043e \u043d\u0435 \u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0430\u043b\u0441\u044f" : "\u043f\u043e\u0441\u043b\u0435\u0434\u043d\u0438\u0439: " + McpServer.get().lastClient();
            Text.of(Fonts.MEDIUM, McpView.fit(client, 4.8f, pw), px, by + 4.75f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(GRAY).render(g);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderClients(GuiGraphicsExtractor g, float x, float y, float w, float h, Color accent, float delta) {
        McpView.card(g, x, y, w, h);
        Text.of(Fonts.MEDIUM, "\u041f\u0420\u041e\u0413\u0420\u0410\u041c\u041c\u042b \u041d\u0410 \u0422\u0412\u041e\u0401\u041c \u041f\u041a", x + 7.0f, y + 7.5f, 4.8f).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(100, 105, 120)).render(g);
        this.listX = x;
        this.listY = y + 14.0f;
        this.listW = w;
        this.listH = h - 16.0f;
        ArrayList<McpClients.Client> clients = new ArrayList<McpClients.Client>(McpClients.ALL);
        clients.sort((a, b) -> Integer.compare(this.order((McpClients.Client)a), this.order((McpClients.Client)b)));
        this.maxScroll = Math.max(0.0f, (float)clients.size() * 30.0f - this.listH + 2.0f);
        this.scrollTarget = Math.clamp(this.scrollTarget, 0.0f, this.maxScroll);
        this.scroll += (this.scrollTarget - this.scroll) * Math.min(1.0f, delta * 16.0f);
        Scissor.push(g, this.listX, this.listY, this.listW, this.listH);
        try {
            for (int i = 0; i < clients.size(); ++i) {
                McpClients.Client c = (McpClients.Client)clients.get(i);
                float iy = this.listY + (float)i * 30.0f - this.scroll;
                if (iy + 27.0f < this.listY || iy > this.listY + this.listH) continue;
                this.renderClient(g, c, x + 5.0f, iy, w - 12.0f, accent);
            }
        }
        finally {
            Scissor.pop(g);
        }
        if (this.maxScroll > 1.0f) {
            float thumbH = Math.max(14.0f, this.listH / (this.listH + this.maxScroll) * this.listH);
            float thumbY = this.listY + Math.clamp(this.scroll / this.maxScroll, 0.0f, 1.0f) * (this.listH - thumbH);
            RoundedRect.of(x + w - 3.5f, thumbY, 2.0f, thumbH).radius(1.0f).color(accent.withAlpha(130)).render(g);
        }
    }

    private int order(McpClients.Client c) {
        if (c.kind() == McpSetup.Kind.MANUAL) {
            return 3;
        }
        State s = this.states.getOrDefault(c.kind(), State.MISSING);
        return switch (s.ordinal()) {
            default -> throw new MatchException(null, null);
            case 2 -> 0;
            case 1 -> 1;
            case 0 -> 2;
        };
    }

    private void renderClient(GuiGraphicsExtractor g, McpClients.Client c, float x, float y, float w, Color accent) {
        Color base;
        String label;
        Color color;
        String chip;
        boolean hov;
        boolean manual = c.kind() == McpSetup.Kind.MANUAL;
        State state = manual ? State.FOUND : this.states.getOrDefault(c.kind(), State.MISSING);
        boolean visible = y >= this.listY - 1.0f && y + 27.0f <= this.listY + this.listH + 1.0f;
        boolean bl = hov = visible && this.hover(x, y, w, 27.0f);
        Color bg = state == State.CONNECTED ? Color.rgba(46, 204, 113, hov ? 22 : 14) : Color.rgba(255, 255, 255, hov ? 10 : 5);
        RoundedRect.of(x, y, w, 27.0f).radius(4.0f).color(bg).render(g);
        if (state == State.CONNECTED) {
            Outline.of(x, y, w, 27.0f).radius(4.0f).thickness(0.6f).color(Color.rgba(46, 204, 113, 70)).render(g);
        }
        float alpha = state == State.MISSING ? 0.55f : 1.0f;
        RoundedRect.of(x + 4.0f, y + 5.5f, 16.0f, 16.0f).radius(4.0f).color(accent.withAlpha((int)(50.0f * alpha))).render(g);
        Text.of(Fonts.MEDIUM, c.name().substring(0, 1), x + 12.0f, y + 13.5f, 7.0f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(accent.brighter(0.3f).multiplyAlpha(alpha)).render(g);
        float btnW = 44.0f;
        float textW = w - 24.0f - btnW - 8.0f;
        Text.of(Fonts.MEDIUM, McpView.fit(c.name(), 6.0f, textW - 30.0f), x + 24.0f, y + 7.0f, 6.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE.multiplyAlpha(alpha)).render(g);
        if (manual) {
            chip = "\u0432\u0440\u0443\u0447\u043d\u0443\u044e";
        } else {
            switch (state.ordinal()) {
                default: {
                    throw new MatchException(null, null);
                }
                case 2: {
                    chip = "\u043f\u043e\u0434\u043a\u043b\u044e\u0447\u0435\u043d\u043e";
                    break;
                }
                case 1: {
                    chip = "\u043d\u0430\u0439\u0434\u0435\u043d\u043e";
                    break;
                }
                case 0: {
                    chip = "\u043d\u0435 \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u043e";
                }
            }
        }
        if (manual) {
            color = GRAY;
        } else {
            switch (state.ordinal()) {
                default: {
                    throw new MatchException(null, null);
                }
                case 2: {
                    color = GREEN;
                    break;
                }
                case 1: {
                    color = accent.brighter(0.2f);
                    break;
                }
                case 0: {
                    color = GRAY;
                }
            }
        }
        Color chipCol = color;
        float nameW = Math.min(McpView.width(c.name(), 6.0f), textW - 30.0f);
        float chipW = McpView.width(chip, 4.4f) + 6.0f;
        RoundedRect.of(x + 27.0f + nameW, y + 3.5f, chipW, 7.0f).radius(2.0f).color(chipCol.withAlpha(40)).render(g);
        Text.of(Fonts.MEDIUM, chip, x + 27.0f + nameW + chipW * 0.5f, y + 7.0f, 4.4f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(chipCol).render(g);
        String sub = c.models();
        Color subCol = accent.brighter(0.2f).multiplyAlpha(alpha);
        Long t = this.messageTime.get(c.kind());
        if (t != null && System.currentTimeMillis() - t < 6000L) {
            sub = this.messages.get(c.kind());
            subCol = sub.startsWith("\u0413\u043e\u0442\u043e\u0432\u043e") ? GREEN.brighter(0.2f) : RED.brighter(0.2f);
        } else if (state == State.CONNECTED) {
            sub = McpSetup.afterHint(c.kind());
            subCol = Color.rgb(150, 200, 165);
        }
        List<String> subLines = McpView.wrap(sub, 4.6f, textW);
        float ly = y + 14.0f;
        for (int i = 0; i < Math.min(2, subLines.size()); ++i) {
            Text.of(Fonts.MEDIUM, subLines.get(i), x + 24.0f, ly + 2.8f, 4.6f).verticalAlign(VerticalAlign.MIDDLE).color(subCol).render(g);
            ly += 5.8f;
        }
        float bx = x + w - btnW - 4.0f;
        float by = y + 4.0f;
        if (manual) {
            label = "\u041a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u0442\u044c";
            base = accent;
        } else if (state == State.CONNECTED) {
            label = "\u041e\u0442\u043a\u043b\u044e\u0447\u0438\u0442\u044c";
            base = Color.rgba(235, 75, 75, 130);
        } else if (state == State.FOUND) {
            label = "\u041d\u0430\u0441\u0442\u0440\u043e\u0438\u0442\u044c";
            base = accent;
        } else {
            label = "\u0412\u0441\u0451 \u0440\u0430\u0432\u043d\u043e";
            base = Color.rgba(255, 255, 255, 20);
        }
        if (visible) {
            boolean connected = state == State.CONNECTED;
            this.button(g, label, bx, by, btnW, 10.5f, base, 5.0f, () -> {
                if (connected) {
                    McpSetup.Result r = McpSetup.remove(c.kind());
                    this.message(c.kind(), r.ok() ? "\u0413\u043e\u0442\u043e\u0432\u043e! \u041e\u0442\u043a\u043b\u044e\u0447\u0435\u043d\u043e, \u043f\u0435\u0440\u0435\u0437\u0430\u043f\u0443\u0441\u0442\u0438 \u043f\u0440\u043e\u0433\u0440\u0430\u043c\u043c\u0443" : r.message());
                    this.scan(true);
                } else {
                    this.setup(c);
                }
            });
            if (!manual) {
                boolean cHov = this.hover(bx, by + 12.5f, btnW, 8.0f);
                Text.of(Fonts.MEDIUM, "\u043a\u043e\u043f\u0438\u0440\u043e\u0432\u0430\u0442\u044c \u043a\u043e\u043d\u0444\u0438\u0433", bx + btnW * 0.5f, by + 16.5f, 4.2f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(cHov ? Color.WHITE : GRAY).render(g);
                this.box(bx, by + 12.5f, btnW, 8.0f, () -> {
                    this.ensureRunning();
                    McpView.copy(c.snippet(McpServer.get().url()));
                    this.message(c.kind(), "\u0413\u043e\u0442\u043e\u0432\u043e! \u041a\u043e\u043d\u0444\u0438\u0433 \u0432 \u0431\u0443\u0444\u0435\u0440\u0435 - \u0432\u0441\u0442\u0430\u0432\u044c \u0432 " + c.where());
                });
            }
        }
    }

    public boolean mouseClicked(float mx, float my) {
        this.mouseX = mx;
        this.mouseY = my;
        for (int i = this.boxes.size() - 1; i >= 0; --i) {
            float[] b = this.boxes.get(i);
            if (!(mx >= b[0]) || !(mx <= b[0] + b[2]) || !(my >= b[1]) || !(my <= b[1] + b[3])) continue;
            this.actions.get(i).run();
            return true;
        }
        return false;
    }

    public void mouseScrolled(float mx, float my, double amount) {
        this.scrollTarget = Math.clamp(this.scrollTarget - (float)amount * 20.0f, 0.0f, this.maxScroll);
    }

    private static enum State {
        MISSING,
        FOUND,
        CONNECTED;

    }
}

