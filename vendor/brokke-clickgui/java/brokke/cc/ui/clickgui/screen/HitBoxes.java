/*
 * Decompiled with CFR 0.152.
 */
package brokke.cc.ui.clickgui.screen;

import java.util.ArrayList;
import java.util.List;

public final class HitBoxes {
    private final List<float[]> hitBoxes = new ArrayList<float[]>();
    private final List<Runnable> hitActions = new ArrayList<Runnable>();
    private final List<Runnable> hitRightActions = new ArrayList<Runnable>();
    private final List<float[]> middleBoxes = new ArrayList<float[]>();
    private final List<Runnable> middleActions = new ArrayList<Runnable>();
    private float[] clip;

    public void clear() {
        this.clip = null;
        this.hitBoxes.clear();
        this.hitActions.clear();
        this.hitRightActions.clear();
        this.middleBoxes.clear();
        this.middleActions.clear();
    }

    public void hit(float x, float y, float w, float h, Runnable left, Runnable right) {
        float[] box = this.clipped(x, y, w, h);
        if (box == null) {
            return;
        }
        this.hitBoxes.add(box);
        this.hitActions.add(left);
        this.hitRightActions.add(right);
    }

    public void middle(float x, float y, float w, float h, Runnable action) {
        float[] box = this.clipped(x, y, w, h);
        if (box == null) {
            return;
        }
        this.middleBoxes.add(box);
        this.middleActions.add(action);
    }

    public void setClip(float x, float y, float w, float h) {
        this.clip = new float[]{x, y, Math.max(0.0f, w), Math.max(0.0f, h)};
    }

    public void clearClip() {
        this.clip = null;
    }

    private float[] clipped(float x, float y, float w, float h) {
        float[] fArray;
        float right = x + w;
        float bottom = y + h;
        if (this.clip != null) {
            right = Math.min(right, this.clip[0] + this.clip[2]);
            bottom = Math.min(bottom, this.clip[1] + this.clip[3]);
            x = Math.max(x, this.clip[0]);
            y = Math.max(y, this.clip[1]);
        }
        if (right > x && bottom > y) {
            float[] fArray2 = new float[4];
            fArray2[0] = x;
            fArray2[1] = y;
            fArray2[2] = right - x;
            fArray = fArray2;
            fArray2[3] = bottom - y;
        } else {
            fArray = null;
        }
        return fArray;
    }

    public boolean clickMiddle(float mx, float my) {
        for (int i = this.middleBoxes.size() - 1; i >= 0; --i) {
            float[] b = this.middleBoxes.get(i);
            if (!(mx >= b[0]) || !(mx <= b[0] + b[2]) || !(my >= b[1]) || !(my <= b[1] + b[3])) continue;
            this.middleActions.get(i).run();
            return true;
        }
        return false;
    }

    public boolean click(float mx, float my, boolean left, boolean right) {
        for (int i = this.hitBoxes.size() - 1; i >= 0; --i) {
            Runnable action;
            float[] b = this.hitBoxes.get(i);
            if (!(mx >= b[0]) || !(mx <= b[0] + b[2]) || !(my >= b[1]) || !(my <= b[1] + b[3])) continue;
            action = left ? this.hitActions.get(i) : right ? this.hitRightActions.get(i) : null;
            if (action == null) continue;
            action.run();
            return true;
        }
        return false;
    }
}

