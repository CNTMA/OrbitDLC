/*
 * Decompiled with CFR 0.152.
 */
package brokke.cc.util.animation;

import brokke.cc.util.animation.Easing;

public class Animation {
    private long duration = 280L;
    private float value;
    private Easing easing = Easing.SPRING_SOFT;
    private long startTime;
    private float startValue;
    private float targetValue;
    private boolean done = true;

    public Animation() {
        this(0.0f);
    }

    public Animation(float initialValue) {
        this.value = initialValue;
        this.startValue = initialValue;
        this.targetValue = initialValue;
    }

    public void set(float v) {
        this.value = v;
        this.startValue = v;
        this.targetValue = v;
        this.done = true;
    }

    public void animate(float target, float delta, float speed) {
        this.animate(target, delta, speed, this.easing, System.currentTimeMillis());
    }

    public void animate(float target, float delta, float speed, Easing customEasing) {
        this.animate(target, delta, speed, customEasing, System.currentTimeMillis());
    }

    public void animate(float target, float delta, float speed, long currentTime) {
        this.animate(target, delta, speed, this.easing, currentTime);
    }

    public void animate(float target, float delta, float speed, Easing customEasing, long currentTime) {
        long dur = (long)Math.clamp(1800.0f / Math.max(speed, 0.01f), 80.0f, 1200.0f);
        if (target != this.targetValue || this.duration != dur) {
            this.duration = dur;
            if (target != this.targetValue) {
                this.startValue = this.value;
                this.startTime = currentTime;
                this.done = false;
            }
            this.targetValue = target;
        }
        if (this.done) {
            this.value = this.targetValue;
            return;
        }
        long elapsed = currentTime - this.startTime;
        if (elapsed >= this.duration) {
            this.value = this.targetValue;
            this.done = true;
            return;
        }
        float progress = Math.clamp((float)elapsed / (float)this.duration, 0.0f, 1.0f);
        float easedProgress = customEasing.ease(progress, 0.0f, 1.0f, 1.0f);
        this.value = this.startValue + (this.targetValue - this.startValue) * easedProgress;
    }

    public boolean isDone() {
        return this.done;
    }

    public void setEasing(Easing easing) {
        this.easing = easing;
    }

    public float value() {
        return this.value;
    }
}
