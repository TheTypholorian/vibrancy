package net.typho.vibrancy.mixin;

import net.minecraft.registry.ResourceKey;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

@Mixin(ResourceKey.class)
public abstract class ResourceKeyMixin {
    @Shadow
    public abstract ResourceLocation getValue();

    @Shadow
    public abstract ResourceLocation getRegistry();

    @Unique
    @Intrinsic
    public boolean equals(Object o) {
        return o instanceof ResourceKey<?> key && key.getValue().equals(getValue()) && key.getRegistry().equals(getRegistry());
    }

    @Unique
    @Intrinsic
    public int hashCode() {
        return Objects.hash(getValue(), getRegistry());
    }
}
