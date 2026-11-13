/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.util.render.display.text;

import brokke.cc.util.animation.Easing;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.scissor.Scissor;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Glyph;
import brokke.cc.util.render.display.text.font.MsdfFont;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class RollingNumber {
    private static final Easing EASE = Easing.CUBIC_OUT;
    private static final String DIGITS = "0123456789";
    private static final String[] DIGIT_STRINGS = new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
    private static final float BAND_PAD = 0.12f;
    private static final float FADE_SPEED = 1.15f;
    private static final float SLIDE_RATE = 22.0f;
    private final List<Slot> slots = new ArrayList<Slot>();
    private String settled = "";
    private float totalWidth;
    private float duration = 0.26f;
    private float stagger = 0.04f;

    public RollingNumber duration(float seconds) {
        this.duration = Math.max(0.01f, seconds);
        return this;
    }

    public RollingNumber stagger(float seconds) {
        this.stagger = Math.max(0.0f, seconds);
        return this;
    }

    public float width() {
        return this.totalWidth;
    }

    public boolean animating() {
        for (Slot slot : this.slots) {
            if (!slot.animating) continue;
            return true;
        }
        return false;
    }

    public void update(MsdfFont font, float size, String value, float dt) {
        String next;
        String string = next = value == null ? "" : value;
        if (!next.equals(this.settled)) {
            int i;
            float dir = RollingNumber.direction(this.settled, next);
            int needed = Math.max(next.length(), this.settled.length());
            while (this.slots.size() < needed) {
                this.slots.add(new Slot());
            }
            int first = -1;
            for (i = 0; i < this.slots.size(); ++i) {
                if (this.slots.get((int)i).incoming == RollingNumber.fromRight(next, i)) continue;
                first = i;
                break;
            }
            for (i = 0; i < this.slots.size(); ++i) {
                Slot slot = this.slots.get(i);
                char target = RollingNumber.fromRight(next, i);
                if (slot.incoming == target) continue;
                slot.restart(target, dir, (float)(i - first) * this.stagger, this.duration);
            }
            this.settled = next;
        }
        for (Slot slot : this.slots) {
            slot.advance(dt);
        }
        while (!this.slots.isEmpty()) {
            Slot last = this.slots.get(this.slots.size() - 1);
            if (last.incoming != '\u0000' || last.animating) break;
            this.slots.remove(this.slots.size() - 1);
        }
        this.layout(font, size, dt);
    }

    private void layout(MsdfFont font, float size, float dt) {
        float digitCell = RollingNumber.digitCell(font, size);
        float cursor = 0.0f;
        for (int i = this.slots.size() - 1; i >= 0; --i) {
            Slot slot = this.slots.get(i);
            if (slot.incoming == '\u0000') continue;
            slot.cell = RollingNumber.cellWidth(font, size, slot.incoming, digitCell);
            slot.targetX = cursor;
            cursor += slot.cell;
        }
        float factor = 1.0f - (float)Math.exp(-22.0f * dt);
        if (cursor >= this.totalWidth) {
            this.totalWidth = cursor;
        } else {
            this.totalWidth += (cursor - this.totalWidth) * factor;
            if (Math.abs(cursor - this.totalWidth) < 0.02f) {
                this.totalWidth = cursor;
            }
        }
        for (Slot slot : this.slots) {
            if (Float.isNaN(slot.relX)) {
                slot.relX = slot.targetX;
                continue;
            }
            slot.relX += (slot.targetX - slot.relX) * factor;
            if (!(Math.abs(slot.targetX - slot.relX) < 0.02f)) continue;
            slot.relX = slot.targetX;
        }
    }

    public void render(GuiGraphicsExtractor graphics, MsdfFont font, float x, float midY, float size, Color color, float alpha) {
        float bandHeight;
        if (this.slots.isEmpty() || alpha <= 0.0f) {
            return;
        }
        if (!this.animating()) {
            this.drawCells(graphics, font, x, midY, size, color, alpha, 0.0f);
            return;
        }
        float[] band = RollingNumber.band(font, size);
        float bandTop = (float)Math.floor(midY + band[0]);
        float slide = bandHeight = (float)Math.ceil(midY + band[1]) - bandTop;
        float clipX = x - size;
        float clipW = this.width() + size * 2.0f;
        Scissor.push(graphics, clipX, bandTop, clipW, bandHeight);
        this.drawCells(graphics, font, x, midY, size, color, alpha, slide);
        Scissor.pop(graphics);
    }

    private void drawCells(GuiGraphicsExtractor graphics, MsdfFont font, float x, float midY, float size, Color color, float alpha, float slide) {
        for (Slot slot : this.slots) {
            float fade;
            float offset;
            if (slot.incoming == '\u0000' && slot.outgoing == '\u0000') continue;
            float cellX = x + (Float.isNaN(slot.relX) ? slot.targetX : slot.relX);
            if (slide <= 0.0f || !slot.animating) {
                RollingNumber.drawChar(graphics, font, slot.incoming, cellX, slot.cell, midY, size, color, alpha);
                continue;
            }
            float progress = slot.progress();
            if (slot.outgoing != '\u0000') {
                offset = (slot.outFrom + (-slot.dir - slot.outFrom) * progress) * slide;
                fade = Math.max(0.0f, slot.outFromAlpha - progress * 1.15f);
                RollingNumber.drawChar(graphics, font, slot.outgoing, cellX, slot.cell, midY + offset, size, color, alpha * fade);
            }
            if (slot.incoming == '\u0000') continue;
            offset = slot.dir * (1.0f - progress) * slide;
            fade = Math.min(1.0f, progress * 1.15f);
            RollingNumber.drawChar(graphics, font, slot.incoming, cellX, slot.cell, midY + offset, size, color, alpha * fade);
        }
    }

    private static void drawChar(GuiGraphicsExtractor graphics, MsdfFont font, char c, float cellX, float cell, float midY, float size, Color color, float alpha) {
        if (c == '\u0000' || alpha <= 0.0f) {
            return;
        }
        float charW = font.advance(c) * size;
        float drawX = RollingNumber.isDigit(c) ? cellX + (cell - charW) * 0.5f : cellX;
        int a = (int)(255.0f * Math.clamp(alpha, 0.0f, 1.0f));
        Text.of(font, RollingNumber.glyphString(c), drawX, midY, size).verticalAlign(VerticalAlign.MIDDLE).color(Color.rgba(color.red(), color.green(), color.blue(), a)).render(graphics);
    }

    public static float measure(MsdfFont font, float size, String value) {
        if (value == null || value.isEmpty()) {
            return 0.0f;
        }
        float digitCell = RollingNumber.digitCell(font, size);
        float total = 0.0f;
        for (int i = 0; i < value.length(); ++i) {
            total += RollingNumber.cellWidth(font, size, value.charAt(i), digitCell);
        }
        return total;
    }

    private static float cellWidth(MsdfFont font, float size, char c, float digitCell) {
        return RollingNumber.isDigit(c) ? digitCell : font.advance(c) * size;
    }

    private static float digitCell(MsdfFont font, float size) {
        float max = 0.0f;
        for (int i = 0; i < DIGITS.length(); ++i) {
            max = Math.max(max, font.advance(DIGITS.charAt(i)));
        }
        return max * size;
    }

    private static float[] band(MsdfFont font, float size) {
        float ascender = font.metrics().ascender();
        float lineHeight = font.metrics().lineHeight();
        float inkPad = font.atlas().size() > 0.0f ? font.atlas().distanceRange() / (2.0f * font.atlas().size()) : 0.0f;
        float top = Float.MAX_VALUE;
        float bottom = -3.4028235E38f;
        for (int i = 0; i < DIGITS.length(); ++i) {
            Glyph glyph = font.glyph(DIGITS.charAt(i));
            if (glyph == null || !glyph.visible()) continue;
            top = Math.min(top, ascender - glyph.top() + inkPad);
            bottom = Math.max(bottom, ascender - glyph.bottom() - inkPad);
        }
        if (top > bottom) {
            top = 0.0f;
            bottom = lineHeight;
        }
        float originTop = -lineHeight * size * 0.5f;
        float glyphTop = originTop + top * size;
        float glyphBottom = originTop + bottom * size;
        float pad = (glyphBottom - glyphTop) * 0.12f;
        return new float[]{glyphTop - pad, glyphBottom + pad};
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private static String glyphString(char c) {
        return RollingNumber.isDigit(c) ? DIGIT_STRINGS[c - 48] : String.valueOf(c);
    }

    private static char fromRight(String s, int indexFromRight) {
        int i = s.length() - 1 - indexFromRight;
        return i >= 0 ? s.charAt(i) : (char)'\u0000';
    }

    private static float direction(String from, String to) {
        try {
            if (!from.isEmpty() && !to.isEmpty()) {
                double a = Double.parseDouble(from);
                double b = Double.parseDouble(to);
                if (b != a) {
                    return b > a ? 1.0f : -1.0f;
                }
            }
        }
        catch (NumberFormatException numberFormatException) {
            // empty catch block
        }
        return to.length() >= from.length() ? 1.0f : -1.0f;
    }

    private static final class Slot {
        private char outgoing;
        private char incoming;
        private float dir = 1.0f;
        private float time;
        private float delay;
        private float duration = 0.26f;
        private float outFrom;
        private float outFromAlpha = 1.0f;
        private boolean animating;
        private float targetX;
        private float relX = Float.NaN;
        private float cell;

        private Slot() {
        }

        private void restart(char target, float newDir, float newDelay, float newDuration) {
            float carryOffset = 0.0f;
            float carryAlpha = 1.0f;
            if (this.animating && this.incoming != '\u0000') {
                float p = this.progress();
                carryOffset = this.dir * (1.0f - p);
                carryAlpha = Math.min(1.0f, p * 1.15f);
            }
            this.outgoing = this.incoming;
            this.outFrom = carryOffset;
            this.outFromAlpha = carryAlpha;
            this.incoming = target;
            this.dir = newDir;
            this.time = 0.0f;
            this.delay = newDelay;
            this.duration = newDuration;
            this.animating = true;
        }

        private void advance(float dt) {
            if (!this.animating) {
                return;
            }
            this.time += dt;
            if (this.time >= this.delay + this.duration) {
                this.animating = false;
                this.outgoing = '\u0000';
                this.outFrom = 0.0f;
                this.outFromAlpha = 1.0f;
            }
        }

        private float progress() {
            if (!this.animating || this.duration <= 0.0f) {
                return 1.0f;
            }
            float t = Math.clamp(this.time - this.delay, 0.0f, this.duration);
            return EASE.ease(t, 0.0f, 1.0f, this.duration);
        }
    }
}

