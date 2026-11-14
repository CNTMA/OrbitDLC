package com.orbitdlc.client;

import com.orbitdlc.OrbitDLC;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

final class GuiAudio {
    private static final SoundEvent OPEN = sound("gui.open");
    private static final SoundEvent CLOSE = sound("gui.close");
    private static final SoundEvent ENABLE = sound("gui.enable");
    private static final SoundEvent DISABLE = sound("gui.disable");

    private GuiAudio() {}

    static void open() { play(OPEN); }
    static void close() { play(CLOSE); }

    static void toggle(boolean enabled) {
        play(enabled ? ENABLE : DISABLE);
    }

    private static SoundEvent sound(String name) {
        return SoundEvent.createVariableRangeEvent(OrbitDLC.id(name));
    }

    private static void play(SoundEvent sound) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, 1f, 1f));
    }
}
