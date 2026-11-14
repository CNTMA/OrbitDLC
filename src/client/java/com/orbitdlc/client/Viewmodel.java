package com.orbitdlc.client;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.world.entity.HumanoidArm;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Locale;
import java.util.List;

public final class Viewmodel extends Module {
    static final Viewmodel INSTANCE = new Viewmodel();

    private enum Control {
        SCALE("Scale", 100, 50, 175, 5),
        X("X", 0, -40, 40, 2), Y("Y", 0, -40, 40, 2), Z("Z", 0, -40, 40, 2),
        PITCH("Pitch", 0, -60, 60, 5), YAW("Yaw", 0, -60, 60, 5), ROLL("Roll", 0, -60, 60, 5);

        final String label;
        final int initial, min, max, step;

        Control(String label, int initial, int min, int max, int step) {
            this.label = label;
            this.initial = initial;
            this.min = min;
            this.max = max;
            this.step = step;
        }

        String key() { return name().toLowerCase(Locale.ROOT); }

        String display(int value) {
            return label + " " + switch (this) {
                case SCALE -> value + "%";
                case X, Y, Z -> String.format(Locale.ROOT, "%.2f", value / 100f);
                default -> value + "°";
            };
        }
    }

    private final EnumMap<Control, Integer> right = new EnumMap<>(Control.class);
    private final EnumMap<Control, Integer> left = new EnumMap<>(Control.class);
    private HumanoidArm editing = HumanoidArm.RIGHT;

    private Viewmodel() { super("viewmodel", Category.VISUALS); }

    @Override
    public String description() { return "Changes the scale, position and rotation of hands and held items."; }

    private EnumMap<Control, Integer> profile(HumanoidArm arm) { return arm == HumanoidArm.RIGHT ? right : left; }

    private int value(HumanoidArm arm, Control control) { return profile(arm).getOrDefault(control, control.initial); }

    public static void transform(PoseStack pose, HumanoidArm arm) {
        var model = INSTANCE;
        if (!model.isEnabled() || model.profile(arm).isEmpty()) return;
        pose.translate(model.value(arm, Control.X) / 100f, model.value(arm, Control.Y) / 100f, model.value(arm, Control.Z) / 100f);
        pose.translate(0f, 0f, -0.7f);
        pose.rotateDegrees(Axis.YP, model.value(arm, Control.YAW));
        pose.rotateDegrees(Axis.XP, model.value(arm, Control.PITCH));
        pose.rotateDegrees(Axis.ZP, model.value(arm, Control.ROLL));
        float scale = model.value(arm, Control.SCALE) / 100f;
        pose.scale(scale, scale, scale);
        pose.translate(0f, 0f, 0.7f);
    }

    static void translateSwing(PoseStack pose, int side, float blend) {
        var model = INSTANCE;
        var arm = side > 0 ? HumanoidArm.RIGHT : HumanoidArm.LEFT;
        float inward = 0.10f;
        float lift = 0.12f;
        if (model.isEnabled()) {
            inward += Math.max(0f, side * model.value(arm, Control.X) / 100f) * 0.65f;
            lift += Math.max(0f, -model.value(arm, Control.Y) / 100f) * 0.75f;
        }
        var offset = new Vector3f(-side * inward, lift, -0.17f).mul(blend);
        if (model.isEnabled()) {
            // Convert the camera-relative motion to the transformed Viewmodel axes.
            offset.rotateY((float) Math.toRadians(-model.value(arm, Control.YAW)))
                    .rotateX((float) Math.toRadians(-model.value(arm, Control.PITCH)))
                    .rotateZ((float) Math.toRadians(-model.value(arm, Control.ROLL)))
                    .mul(100f / model.value(arm, Control.SCALE));
        }
        pose.translate(offset.x, offset.y, offset.z);
    }

    @Override
    public JsonObject saveSettings() {
        var settings = new JsonObject();
        settings.add("right", saveProfile(right));
        settings.add("left", saveProfile(left));
        return settings;
    }

    private static JsonObject saveProfile(EnumMap<Control, Integer> profile) {
        var settings = new JsonObject();
        profile.forEach((control, value) -> settings.addProperty(control.key(), value));
        return settings;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        loadProfile(right, settings.has("right") ? settings.getAsJsonObject("right") : settings);
        loadProfile(left, settings.has("left") ? settings.getAsJsonObject("left") : settings);
    }

    private static void loadProfile(EnumMap<Control, Integer> profile, JsonObject settings) {
        profile.clear();
        for (var control : Control.values()) {
            if (settings.has(control.key())) {
                profile.put(control, Math.clamp(settings.get(control.key()).getAsInt(), control.min, control.max));
            }
        }
    }

    ModuleSettings settings(ClickGui parent) {
        return new ModuleSettings("Viewmodel", parent) {
            @Override
            protected void init() {
                int row = choicesAt(List.of("Right", "Left"),
                        index -> editing == (index == 0 ? HumanoidArm.RIGHT : HumanoidArm.LEFT),
                        index -> editing = index == 0 ? HumanoidArm.RIGHT : HumanoidArm.LEFT, 0) + 4;
                for (var control : Control.values()) {
                    slider(control.label, "", row + control.ordinal() * 24, control.min, control.max,
                            () -> value(editing, control), value -> profile(editing).put(control, value));
                }
            }
        };
    }

}
