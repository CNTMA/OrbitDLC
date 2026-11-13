/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.gui.GuiGraphicsExtractor
 *  net.minecraft.resources.Identifier
 */
package brokke.cc.ui.clickgui.widget;

import brokke.cc.util.color.Color;
import brokke.cc.util.render.display.image.Image;
import brokke.cc.util.render.display.rounded.RoundedRect;
import brokke.cc.util.render.display.text.Text;
import brokke.cc.util.render.display.text.font.Fonts;
import com.mojang.blaze3d.platform.NativeImage;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.slf4j.LoggerFactory;

public enum ServerBadge {
    HW(27.0f),
    DSMP(31.0f),
    RW(27.0f);

    private static final Identifier HW_LOGO;
    private static final Identifier RW_LOGO;
    private static Identifier hwThumbnail;
    private static Identifier rwThumbnail;
    private static boolean rwMissing;
    private final float width;

    private ServerBadge(float width) {
        this.width = width;
    }

    public static List<ServerBadge> forModule(String name) {
        return switch (name) {
            case "AutoSort" -> List.of(HW, DSMP);
            case "AutoBuy", "AutoCase", "AutoMine", "AutoSbor", "BaseFinder", "Bots", "KTLeave", "PvP Sell", "PvPFarm", "ItemCrack", "ExpkeeperHelp", "AuctionHelper", "ExperienceInfo", "Chest Stealer", "ChestStealer" -> List.of(HW);
            case "AutoSell", "AutoEC", "AnchorFarm" -> List.of(DSMP);
            case "AutoQuest", "AutoFish", "AutoFarm", "KeyFinder", "StructureFinder" -> List.of(RW);
            case "CreeperFarm", "AutoZamok" -> List.of(HW);
            default -> List.of();
        };
    }

    public float width() {
        return this.width;
    }

    private static Identifier hwTexture() {
        if (hwThumbnail == null) {
            hwThumbnail = ServerBadge.thumbnail(HW_LOGO, "holyworld");
        }
        return hwThumbnail;
    }

    private static Identifier rwTexture() {
        if (rwThumbnail == null && !rwMissing) {
            if (Minecraft.getInstance().getResourceManager().getResource(RW_LOGO).isEmpty()) {
                rwMissing = true;
            } else {
                rwThumbnail = ServerBadge.thumbnail(RW_LOGO, "reallyworld");
            }
        }
        return rwThumbnail;
    }

    private static Identifier thumbnail(Identifier logo, String key) {
        Minecraft mc = Minecraft.getInstance();
        try (InputStream in = mc.getResourceManager().getResourceOrThrow(logo).open();){
            BufferedImage src = ImageIO.read(in);
            BufferedImage dst = new BufferedImage(32, 32, 2);
            Graphics2D graphics = dst.createGraphics();
            java.awt.Image scaled = src.getScaledInstance(32, 32, java.awt.Image.SCALE_AREA_AVERAGING);
            try {
                graphics.drawImage(scaled, 0, 0, null);
            } finally {
                graphics.dispose();
                scaled.flush();
                src.flush();
            }
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream();){
                ImageIO.write(dst, "png", bos);
                NativeImage image = NativeImage.read(new ByteArrayInputStream(bos.toByteArray()));
                DynamicTexture texture = new DynamicTexture(() -> key + " badge thumbnail", image);
                texture.upload();
                Identifier thumbnail = Identifier.fromNamespaceAndPath("brokke", "gui/badges/" + key + "_thumbnail");
                mc.getTextureManager().register(thumbnail, texture);
                return thumbnail;
            }
            finally {
                dst.flush();
            }
        }
        catch (Exception e) {
            LoggerFactory.getLogger(ServerBadge.class).warn("Cannot prepare {} badge thumbnail", (Object)key, (Object)e);
            return logo;
        }
    }

    public void render(GuiGraphicsExtractor graphics, float x, float centerY, float alpha) {
        float y = centerY - 5.0f;
        double phase = (double)System.nanoTime() / 7.0E8;
        float pulse = 0.5f + 0.5f * (float)Math.sin(phase);
        if (this == HW) {
            RoundedRect.of(x, y, this.width, 10.0f).radius(3.0f).horizontalGradient(Color.rgb(69, 48, 26).lerp(Color.rgb(91, 61, 31), pulse), Color.rgb(40, 31, 24).lerp(Color.rgb(58, 45, 29), 1.0f - pulse)).alpha(alpha).render(graphics);
            Image.of(ServerBadge.hwTexture(), x + 1.0f, centerY - 4.5f, 9.0f, 9.0f).alpha(alpha).render(graphics);
            Text.of(Fonts.SEMIBOLD, "HW", x + 18.0f, centerY, 5.0f).centered().colors((glyph, px, py) -> Color.rgb(255, 198, 103).lerp(Color.rgb(255, 253, 234), 0.5f + 0.5f * (float)Math.sin((double)px * Math.PI * 2.0 - phase)).withAlpha(alpha).argb()).render(graphics);
        } else if (this == RW) {
            RoundedRect.of(x, y, this.width, 10.0f).radius(3.0f).horizontalGradient(Color.rgb(92, 22, 28).lerp(Color.rgb(120, 30, 36), pulse), Color.rgb(48, 16, 20).lerp(Color.rgb(68, 22, 26), 1.0f - pulse)).alpha(alpha).render(graphics);
            Identifier logo = ServerBadge.rwTexture();
            if (logo != null) {
                Image.of(logo, x + 1.0f, centerY - 4.5f, 9.0f, 9.0f).alpha(alpha).render(graphics);
            } else {
                RoundedRect.of(x + 1.5f, centerY - 4.0f, 8.0f, 8.0f).radius(4.0f).verticalGradient(Color.rgb(255, 92, 92), Color.rgb(170, 20, 30)).alpha(alpha).render(graphics);
            }
            Text.of(Fonts.SEMIBOLD, "RW", x + 18.0f, centerY, 5.0f).centered().colors((glyph, px, py) -> Color.rgb(255, 110, 110).lerp(Color.rgb(255, 236, 236), 0.5f + 0.5f * (float)Math.sin((double)px * Math.PI * 2.0 - phase)).withAlpha(alpha).argb()).render(graphics);
        } else {
            RoundedRect.of(x, y, this.width, 10.0f).radius(3.0f).horizontalGradient(Color.rgb(23, 57, 89).lerp(Color.rgb(31, 77, 112), pulse), Color.rgb(15, 32, 57).lerp(Color.rgb(22, 49, 77), 1.0f - pulse)).alpha(alpha).render(graphics);
            RoundedRect.of(x + 2.0f, centerY - 3.0f, 6.0f, 6.0f).radius(3.0f).color(Color.rgb(158, 93, 40).withAlpha(alpha)).render(graphics);
            RoundedRect.of(x + 2.0f, centerY - 3.5f, 6.0f, 5.5f).radius(2.75f).verticalGradient(Color.rgb(105, 226, 255).lerp(Color.WHITE, pulse * 0.3f), Color.rgb(30, 145, 235)).alpha(alpha).render(graphics);
            RoundedRect.of(x + 4.0f, centerY - 1.5f, 2.0f, 2.0f).radius(1.0f).color(Color.rgb(19, 42, 66).withAlpha(alpha)).render(graphics);
            Text.of(Fonts.SEMIBOLD, "DSMP", x + 19.0f, centerY, 4.8f).centered().colors((glyph, px, py) -> Color.rgb(112, 206, 255).lerp(Color.WHITE, 0.5f + 0.5f * (float)Math.sin((double)px * Math.PI * 2.0 - phase)).withAlpha(alpha).argb()).render(graphics);
        }
    }

    static {
        HW_LOGO = Identifier.fromNamespaceAndPath((String)"brokke", (String)"textures/gui/badges/holyworld.png");
        RW_LOGO = Identifier.fromNamespaceAndPath((String)"brokke", (String)"textures/gui/badges/reallyworld.png");
    }
}

