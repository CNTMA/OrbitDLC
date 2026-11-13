/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.ui.clickgui.tab;

import brokke.cc.core.setting.Setting;
import brokke.cc.system.scripting.mcp.McpServer;
import brokke.cc.system.scripting.runtime.ScriptInstance;
import brokke.cc.system.scripting.runtime.ScriptManager;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.ui.clickgui.scripts.DuckAnimation;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.image.Image;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class ScriptsTab
extends GuiComponent {
    private final Set<String> expandedScripts = new HashSet<String>();
    public static boolean scriptsMcp = false;
    public static boolean scriptsDocs = false;

    public ScriptsTab(ClickGuiContext ctx) {
        super(ctx);
    }

    public float renderScripts(GuiGraphicsExtractor graphics, float x, float y, float w, float prog, float top, float bottom, long now) {
        float startY = y;
        float appear = GuiDraw.easeOut((float)(now - this.ctx.switchTime) / 380.0f);
        float a = prog * appear;
        y += (1.0f - appear) * 8.0f;
        Color accent = GuiDraw.getAccent();
        if (scriptsDocs) {
            return this.ctx.docs.renderDocs(graphics, x, y, w, a, accent, top, bottom, now) + (y - startY);
        }
        if (scriptsMcp) {
            return this.ctx.mcp.renderMcp(graphics, x, y, w, a, accent, top, bottom, now) + (y - startY);
        }
        ScriptManager manager = ScriptManager.get();
        float bw = (w - 15.0f) * 0.25f;
        float bh = 15.0f;
        this.ctx.widgets.renderConfigButton(graphics, "script-folder", "\u041f\u0430\u043f\u043a\u0430", "F", x, y, bw, bh, a, false, top, bottom, manager::openFolder);
        this.ctx.widgets.renderConfigButton(graphics, "script-reload", "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c", "a", x + bw + 5.0f, y, bw, bh, a, false, top, bottom, manager::forceReloadAll);
        this.ctx.widgets.renderConfigButton(graphics, "script-docs", "\u0414\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u0430\u0446\u0438\u044f", "I", x + (bw + 5.0f) * 2.0f, y, bw, bh, a, false, top, bottom, () -> {
            scriptsDocs = true;
            this.ctx.switchTime = System.currentTimeMillis();
            this.ctx.scrollTarget = 0.0f;
        });
        this.ctx.widgets.renderConfigButton(graphics, "script-mcp", McpServer.get().isRunning() ? "MCP (\u0432\u043a\u043b)" : "MCP", "G", x + (bw + 5.0f) * 3.0f, y, bw, bh, a, false, top, bottom, () -> {
            scriptsMcp = true;
            this.ctx.mcp.scanMcp(true);
            this.ctx.switchTime = System.currentTimeMillis();
            this.ctx.scrollTarget = 0.0f;
        });
        y += bh + 7.0f;
        ArrayList<ScriptInstance> scripts = new ArrayList<ScriptInstance>(manager.all());
        scripts.sort((s1, s2) -> String.CASE_INSENSITIVE_ORDER.compare(s1.displayName(), s2.displayName()));
        y = GuiDraw.sectionTitle(graphics, "\u0421\u043a\u0440\u0438\u043f\u0442\u044b (" + manager.countRunning() + "/" + scripts.size() + ")", x, y, a);
        if (scripts.isEmpty()) {
            float duckSize = w / 3.0f;
            Image.of(DuckAnimation.frame(), x + (w - duckSize) * 0.5f, y + 6.0f, duckSize, duckSize).alpha(a).render(graphics);
            Text.of(Fonts.MEDIUM, manager.engineReady() ? "\u0421\u043a\u0440\u0438\u043f\u0442\u043e\u0432 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442" : "\u0414\u0432\u0438\u0436\u043e\u043a \u0437\u0430\u0433\u0440\u0443\u0436\u0430\u0435\u0442\u0441\u044f...", x + w * 0.5f, y + duckSize + 14.0f, 5.4f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
            Text.of(Fonts.MEDIUM, "\u041f\u043e\u043b\u043e\u0436\u0438 .kts \u0444\u0430\u0439\u043b \u0432 \u043f\u0430\u043f\u043a\u0443 \u0441\u043a\u0440\u0438\u043f\u0442\u043e\u0432", x + w * 0.5f, y + duckSize + 23.0f, 4.6f).centered().color(Color.rgb(90, 95, 106).withAlpha(a)).render(graphics);
            return y + duckSize + 32.0f - startY;
        }
        float colW = (w - 5.0f) * 0.5f;
        float[] colY = new float[]{y, y};
        for (int i = 0; i < scripts.size(); ++i) {
            int col = i % 2;
            float t = GuiDraw.easeOut((float)(now - this.ctx.switchTime - (long)i * 30L) / 360.0f);
            float h = this.renderScriptCard(graphics, (ScriptInstance)scripts.get(i), x + (float)col * (colW + 5.0f), colY[col] + (1.0f - t) * 8.0f, colW, a * t, accent, top, bottom);
            int n = col;
            colY[n] = colY[n] + (h + 5.0f);
        }
        return Math.max(colY[0], colY[1]) - 5.0f - startY;
    }

    private float renderScriptCard(GuiGraphicsExtractor graphics, ScriptInstance script, float x, float y, float w, float a, Color accent, float clipTop, float clipBottom) {
        Color descColor;
        String desc;
        String key = "script-" + script.id;
        List<Setting<?>> settings = script.nativeSettings();
        float settingsH = 0.0f;
        for (Setting<?> setting : settings) {
            settingsH += this.ctx.settings.settingHeight(setting) * this.ctx.settings.settingVisibility(setting);
        }
        boolean hasSettings = settingsH > 0.0f;
        float expand = this.anim(key + "-exp", hasSettings && this.expandedScripts.contains(script.id), 6.0f);
        float h = 27.0f + (hasSettings ? (settingsH + 3.0f) * expand : 0.0f);
        if (y > clipBottom || y + h < clipTop) {
            return h;
        }
        float hover = this.anim(key, this.hov(key, this.hovered(x, y, w, 27.0f) && this.hovered(x, clipTop, w, clipBottom - clipTop)), 6.0f);
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hover) * a))).render(graphics);
        float toggle = this.anim(key + "-on", script.isEnabled(), 8.0f);
        Text.of(Fonts.MEDIUM, GuiDraw.fit(script.displayName(), 6.6f, w - 42.0f), x + 8.0f, y + 9.5f, 6.6f).verticalAlign(VerticalAlign.MIDDLE).color(GuiDraw.mix(Color.rgb(200, 204, 214), Color.rgb(245, 247, 252), toggle).withAlpha(a)).render(graphics);
        if (script.status == ScriptInstance.Status.ERROR) {
            desc = script.error != null ? script.error : "\u041e\u0448\u0438\u0431\u043a\u0430";
            descColor = Color.rgb(235, 110, 110);
        } else if (script.status == ScriptInstance.Status.LOADING) {
            desc = "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430...";
            descColor = Color.rgb(230, 200, 100);
        } else {
            desc = script.description == null || script.description.isBlank() ? script.file.getName() : script.description;
            descColor = Color.rgb(115, 120, 132);
        }
        Text.of(Fonts.MEDIUM, GuiDraw.fit(desc, 5.0f, w - 42.0f), x + 8.0f, y + 18.5f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(descColor.withAlpha(a)).render(graphics);
        boolean ready = script.status == ScriptInstance.Status.READY;
        float swW = 16.0f;
        float swH = 8.5f;
        float swX = x + w - swW - (hasSettings ? 16.0f : 8.0f);
        float swY = y + (27.0f - swH) * 0.5f;
        GuiDraw.renderSwitch(graphics, swX, swY, swW, swH, toggle, accent, a * (ready ? 1.0f : 0.4f));
        if (hasSettings) {
            Text.of(Fonts.ICONS, expand > 0.5f ? "P" : "O", x + w - 8.5f, y + 13.5f, 5.5f).centered().color(Color.rgb(140, 145, 155).withAlpha(a)).render(graphics);
        }
        Runnable expandAction = hasSettings ? () -> {
            if (!this.expandedScripts.remove(script.id)) {
                this.expandedScripts.add(script.id);
            }
            ClientSound.ui(ClientSound.Ui.CLICK);
        } : null;
        float hitTop = Math.max(y, clipTop);
        float hitBottom = Math.min(y + 27.0f, clipBottom);
        if (hitBottom > hitTop) {
            this.hit(x, hitTop, w, hitBottom - hitTop, ready ? () -> {
                script.toggle();
                ClientSound.ui(ClientSound.Ui.CLICK);
            } : null, expandAction);
            if (hasSettings) {
                this.hit(x + w - 15.0f, hitTop, 15.0f, hitBottom - hitTop, expandAction, expandAction);
            }
        }
        if (expand > 0.01f && hasSettings) {
            float sy = y + 27.0f;
            float visibleTop = Math.max(sy, clipTop);
            float visibleBottom = Math.min(y + h, clipBottom);
            if (visibleBottom - visibleTop >= 1.5f) {
                Scissor.push(graphics, x, visibleTop, w, visibleBottom - visibleTop);
                float top = expand > 0.95f ? visibleTop : Float.MAX_VALUE;
                for (Setting<?> setting : settings) {
                    sy += this.ctx.settings.renderAnimatedSetting(graphics, setting, x + 8.0f, sy, w - 16.0f, a * expand, accent, top, visibleBottom);
                }
                Scissor.pop(graphics);
            }
        }
        return h;
    }
}

