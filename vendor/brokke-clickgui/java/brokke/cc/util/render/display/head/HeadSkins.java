/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.authlib.GameProfile
 *  net.minecraft.client.Minecraft
 *  net.minecraft.world.entity.player.PlayerSkin
 */
package brokke.cc.util.render.display.head;

import com.mojang.authlib.GameProfile;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerSkin;

final class HeadSkins {
    private static final int LIMIT = 128;
    private static final Map<UUID, Supplier<PlayerSkin>> CACHE = new LinkedHashMap<UUID, Supplier<PlayerSkin>>(32, 0.75f, true){

        @Override
        protected boolean removeEldestEntry(Map.Entry<UUID, Supplier<PlayerSkin>> eldest) {
            return this.size() > 128;
        }
    };

    private HeadSkins() {
    }

    static Supplier<PlayerSkin> lookup(GameProfile profile) {
        return CACHE.computeIfAbsent(profile.id(), id -> Minecraft.getInstance().getSkinManager().createLookup(profile, false));
    }

    static void clear() {
        CACHE.clear();
    }
}

