/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  org.jspecify.annotations.Nullable
 */
package brokke.cc.util.render.display.text;

import brokke.cc.core.module.impl.render.GuiSettings;
import brokke.cc.core.module.impl.render.NameProtect;
import brokke.cc.core.module.impl.render.ported.StreamerMode;
import brokke.cc.ui.hud.SecretFont;
import brokke.cc.ui.hud.SecretStyle;
import brokke.cc.util.color.Color;
import brokke.cc.util.color.ColorMath;
import brokke.cc.util.color.Gradient;
import brokke.cc.util.render.display.batch.DisplayBatcher;
import brokke.cc.util.render.display.text.ColorFunction;
import brokke.cc.util.render.display.text.TextAlign;
import brokke.cc.util.render.display.text.TextBounds;
import brokke.cc.util.render.display.text.TextLayout;
import brokke.cc.util.render.display.text.TextRenderState;
import brokke.cc.util.render.display.text.VerticalAlign;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.render.display.text.font.MsdfFont;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.jspecify.annotations.Nullable;

public final class Text {
    public static final float DEFAULT_SIZE = 9.0f;
    private MsdfFont font = Fonts.defaultFont();
    private String text = "";
    private float x;
    private float y;
    private float size = 9.0f;
    private float letterSpacing;
    private TextAlign align = TextAlign.LEFT;
    private VerticalAlign verticalAlign = VerticalAlign.TOP;
    private int solid = -1;
    private @Nullable ColorFunction function;
    private float alpha = 1.0f;

    private Text() {
    }

    public static Text create() {
        return new Text();
    }

    public static Text of(String text) {
        return new Text().text(text);
    }

    public static Text of(MsdfFont font, String text) {
        return new Text().font(font).text(text);
    }

    public static Text of(MsdfFont font, String text, float x, float y, float size) {
        return Text.of(font, text).position(x, y).size(size);
    }

    public Text font(MsdfFont font) {
        this.font = font;
        return this;
    }

    public Text font(String name) {
        return this.font(Fonts.get(name));
    }

    public Text text(String text) {
        this.text = text == null ? "" : (NameProtect.isActive() ? NameProtect.protect(text) : text);
        return this;
    }

    public Text position(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Text size(float size) {
        this.size = size;
        return this;
    }

    public Text letterSpacing(float letterSpacing) {
        this.letterSpacing = letterSpacing;
        return this;
    }

    public Text align(TextAlign align) {
        this.align = align;
        return this;
    }

    public Text verticalAlign(VerticalAlign verticalAlign) {
        this.verticalAlign = verticalAlign;
        return this;
    }

    public Text centered() {
        return this.align(TextAlign.CENTER).verticalAlign(VerticalAlign.MIDDLE);
    }

    public Text centered(float x, float y) {
        return this.position(x, y).centered();
    }

    public Text color(int argb) {
        this.solid = argb;
        this.function = null;
        return this;
    }

    public Text color(Color color) {
        return this.color(color.argb());
    }

    public Text verticalGradient(int top, int bottom) {
        this.function = (glyph, px, py) -> ColorMath.lerp(top, bottom, py);
        return this;
    }

    public Text verticalGradient(Color top, Color bottom) {
        return this.verticalGradient(top.argb(), bottom.argb());
    }

    public Text horizontalGradient(int left, int right) {
        this.function = (glyph, px, py) -> ColorMath.lerp(left, right, px);
        return this;
    }

    public Text horizontalGradient(Color left, Color right) {
        return this.horizontalGradient(left.argb(), right.argb());
    }

    public Text corners(int topLeft, int topRight, int bottomRight, int bottomLeft) {
        this.function = (glyph, px, py) -> ColorMath.bilerp(topLeft, topRight, bottomRight, bottomLeft, px, py);
        return this;
    }

    public Text corners(Color topLeft, Color topRight, Color bottomRight, Color bottomLeft) {
        return this.corners(topLeft.argb(), topRight.argb(), bottomRight.argb(), bottomLeft.argb());
    }

    public Text gradient(Gradient gradient) {
        this.function = (glyph, px, py) -> gradient.sample(px, py);
        return this;
    }

    public Text colors(ColorFunction function) {
        this.function = function;
        return this;
    }

    public Text alpha(float alpha) {
        this.alpha = Math.clamp(alpha, 0.0f, 1.0f);
        return this;
    }

    public MsdfFont font() {
        return this.font;
    }

    public String text() {
        return this.text;
    }

    public TextLayout layout() {
        return TextLayout.of(this.font, this.text, this.letterSpacing / Math.max(this.size, 0.001f));
    }

    public float width() {
        if (SecretStyle.usesFont(this.font)) {
            return SecretFont.scaledWidth(this.text, this.size);
        }
        return this.layout().width() * this.size;
    }

    public float height() {
        if (SecretStyle.usesFont(this.font)) {
            return this.size * 1.25f;
        }
        return this.layout().height() * this.size;
    }

    public TextBounds bounds() {
        if (SecretStyle.usesFont(this.font)) {
            return new TextBounds(this.secretX(), this.secretY(), this.width(), this.height());
        }
        TextLayout layout = this.layout();
        return new TextBounds(this.originX(layout), this.originY(layout), layout.width() * this.size, layout.height() * this.size);
    }

    public TextRenderState build(GuiGraphicsExtractor graphics) {
        return this.build(graphics, this.layout());
    }

    public void render(GuiGraphicsExtractor graphics) {
        if (this.text.isEmpty() || this.size <= 0.0f || this.alpha <= 0.0f) {
            return;
        }
        if (SecretStyle.usesFont(this.font)) {
            SecretFont.scaledText(graphics, this.text, this.secretX(), this.secretY(), this.size, this.solid, this.function, this.alpha * DisplayBatcher.getAlphaScale());
            return;
        }
        TextLayout layout = this.layout();
        if (layout.glyphCount() == 0) {
            return;
        }
        DisplayBatcher.submit(graphics, this.build(graphics, layout));
    }

    private TextRenderState build(GuiGraphicsExtractor graphics, TextLayout layout) {
        ColorFunction colors = this.function;
        GuiSettings settings = GuiSettings.getInstance();
        if (settings != null && (this.font.name().equals("lucide") || this.font.name().contains("icons"))) {
            float phase = this.x * 0.002f + this.y * 0.001f;
            int left = settings.getAnimatedThemeColor(phase).withAlpha(this.solid >>> 24).argb();
            int right = settings.getAnimatedThemeColor(phase + 0.12f).withAlpha(this.solid >>> 24).argb();
            colors = (glyph, px, py) -> ColorMath.lerp(left, right, px);
        }
        colors = StreamerMode.nicknameColors(this.text, colors, this.solid);
        return new TextRenderState(layout, DisplayBatcher.pose(graphics), this.originX(layout), this.originY(layout), this.size, this.solid, colors, this.alpha * DisplayBatcher.getAlphaScale(), graphics.scissorStack.peek());
    }

    private float originX(TextLayout layout) {
        return this.x - layout.width() * this.size * this.align.factor();
    }

    private float secretX() {
        return this.x - this.width() * this.align.factor();
    }

    private float secretY() {
        return switch (this.verticalAlign) {
            default -> throw new MatchException(null, null);
            case VerticalAlign.TOP -> this.y;
            case VerticalAlign.MIDDLE -> this.y - this.height() * 0.5f;
            case VerticalAlign.BOTTOM -> this.y - this.height();
            case VerticalAlign.BASELINE -> this.y - this.size;
        };
    }

    private float originY(TextLayout layout) {
        return switch (this.verticalAlign) {
            default -> throw new MatchException(null, null);
            case VerticalAlign.TOP -> this.y;
            case VerticalAlign.MIDDLE -> this.y - layout.height() * this.size * 0.5f;
            case VerticalAlign.BOTTOM -> this.y - layout.height() * this.size;
            case VerticalAlign.BASELINE -> this.y - layout.ascender() * this.size;
        };
    }
}

