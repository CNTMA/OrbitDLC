/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Camera
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.renderer.state.GameRenderState
 *  net.minecraft.client.renderer.state.level.CameraRenderState
 *  net.minecraft.world.phys.Vec3
 *  org.joml.Vector3fc
 *  org.joml.Vector4f
 */
package brokke.cc.ui.clickgui;

import brokke.cc.ui.clickgui.NewClickGuiScreen;
import brokke.cc.util.render.display.DisplayUtil;
import brokke.cc.util.render.display.batch.DisplayBatcher;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;
import org.joml.Vector4f;

public final class ClickGuiWorldHologram {
    private static final long DURATION_MS = 500L;
    private static final double SPAWN_DIST = 2.0;
    private static boolean active = false;
    private static long startTime = 0L;
    private static float spawnScale = 1.0f;
    private static Vec3 worldCenter = Vec3.ZERO;
    private static Vec3 spawnForward = new Vec3(0.0, 0.0, 1.0);
    private static NewClickGuiScreen screenInstance = null;

    private ClickGuiWorldHologram() {
    }

    public static void spawn(NewClickGuiScreen screen) {
        Vec3 forward;
        Vec3 camPos;
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null || mc.gameRenderer == null) {
            ClickGuiWorldHologram.cancel();
            return;
        }
        Camera cam = mc.gameRenderer.mainCamera();
        if (cam != null && cam.isInitialized()) {
            camPos = cam.position();
            Vector3fc fwd = cam.forwardVector();
            forward = new Vec3((double)fwd.x(), (double)fwd.y(), (double)fwd.z()).normalize();
        } else {
            camPos = mc.player.getEyePosition();
            Vec3 lk = mc.player.getLookAngle();
            forward = lk != null && lk.lengthSqr() > 1.0E-4 ? lk.normalize() : new Vec3(0.0, 0.0, 1.0);
        }
        worldCenter = camPos.add(forward.scale(2.0));
        spawnForward = forward;
        startTime = System.currentTimeMillis();
        screenInstance = screen;
        spawnScale = screen.closingScale();
        active = true;
    }

    public static boolean isActive() {
        return active;
    }

    public static void cancel() {
        active = false;
        screenInstance = null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void render(GuiGraphicsExtractor graphics, Minecraft mc, GameRenderState state) {
        CameraRenderState cameraState;
        if (!active || screenInstance == null || mc == null || mc.player == null || mc.level == null) {
            return;
        }
        if (mc.gui != null && mc.gui.screen() instanceof NewClickGuiScreen) {
            return;
        }
        CameraRenderState cameraRenderState = cameraState = state != null && state.levelRenderState != null ? state.levelRenderState.cameraRenderState : null;
        if (cameraState == null || !cameraState.initialized || cameraState.pos == null || cameraState.viewRotationMatrix == null || cameraState.projectionMatrix == null) {
            return;
        }
        long elapsed = System.currentTimeMillis() - startTime;
        if (elapsed >= 500L) {
            ClickGuiWorldHologram.cancel();
            return;
        }
        float progress = (float)elapsed / 500.0f;
        double floatUp = (double)progress * 0.08;
        Vector4f clipCenter = ClickGuiWorldHologram.project(ClickGuiWorldHologram.worldCenter.x, ClickGuiWorldHologram.worldCenter.y + floatUp, ClickGuiWorldHologram.worldCenter.z, cameraState);
        if (clipCenter == null || clipCenter.w <= 0.05f) {
            return;
        }
        Vec3 camToCenter = worldCenter.subtract(cameraState.pos);
        if (spawnForward.dot(camToCenter) <= 0.05) {
            return;
        }
        float ndcX = clipCenter.x / clipCenter.w;
        float ndcY = clipCenter.y / clipCenter.w;
        if (Math.abs(ndcX) > 2.0f || Math.abs(ndcY) > 2.0f) {
            return;
        }
        float screenW = DisplayUtil.width();
        float screenH = DisplayUtil.height();
        float projX = (ndcX * 0.5f + 0.5f) * screenW;
        float projY = (1.0f - (ndcY * 0.5f + 0.5f)) * screenH;
        float baseScale = (float)(2.0 / Math.max(0.3, (double)clipCenter.w));
        float alpha = 1.0f;
        float shrink = 1.0f;
        if (progress > 0.15f) {
            float fadeP = (progress - 0.15f) / 0.85f;
            alpha = Math.max(0.0f, 1.0f - fadeP);
            shrink = 1.0f - 0.1f * fadeP;
        }
        if (alpha <= 0.01f) {
            return;
        }
        float finalScale = baseScale * shrink * spawnScale;
        float winW = 400.0f;
        float winH = 250.0f;
        graphics.pose().pushMatrix();
        graphics.pose().translate(projX, projY);
        graphics.pose().scale(finalScale, finalScale);
        graphics.pose().translate(-winW * 0.5f, -winH * 0.5f);
        DisplayBatcher.flush();
        DisplayBatcher.setAlphaScale(alpha);
        try {
            screenInstance.renderHologram(graphics, alpha);
        }
        finally {
            DisplayBatcher.flush();
            DisplayBatcher.setAlphaScale(1.0f);
            graphics.pose().popMatrix();
        }
    }

    private static Vector4f project(double x, double y, double z, CameraRenderState cameraState) {
        double rx = x - cameraState.pos.x;
        double ry = y - cameraState.pos.y;
        double rz = z - cameraState.pos.z;
        Vector4f vec = new Vector4f((float)rx, (float)ry, (float)rz, 1.0f);
        cameraState.viewRotationMatrix.transform(vec);
        cameraState.projectionMatrix.transform(vec);
        return vec;
    }
}

