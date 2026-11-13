/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.input.CharacterEvent
 *  net.minecraft.client.input.KeyEvent
 */
package brokke.cc.ui.clickgui.scripts;

import brokke.cc.core.module.Module;
import brokke.cc.core.setting.BindSetting;
import brokke.cc.core.setting.BooleanSetting;
import brokke.cc.core.setting.ButtonSetting;
import brokke.cc.core.setting.ColorSetting;
import brokke.cc.core.setting.FloatSetting;
import brokke.cc.core.setting.ModeSetting;
import brokke.cc.core.setting.MultiBooleanSetting;
import brokke.cc.core.setting.NumberSetting;
import brokke.cc.core.setting.Setting;
import brokke.cc.core.setting.StringSetting;
import brokke.cc.system.scripting.mcp.McpServer;
import brokke.cc.system.scripting.runtime.ScriptConsole;
import brokke.cc.system.scripting.runtime.ScriptInstance;
import brokke.cc.system.scripting.runtime.ScriptManager;
import brokke.cc.ui.clickgui.scripts.CodeEditor;
import brokke.cc.ui.clickgui.scripts.DocsView;
import brokke.cc.ui.clickgui.scripts.DuckAnimation;
import brokke.cc.ui.clickgui.scripts.KotlinHighlighter;
import brokke.cc.ui.clickgui.scripts.McpView;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.DisplayUtil;
import brokke.cc.util.render.display.image.Image;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

public class ScriptsPanel {
    private static final float LEFT_W = 95.0f;
    private static final float GAP = 6.0f;
    private static final float LIST_CARD_H = 175.0f;
    private static final float EDITOR_H = 150.0f;
    private static final float ITEM_H = 16.0f;
    private static final float CODE_SIZE = 5.5f;
    private static final float LINE_H = 7.0f;
    private static final float GUTTER_W = 19.0f;
    private static final Color TEXT_MAIN = Color.rgb(200, 205, 220);
    private static final Color TEXT_MUTED = Color.rgb(130, 135, 150);
    private static final Color GREEN = Color.rgb(46, 204, 113);
    private static final Color RED = Color.rgb(235, 87, 87);
    private static final Color YELLOW = Color.rgb(242, 190, 74);
    private static final String TEMPLATE = String.join((CharSequence)"\n", "name(\"\u041d\u043e\u0432\u044b\u0439 \u0441\u043a\u0440\u0438\u043f\u0442\")", "description(\"\u041e\u043f\u0438\u0441\u0430\u043d\u0438\u0435 \u0441\u043a\u0440\u0438\u043f\u0442\u0430\")", "", "val message by checkBox(\"\u0421\u043e\u043e\u0431\u0449\u0435\u043d\u0438\u0435\", true)", "val delay by slider(\"\u0417\u0430\u0434\u0435\u0440\u0436\u043a\u0430\", 20, 1, 100)", "", "var ticks = 0", "", "onEnable {", "    ticks = 0", "    if (message) notify(\"\u0421\u043a\u0440\u0438\u043f\u0442 \u0432\u043a\u043b\u044e\u0447\u0451\u043d\")", "}", "", "on<ClientTickEvent> {", "    whenInGame {", "        if (++ticks >= delay) {", "            ticks = 0", "        }", "    }", "}", "");
    private final List<float[]> boxes = new ArrayList<float[]>();
    private final List<Runnable> actions = new ArrayList<Runnable>();
    private final List<float[]> rightBoxes = new ArrayList<float[]>();
    private final List<Runnable> rightActions = new ArrayList<Runnable>();
    private final List<float[]> middleBoxes = new ArrayList<float[]>();
    private final List<Runnable> middleActions = new ArrayList<Runnable>();
    private final Map<Character, Float> charWidths = new HashMap<Character, Float>();
    private final CodeEditor editor = new CodeEditor();
    private String selectedId;
    private String editorFor;
    private long editorFileStamp;
    private boolean editorFocused;
    private boolean selectingText;
    private int view;
    private float listScroll;
    private float listScrollTarget;
    private float listMaxScroll;
    private float codeScrollY;
    private float codeScrollX;
    private float settingsScroll;
    private float settingsMaxScroll;
    private float consoleScroll;
    private float consoleMaxScroll;
    private boolean consoleFollow = true;
    private float listX;
    private float listY;
    private float listW;
    private float listH;
    private float codeX;
    private float codeY;
    private float codeW;
    private float codeH;
    private float settingsX;
    private float settingsY;
    private float settingsW;
    private float settingsH;
    private float consoleX;
    private float consoleY;
    private float consoleW;
    private float consoleH;
    private boolean listeningScriptBind;
    private BindSetting listeningSetting;
    private StringSetting focusedString;
    private FloatSetting draggingSlider;
    private NumberSetting draggingNumber;
    private ColorSetting draggingHue;
    private float dragTrackX;
    private float dragTrackW;
    private float mouseX;
    private float mouseY;
    private boolean docsOpen;
    private boolean mcpOpen;
    private final McpView mcpView = new McpView(() -> {
        this.mcpOpen = false;
    });
    private final DocsView docsView = new DocsView(() -> {
        this.docsOpen = false;
    });
    private long lastClickTime;
    private int lastClickPos = -1;

    public void resetScroll() {
        this.listScrollTarget = 0.0f;
        this.listScroll = 0.0f;
        this.settingsScroll = 0.0f;
    }

    public boolean isTyping() {
        return this.editorFocused || this.focusedString != null || this.listeningScriptBind || this.listeningSetting != null;
    }

    public void unfocus() {
        this.editorFocused = false;
        this.focusedString = null;
        this.listeningScriptBind = false;
        this.listeningSetting = null;
        this.selectingText = false;
    }

    public int count() {
        return ScriptManager.get().all().size();
    }

    public int countRunning() {
        return ScriptManager.get().countRunning();
    }

    private ScriptInstance selected(List<ScriptInstance> scripts) {
        if (scripts.isEmpty()) {
            this.selectedId = null;
            return null;
        }
        for (ScriptInstance s : scripts) {
            if (!s.id.equals(this.selectedId)) continue;
            return s;
        }
        this.selectedId = scripts.get((int)0).id;
        return scripts.get(0);
    }

    private void syncEditor(ScriptInstance script) {
        if (script == null) {
            if (this.editorFor != null) {
                this.editor.setText("");
                this.editorFor = null;
            }
            return;
        }
        long stamp = script.file.lastModified();
        if (!script.id.equals(this.editorFor) || stamp != this.editorFileStamp && !this.editor.modified()) {
            String content = "";
            try {
                if (script.file.exists()) {
                    content = Files.readString(script.file.toPath(), StandardCharsets.UTF_8);
                }
            }
            catch (IOException iOException) {
                // empty catch block
            }
            boolean sameScript = script.id.equals(this.editorFor);
            int line = this.editor.caretLine();
            int col = this.editor.caretColumn();
            this.editor.setText(content);
            if (sameScript) {
                this.editor.moveTo(line, col, false);
            } else {
                this.codeScrollX = 0.0f;
                this.codeScrollY = 0.0f;
                this.settingsScroll = 0.0f;
            }
            this.editorFor = script.id;
            this.editorFileStamp = stamp;
        }
    }

    private float charWidth(char c) {
        Float w = this.charWidths.get(Character.valueOf(c));
        if (w == null) {
            float wide = Text.of(Fonts.MEDIUM, "|" + c + "|").size(5.5f).width();
            float narrow = Text.of(Fonts.MEDIUM, "||").size(5.5f).width();
            w = Float.valueOf(Math.max(0.5f, wide - narrow));
            this.charWidths.put(Character.valueOf(c), w);
        }
        return w.floatValue();
    }

    private float measure(String s, int from, int to) {
        float w = 0.0f;
        for (int i = from; i < to; ++i) {
            w += this.charWidth(s.charAt(i));
        }
        return w;
    }

    private int columnAt(String line, float px) {
        float acc = 0.0f;
        for (int i = 0; i < line.length(); ++i) {
            float cw = this.charWidth(line.charAt(i));
            if (px < acc + cw * 0.5f) {
                return i;
            }
            acc += cw;
        }
        return line.length();
    }

    private void box(float x, float y, float w, float h, Runnable action) {
        this.boxes.add(new float[]{x, y, w, h});
        this.actions.add(action);
    }

    private void rightBox(float x, float y, float w, float h, Runnable action) {
        this.rightBoxes.add(new float[]{x, y, w, h});
        this.rightActions.add(action);
    }

    private boolean hover(float x, float y, float w, float h) {
        return this.mouseX >= x && this.mouseX <= x + w && this.mouseY >= y && this.mouseY <= y + h;
    }

    private static Color cardBg() {
        return Color.rgba(12, 12, 18, 90);
    }

    private void card(GuiGraphicsExtractor g, float x, float y, float w, float h) {
        RoundedRect.of(x, y, w, h).radius(5.0f).color(ScriptsPanel.cardBg()).render(g);
        Outline.of(x, y, w, h).radius(5.0f).thickness(0.7f).color(Color.rgba(255, 255, 255, 10)).render(g);
    }

    private void pill(GuiGraphicsExtractor g, String label, float x, float y, float w, float h, Color bg, Color fg, Runnable action) {
        RoundedRect.of(x, y, w, h).radius(2.5f).color(bg).render(g);
        Text.of(Fonts.MEDIUM, label, x + w * 0.5f, y + h * 0.5f, 5.2f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(fg).render(g);
        if (action != null) {
            this.box(x, y, w, h, action);
        }
    }

    private static Color statusColor(ScriptInstance s) {
        if (s.status == ScriptInstance.Status.ERROR) {
            return RED;
        }
        if (s.status == ScriptInstance.Status.LOADING) {
            return YELLOW;
        }
        if (s.isEnabled()) {
            return GREEN;
        }
        return Color.rgb(90, 95, 110);
    }

    public void render(GuiGraphicsExtractor g, float x, float y, float w, float h, float mx, float my, Color accent, float delta) {
        this.mouseX = mx;
        this.mouseY = my;
        this.boxes.clear();
        this.actions.clear();
        this.rightBoxes.clear();
        this.rightActions.clear();
        this.middleBoxes.clear();
        this.middleActions.clear();
        if (this.mcpOpen) {
            this.mcpView.render(g, x, y, w, h, mx, my, accent, delta);
            return;
        }
        if (this.docsOpen) {
            this.docsView.render(g, x, y, w, h, mx, my, accent, delta);
            return;
        }
        List<ScriptInstance> scripts = ScriptManager.get().all();
        ScriptInstance current = this.selected(scripts);
        this.syncEditor(current);
        this.renderList(g, x, y, scripts, current, accent, delta);
        float rightX = x + 95.0f + 6.0f;
        float rightW = w - 95.0f - 6.0f;
        this.renderEditorCard(g, rightX, y, rightW, current, accent);
        this.renderConsole(g, rightX, y + 150.0f + 5.0f, rightW, h - 150.0f - 5.0f, accent);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderList(GuiGraphicsExtractor g, float x, float y, List<ScriptInstance> scripts, ScriptInstance current, Color accent, float delta) {
        this.card(g, x, y, 95.0f, 175.0f);
        Text.of(Fonts.MEDIUM, "\u0421\u043a\u0440\u0438\u043f\u0442\u044b", x + 7.0f, y + 9.0f, 7.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(g);
        float plusX = x + 95.0f - 16.0f;
        float plusY = y + 4.0f;
        boolean plusHov = this.hover(plusX, plusY, 10.0f, 10.0f);
        RoundedRect.of(plusX, plusY, 10.0f, 10.0f).radius(2.5f).color(plusHov ? Color.rgba(255, 255, 255, 25) : Color.rgba(255, 255, 255, 10)).render(g);
        Text.of(Fonts.MEDIUM, "+", plusX + 5.0f, plusY + 5.0f, 6.8f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(g);
        this.box(plusX, plusY, 10.0f, 10.0f, this::createScript);
        RoundedRect.of(x + 5.0f, y + 18.0f, 85.0f, 1.0f).color(Color.rgba(255, 255, 255, 6)).render(g);
        this.listX = x;
        this.listY = y + 20.0f;
        this.listW = 95.0f;
        this.listH = 151.0f;
        this.listMaxScroll = Math.max(0.0f, (float)scripts.size() * 16.0f - this.listH + 6.0f);
        this.listScrollTarget = Math.clamp(this.listScrollTarget, 0.0f, this.listMaxScroll);
        this.listScroll += (this.listScrollTarget - this.listScroll) * Math.min(1.0f, delta * 16.0f);
        if (scripts.isEmpty()) {
            float duckSize = 40.0f;
            Image.of(DuckAnimation.frame(), x + (95.0f - duckSize) * 0.5f, this.listY + 8.0f, duckSize, duckSize).render(g);
            Text.of(Fonts.MEDIUM, "\u041d\u0435\u0442 \u0441\u043a\u0440\u0438\u043f\u0442\u043e\u0432", x + 47.5f, this.listY + 56.0f, 5.8f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(TEXT_MUTED).render(g);
            Text.of(Fonts.MEDIUM, "\u043d\u0430\u0436\u043c\u0438 +", x + 47.5f, this.listY + 65.0f, 5.2f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(90, 95, 110)).render(g);
        }
        Scissor.push(g, this.listX, this.listY, this.listW, this.listH);
        try {
            for (int i = 0; i < scripts.size(); ++i) {
                boolean hov;
                ScriptInstance s = scripts.get(i);
                float itY = this.listY + 1.0f + (float)i * 16.0f - this.listScroll;
                if (itY + 16.0f < this.listY || itY > this.listY + this.listH) continue;
                boolean sel = s == current;
                boolean bl = hov = this.hover(x + 4.0f, itY, 87.0f, 16.0f) && this.hover(this.listX, this.listY, this.listW, this.listH);
                if (sel) {
                    RoundedRect.of(x + 4.0f, itY, 87.0f, 16.0f).radius(3.5f).color(accent.withAlpha(30)).render(g);
                    Outline.of(x + 4.0f, itY, 87.0f, 16.0f).radius(3.5f).thickness(0.6f).color(accent.withAlpha(120)).render(g);
                } else if (hov) {
                    RoundedRect.of(x + 4.0f, itY, 87.0f, 16.0f).radius(3.5f).color(Color.rgba(255, 255, 255, 6)).render(g);
                }
                RoundedRect.of(x + 7.0f, itY + 6.25f, 3.5f, 3.5f).radius(1.75f).color(ScriptsPanel.statusColor(s)).render(g);
                Color nameCol = sel ? Color.WHITE : (hov ? Color.rgb(220, 225, 235) : Color.rgb(150, 155, 170));
                float maxNameW = 73.0f;
                String bindName = s.bind != -1 && s.bind != 0 ? Module.getKeyName(s.bind) : "";
                float delW = 12.0f;
                float delH = 11.0f;
                float delX = x + 95.0f - delW - 6.0f;
                float delY = itY + (16.0f - delH) * 0.5f;
                if (hov) {
                    maxNameW -= delW + 4.0f;
                } else if (!bindName.isEmpty()) {
                    maxNameW -= Text.of(Fonts.MEDIUM, bindName).size(5.0f).width() + 6.0f;
                }
                Text.of(Fonts.MEDIUM, this.ellipsize(s.file.getName(), 6.2f, maxNameW), x + 14.0f, itY + 8.0f, 6.2f).verticalAlign(VerticalAlign.MIDDLE).color(nameCol).render(g);
                if (hov) {
                    boolean delHov = this.hover(delX, delY, delW, delH);
                    RoundedRect.of(delX, delY, delW, delH).radius(3.0f).color(delHov ? Color.rgba(235, 75, 75, 50) : Color.rgba(255, 255, 255, 8)).render(g);
                    Outline.of(delX, delY, delW, delH).radius(3.0f).thickness(0.6f).color(delHov ? Color.rgba(235, 75, 75, 150) : Color.rgba(255, 255, 255, 12)).render(g);
                    DisplayUtil.renderCross(g, delX + delW * 0.5f, delY + delH * 0.5f, 2.2f, 1.1f, delHov ? Color.rgb(255, 120, 120) : Color.rgb(150, 155, 170));
                } else if (!bindName.isEmpty()) {
                    float tagW = Text.of(Fonts.MEDIUM, bindName).size(5.0f).width() + 5.0f;
                    float tagH = 8.5f;
                    float tagX = x + 95.0f - tagW - 7.0f;
                    float tagY = itY + (16.0f - tagH) * 0.5f;
                    RoundedRect.of(tagX, tagY, tagW, tagH).radius(2.0f).color(Color.rgba(255, 255, 255, 8)).render(g);
                    Text.of(Fonts.MEDIUM, bindName, tagX + tagW * 0.5f, tagY + tagH * 0.5f, 5.0f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(TEXT_MUTED).render(g);
                }
                if (!(itY >= this.listY - 16.0f) || !(itY <= this.listY + this.listH)) continue;
                String id = s.id;
                float bx = x + 4.0f;
                float by = Math.max(itY, this.listY);
                float bh = Math.min(itY + 16.0f, this.listY + this.listH) - by;
                if (!(bh > 0.0f)) continue;
                this.box(bx, by, 87.0f, bh, () -> this.select(id));
                this.rightBox(bx, by, 87.0f, bh, () -> this.openSettings(id));
                if (!hov || !(delY >= this.listY) || !(delY + delH <= this.listY + this.listH)) continue;
                this.box(delX, delY, delW, delH, () -> this.deleteScript(id));
            }
        }
        finally {
            Scissor.pop(g);
        }
        if (this.listMaxScroll > 1.0f) {
            float trackY = this.listY + 1.0f;
            float thumbH = Math.max(14.0f, this.listH / ((float)scripts.size() * 16.0f) * this.listH);
            float thumbY = trackY + Math.clamp(this.listScroll / this.listMaxScroll, 0.0f, 1.0f) * (this.listH - thumbH);
            RoundedRect.of(x + 95.0f - 3.5f, trackY, 2.0f, this.listH).radius(1.0f).color(Color.rgba(255, 255, 255, 8)).render(g);
            RoundedRect.of(x + 95.0f - 3.5f, thumbY, 2.0f, thumbH).radius(1.0f).color(accent.withAlpha(130)).render(g);
        }
        float btnY = y + 175.0f + 4.0f;
        float halfW = 45.5f;
        this.smallButton(g, "\u041f\u0430\u043f\u043a\u0430", x, btnY, halfW, 15.0f, () -> ScriptManager.get().openFolder());
        this.smallButton(g, "\u041e\u0431\u043d\u043e\u0432\u0438\u0442\u044c", x + halfW + 4.0f, btnY, halfW, 15.0f, () -> {
            ScriptManager.get().forceReloadAll();
            ScriptConsole.info("scripts", "\u0432\u0441\u0435 \u0441\u043a\u0440\u0438\u043f\u0442\u044b \u043f\u0435\u0440\u0435\u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043d\u044b");
        });
        float docY = btnY + 15.0f + 9.0f;
        boolean docHov = this.hover(x, docY - 5.0f, 95.0f, 10.0f);
        Text.of(Fonts.MEDIUM, "\u0414\u043e\u043a\u0443\u043c\u0435\u043d\u0442\u0430\u0446\u0438\u044f API ->", x + 47.5f, docY, 6.0f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(docHov ? accent.brighter(0.1f) : accent).render(g);
        this.box(x, docY - 5.0f, 95.0f, 10.0f, () -> {
            this.docsOpen = true;
            this.docsView.reset();
            this.unfocus();
        });
    }

    private void smallButton(GuiGraphicsExtractor g, String label, float x, float y, float w, float h, Runnable action) {
        boolean hov = this.hover(x, y, w, h);
        RoundedRect.of(x, y, w, h).radius(3.5f).color(hov ? Color.rgba(255, 255, 255, 12) : ScriptsPanel.cardBg()).render(g);
        Outline.of(x, y, w, h).radius(3.5f).thickness(0.7f).color(Color.rgba(255, 255, 255, hov ? 20 : 10)).render(g);
        Text.of(Fonts.MEDIUM, label, x + w * 0.5f, y + h * 0.5f, 6.0f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(hov ? Color.WHITE : Color.rgb(170, 175, 190)).render(g);
        this.box(x, y, w, h, action);
    }

    private String ellipsize(String s, float size, float maxW) {
        if (Text.of(Fonts.MEDIUM, s).size(size).width() <= maxW) {
            return s;
        }
        String cut = s;
        while (cut.length() > 1 && Text.of(Fonts.MEDIUM, cut + "..").size(size).width() > maxW) {
            cut = cut.substring(0, cut.length() - 1);
        }
        return cut + "..";
    }

    private void renderEditorCard(GuiGraphicsExtractor g, float x, float y, float w, ScriptInstance script, Color accent) {
        Color statusBg;
        String statusText;
        this.card(g, x, y, w, 150.0f);
        if (script == null) {
            Text.of(Fonts.MEDIUM, "\u0421\u043e\u0437\u0434\u0430\u0439 \u0441\u043a\u0440\u0438\u043f\u0442 \u043a\u043d\u043e\u043f\u043a\u043e\u0439 +", x + w * 0.5f, y + 75.0f, 6.5f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(TEXT_MUTED).render(g);
            this.codeW = 0.0f;
            this.settingsW = 0.0f;
            return;
        }
        boolean running = script.isEnabled();
        String name = script.file.getName() + (this.editor.modified() ? " *" : "");
        float nameSize = 6.6f;
        float buttonsW = 127.0f;
        float maxName = w - 14.0f - buttonsW - 50.0f;
        String shown = this.ellipsize(name, nameSize, maxName);
        Text.of(Fonts.MEDIUM, shown, x + 7.0f, y + 9.0f, nameSize).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(g);
        float nameW = Text.of(Fonts.MEDIUM, shown).size(nameSize).width();
        Color statusFg = switch (script.status) {
            case ScriptInstance.Status.ERROR -> {
                statusText = "\u041e\u0448\u0438\u0431\u043a\u0430";
                statusBg = Color.rgba(235, 87, 87, 35);
                yield RED;
            }
            case ScriptInstance.Status.LOADING -> {
                statusText = "\u0417\u0430\u0433\u0440\u0443\u0437\u043a\u0430";
                statusBg = Color.rgba(242, 190, 74, 35);
                yield YELLOW;
            }
            default -> {
                statusText = running ? "\u0420\u0430\u0431\u043e\u0442\u0430\u0435\u0442" : "\u041e\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d";
                statusBg = running ? Color.rgba(46, 204, 113, 35) : Color.rgba(120, 125, 140, 35);
                yield running ? GREEN : Color.rgb(150, 155, 165);
            }
        };
        float pillW = Text.of(Fonts.MEDIUM, statusText).size(5.2f).width() + 8.0f;
        this.pill(g, statusText, x + 7.0f + nameW + 4.0f, y + 4.0f, pillW, 10.0f, statusBg, statusFg, null);
        float bx = x + w - 3.0f - 40.0f;
        float by = y + 3.25f;
        float bh = 11.5f;
        boolean saveHov = this.hover(bx, by, 40.0f, bh);
        this.pill(g, this.editor.modified() ? "\u0421\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c" : "\u041f\u0435\u0440\u0435\u0437\u0430\u043f\u0443\u0441\u043a", bx, by, 40.0f, bh, saveHov ? accent.brighter(0.1f) : accent, Color.WHITE, this::saveAndReload);
        boolean toggleHov = this.hover(bx -= 29.0f, by, 26.0f, bh);
        Color tBg = running ? (toggleHov ? Color.rgba(235, 75, 75, 60) : Color.rgba(235, 75, 75, 30)) : (toggleHov ? Color.rgba(46, 204, 113, 60) : Color.rgba(46, 204, 113, 30));
        this.pill(g, running ? "\u0421\u0442\u043e\u043f" : "\u041f\u0443\u0441\u043a", bx, by, 26.0f, bh, tBg, Color.WHITE, () -> {
            if (script.status == ScriptInstance.Status.READY) {
                script.toggle();
                ScriptConsole.info(script.id, script.isEnabled() ? "\u0437\u0430\u043f\u0443\u0449\u0435\u043d" : "\u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d");
            } else if (script.status == ScriptInstance.Status.ERROR) {
                ScriptConsole.error(script.id, "\u0441\u043a\u0440\u0438\u043f\u0442 \u0441 \u043e\u0448\u0438\u0431\u043a\u043e\u0439 \u043d\u0435\u043b\u044c\u0437\u044f \u0437\u0430\u043f\u0443\u0441\u0442\u0438\u0442\u044c: " + script.error);
            }
        });
        String tabLabel = this.view == 0 ? "\u041d\u0430\u0441\u0442\u0440\u043e\u0439\u043a\u0438 (" + script.settings.size() + ")" : "\u041a\u043e\u0434";
        boolean tabHov = this.hover(bx -= 58.0f, by, 55.0f, bh);
        this.pill(g, tabLabel, bx, by, 55.0f, bh, tabHov ? Color.rgba(255, 255, 255, 25) : Color.rgba(255, 255, 255, 10), Color.WHITE, () -> {
            this.view = this.view == 0 ? 1 : 0;
            this.focusedString = null;
            this.listeningSetting = null;
            this.editorFocused = false;
        });
        float areaX = x + 3.0f;
        float areaY = y + 18.0f;
        float areaW = w - 6.0f;
        float areaH = 129.0f;
        RoundedRect.of(areaX, areaY, areaW, areaH).radius(3.5f).color(Color.rgba(14, 16, 22, 170)).render(g);
        if (this.editorFocused && this.view == 0) {
            Outline.of(areaX, areaY, areaW, areaH).radius(3.5f).thickness(0.6f).color(accent.withAlpha(90)).render(g);
        }
        if (this.view == 0) {
            this.settingsW = 0.0f;
            this.renderCode(g, areaX, areaY, areaW, areaH, accent);
        } else {
            this.codeW = 0.0f;
            this.renderSettings(g, areaX, areaY, areaW, areaH, script, accent);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderCode(GuiGraphicsExtractor g, float x, float y, float w, float h, Color accent) {
        float mid;
        float lineY;
        int l;
        this.codeX = x;
        this.codeY = y;
        this.codeW = w;
        this.codeH = h;
        float textX = x + 19.0f + 2.0f;
        float viewW = w - 19.0f - 6.0f;
        float viewH = h - 4.0f;
        int lines = this.editor.lineCount();
        float maxScrollY = Math.max(0.0f, (float)lines * 7.0f - viewH + 2.0f);
        if (this.editorFocused) {
            String caretLineText;
            float caretX;
            float caretTop = (float)this.editor.caretLine() * 7.0f;
            if (caretTop - this.codeScrollY < 0.0f) {
                this.codeScrollY = caretTop;
            }
            if (caretTop + 7.0f - this.codeScrollY > viewH) {
                this.codeScrollY = caretTop + 7.0f - viewH;
            }
            if ((caretX = this.measure(caretLineText = this.editor.line(this.editor.caretLine()), 0, this.editor.caretColumn())) - this.codeScrollX < 0.0f) {
                this.codeScrollX = Math.max(0.0f, caretX - 20.0f);
            }
            if (caretX - this.codeScrollX > viewW - 4.0f) {
                this.codeScrollX = caretX - viewW + 20.0f;
            }
        }
        this.codeScrollY = Math.clamp(this.codeScrollY, 0.0f, maxScrollY);
        this.codeScrollX = Math.max(0.0f, this.codeScrollX);
        int first = Math.max(0, (int)(this.codeScrollY / 7.0f));
        int last = Math.min(lines - 1, (int)((this.codeScrollY + viewH) / 7.0f) + 1);
        int selStart = this.editor.selectionStart();
        int selEnd = this.editor.selectionEnd();
        int caretLine = this.editor.caretLine();
        boolean blink = System.currentTimeMillis() / 500L % 2L == 0L;
        Scissor.push(g, x, y + 2.0f, w, viewH);
        try {
            for (l = first; l <= last; ++l) {
                lineY = y + 3.0f + (float)l * 7.0f - this.codeScrollY;
                mid = lineY + 3.5f;
                if (this.editorFocused && l == caretLine && !this.editor.hasSelection()) {
                    RoundedRect.of(x + 1.0f, lineY, w - 2.0f, 7.0f).color(Color.rgba(255, 255, 255, 6)).render(g);
                }
                Text.of(Fonts.MEDIUM, String.valueOf(l + 1), x + 19.0f - 5.0f, mid, 5.5f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(l == caretLine && this.editorFocused ? Color.rgb(160, 165, 180) : Color.rgb(80, 85, 98)).render(g);
            }
        }
        finally {
            Scissor.pop(g);
        }
        Scissor.push(g, textX - 1.0f, y + 2.0f, viewW + 3.0f, viewH);
        try {
            for (l = first; l <= last; ++l) {
                KotlinHighlighter.Span span;
                float sx;
                lineY = y + 3.0f + (float)l * 7.0f - this.codeScrollY;
                mid = lineY + 3.5f;
                String text = this.editor.line(l);
                int lineStart = this.editor.lineStart(l);
                int lineEnd = lineStart + text.length();
                float baseX = textX - this.codeScrollX;
                if (selStart != selEnd && selStart <= lineEnd && selEnd >= lineStart) {
                    int a = Math.max(selStart, lineStart) - lineStart;
                    int b = Math.min(selEnd, lineEnd) - lineStart;
                    sx = baseX + this.measure(text, 0, a);
                    float sw = this.measure(text, a, b) + (selEnd > lineEnd ? 2.5f : 0.0f);
                    if (sw > 0.0f) {
                        RoundedRect.of(sx, lineY, sw, 7.0f).color(accent.withAlpha(70)).render(g);
                    }
                }
                Iterator<KotlinHighlighter.Span> a = KotlinHighlighter.tokenize(text).iterator();
                while (a.hasNext() && !((sx = baseX + this.measure(text, 0, (span = a.next()).start())) > textX + viewW + 4.0f)) {
                    float ex = sx + this.measure(text, span.start(), span.end());
                    if (ex < textX - 2.0f) continue;
                    Text.of(Fonts.MEDIUM, text.substring(span.start(), span.end()), sx, mid, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(span.color()).render(g);
                }
                if (!this.editorFocused || !blink || l != caretLine) continue;
                float cx = baseX + this.measure(text, 0, this.editor.caretColumn());
                RoundedRect.of(cx, lineY + 0.5f, 0.6f, 6.0f).color(Color.WHITE).render(g);
            }
        }
        finally {
            Scissor.pop(g);
        }
        if (maxScrollY > 1.0f) {
            float trackY = y + 3.0f;
            float trackH = h - 6.0f;
            float thumbH = Math.max(10.0f, viewH / ((float)lines * 7.0f) * trackH);
            float thumbY = trackY + this.codeScrollY / maxScrollY * (trackH - thumbH);
            RoundedRect.of(x + w - 3.0f, thumbY, 1.5f, thumbH).radius(0.75f).color(accent.withAlpha(110)).render(g);
        }
        if (this.editorFocused) {
            String pos = this.editor.caretLine() + 1 + ":" + (this.editor.caretColumn() + 1);
            Text.of(Fonts.MEDIUM, pos, x + w - 5.0f, y + h - 4.5f, 4.6f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(90, 95, 110)).render(g);
        }
    }

    private int[] codePosAt(float mx, float my) {
        float textX = this.codeX + 19.0f + 2.0f;
        int line = (int)Math.floor((my - this.codeY - 3.0f + this.codeScrollY) / 7.0f);
        line = Math.max(0, Math.min(this.editor.lineCount() - 1, line));
        int col = this.columnAt(this.editor.line(line), mx - textX + this.codeScrollX);
        return new int[]{line, col};
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderSettings(GuiGraphicsExtractor g, float x, float y, float w, float h, ScriptInstance script, Color accent) {
        this.settingsX = x;
        this.settingsY = y;
        this.settingsW = w;
        this.settingsH = h;
        float px = x + 6.0f;
        float pw = w - 12.0f;
        float cy = y + 4.0f - this.settingsScroll;
        Scissor.push(g, x, y + 1.0f, w, h - 2.0f);
        try {
            List<Setting<?>> settings;
            float rowH = 13.0f;
            Text.of(Fonts.MEDIUM, "\u0411\u0438\u043d\u0434 \u0441\u043a\u0440\u0438\u043f\u0442\u0430", px, cy + rowH * 0.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(TEXT_MAIN).render(g);
            String bindName = this.listeningScriptBind ? "[...]" : (script.bind == -1 || script.bind == 0 ? "[-]" : "[" + Module.getKeyName(script.bind) + "]");
            float bw = Text.of(Fonts.MEDIUM, bindName).size(5.2f).width() + 8.0f;
            this.pill(g, bindName, px + pw - bw, cy + 2.0f, bw, 9.0f, this.listeningScriptBind ? accent.withAlpha(80) : Color.rgba(255, 255, 255, 14), Color.WHITE, () -> {
                this.listeningScriptBind = !this.listeningScriptBind;
                this.listeningSetting = null;
                this.focusedString = null;
            });
            cy += rowH + 2.0f;
            if (!script.description.isEmpty()) {
                Text.of(Fonts.MEDIUM, this.ellipsize(script.description, 5.0f, pw), px, cy + 4.0f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(TEXT_MUTED).render(g);
                cy += 10.0f;
            }
            if (script.status == ScriptInstance.Status.ERROR && script.error != null) {
                for (String part : this.wrap(script.error, 5.0f, pw)) {
                    Text.of(Fonts.MEDIUM, part, px, cy + 4.0f, 5.0f).verticalAlign(VerticalAlign.MIDDLE).color(RED).render(g);
                    cy += 7.0f;
                }
                cy += 3.0f;
            }
            if ((settings = script.nativeSettings()).isEmpty()) {
                Text.of(Fonts.MEDIUM, "\u0423 \u0441\u043a\u0440\u0438\u043f\u0442\u0430 \u043d\u0435\u0442 \u043d\u0430\u0441\u0442\u0440\u043e\u0435\u043a", x + w * 0.5f, cy + 10.0f, 5.8f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(TEXT_MUTED).render(g);
                cy += 20.0f;
            }
            for (Setting<?> s : settings) {
                if (!s.isVisible()) continue;
                cy = this.renderSetting(g, s, px, cy, pw, accent) + 3.0f;
            }
        }
        finally {
            Scissor.pop(g);
        }
        for (float[] b : this.middleBoxes) {
            float boxBottom = Math.min(b[1] + b[3], y + h);
            b[1] = Math.max(b[1], y);
            b[3] = Math.max(0.0f, boxBottom - b[1]);
        }
        float contentH = cy + this.settingsScroll - (y + 4.0f);
        this.settingsMaxScroll = Math.max(0.0f, contentH - h + 6.0f);
        this.settingsScroll = Math.clamp(this.settingsScroll, 0.0f, this.settingsMaxScroll);
    }

    private List<String> wrap(String text, float size, float maxW) {
        ArrayList<String> out = new ArrayList<String>();
        for (String raw : text.split("\n")) {
            StringBuilder line = new StringBuilder();
            for (String word : raw.split(" ")) {
                String candidate;
                String string = candidate = line.isEmpty() ? word : String.valueOf(line) + " " + word;
                if (Text.of(Fonts.MEDIUM, candidate).size(size).width() > maxW && !line.isEmpty()) {
                    out.add(line.toString());
                    line = new StringBuilder(word);
                    continue;
                }
                line = new StringBuilder(candidate);
            }
            if (!line.isEmpty()) {
                out.add(line.toString());
            }
            if (out.size() > 12) break;
        }
        return out;
    }

    private float renderSetting(GuiGraphicsExtractor g, Setting<?> s, float x, float y, float w, Color accent) {
        if (s instanceof BooleanSetting) {
            BooleanSetting bs = (BooleanSetting)s;
            float h = 13.0f;
            Text.of(Fonts.MEDIUM, bs.getName(), x, y + h * 0.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(TEXT_MAIN).render(g);
            float chkW = 14.0f;
            float chkH = 8.0f;
            float chkX = x + w - chkW;
            float chkY = y + (h - chkH) * 0.5f;
            boolean on = bs.isEnabled();
            RoundedRect.of(chkX, chkY, chkW, chkH).radius(chkH * 0.5f).color(on ? accent : Color.rgba(45, 48, 60, 200)).render(g);
            float kr = chkH * 0.5f - 1.0f;
            float kx = on ? chkX + chkW - kr * 2.0f - 1.0f : chkX + 1.0f;
            RoundedRect.of(kx, chkY + 1.0f, kr * 2.0f, kr * 2.0f).radius(kr).color(Color.WHITE).render(g);
            this.box(x, y, w, h, bs::toggle);
            return y + h;
        }
        if (s instanceof FloatSetting) {
            float pct;
            FloatSetting fs = (FloatSetting)s;
            float h = 17.0f;
            this.middleBoxes.add(new float[]{x, y, w, h});
            this.middleActions.add(() -> {
                this.draggingSlider = null;
                this.draggingNumber = null;
                fs.reset();
            });
            Text.of(Fonts.MEDIUM, fs.getName(), x, y + 4.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(TEXT_MAIN).render(g);
            String val = fs.getStep() >= 1.0f ? String.valueOf(Math.round(((Float)fs.get()).floatValue())) : String.format(Locale.ROOT, "%.2f", fs.get());
            Text.of(Fonts.MEDIUM, val + fs.getSuffix(), x + w, y + 4.5f, 5.5f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(accent).render(g);
            float trkY = y + 11.0f;
            RoundedRect.of(x, trkY, w, 3.0f).radius(1.5f).color(Color.rgba(255, 255, 255, 15)).render(g);
            float range = fs.getMax() - fs.getMin();
            float f = pct = range <= 0.0f ? 0.0f : Math.clamp((((Float)fs.get()).floatValue() - fs.getMin()) / range, 0.0f, 1.0f);
            if (pct > 0.0f) {
                RoundedRect.of(x, trkY, w * pct, 3.0f).radius(1.5f).color(accent).render(g);
            }
            RoundedRect.of(x + w * pct - 2.5f, trkY - 1.0f, 5.0f, 5.0f).radius(2.5f).color(Color.WHITE).render(g);
            this.box(x, y + 7.0f, w, 10.0f, () -> {
                this.draggingSlider = fs;
                this.dragTrackX = x;
                this.dragTrackW = w;
                this.applySlider(this.mouseX);
            });
            return y + h;
        }
        if (s instanceof NumberSetting) {
            float pct;
            NumberSetting ns = (NumberSetting)s;
            float h = 17.0f;
            this.middleBoxes.add(new float[]{x, y, w, h});
            this.middleActions.add(() -> {
                this.draggingSlider = null;
                this.draggingNumber = null;
                ns.reset();
            });
            Text.of(Fonts.MEDIUM, ns.getName(), x, y + 4.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(TEXT_MAIN).render(g);
            Text.of(Fonts.MEDIUM, ns.getFormattedValue(), x + w, y + 4.5f, 5.5f).align(TextAlign.RIGHT).verticalAlign(VerticalAlign.MIDDLE).color(accent).render(g);
            float trkY = y + 11.0f;
            RoundedRect.of(x, trkY, w, 3.0f).radius(1.5f).color(Color.rgba(255, 255, 255, 15)).render(g);
            double range = ns.max() - ns.min();
            float f = pct = range <= 0.0 ? 0.0f : (float)Math.clamp((ns.getValue() - ns.min()) / range, 0.0, 1.0);
            if (pct > 0.0f) {
                RoundedRect.of(x, trkY, w * pct, 3.0f).radius(1.5f).color(accent).render(g);
            }
            this.box(x, y + 7.0f, w, 10.0f, () -> {
                this.draggingNumber = ns;
                this.dragTrackX = x;
                this.dragTrackW = w;
                this.applySlider(this.mouseX);
            });
            return y + h;
        }
        if (s instanceof ModeSetting) {
            ModeSetting ms = (ModeSetting)s;
            return this.renderChips(g, ms.getName(), ms.getModes(), ms::is, ms::set, x, y, w, accent);
        }
        if (s instanceof MultiBooleanSetting) {
            MultiBooleanSetting mb = (MultiBooleanSetting)s;
            return this.renderChips(g, mb.getName(), mb.getOptions(), mb::isSelected, mb::toggleOption, x, y, w, accent);
        }
        if (s instanceof StringSetting) {
            StringSetting ss = (StringSetting)s;
            Text.of(Fonts.MEDIUM, ss.getName(), x, y + 4.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(TEXT_MAIN).render(g);
            float fy = y + 9.0f;
            float fh = 10.0f;
            boolean focused = this.focusedString == ss;
            RoundedRect.of(x, fy, w, fh).radius(2.5f).color(Color.rgba(255, 255, 255, focused ? 16 : 8)).render(g);
            if (focused) {
                Outline.of(x, fy, w, fh).radius(2.5f).thickness(0.6f).color(accent.withAlpha(120)).render(g);
            }
            String value = ss.get() == null ? "" : ss.get();
            String shown = value + (focused && System.currentTimeMillis() / 500L % 2L == 0L ? "_" : "");
            Text.of(Fonts.MEDIUM, this.ellipsizeLeft(shown, 5.2f, w - 6.0f), x + 3.0f, fy + fh * 0.5f, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(g);
            this.box(x, fy, w, fh, () -> {
                this.focusedString = this.focusedString == ss ? null : ss;
                this.listeningSetting = null;
                this.listeningScriptBind = false;
                this.editorFocused = false;
            });
            return fy + fh;
        }
        if (s instanceof BindSetting) {
            BindSetting bs = (BindSetting)s;
            float h = 13.0f;
            Text.of(Fonts.MEDIUM, bs.getName(), x, y + h * 0.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(TEXT_MAIN).render(g);
            String name = this.listeningSetting == bs ? "[...]" : (bs.getKey() == 0 || bs.getKey() == -1 ? "[-]" : "[" + Module.getKeyName(bs.getKey()) + "]");
            float bw = Text.of(Fonts.MEDIUM, name).size(5.2f).width() + 8.0f;
            this.pill(g, name, x + w - bw, y + 2.0f, bw, 9.0f, this.listeningSetting == bs ? accent.withAlpha(80) : Color.rgba(255, 255, 255, 14), Color.WHITE, () -> {
                this.listeningSetting = this.listeningSetting == bs ? null : bs;
                this.listeningScriptBind = false;
                this.focusedString = null;
            });
            return y + h;
        }
        if (s instanceof ColorSetting) {
            ColorSetting cs = (ColorSetting)s;
            float h = 18.0f;
            Text.of(Fonts.MEDIUM, cs.getName(), x, y + 4.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(TEXT_MAIN).render(g);
            RoundedRect.of(x + w - 14.0f, y + 1.0f, 14.0f, 7.0f).radius(2.0f).color(cs.getColor()).render(g);
            float barY = y + 11.0f;
            int segments = 12;
            float segW = w / (float)segments;
            for (int i = 0; i < segments; ++i) {
                Color left = Color.hsb((float)i / (float)segments, 1.0f, 1.0f);
                Color right = Color.hsb((float)(i + 1) / (float)segments, 1.0f, 1.0f);
                RoundedRect.of(x + (float)i * segW, barY, segW + 0.3f, 4.0f).horizontalGradient(left, right).render(g);
            }
            float hx = x + Math.clamp(cs.getHue(), 0.0f, 1.0f) * w;
            RoundedRect.of(hx - 1.0f, barY - 1.0f, 2.0f, 6.0f).radius(1.0f).color(Color.WHITE).render(g);
            this.box(x, barY - 2.0f, w, 8.0f, () -> {
                this.draggingHue = cs;
                this.dragTrackX = x;
                this.dragTrackW = w;
                this.applySlider(this.mouseX);
            });
            return y + h;
        }
        if (s instanceof ButtonSetting) {
            ButtonSetting bt = (ButtonSetting)s;
            float h = 12.0f;
            boolean hov = this.hover(x, y, w, h);
            this.pill(g, bt.getName(), x, y, w, h, hov ? accent.withAlpha(120) : accent.withAlpha(60), Color.WHITE, bt::runAction);
            return y + h;
        }
        Text.of(Fonts.MEDIUM, s.getName(), x, y + 6.0f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(TEXT_MUTED).render(g);
        return y + 12.0f;
    }

    private String ellipsizeLeft(String s, float size, float maxW) {
        if (Text.of(Fonts.MEDIUM, s).size(size).width() <= maxW) {
            return s;
        }
        String cut = s;
        while (cut.length() > 1 && Text.of(Fonts.MEDIUM, ".." + cut).size(size).width() > maxW) {
            cut = cut.substring(1);
        }
        return ".." + cut;
    }

    private float renderChips(GuiGraphicsExtractor g, String name, List<String> options, Predicate<String> active, Consumer<String> click, float x, float y, float w, Color accent) {
        Text.of(Fonts.MEDIUM, name, x, y + 4.5f, 5.5f).verticalAlign(VerticalAlign.MIDDLE).color(TEXT_MAIN).render(g);
        float chipX = x;
        float chipY = y + 9.5f;
        float chipH = 9.5f;
        for (String option : options) {
            float chipW = Text.of(Fonts.MEDIUM, option).size(5.0f).width() + 8.0f;
            if (chipX + chipW > x + w && chipX > x) {
                chipX = x;
                chipY += chipH + 3.0f;
            }
            boolean sel = active.test(option);
            boolean hov = this.hover(chipX, chipY, chipW, chipH);
            RoundedRect.of(chipX, chipY, chipW, chipH).radius(2.0f).color(sel ? accent : (hov ? Color.rgba(255, 255, 255, 14) : Color.rgba(255, 255, 255, 6))).render(g);
            Text.of(Fonts.MEDIUM, option, chipX + chipW * 0.5f, chipY + chipH * 0.5f, 5.0f).centered().verticalAlign(VerticalAlign.MIDDLE).color(sel ? Color.WHITE : Color.rgb(160, 165, 180)).render(g);
            this.box(chipX, chipY, chipW, chipH, () -> click.accept(option));
            chipX += chipW + 3.0f;
        }
        return chipY + chipH;
    }

    private void applySlider(float mx) {
        float pct;
        float f = pct = this.dragTrackW <= 0.0f ? 0.0f : Math.clamp((mx - this.dragTrackX) / this.dragTrackW, 0.0f, 1.0f);
        if (this.draggingSlider != null) {
            FloatSetting fs = this.draggingSlider;
            float v = fs.getMin() + pct * (fs.getMax() - fs.getMin());
            if (fs.getStep() > 0.0f) {
                v = (float)Math.round(v / fs.getStep()) * fs.getStep();
            }
            fs.set(v);
        } else if (this.draggingNumber != null) {
            NumberSetting ns = this.draggingNumber;
            double v = ns.min() + (double)pct * (ns.max() - ns.min());
            if (ns.step() > 0.0) {
                v = (double)Math.round(v / ns.step()) * ns.step();
            }
            ns.setValue(v);
        } else if (this.draggingHue != null) {
            this.draggingHue.setRainbow(false);
            Color c = Color.hsb(Math.min(0.999f, pct), 1.0f, 1.0f);
            this.draggingHue.setColor(Color.rgba(c.red(), c.green(), c.blue(), this.draggingHue.getAlpha()));
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private void renderConsole(GuiGraphicsExtractor g, float x, float y, float w, float h, Color accent) {
        RoundedRect.of(x, y, w, h).radius(5.0f).color(ScriptsPanel.cardBg()).render(g);
        Text.of(Fonts.MEDIUM, "\u041a\u043e\u043d\u0441\u043e\u043b\u044c", x + 7.0f, y + 8.0f, 7.0f).verticalAlign(VerticalAlign.MIDDLE).color(Color.WHITE).render(g);
        float clrX = x + w - 36.0f;
        float mcpX = clrX - 28.0f;
        boolean mcpOn = McpServer.get().isRunning();
        boolean mcpHov = this.hover(mcpX, y + 3.0f, 25.0f, 10.0f);
        this.pill(g, "MCP", mcpX, y + 3.0f, 25.0f, 10.0f, mcpOn ? (mcpHov ? Color.rgba(46, 204, 113, 90) : Color.rgba(46, 204, 113, 50)) : (mcpHov ? accent.withAlpha(120) : accent.withAlpha(70)), Color.WHITE, () -> {
            this.mcpOpen = true;
            this.mcpView.reset();
            this.unfocus();
        });
        float clrY = y + 3.0f;
        boolean clrHov = this.hover(clrX, clrY, 32.0f, 10.0f);
        this.pill(g, "\u041e\u0447\u0438\u0441\u0442\u0438\u0442\u044c", clrX, clrY, 32.0f, 10.0f, clrHov ? Color.rgba(255, 255, 255, 15) : Color.rgba(255, 255, 255, 5), clrHov ? Color.WHITE : TEXT_MUTED, () -> {
            ScriptConsole.clear();
            this.consoleScroll = 0.0f;
            this.consoleFollow = true;
        });
        RoundedRect.of(x + 5.0f, y + 16.0f, w - 10.0f, 1.0f).color(Color.rgba(255, 255, 255, 6)).render(g);
        this.consoleX = x;
        this.consoleY = y + 18.0f;
        this.consoleW = w;
        this.consoleH = h - 20.0f;
        List<ScriptConsole.Line> lines = ScriptConsole.snapshot();
        float rowH = 7.5f;
        float textW = w - 14.0f;
        List<Object[]> rows = new ArrayList();
        for (ScriptConsole.Line line : lines) {
            Color col = switch (line.level) {
                case ScriptConsole.Level.ERROR -> Color.rgb(235, 110, 110);
                case ScriptConsole.Level.WARN -> YELLOW;
                default -> Color.rgb(180, 185, 200);
            };
            String full = line.time + "  " + line.script + " - " + line.text;
            for (String part : this.wrap(full, 5.2f, textW)) {
                rows.add(new Object[]{part, col});
            }
        }
        if (rows.size() > 400) {
            rows = rows.subList(rows.size() - 400, rows.size());
        }
        this.consoleMaxScroll = Math.max(0.0f, (float)rows.size() * rowH - this.consoleH + 2.0f);
        if (this.consoleFollow) {
            this.consoleScroll = this.consoleMaxScroll;
        }
        this.consoleScroll = Math.clamp(this.consoleScroll, 0.0f, this.consoleMaxScroll);
        if (rows.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u041f\u0443\u0441\u0442\u043e", x + w * 0.5f, this.consoleY + this.consoleH * 0.5f, 5.5f).align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgb(90, 95, 110)).render(g);
            return;
        }
        Scissor.push(g, x, this.consoleY, w, this.consoleH);
        try {
            int first;
            int i = first = Math.max(0, (int)(this.consoleScroll / rowH));
            while (i < rows.size()) {
                float ry = this.consoleY + 1.0f + (float)i * rowH - this.consoleScroll;
                if (ry > this.consoleY + this.consoleH) {
                    return;
                }
                Text.of(Fonts.MEDIUM, (String)((Object[])rows.get(i))[0], x + 7.0f, ry + rowH * 0.5f, 5.2f).verticalAlign(VerticalAlign.MIDDLE).color((Color)((Object[])rows.get(i))[1]).render(g);
                ++i;
            }
            return;
        }
        finally {
            Scissor.pop(g);
        }
    }

    private void select(String id) {
        if (id.equals(this.selectedId)) {
            return;
        }
        if (this.editor.modified() && this.editorFor != null) {
            this.saveCurrent(false);
        }
        this.selectedId = id;
        this.editorFocused = false;
        this.focusedString = null;
        this.listeningSetting = null;
        this.listeningScriptBind = false;
    }

    private void deleteScript(String id) {
        ScriptInstance script = ScriptManager.get().byId(id);
        Object fileName = script != null ? script.file.getName() : id + ".kts";
        boolean ok = ScriptManager.get().delete(id);
        if (ok) {
            ScriptConsole.info("scripts", (String)fileName + " \u0443\u0434\u0430\u043b\u0451\u043d");
        } else {
            ScriptConsole.warn("scripts", "\u043d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0443\u0434\u0430\u043b\u0438\u0442\u044c " + (String)fileName);
        }
        if (id.equals(this.editorFor)) {
            this.editorFor = null;
            this.editor.setText("");
        }
        if (id.equals(this.selectedId)) {
            this.selectedId = null;
        }
    }

    private void openSettings(String id) {
        this.select(id);
        this.selectedId = id;
        this.view = 1;
        this.settingsScroll = 0.0f;
        this.editorFocused = false;
    }

    private void createScript() {
        File file;
        File dir = ScriptManager.get().directory();
        int n = 1;
        do {
            file = new File(dir, "script" + n + ".kts");
            ++n;
        } while (file.exists());
        try {
            Files.writeString(file.toPath(), (CharSequence)TEMPLATE, StandardCharsets.UTF_8, new OpenOption[0]);
            ScriptManager.get().load(file);
            String id = file.getName().substring(0, file.getName().length() - 4);
            this.select(id);
            this.selectedId = id;
            this.view = 0;
            this.editorFocused = true;
            ScriptConsole.info("scripts", file.getName() + " \u0441\u043e\u0437\u0434\u0430\u043d");
        }
        catch (IOException e) {
            ScriptConsole.error("scripts", "\u043d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u0441\u043a\u0440\u0438\u043f\u0442: " + e.getMessage());
        }
    }

    private boolean saveCurrent(boolean reload) {
        if (this.editorFor == null) {
            return false;
        }
        ScriptInstance script = ScriptManager.get().byId(this.editorFor);
        File file = script != null ? script.file : new File(ScriptManager.get().directory(), this.editorFor + ".kts");
        try {
            Files.writeString(file.toPath(), (CharSequence)this.editor.text(), StandardCharsets.UTF_8, new OpenOption[0]);
            this.editor.markSaved();
            this.editorFileStamp = file.lastModified();
            if (reload) {
                ScriptManager.get().load(file);
            }
            return true;
        }
        catch (IOException e) {
            ScriptConsole.error(this.editorFor, "\u043d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0445\u0440\u0430\u043d\u0438\u0442\u044c: " + e.getMessage());
            return false;
        }
    }

    private void saveAndReload() {
        if (this.editorFor == null) {
            return;
        }
        if (this.saveCurrent(true)) {
            ScriptConsole.info(this.editorFor, "\u0441\u043e\u0445\u0440\u0430\u043d\u0451\u043d, \u043f\u0435\u0440\u0435\u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0430...");
        }
    }

    public boolean mouseClicked(float mx, float my, int button) {
        this.mouseX = mx;
        this.mouseY = my;
        if (this.mcpOpen) {
            return button == 1 && this.mcpView.mouseClicked(mx, my);
        }
        if (this.docsOpen) {
            return button == 1 && this.docsView.mouseClicked(mx, my);
        }
        if (button == 2) {
            for (int i = this.middleBoxes.size() - 1; i >= 0; --i) {
                float[] b = this.middleBoxes.get(i);
                if (!(b[3] > 0.0f) || !(mx >= b[0]) || !(mx <= b[0] + b[2]) || !(my >= b[1]) || !(my <= b[1] + b[3])) continue;
                this.middleActions.get(i).run();
                return true;
            }
            return false;
        }
        if (button == 3) {
            for (int i = this.rightBoxes.size() - 1; i >= 0; --i) {
                float[] b = this.rightBoxes.get(i);
                if (!(mx >= b[0]) || !(mx <= b[0] + b[2]) || !(my >= b[1]) || !(my <= b[1] + b[3])) continue;
                this.rightActions.get(i).run();
                return true;
            }
            return false;
        }
        if (button != 1) {
            return false;
        }
        if (this.listeningScriptBind || this.listeningSetting != null) {
            this.listeningScriptBind = false;
            this.listeningSetting = null;
        }
        StringSetting wasFocused = this.focusedString;
        for (int i = this.boxes.size() - 1; i >= 0; --i) {
            float[] b = this.boxes.get(i);
            if (!(mx >= b[0]) || !(mx <= b[0] + b[2]) || !(my >= b[1]) || !(my <= b[1] + b[3])) continue;
            if (this.focusedString == wasFocused) {
                this.focusedString = null;
            }
            this.editorFocused = false;
            this.actions.get(i).run();
            return true;
        }
        this.focusedString = null;
        if (this.codeW > 0.0f && mx >= this.codeX && mx <= this.codeX + this.codeW && my >= this.codeY && my <= this.codeY + this.codeH) {
            this.editorFocused = true;
            int[] pos = this.codePosAt(mx, my);
            int flat = this.editor.lineStart(pos[0]) + pos[1];
            long now = System.currentTimeMillis();
            if (now - this.lastClickTime < 350L && flat == this.lastClickPos) {
                this.editor.selectWordAt(pos[0], pos[1]);
                this.selectingText = false;
            } else {
                boolean shift = InputConstants.isKeyDown((int)225) || InputConstants.isKeyDown((int)229);
                this.editor.moveTo(pos[0], pos[1], shift);
                this.selectingText = true;
            }
            this.lastClickTime = now;
            this.lastClickPos = flat;
            return true;
        }
        this.editorFocused = false;
        return false;
    }

    public boolean mouseDragged(float mx, float my) {
        this.mouseX = mx;
        this.mouseY = my;
        if (this.draggingSlider != null || this.draggingNumber != null || this.draggingHue != null) {
            this.applySlider(mx);
            return true;
        }
        if (this.selectingText && this.editorFocused && this.codeW > 0.0f) {
            if (my < this.codeY) {
                this.codeScrollY = Math.max(0.0f, this.codeScrollY - 7.0f);
            }
            if (my > this.codeY + this.codeH) {
                this.codeScrollY += 7.0f;
            }
            int[] pos = this.codePosAt(mx, my);
            this.editor.moveTo(pos[0], pos[1], true);
            return true;
        }
        return false;
    }

    public void mouseReleased() {
        this.draggingSlider = null;
        this.draggingNumber = null;
        this.draggingHue = null;
        this.selectingText = false;
    }

    public void mouseScrolled(float mx, float my, double amount) {
        if (this.mcpOpen) {
            this.mcpView.mouseScrolled(mx, my, amount);
            return;
        }
        if (this.docsOpen) {
            this.docsView.mouseScrolled(mx, my, amount);
            return;
        }
        if (mx >= this.listX && mx <= this.listX + this.listW && my >= this.listY && my <= this.listY + this.listH) {
            this.listScrollTarget = Math.clamp(this.listScrollTarget - (float)amount * 20.0f, 0.0f, this.listMaxScroll);
        } else if (this.codeW > 0.0f && mx >= this.codeX && mx <= this.codeX + this.codeW && my >= this.codeY && my <= this.codeY + this.codeH) {
            boolean shift;
            boolean bl = shift = InputConstants.isKeyDown((int)225) || InputConstants.isKeyDown((int)229);
            if (shift) {
                this.codeScrollX = Math.max(0.0f, this.codeScrollX - (float)amount * 20.0f);
            } else {
                this.codeScrollY = Math.max(0.0f, this.codeScrollY - (float)amount * 7.0f * 3.0f);
            }
        } else if (this.settingsW > 0.0f && mx >= this.settingsX && mx <= this.settingsX + this.settingsW && my >= this.settingsY && my <= this.settingsY + this.settingsH) {
            this.settingsScroll = Math.clamp(this.settingsScroll - (float)amount * 18.0f, 0.0f, this.settingsMaxScroll);
        } else if (mx >= this.consoleX && mx <= this.consoleX + this.consoleW && my >= this.consoleY && my <= this.consoleY + this.consoleH) {
            this.consoleScroll = Math.clamp(this.consoleScroll - (float)amount * 15.0f, 0.0f, this.consoleMaxScroll);
            this.consoleFollow = this.consoleScroll >= this.consoleMaxScroll - 0.5f;
        }
    }

    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (this.mcpOpen && key == 41) {
            this.mcpOpen = false;
            return true;
        }
        if (this.docsOpen && key == 41) {
            this.docsOpen = false;
            return true;
        }
        if (this.listeningScriptBind) {
            ScriptInstance script;
            ScriptInstance scriptInstance = script = this.editorFor == null ? null : ScriptManager.get().byId(this.editorFor);
            if (script != null && key != 41) {
                boolean clear = key == 76 || key == 42;
                script.bind = clear ? -1 : key;
                ScriptManager.get().markDirty();
            }
            this.listeningScriptBind = false;
            return true;
        }
        if (this.listeningSetting != null) {
            if (key != 41) {
                boolean clear = key == 76 || key == 42;
                this.listeningSetting.setKey(clear ? -1 : key);
            }
            this.listeningSetting = null;
            return true;
        }
        if (this.focusedString != null) {
            String clip;
            StringSetting ss = this.focusedString;
            String value = ss.get() == null ? "" : ss.get();
            boolean ctrl = event.hasControlDown();
            if (key == 41 || key == 40 || key == 88) {
                this.focusedString = null;
            } else if (key == 42) {
                if (!value.isEmpty()) {
                    ss.setValue(ctrl ? "" : value.substring(0, value.length() - 1));
                }
            } else if (ctrl && key == 25 && (clip = Minecraft.getInstance().keyboardHandler.getClipboard()) != null) {
                ss.setValue(value + clip.replace("\r", "").replace("\n", ""));
            }
            return true;
        }
        if (this.editorFocused) {
            boolean ctrl;
            boolean bl = ctrl = event.hasControlDown() || InputConstants.isKeyDown((int)224) || InputConstants.isKeyDown((int)228);
            if (key == 41) {
                this.editorFocused = false;
                return true;
            }
            if (ctrl && key == 22) {
                this.saveAndReload();
                return true;
            }
            this.editor.key(event);
            return true;
        }
        return false;
    }

    public boolean charTyped(CharacterEvent event) {
        if (!event.isAllowedChatCharacter()) {
            return false;
        }
        String c = event.codepointAsString();
        if (this.focusedString != null) {
            this.focusedString.setValue((this.focusedString.get() == null ? "" : this.focusedString.get()) + c);
            return true;
        }
        if (this.editorFocused) {
            this.editor.type(c);
            return true;
        }
        return false;
    }
}

