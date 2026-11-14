package com.orbitdlc.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

final class CosmeticMotion {
    private static final Map<String, CosmeticMotion> CACHE = new HashMap<>();
    private final Map<String, Tracks> bones = new HashMap<>();
    private float duration;

    private record Tracks(Track position, Track rotation, Track scale) {}
    private record Keys(Vector3f before, Vector3f after, boolean catmullRom) {}

    private record Track(TreeMap<Float, Keys> keys, Vector3f fallback) {
        Vector3f sample(float time) {
            var left = keys.floorEntry(time);
            var right = keys.higherEntry(time);
            if (left == null) return right == null ? fallback : right.getValue().before();
            if (right == null) return left.getValue().after();
            float blend = (time - left.getKey()) / (right.getKey() - left.getKey());
            var start = left.getValue().after();
            var end = right.getValue().before();
            if (!right.getValue().catmullRom()) return new Vector3f(start).lerp(end, blend);
            var previous = keys.lowerEntry(left.getKey());
            var next = keys.higherEntry(right.getKey());
            var before = previous == null ? start : previous.getValue().after();
            var after = next == null ? end : next.getValue().before();
            return new Vector3f(
                spline(before.x, start.x, end.x, after.x, blend),
                spline(before.y, start.y, end.y, after.y, blend),
                spline(before.z, start.z, end.z, after.z, blend)
            );
        }

        private static float spline(float a, float b, float c, float d, float t) {
            return b + 0.5f * t * (c - a + t * (2f * a - 5f * b + 4f * c - d
                + t * (3f * (b - c) + d - a)));
        }
    }

    static CosmeticMotion byId(String id) { return CACHE.computeIfAbsent(id, CosmeticMotion::load); }

    private static CosmeticMotion load(String id) {
        var motion = new CosmeticMotion();
        try (var stream = CosmeticMotion.class.getResourceAsStream("/assets/orbitdlc/geckolib/animations/" + id + ".animation.json")) {
            if (stream == null) return motion;
            var json = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            var clip = json.getAsJsonObject("animations").entrySet().iterator().next().getValue().getAsJsonObject();
            motion.duration = clip.has("animation_length") ? clip.get("animation_length").getAsFloat() : 0f;
            for (var entry : clip.getAsJsonObject("bones").entrySet()) {
                var bone = entry.getValue().getAsJsonObject();
                var tracks = new Tracks(track(bone.get("position"), 0f), track(bone.get("rotation"), 0f), track(bone.get("scale"), 1f));
                motion.bones.put(entry.getKey(), tracks);
                for (var track : new Track[]{tracks.position(), tracks.rotation(), tracks.scale()}) {
                    if (!track.keys().isEmpty()) motion.duration = Math.max(motion.duration, track.keys().lastKey());
                }
            }
        } catch (java.io.IOException | RuntimeException error) {
            com.orbitdlc.OrbitDLC.LOGGER.warn("Could not load cosmetic animation {}", id, error);
        }
        return motion;
    }

    private static Track track(JsonElement value, float initial) {
        var fallback = new Vector3f(initial);
        var keys = new TreeMap<Float, Keys>();
        if (value != null && value.isJsonObject() && !value.getAsJsonObject().has("vector")) {
            for (var entry : value.getAsJsonObject().entrySet()) {
                var node = entry.getValue();
                JsonObject object = node.isJsonObject() ? node.getAsJsonObject() : new JsonObject();
                var after = vector(object.has("post") ? object.get("post") : node, fallback);
                var before = vector(object.has("pre") ? object.get("pre") : null, after);
                boolean catmullRom = object.has("lerp_mode") && object.get("lerp_mode").getAsString().equals("catmullrom");
                keys.put(Float.parseFloat(entry.getKey()), new Keys(before, after, catmullRom));
            }
        } else {
            var constant = vector(value, fallback);
            keys.put(0f, new Keys(constant, constant, false));
        }
        return new Track(keys, fallback);
    }

    private static Vector3f vector(JsonElement node, Vector3f fallback) {
        if (node == null) return fallback;
        if (node.isJsonObject()) return vector(node.getAsJsonObject().get("vector"), fallback);
        if (node.isJsonPrimitive()) {
            try { return new Vector3f(node.getAsFloat()); }
            catch (NumberFormatException ignored) { return fallback; }
        }
        if (!node.isJsonArray() || node.getAsJsonArray().size() != 3) return fallback;
        var array = node.getAsJsonArray();
        try { return new Vector3f(array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat()); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    void apply(Matrix4f matrix, String bone, double seconds) {
        var tracks = bones.get(bone);
        if (tracks == null) return;
        float time = duration > 0f ? (float) (seconds % duration) : 0f;
        var offset = tracks.position().sample(time);
        var rotation = tracks.rotation().sample(time);
        var scale = tracks.scale().sample(time);
        float radians = (float) (Math.PI / 180);
        matrix.translate(-offset.x / 16f, offset.y / 16f, offset.z / 16f);
        matrix.rotateZYX(rotation.z * radians, -rotation.y * radians, -rotation.x * radians);
        matrix.scale(scale);
    }
}
