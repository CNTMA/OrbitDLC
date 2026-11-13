/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.renderer.state.level.CameraRenderState
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 */
package brokke.cc.util.render.display;

import brokke.cc.util.render.display.DisplayUtil;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class WorldToScreen {
    private static final float NEAR_CLIP = 0.05f;
    private static CameraRenderState lastCameraState;
    private static long frameId;
    private static final Matrix4f VIEW_PROJECTION;
    private static final ThreadLocal<float[]> LINE_RESULT;
    private static boolean valid;

    private WorldToScreen() {
    }

    public static void update(CameraRenderState cameraState) {
        lastCameraState = cameraState;
        boolean bl = valid = cameraState != null && cameraState.initialized && cameraState.pos != null && cameraState.viewRotationMatrix != null && cameraState.projectionMatrix != null;
        if (valid) {
            VIEW_PROJECTION.set((Matrix4fc)cameraState.projectionMatrix).mul((Matrix4fc)cameraState.viewRotationMatrix);
        }
        ++frameId;
    }

    public static long getFrameId() {
        return frameId;
    }

    public static CameraRenderState getCameraState() {
        return lastCameraState;
    }

    public static float[] project(Vec3 worldPos) {
        if (worldPos == null) {
            return null;
        }
        return WorldToScreen.project(worldPos.x, worldPos.y, worldPos.z);
    }

    public static float[] project(double wx, double wy, double wz) {
        float[] result = new float[3];
        return (float[])(WorldToScreen.project(wx, wy, wz, result) ? result : null);
    }

    public static boolean project(double wx, double wy, double wz, float[] result) {
        CameraRenderState cam = lastCameraState;
        if (!valid) {
            return false;
        }
        double rx = wx - cam.pos.x;
        double ry = wy - cam.pos.y;
        double rz = wz - cam.pos.z;
        float x = (float)rx;
        float y = (float)ry;
        float z = (float)rz;
        Matrix4f matrix = VIEW_PROJECTION;
        float w = matrix.m03() * x + matrix.m13() * y + matrix.m23() * z + matrix.m33();
        if (w <= 0.05f || !Float.isFinite(w)) {
            return false;
        }
        float ndcX = (matrix.m00() * x + matrix.m10() * y + matrix.m20() * z + matrix.m30()) / w;
        float ndcY = (matrix.m01() * x + matrix.m11() * y + matrix.m21() * z + matrix.m31()) / w;
        float sw = DisplayUtil.width();
        float sh = DisplayUtil.height();
        float screenX = (ndcX * 0.5f + 0.5f) * sw;
        float screenY = (1.0f - (ndcY * 0.5f + 0.5f)) * sh;
        result[0] = screenX;
        result[1] = screenY;
        result[2] = w;
        return true;
    }

    public static float[][] projectLine(Vec3 from, Vec3 to) {
        if (from == null || to == null) {
            return null;
        }
        float[] result = LINE_RESULT.get();
        if (!WorldToScreen.projectLine(from.x, from.y, from.z, to.x, to.y, to.z, result)) {
            return null;
        }
        return new float[][]{{result[0], result[1]}, {result[2], result[3]}};
    }

    public static boolean projectLine(double ax, double ay, double az, double bx, double by, double bz, float[] result) {
        CameraRenderState cam = lastCameraState;
        if (!valid) {
            return false;
        }
        Matrix4f m = VIEW_PROJECTION;
        float axr = (float)(ax - cam.pos.x);
        float ayr = (float)(ay - cam.pos.y);
        float azr = (float)(az - cam.pos.z);
        float bxr = (float)(bx - cam.pos.x);
        float byr = (float)(by - cam.pos.y);
        float bzr = (float)(bz - cam.pos.z);
        float aw = m.m03() * axr + m.m13() * ayr + m.m23() * azr + m.m33();
        float bw = m.m03() * bxr + m.m13() * byr + m.m23() * bzr + m.m33();
        if (!Float.isFinite(aw) || !Float.isFinite(bw) || aw < 0.05f && bw < 0.05f) {
            return false;
        }
        float x1 = m.m00() * axr + m.m10() * ayr + m.m20() * azr + m.m30();
        float y1 = m.m01() * axr + m.m11() * ayr + m.m21() * azr + m.m31();
        float x2 = m.m00() * bxr + m.m10() * byr + m.m20() * bzr + m.m30();
        float y2 = m.m01() * bxr + m.m11() * byr + m.m21() * bzr + m.m31();
        if (aw < 0.05f) {
            float t = (0.05f - aw) / (bw - aw);
            x1 += t * (x2 - x1);
            y1 += t * (y2 - y1);
            aw = 0.05f;
        } else if (bw < 0.05f) {
            float t = (0.05f - aw) / (bw - aw);
            x2 = x1 + t * (x2 - x1);
            y2 = y1 + t * (y2 - y1);
            bw = 0.05f;
        }
        float sw = DisplayUtil.width();
        float sh = DisplayUtil.height();
        result[0] = (x1 / aw * 0.5f + 0.5f) * sw;
        result[1] = (0.5f - y1 / aw * 0.5f) * sh;
        result[2] = (x2 / bw * 0.5f + 0.5f) * sw;
        result[3] = (0.5f - y2 / bw * 0.5f) * sh;
        return true;
    }

    static {
        VIEW_PROJECTION = new Matrix4f();
        LINE_RESULT = ThreadLocal.withInitial(() -> new float[4]);
    }
}

