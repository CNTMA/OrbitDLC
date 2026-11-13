/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 */
package brokke.cc.util.render.display;

import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.DisplayUtil;
import brokke.cc.util.render.display.rounded.RoundedRect;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class CustomTotemParticles {
    private final List<Spark> sparks = new ArrayList<Spark>();
    private long started;
    private float duration;
    private float spread;
    private String style;
    private int color;

    public void spawn(String style, int count, float size, float spread, float duration, int color) {
        if (System.nanoTime() - this.started < 100000000L) {
            return;
        }
        this.sparks.clear();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < Math.clamp((long)count, 1, 100); ++i) {
            this.sparks.add(new Spark((float)((double)i * Math.PI * 2.0 / (double)count + random.nextDouble(-0.08, 0.08)), random.nextFloat(0.5f, 1.0f), random.nextFloat(0.65f, 1.2f), size * random.nextFloat(2.2f, 4.2f), random.nextFloat(-2.0f, 2.0f)));
        }
        this.style = style;
        this.spread = spread;
        this.duration = Math.max(0.1f, duration);
        this.color = color;
        this.started = System.nanoTime();
    }

    public void clear() {
        this.sparks.clear();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void render(GuiGraphicsExtractor graphics) {
        if (this.sparks.isEmpty()) {
            return;
        }
        float age = (float)(System.nanoTime() - this.started) / 1.0E9f / this.duration;
        if (age >= 1.0f) {
            this.clear();
            return;
        }
        float motion = 1.0f - (float)Math.pow(1.0f - age, 3.0);
        float alpha = Math.min(1.0f, age * 15.0f) * (1.0f - age) * (1.0f - age);
        float cx = DisplayUtil.width() * 0.5f;
        float cy = DisplayUtil.height() * 0.5f;
        for (Spark spark : this.sparks) {
            float angle = spark.angle() + age * spark.spin() * 0.35f;
            float radius = 12.0f + this.spread * spark.radius() * motion;
            float x = cx + (float)Math.cos(angle) * radius;
            float y = cy + (float)Math.sin(angle) * radius * 0.7f - age * 18.0f * spark.speed();
            float size = spark.size() * (1.0f - age * 0.5f);
            Color accent = Color.of(this.color).withAlpha(alpha);
            graphics.pose().pushMatrix();
            try {
                graphics.pose().translate(x, y);
                graphics.pose().rotate(angle + age * spark.spin());
                if ("\u0420\u0443\u043d\u044b".equals(this.style)) {
                    CustomTotemParticles.line(graphics, -size, 0.0f, 0.0f, -size * 1.4f, accent);
                    CustomTotemParticles.line(graphics, 0.0f, -size * 1.4f, size, 0.0f, accent);
                    CustomTotemParticles.line(graphics, size, 0.0f, 0.0f, size * 1.4f, accent);
                    CustomTotemParticles.line(graphics, 0.0f, size * 1.4f, -size, 0.0f, accent);
                    CustomTotemParticles.line(graphics, 0.0f, -size * 2.0f, 0.0f, size * 2.0f, accent);
                    CustomTotemParticles.line(graphics, -size * 0.6f, size * 0.4f, size * 0.6f, -size * 0.4f, accent);
                    continue;
                }
                if ("\u0417\u0432\u0451\u0437\u0434\u044b".equals(this.style)) {
                    for (int i = 0; i < 8; ++i) {
                        float a = (float)i * (float)Math.PI / 4.0f;
                        float b = (float)(i + 1) * (float)Math.PI / 4.0f;
                        float ra = i % 2 == 0 ? size * 1.7f : size * 0.35f;
                        float rb = i % 2 == 0 ? size * 0.35f : size * 1.7f;
                        CustomTotemParticles.line(graphics, (float)Math.cos(a) * ra, (float)Math.sin(a) * ra, (float)Math.cos(b) * rb, (float)Math.sin(b) * rb, accent);
                    }
                    continue;
                }
                CustomTotemParticles.line(graphics, -size, size, 0.0f, -size * 1.8f, accent);
                CustomTotemParticles.line(graphics, 0.0f, -size * 1.8f, size * 0.7f, size * 0.4f, accent);
                CustomTotemParticles.line(graphics, size * 0.7f, size * 0.4f, -size, size, accent);
            }
            finally {
                graphics.pose().popMatrix();
            }
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void line(GuiGraphicsExtractor graphics, float x, float y, float endX, float endY, Color color) {
        float dx = endX - x;
        float dy = endY - y;
        float length = (float)Math.hypot(dx, dy);
        graphics.pose().pushMatrix();
        try {
            graphics.pose().translate(x, y);
            graphics.pose().rotate((float)Math.atan2(dy, dx));
            RoundedRect.of(0.0f, -1.8f, length, 3.6f).radius(1.8f).color(color.withAlpha((float)color.alpha() / 255.0f * 0.15f)).render(graphics);
            RoundedRect.of(0.0f, -0.6f, length, 1.2f).radius(0.6f).color(color).render(graphics);
        }
        finally {
            graphics.pose().popMatrix();
        }
    }

    private record Spark(float angle, float radius, float speed, float size, float spin) {
    }
}

