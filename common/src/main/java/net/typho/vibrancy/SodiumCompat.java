package net.typho.vibrancy;

import com.google.common.collect.ImmutableList;
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpact;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpl;
import net.caffeinemc.mods.sodium.client.gui.options.OptionPage;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import net.caffeinemc.mods.sodium.client.gui.options.control.TickBoxControl;
import net.caffeinemc.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import net.minecraft.network.chat.Component;

import java.util.LinkedList;
import java.util.List;

public final class SodiumCompat {
    private SodiumCompat() {
    }

    public static OptionPage rtxPage(MinecraftOptionsStorage vanillaOpts) {
        List<OptionGroup> groups = new LinkedList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Component.translatable("options.vibrancy.better_fog"))
                        .setTooltip(Component.translatable("options.vibrancy.better_fog.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> Vibrancy.BETTER_FOG.set(value), opts -> Vibrancy.BETTER_FOG.get())
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Component.translatable("options.vibrancy.elytra_trails"))
                        .setTooltip(Component.translatable("options.vibrancy.elytra_trails.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> Vibrancy.ELYTRA_TRAILS.set(value), opts -> Vibrancy.ELYTRA_TRAILS.get())
                        .build())
                .build());

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Component.translatable("options.vibrancy.transparency_test"))
                        .setTooltip(Component.translatable("options.vibrancy.transparency_test.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> Vibrancy.TRANSPARENCY_TEST.set(value), opts -> Vibrancy.TRANSPARENCY_TEST.get())
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Component.translatable("options.vibrancy.sky_shadow_distance"))
                        .setTooltip(Component.translatable("options.vibrancy.sky_shadow_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 1, 8, 1, v -> Component.translatable("options.vibrancy.sky_shadow_distance.value", v)))
                        .setBinding((opts, value) -> Vibrancy.SKY_SHADOW_DISTANCE.set(value), opts -> Vibrancy.SKY_SHADOW_DISTANCE.get())
                        .setImpact(OptionImpact.HIGH)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Component.translatable("options.vibrancy.raytrace_distance"))
                        .setTooltip(Component.translatable("options.vibrancy.raytrace_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 1, 32, 1, v -> Component.translatable("options.vibrancy.light_cull_distance.value", v * 16)))
                        .setBinding((opts, value) -> Vibrancy.RAYTRACE_DISTANCE.set(value), opts -> Vibrancy.RAYTRACE_DISTANCE.get())
                        .setImpact(OptionImpact.HIGH)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Component.translatable("options.vibrancy.light_cull_distance"))
                        .setTooltip(Component.translatable("options.vibrancy.light_cull_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 1, 32, 1, v -> Component.translatable("options.vibrancy.light_cull_distance.value", v * 16)))
                        .setBinding((opts, value) -> Vibrancy.LIGHT_CULL_DISTANCE.set(value), opts -> Vibrancy.LIGHT_CULL_DISTANCE.get())
                        .setImpact(OptionImpact.HIGH)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Component.translatable("options.vibrancy.max_raytraced_lights"))
                        .setTooltip(Component.translatable("options.vibrancy.max_raytraced_lights.tooltip"))
                        .setControl(option -> new SliderControl(option, 5, 105, 5, v -> v > 100 ? Component.translatable("options.vibrancy.max_raytraced_lights.max") : Component.translatable("options.vibrancy.max_raytraced_lights.value", v)))
                        .setBinding((opts, value) -> Vibrancy.MAX_RAYTRACED_LIGHTS.set(value), opts -> Vibrancy.MAX_RAYTRACED_LIGHTS.get())
                        .setImpact(OptionImpact.HIGH)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Component.translatable("options.vibrancy.max_shadow_distance"))
                        .setTooltip(Component.translatable("options.vibrancy.max_shadow_distance.tooltip"))
                        .setControl(option -> new SliderControl(option, 1, 16, 1, v -> v > 15 ? Component.translatable("options.vibrancy.max_shadow_distance.max") : Component.translatable("options.vibrancy.max_shadow_distance.value", v)))
                        .setBinding((opts, value) -> Vibrancy.MAX_SHADOW_DISTANCE.set(value), opts -> Vibrancy.MAX_SHADOW_DISTANCE.get())
                        .setImpact(OptionImpact.HIGH)
                        .build())
                .add(OptionImpl.createBuilder(int.class, vanillaOpts)
                        .setName(Component.translatable("options.vibrancy.max_light_radius"))
                        .setTooltip(Component.translatable("options.vibrancy.max_light_radius.tooltip"))
                        .setControl(option -> new SliderControl(option, 1, 16, 1, v -> v > 15 ? Component.translatable("options.vibrancy.max_light_radius.max") : Component.translatable("options.vibrancy.max_light_radius.value", v)))
                        .setBinding((opts, value) -> Vibrancy.MAX_LIGHT_RADIUS.set(value), opts -> Vibrancy.MAX_LIGHT_RADIUS.get())
                        .setImpact(OptionImpact.HIGH)
                        .build())
                .build());

        return new OptionPage(Component.translatable("options.vibrancy.page"), ImmutableList.copyOf(groups));
    }
}
