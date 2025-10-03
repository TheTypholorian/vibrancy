package net.typho.vibrancy;

import com.google.common.collect.ImmutableList;
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpact;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpl;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatter;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.TickBoxControl;
import net.caffeinemc.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import net.minecraft.text.Text;

import java.util.LinkedList;
import java.util.List;

public final class SodiumCompat {
    private SodiumCompat() {
    }

    public static OptionPage rtxPage(MinecraftOptionsStorage vanillaOpts) {
        List<OptionGroup> groups = new LinkedList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Text.translatable("options.vibrancy.dynamic_lightmap"))
                        .setTooltip(Text.translatable("options.vibrancy.dynamic_lightmap.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> Vibrancy.DYNAMIC_LIGHTMAP.setValue(value), opts -> Vibrancy.DYNAMIC_LIGHTMAP.getValue())
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Text.translatable("options.vibrancy.better_sky"))
                        .setTooltip(Text.translatable("options.vibrancy.better_sky.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> Vibrancy.BETTER_SKY.setValue(value), opts -> Vibrancy.BETTER_SKY.getValue())
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Text.translatable("options.vibrancy.better_fog"))
                        .setTooltip(Text.translatable("options.vibrancy.better_fog.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> Vibrancy.BETTER_FOG.setValue(value), opts -> Vibrancy.BETTER_FOG.getValue())
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Text.translatable("options.vibrancy.transparency_test"))
                        .setTooltip(Text.translatable("options.vibrancy.transparency_test.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> Vibrancy.TRANSPARENCY_TEST.setValue(value), opts -> Vibrancy.TRANSPARENCY_TEST.getValue())
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Text.translatable("options.vibrancy.raytrace_distance"))
                        .setTooltip(Text.translatable("options.vibrancy.raytrace_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 1, 16, 1, ControlValueFormatter.translateVariable("options.chunks")))
                        .setBinding((opts, value) -> Vibrancy.RAYTRACE_DISTANCE.setValue(value), opts -> Vibrancy.RAYTRACE_DISTANCE.getValue())
                        .setImpact(OptionImpact.HIGH)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Text.translatable("options.vibrancy.light_cull_distance"))
                        .setTooltip(Text.translatable("options.vibrancy.light_cull_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 1, 16, 1, ControlValueFormatter.translateVariable("options.chunks")))
                        .setBinding((opts, value) -> Vibrancy.LIGHT_CULL_DISTANCE.setValue(value), opts -> Vibrancy.LIGHT_CULL_DISTANCE.getValue())
                        .setImpact(OptionImpact.HIGH)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Text.translatable("options.vibrancy.max_raytraced_lights"))
                        .setTooltip(Text.translatable("options.vibrancy.max_raytraced_lights.tooltip"))
                        .setControl(option -> new SliderControl(option, 5, 105, 5, v -> v > 100 ? Text.translatable("options.vibrancy.max_raytraced_lights.max") : Text.translatable("options.vibrancy.max_raytraced_lights.value", v)))
                        .setBinding((opts, value) -> Vibrancy.MAX_RAYTRACED_LIGHTS.setValue(value), opts -> Vibrancy.MAX_RAYTRACED_LIGHTS.getValue())
                        .setImpact(OptionImpact.HIGH)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Text.translatable("options.vibrancy.max_shadow_distance"))
                        .setTooltip(Text.translatable("options.vibrancy.max_shadow_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 1, 16, 1, v -> v > 15 ? Text.translatable("options.vibrancy.max_shadow_distance.max") : Text.translatable("options.vibrancy.max_shadow_distance.value", v)))
                        .setBinding((opts, value) -> Vibrancy.MAX_SHADOW_DISTANCE.setValue(value), opts -> Vibrancy.MAX_SHADOW_DISTANCE.getValue())
                        .setImpact(OptionImpact.HIGH)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Text.translatable("options.vibrancy.max_light_radius"))
                        .setTooltip(Text.translatable("options.vibrancy.max_light_radius.tooltip"))
                        .setControl(option -> new SliderControl(option, 1, 16, 1, v -> v > 15 ? Text.translatable("options.vibrancy.max_light_radius.max") : Text.translatable("options.vibrancy.max_light_radius.value", v)))
                        .setBinding((opts, value) -> Vibrancy.MAX_LIGHT_RADIUS.setValue(value), opts -> Vibrancy.MAX_LIGHT_RADIUS.getValue())
                        .setImpact(OptionImpact.HIGH)
                        .build())
                .build());

        return new OptionPage(Text.translatable("options.vibrancy.page"), ImmutableList.copyOf(groups));
    }
}
