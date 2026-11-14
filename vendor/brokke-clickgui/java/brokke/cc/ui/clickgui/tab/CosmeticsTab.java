/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.client.model.HumanoidModel$ArmPose
 *  net.minecraft.client.player.LocalPlayer
 *  net.minecraft.client.renderer.entity.EntityRenderer
 *  net.minecraft.client.renderer.entity.state.AvatarRenderState
 *  net.minecraft.client.renderer.entity.state.EntityRenderState
 *  net.minecraft.client.renderer.entity.state.LivingEntityRenderState
 *  net.minecraft.resources.Identifier
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Pose
 *  org.joml.Quaternionf
 *  org.joml.Quaternionfc
 *  org.joml.Vector2f
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 */
package brokke.cc.ui.clickgui.tab;

import brokke.cc.core.cosmetics.SwordSkins;
import brokke.cc.core.cosmetics.system.CharacterCosmetics;
import brokke.cc.core.cosmetics.system.FiguraCosmetics;
import brokke.cc.core.cosmetics.system.LocalCosmetics;
import brokke.cc.core.cosmetics.system.WristWatches;
import brokke.cc.ui.clickgui.screen.ClickGuiContext;
import brokke.cc.ui.clickgui.screen.GuiComponent;
import brokke.cc.ui.clickgui.screen.GuiDraw;
import brokke.cc.util.animation.Animation;
import brokke.cc.util.animation.Easing;
import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.DisplayUtil;
import brokke.cc.util.render.display.image.Image;
import brokke.cc.util.render.display.outline.Outline;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.font.Fonts;
import brokke.cc.util.sound.ClientSound;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public final class CosmeticsTab
extends GuiComponent {
    private static final String[] COSMETIC_TABS = new String[]{"\u041c\u0435\u0447", "\u041f\u0435\u0440\u0441\u043e\u043d\u0430\u0436", "\u041f\u0438\u0442\u043e\u043c\u0446\u044b", "\u0421\u043f\u0438\u043d\u0430", "\u0413\u043e\u043b\u043e\u0432\u0430", "\u041f\u0440\u0435\u0434\u043c\u0435\u0442\u044b", "\u0427\u0430\u0441\u044b"};
    public int cosmeticTab = 0;
    public long cosmeticSwitchTime = 0L;
    private final Animation cosmeticTabAnim = new Animation();

    public CosmeticsTab(ClickGuiContext ctx) {
        super(ctx);
    }

    private static Identifier localPreview(int idx) {
        Identifier preview = LocalCosmetics.previewTexture(idx);
        return preview != null && preview.equals(LocalCosmetics.texture(idx)) ? null : preview;
    }

    private List<CosmeticItem> cosmeticItems(int tab) {
        ArrayList<CosmeticItem> items = new ArrayList<CosmeticItem>();
        if (tab == 0) {
            for (SwordSkins.Entry entry : SwordSkins.ALL) {
                items.add(new CosmeticItem("sword-" + entry.id(), entry.isDefault() ? "\u041e\u0431\u044b\u0447\u043d\u044b\u0439" : entry.displayName(), entry.texture(), () -> SwordSkins.isSkinSelected(entry.id()), () -> SwordSkins.setSelectedSkin(SwordSkins.isSkinSelected(entry.id()) ? "default" : entry.id())));
            }
        } else if (tab == 1) {
            FiguraCosmetics fig = FiguraCosmetics.get();
            for (int idx : fig.indicesFor(false)) {
                items.add(new CosmeticItem("fig-" + idx, fig.name(idx), fig.previewTexture(idx), () -> fig.isSelected(idx), () -> fig.select(idx)));
            }
        } else if (tab == 4) {
            for (int idx : LocalCosmetics.indicesForCategory(4)) {
                items.add(new CosmeticItem("local-" + idx, LocalCosmetics.name(idx), CosmeticsTab.localPreview(idx), () -> LocalCosmetics.isSelected(idx), () -> LocalCosmetics.toggle(idx)));
            }
            FiguraCosmetics fig = FiguraCosmetics.get();
            for (int idx : fig.indicesFor(true)) {
                items.add(new CosmeticItem("fighead-" + idx, fig.name(idx), fig.previewTexture(idx), () -> fig.isSelected(idx), () -> fig.select(idx)));
            }
        } else if (tab == 6) {
            for (WristWatches.Spec spec : WristWatches.ALL) {
                items.add(new CosmeticItem("watch-" + spec.id(), spec.name(), WristWatches.preview(spec), () -> WristWatches.isSelected(spec.id()), () -> WristWatches.toggle(spec.id())));
            }
        } else {
            for (int idx : LocalCosmetics.indicesForCategory(tab)) {
                items.add(new CosmeticItem("local-" + idx, LocalCosmetics.name(idx), CosmeticsTab.localPreview(idx), () -> LocalCosmetics.isSelected(idx), () -> LocalCosmetics.toggle(idx)));
            }
        }
        return items;
    }

    public float renderCosmetics(GuiGraphicsExtractor graphics, float x, float y, float w, float prog, float top, float bottom, long now) {
        float startY = y;
        float appear = GuiDraw.easeOut((float)(now - this.ctx.switchTime) / 380.0f);
        float a = prog * appear;
        y += (1.0f - appear) * 8.0f;
        Color accent = GuiDraw.getAccent();
        float previewW = Math.min(110.0f, w * 0.3f);
        this.renderCosmeticPreview(graphics, x, top, previewW, bottom - top - 2.0f, a);
        float tabH = 14.0f;
        int count = COSMETIC_TABS.length;
        float tabW = ((w -= previewW + 6.0f) - 4.0f) / (float)count;
        RoundedRect.of(x += previewW + 6.0f, y, w, tabH + 4.0f).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        this.cosmeticTabAnim.animate((float)this.cosmeticTab, 1.0f, 5.0f, Easing.CUBIC_OUT);
        float tabPos = this.cosmeticTabAnim.value();
        RoundedRect.of(x + 2.0f + tabPos * tabW, y + 2.0f, tabW, tabH).radius(4.0f).color(Color.rgba(255, 255, 255, Math.round(14.0f * a))).render(graphics);
        for (int i = 0; i < count; ++i) {
            int idx = i;
            float tx = x + 2.0f + (float)i * tabW;
            boolean hover = GuiDraw.canHit(y, tabH + 4.0f, top, bottom) && this.hovered(tx, y + 2.0f, tabW, tabH);
            String key = "cos-tab-" + i;
            float hv = this.anim(key, this.hov(key, hover), 7.0f);
            float act = Math.clamp(1.0f - Math.abs(tabPos - (float)i), 0.0f, 1.0f);
            Text.of(Fonts.MEDIUM, COSMETIC_TABS[i], tx + tabW * 0.5f, y + 2.0f + tabH * 0.5f, 5.2f).centered().color(GuiDraw.mix(Color.rgb(135, 140, 150), Color.rgb(240, 242, 248), Math.max(act, hv * 0.5f)).withAlpha(a)).render(graphics);
            if (!GuiDraw.canHit(y, tabH + 4.0f, top, bottom)) continue;
            this.hit(tx, y + 2.0f, tabW, tabH, () -> {
                if (this.cosmeticTab != idx) {
                    ClientSound.ui(idx > this.cosmeticTab ? ClientSound.Ui.CATEGORY_NEXT : ClientSound.Ui.CATEGORY_PREV);
                    this.cosmeticTab = idx;
                    this.cosmeticSwitchTime = System.currentTimeMillis();
                    this.ctx.scrollTarget = 0.0f;
                }
            }, null);
        }
        y += tabH + 4.0f + 6.0f;
        List<CosmeticItem> items = this.cosmeticItems(this.cosmeticTab);
        if (items.isEmpty()) {
            Text.of(Fonts.MEDIUM, "\u0412 \u044d\u0442\u043e\u0439 \u043a\u0430\u0442\u0435\u0433\u043e\u0440\u0438\u0438 \u043f\u043e\u043a\u0430 \u043d\u0438\u0447\u0435\u0433\u043e \u043d\u0435\u0442", x + w * 0.5f, y + 30.0f, 5.4f).centered().color(Color.rgb(115, 120, 132).withAlpha(a * GuiDraw.easeOut((float)(now - this.cosmeticSwitchTime) / 300.0f))).render(graphics);
            return y + 60.0f - startY;
        }
        int cols = 3;
        float gap = 5.0f;
        float cardW = (w - gap * (float)(cols - 1)) / (float)cols;
        float cardH = cardW * 0.78f + 14.0f;
        for (int i = 0; i < items.size(); ++i) {
            CosmeticItem item = items.get(i);
            float t = GuiDraw.easeOut((float)(now - this.cosmeticSwitchTime - (long)i * 25L) / 340.0f);
            float cx = x + (float)(i % cols) * (cardW + gap);
            float cy = y + (float)(i / cols) * (cardH + gap) + (1.0f - t) * 6.0f;
            if (cy > bottom || cy + cardH < top) continue;
            this.renderCosmeticCard(graphics, item, cx, cy, cardW, cardH, a * t, accent, top, bottom);
        }
        int rows = (items.size() + cols - 1) / cols;
        return (y += (float)rows * (cardH + gap) - gap) - startY;
    }

    private void renderCosmeticPreview(GuiGraphicsExtractor graphics, float x, float y, float w, float h, float a) {
        RoundedRect.of(x, y, w, h).radius(6.0f).color(Color.rgba(255, 255, 255, Math.round(6.0f * a))).render(graphics);
        Text.of(Fonts.MEDIUM, "\u041f\u0440\u0435\u0434\u043f\u0440\u043e\u0441\u043c\u043e\u0442\u0440", x + w * 0.5f, y + 9.0f, 5.2f).centered().color(Color.rgb(225, 228, 236).withAlpha(a)).render(graphics);
        float bx = x + 4.0f;
        float by = y + 18.0f;
        float bw = w - 8.0f;
        float bh = h - 24.0f;
        LocalPlayer player = this.ctx.minecraft().player;
        if (player == null) {
            Text.of(Fonts.MEDIUM, "\u0417\u0430\u0439\u0434\u0438\u0442\u0435 \u0432 \u043c\u0438\u0440", x + w * 0.5f, by + bh * 0.5f, 5.0f).centered().color(Color.rgb(115, 120, 132).withAlpha(a)).render(graphics);
        } else if (!this.ctx.hologramRendering && a > 0.05f) {
            this.renderPreviewEntity(graphics, player, bx, by, bw, bh);
        }
    }

    private void renderPreviewEntity(GuiGraphicsExtractor graphics, LocalPlayer player, float x, float y, float w, float h) {
        EntityRenderer renderer = this.ctx.minecraft().getEntityRenderDispatcher().getRenderer((Entity)player);
        EntityRenderState state = renderer.createRenderState((Entity)player, 1.0f);
        state.shadowPieces.clear();
        state.leashStates = null;
        state.outlineColor = 0;
        state.ageInTicks = 0.0f;
        state.nameTag = null;
        state.scoreText = null;
        state.displayFireAnimation = false;
        state.passengerOffset = null;
        state.boundingBoxWidth = 0.6f;
        state.boundingBoxHeight = 1.8f;
        if (state instanceof LivingEntityRenderState) {
            LivingEntityRenderState living = (LivingEntityRenderState)state;
            living.bodyRot = 180.0f;
            living.yRot = 0.0f;
            living.xRot = 0.0f;
            living.pose = Pose.STANDING;
            living.walkAnimationPos = 0.0f;
            living.walkAnimationSpeed = 0.0f;
            living.deathTime = 0.0f;
            living.isUpsideDown = false;
            living.isAutoSpinAttack = false;
            living.isFullyFrozen = false;
            living.isInWater = false;
            living.bedOrientation = null;
            living.scale = 1.0f;
            living.ageScale = 1.0f;
        }
        if (state instanceof AvatarRenderState) {
            AvatarRenderState avatar = (AvatarRenderState)state;
            avatar.isCrouching = false;
            avatar.isFallFlying = false;
            avatar.isVisuallySwimming = false;
            avatar.isPassenger = false;
            avatar.isUsingItem = false;
            avatar.swimAmount = 0.0f;
            avatar.swingAnimation = 0.0f;
            avatar.speedValue = 1.0f;
            avatar.fallFlyingTimeInTicks = 0.0f;
            avatar.shouldApplyFlyingYRot = false;
            avatar.flyingYRot = 0.0f;
            avatar.rightArmPose = HumanoidModel.ArmPose.EMPTY;
            avatar.leftArmPose = HumanoidModel.ArmPose.EMPTY;
            avatar.heldOnHead.clear();
        }
        Vector2f p0 = graphics.pose().transformPosition(x, y, new Vector2f());
        Vector2f p1 = graphics.pose().transformPosition(x + w, y + h, new Vector2f());
        int x0 = Math.round(p0.x);
        int y0 = Math.round(p0.y);
        int x1 = Math.round(p1.x);
        int y1 = Math.round(p1.y);
        if (x1 - x0 < 4 || y1 - y0 < 4) {
            return;
        }
        float height = CharacterCosmetics.previewHeight();
        float width = CharacterCosmetics.previewWidth();
        boolean wings = false;
        for (int idx : LocalCosmetics.selectedIndices()) {
            if (!"wings".equalsIgnoreCase(LocalCosmetics.type(idx))) continue;
            wings = true;
            break;
        }
        float wk = this.anim("preview-wings", wings, 6.0f);
        float plain = Math.min((float)(y1 - y0) * 0.86f / height, (float)(x1 - x0) * 0.9f / width);
        float winged = Math.min((float)(y1 - y0) * 0.72f / height, (float)(x1 - x0) * 0.56f / width);
        float size = plain + (winged - plain) * wk;
        Vector3f translation = new Vector3f(0.0f, height * 0.5f + 0.0625f, 0.0f);
        Quaternionf rotation = new Quaternionf().rotateZ((float)Math.PI);
        Quaternionf camera = new Quaternionf();
        DisplayUtil.nextStratum(graphics);
        graphics.entity(state, size, (Vector3fc)translation, (Quaternionfc)rotation, (Quaternionfc)camera, x0, y0, x1, y1);
        DisplayUtil.nextStratum(graphics);
    }

    private void renderCosmeticCard(GuiGraphicsExtractor graphics, CosmeticItem item, float x, float y, float w, float h, float a, Color accent, float top, float bottom) {
        boolean selected = item.selected().getAsBoolean();
        boolean hover = GuiDraw.canHit(y, h, top, bottom) && this.hovered(x, y, w, h);
        String key = "cos-" + item.key();
        float hv = this.anim(key, this.hov(key, hover), 7.0f);
        float sel = this.anim(String.valueOf(key) + "-sel", selected, 6.0f);
        RoundedRect.of(x, y, w, h).radius(5.0f).color(Color.rgba(255, 255, 255, Math.round((6.0f + 4.0f * hv) * a))).render(graphics);
        if (sel > 0.01f) {
            RoundedRect.of(x, y, w, h).radius(5.0f).color(accent.withAlpha(0.1f * sel * a)).render(graphics);
            Outline.of(x, y, w, h).radius(5.0f).thickness(0.8f).color(accent.withAlpha(0.85f * sel * a)).render(graphics);
        }
        float img = (h - 14.0f) * (0.86f + 0.08f * hv);
        float ix = x + (w - img) * 0.5f;
        float iy = y + (h - 14.0f - img) * 0.5f + 2.0f;
        if (item.texture() != null) {
            Image.of(item.texture(), ix, iy, img, img).alpha(a).render(graphics);
        } else {
            Text.of(Fonts.ICONS, "H", x + w * 0.5f, iy + img * 0.5f, img * 0.45f).centered().color(Color.rgb(110, 115, 126).withAlpha(a)).render(graphics);
        }
        Text.of(Fonts.MEDIUM, GuiDraw.fit(item.name(), 4.8f, w - 8.0f), x + w * 0.5f, y + h - 7.0f, 4.8f).centered().color(GuiDraw.mix(Color.rgb(150, 155, 166), Color.rgb(240, 242, 248), Math.max(sel, hv * 0.6f)).withAlpha(a)).render(graphics);
        if (sel > 0.01f) {
            float d = 8.0f;
            float dx = x + w - d - 3.0f;
            float dy = y + 3.0f;
            RoundedRect.of(dx, dy, d, d).radius(d * 0.5f).horizontalGradient(accent.withAlpha(sel * a), GuiDraw.getAccent2().withAlpha(sel * a)).render(graphics);
            Text.of(Fonts.ICONS, "Q", dx + d * 0.5f, dy + d * 0.5f, 5.0f).centered().color(Color.rgb(255, 255, 255).withAlpha(sel * a)).render(graphics);
        }
        if (GuiDraw.canHit(y, h, top, bottom)) {
            this.hit(x, y, w, h, () -> {
                item.toggle().run();
                ClientSound.checkbox(item.selected().getAsBoolean());
            }, null);
        }
    }

    private record CosmeticItem(String key, String name, Identifier texture, BooleanSupplier selected, Runnable toggle) {
    }
}

