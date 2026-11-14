package com.orbitdlc.client;

import com.orbitdlc.OrbitDLC;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.network.chat.Component;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class WorldParticles extends Module {
    static final WorldParticles INSTANCE = new WorldParticles();
    static final int REFLECTION_LIMIT = 12;
    private final List<Mote> motes = new ArrayList<>();
    private ClientLevel particleLevel;
    private static final List<String> LABELS =
            List.of("Bloom", "Star", "Snow", "Heart", "Dollar", "Triangle", "Sakura", "Genshin", "Rhombus");
    private static final List<String> TEXTURES =
            List.of("firefly", "star", "snowflake", "heart", "dollar", "triangle", "sakura", "genshin", "rhombus");
    private int style;
    private int amount = 5;
    private int distance = 12;
    private VisualColor color = VisualColor.ORIGINAL;

    private WorldParticles() { super("world_particles", Category.VISUALS); }

    @Override
    public String description() {
        return "Ambient world particles with the original Source1 textures. Right-click to choose a style.";
    }

    void tick(Minecraft client) {
        if (particleLevel != client.level) {
            motes.clear();
            particleLevel = client.level;
        }
        motes.removeIf(mote -> !mote.isAlive());
        if (!isEnabled() || client.player == null || client.level == null || client.player.tickCount % 3 != 0) return;
        if (!(client.getTextureManager().getTexture(TextureAtlas.LOCATION_PARTICLES) instanceof TextureAtlas atlas)) return;
        var sprite = atlas.getSprite(OrbitDLC.id("world_particles/" + TEXTURES.get(style)));
        if (sprite == atlas.missingSprite()) return;
        var halo = atlas.getSprite(OrbitDLC.id("world_particles/firefly"));
        var center = client.player.position();
        var random = ThreadLocalRandom.current();
        int budget = Optimization.level() >= 3 ? 0 : amount >> Optimization.level();
        for (int i = 0; i < budget; i++) {
            double angle = random.nextDouble(Math.TAU);
            double radius = distance * Math.sqrt(random.nextDouble());
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + random.nextDouble(0, distance * 0.75);
            double z = center.z + Math.sin(angle) * radius;
            var pos = BlockPos.containing(x, y, z);
            if (!client.level.isEmptyBlock(pos)) continue;
            var mote = new Mote(this, client.level, x, y, z, sprite, halo);
            motes.add(mote);
            client.particleEngine.add(mote);
        }
    }

    int writeReflectionLights(ByteBuffer data, Camera camera) {
        if (!isEnabled() || particleLevel != Minecraft.getInstance().level) return 0;
        var origin = camera.position();
        var lights = motes.stream().filter(mote -> mote.isAlive() && mote.opacity() > 0f)
                .sorted(Comparator.comparingDouble(mote -> mote.distanceSquared(origin)))
                .limit(REFLECTION_LIMIT).toList();
        for (var mote : lights) mote.writeLight(data, origin);
        return lights.size();
    }

    int tint() { return color.renderColor(); }

    @Override
    public com.google.gson.JsonObject saveSettings() {
        var settings = super.saveSettings();
        settings.addProperty("style", style);
        settings.addProperty("amount", amount);
        settings.addProperty("distance", distance);
        settings.addProperty("color", color.name());
        return settings;
    }

    @Override
    public void loadSettings(com.google.gson.JsonObject settings) {
        style = Math.clamp(settings.get("style").getAsInt(), 0, LABELS.size() - 1);
        amount = settings.has("amount") ? Math.clamp(settings.get("amount").getAsInt(), 1, 25) : 5;
        distance = settings.has("distance") ? Math.clamp(settings.get("distance").getAsInt(), 4, 32) : 12;
        color = VisualColor.load(settings);
    }

    ModuleSettings settings(Screen parent) {
        return new ModuleSettings("World Particles", parent) {
            @Override
            protected void init() {
                slider("Amount", "", 0, 1, 25, () -> amount, value -> amount = value);
                slider("Distance", " m", 24, 4, 32, () -> distance, value -> distance = value);
                chip("Color · " + color.label, 2, button -> {
                    color = color.next();
                    button.setMessage(Component.literal("Color · " + color.label));
                });
                choicesAt(LABELS, index -> style == index, index -> style = index, 75);
            }

        };
    }

    private static final class Mote extends SingleQuadParticle {
        private final WorldParticles owner;
        private final TextureAtlasSprite halo;

        Mote(WorldParticles owner, ClientLevel level, double x, double y, double z, TextureAtlasSprite sprite,
             TextureAtlasSprite halo) {
            super(level, x, y, z, sprite);
            this.owner = owner;
            this.halo = halo;
            var random = ThreadLocalRandom.current();
            xd = random.nextDouble(-0.035, 0.035);
            yd = random.nextDouble(-0.007, 0.007);
            zd = random.nextDouble(-0.035, 0.035);
            quadSize = 0.15f;
            lifetime = 40;
            updateColor();
            setAlpha(0f);
        }

        float opacity() { return alpha; }

        double distanceSquared(net.minecraft.world.phys.Vec3 origin) {
            return origin.distanceToSqr(x, y, z);
        }

        void writeLight(ByteBuffer data, net.minecraft.world.phys.Vec3 origin) {
            data.putFloat((float) (x - origin.x)).putFloat((float) (y - origin.y))
                    .putFloat((float) (z - origin.z)).putFloat(alpha);
        }

        @Override
        public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
            var frustum = Optimization.frustum(camera);
            if (frustum != null && !frustum.isVisible(getBoundingBox().inflate(0.75))) return;
            if (Optimization.level() >= 1) {
                super.extract(state, camera, partialTick);
                return;
            }
            var core = sprite;
            float size = quadSize;
            float opacity = alpha;
            try {
                sprite = halo;
                quadSize = size * 3f;
                alpha = opacity * 0.40f;
                super.extract(state, camera, partialTick);
            } finally {
                sprite = core;
                quadSize = size;
                alpha = opacity;
            }
            super.extract(state, camera, partialTick);
        }

        @Override
        protected int getLightCoords(float partialTick) {
            return net.minecraft.util.LightCoordsUtil.FULL_BRIGHT;
        }

        private void updateColor() {
            int tint = owner.color.renderColor();
            setColor((tint >> 16 & 255) / 255f, (tint >> 8 & 255) / 255f, (tint & 255) / 255f);
        }

        @Override
        public void tick() {
            if (!owner.isEnabled()) {
                remove();
                return;
            }
            super.tick();
            updateColor();
            setAlpha(Math.clamp(Math.min(age / 6f, (lifetime - age) / 12f), 0f, 1f));
        }

        @Override
        protected Layer getLayer() { return Layer.TRANSLUCENT; }
    }
}
