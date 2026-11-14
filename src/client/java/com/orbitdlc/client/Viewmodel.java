package com.orbitdlc.client;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.orbitdlc.client.module.Category;
import com.orbitdlc.client.module.Module;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Locale;

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

    private final EnumMap<Control, Integer> values = new EnumMap<>(Control.class);

    private Viewmodel() { super("viewmodel", Category.VISUALS); }

    @Override
    public String description() { return "Changes the scale, position and rotation of hands and held items."; }

    private int value(Control control) { return values.getOrDefault(control, control.initial); }

    public static void transform(PoseStack pose) {
        var model = INSTANCE;
        if (!model.isEnabled() || model.values.isEmpty()) return;
        pose.translate(model.value(Control.X) / 100f, model.value(Control.Y) / 100f, model.value(Control.Z) / 100f);
        pose.translate(0f, 0f, -0.7f);
        pose.rotateDegrees(Axis.YP, model.value(Control.YAW));
        pose.rotateDegrees(Axis.XP, model.value(Control.PITCH));
        pose.rotateDegrees(Axis.ZP, model.value(Control.ROLL));
        float scale = model.value(Control.SCALE) / 100f;
        pose.scale(scale, scale, scale);
        pose.translate(0f, 0f, 0.7f);
    }

    static void translateSwing(PoseStack pose, int side, float blend) {
        var model = INSTANCE;
        float inward = 0.10f;
        float lift = 0.12f;
        if (model.isEnabled()) {
            inward += Math.max(0f, side * model.value(Control.X) / 100f) * 0.65f;
            lift += Math.max(0f, -model.value(Control.Y) / 100f) * 0.75f;
        }
        var offset = new Vector3f(-side * inward, lift, -0.17f).mul(blend);
        if (model.isEnabled()) {
            // Convert the camera-relative motion to the transformed Viewmodel axes.
            offset.rotateY((float) Math.toRadians(-model.value(Control.YAW)))
                    .rotateX((float) Math.toRadians(-model.value(Control.PITCH)))
                    .rotateZ((float) Math.toRadians(-model.value(Control.ROLL)))
                    .mul(100f / model.value(Control.SCALE));
        }
        pose.translate(offset.x, offset.y, offset.z);
    }

    @Override
    public JsonObject saveSettings() {
        var settings = super.saveSettings();
        values.forEach((control, value) -> settings.addProperty(control.key(), value));
        return settings;
    }

    @Override
    public void loadSettings(JsonObject settings) {
        values.clear();
        for (var control : Control.values()) {
            if (settings.has(control.key())) {
                values.put(control, Math.clamp(settings.get(control.key()).getAsInt(), control.min, control.max));
            }
        }
    }

    ModuleSettings settings(ClickGui parent) {
        return new ModuleSettings("Viewmodel", parent) {
            @Override
            protected void init() {
                for (var control : Control.values()) {
                    slider(control.key(), "", control.ordinal() * 24, control.min, control.max,
                            () -> value(control), value -> values.put(control, value));
                }
            }
        };
    }

}
