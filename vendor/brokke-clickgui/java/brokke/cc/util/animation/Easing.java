/*
 * Decompiled with CFR 0.152.
 */
package brokke.cc.util.animation;

public interface Easing {
    public static final Easing LINEAR = (t, b, c, d) -> c * t / d + b;
    public static final Easing QUAD_OUT = (t, b, c, d) -> {
        if (d <= 0.0f || t <= 0.0f) {
            return b;
        }
        if (t >= d) {
            return b + c;
        }
        float v = t / d;
        return -c * v * (v - 2.0f) + b;
    };
    public static final Easing CUBIC_OUT = (t, b, c, d) -> {
        if (d <= 0.0f || t <= 0.0f) {
            return b;
        }
        if (t >= d) {
            return b + c;
        }
        float f = t / d - 1.0f;
        return c * (f * f * f + 1.0f) + b;
    };
    public static final Easing CUBIC_IN_OUT = (t, b, c, d) -> {
        if (d <= 0.0f || t <= 0.0f) {
            return b;
        }
        if (t >= d) {
            return b + c;
        }
        float x = t / (d / 2.0f);
        return x < 1.0f ? c / 2.0f * x * x * x + b : c / 2.0f * ((x - 2.0f) * (x - 2.0f) * (x - 2.0f) + 2.0f) + b;
    };
    public static final Easing EXPO_OUT = (t, b, c, d) -> t >= d ? b + c : c * (-((float)Math.pow(2.0, -10.0f * t / d)) + 1.0f) + b;
    public static final Easing SPRING = Easing.generate(0.2f, 1.02f, 0.35f, 1.0);
    public static final Easing SPRING_SOFT = Easing.generate(0.16f, 1.0, 0.3f, 1.0);
    public static final Easing BACK_OUT = Easing.generate(0.34f, 1.56f, 0.64f, 1.0);
    public static final Easing SMOOTH_OUT = (t, b, c, d) -> {
        if (d <= 0.0f || t <= 0.0f) {
            return b;
        }
        if (t >= d) {
            return b + c;
        }
        float x = t / d;
        return c * (1.0f - (1.0f - x) * (1.0f - x) * (1.0f - x)) + b;
    };

    public float ease(float var1, float var2, float var3, float var4);

    public static Easing generate(final double x1, final double y1, final double x2, final double y2) {
        return new Easing(){

            @Override
            public float ease(float t, float b, float c, float d) {
                if (d <= 0.0f || t <= 0.0f) {
                    return b;
                }
                if (t >= d) {
                    return b + c;
                }
                float progress = Math.clamp(t / d, 0.0f, 1.0f);
                float tBez = this.solveTBez((float)x1, (float)x2, progress);
                float y = this.bezierY(tBez, (float)y1, (float)y2);
                return b + c * y;
            }

            private float solveTBez(float x12, float x22, float progress) {
                float t = progress;
                for (int i = 0; i < 8; ++i) {
                    float x = this.bezierX(t, x12, x22);
                    float dx = this.bezierDX(t, x12, x22);
                    if (Math.abs(x - progress) < 1.0E-5f || Math.abs(dx) < 1.0E-6f) break;
                    t -= (x - progress) / dx;
                    t = Math.clamp(t, 0.0f, 1.0f);
                }
                return t;
            }

            private float bezierX(float t, float x12, float x22) {
                return 3.0f * (1.0f - t) * (1.0f - t) * t * x12 + 3.0f * (1.0f - t) * t * t * x22 + t * t * t;
            }

            private float bezierDX(float t, float x12, float x22) {
                return 3.0f * ((1.0f - t) * (1.0f - 3.0f * t) * x12 + (2.0f * t - 3.0f * t * t) * x22) + 3.0f * t * t;
            }

            private float bezierY(float t, float y12, float y22) {
                return 3.0f * (1.0f - t) * (1.0f - t) * t * y12 + 3.0f * (1.0f - t) * t * t * y22 + t * t * t;
            }
        };
    }
}

