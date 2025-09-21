package net.typho.vibrancy.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.gui.options.OptionGroup;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpact;
import net.caffeinemc.mods.sodium.client.gui.options.OptionImpl;
import net.caffeinemc.mods.sodium.client.gui.options.control.TickBoxControl;
import net.caffeinemc.mods.sodium.client.gui.options.storage.MinecraftOptionsStorage;
import net.minecraft.text.Text;
import net.typho.vibrancy.client.VibrancyClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.gui.SodiumGameOptionPages", remap = false)
public class SodiumGameOptionPagesMixin {
    @Shadow
    @Final
    private static MinecraftOptionsStorage vanillaOpts;

    @WrapOperation(
            method = "quality",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/client/gui/options/OptionGroup$Builder;add(Lnet/caffeinemc/mods/sodium/client/gui/options/Option;)Lnet/caffeinemc/mods/sodium/client/gui/options/OptionGroup$Builder;",
                    ordinal = 10
            )
    )
    private static OptionGroup.Builder addOptions(OptionGroup.Builder instance, Option<?> option, Operation<OptionGroup.Builder> original) {
        return original.call(instance, option)
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Text.translatable("options.vibrancy.dynamic_lightmap"))
                        .setTooltip(Text.translatable("options.vibrancy.dynamic_lightmap.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> VibrancyClient.DYNAMIC_LIGHTMAP.setValue(value), opts -> VibrancyClient.DYNAMIC_LIGHTMAP.getValue())
                        .build())
                .add(OptionImpl.createBuilder(boolean.class, vanillaOpts)
                        .setName(Text.translatable("options.vibrancy.raytrace_lights"))
                        .setTooltip(Text.translatable("options.vibrancy.raytrace_lights.tooltip"))
                        .setControl(TickBoxControl::new)
                        .setBinding((opts, value) -> VibrancyClient.RAYTRACE_LIGHTS.setValue(value), opts -> VibrancyClient.RAYTRACE_LIGHTS.getValue())
                        .setImpact(OptionImpact.HIGH)
                        .build());
    }
}
