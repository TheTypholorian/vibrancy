package net.typho.vibrancy.mixin;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

@Mixin(ResourceKey.class)
public abstract class ResourceKeyMixin {
    @Shadow
    public abstract ResourceLocation location();

    @Shadow
    public abstract ResourceLocation registry();

    @Unique
    @Intrinsic
    public boolean equals(Object o) {
        return o instanceof ResourceKey<?> key && key.location().equals(location()) && key.registry().equals(registry());
    }

    @Unique
    @Intrinsic
    public int hashCode() {
        return Objects.hash(location(), registry());
    }
}
