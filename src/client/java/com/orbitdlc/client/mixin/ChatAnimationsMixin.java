package com.orbitdlc.client.mixin;

import com.orbitdlc.client.Animations;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
abstract class ChatAnimationsMixin {
    @Unique private long orbitdlc$messageAt;

    @Inject(method = "addMessageToDisplayQueue", at = @At("TAIL"))
    private void orbitdlc$message(GuiMessage message, CallbackInfo callback) {
        orbitdlc$messageAt = System.nanoTime();
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at = @At("HEAD"))
    private void orbitdlc$slide(GuiGraphicsExtractor graphics, Font font, int tick, int x, int y,
                                ChatComponent.DisplayMode mode, boolean focused, CallbackInfo callback) {
        graphics.pose().pushMatrix();
        if (Animations.plays(Animations.Part.CHAT)
                && !(net.minecraft.client.Minecraft.getInstance().gui.screen() instanceof net.minecraft.client.gui.screens.ChatScreen))
            graphics.pose().translate(0, 7 * Animations.remaining(orbitdlc$messageAt, 250));
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at = @At("RETURN"))
    private void orbitdlc$restore(GuiGraphicsExtractor graphics, Font font, int tick, int x, int y,
                                  ChatComponent.DisplayMode mode, boolean focused, CallbackInfo callback) {
        graphics.pose().popMatrix();
    }
}
