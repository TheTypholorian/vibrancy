package net.typho.vibrancy.mixin.client;

import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;

@Mixin(RegistryKey.class)
public abstract class RegistryKeyMixin {
    @Shadow
    public abstract Identifier getValue();

    @Shadow
    public abstract Identifier getRegistry();

    @Unique
    @Intrinsic
    public boolean equals(Object o) {
        return o instanceof RegistryKey<?> key && key.getValue().equals(getValue()) && key.getRegistry().equals(getRegistry());
    }

    @Unique
    @Intrinsic
    public int hashCode() {
        return Objects.hash(getValue(), getRegistry());
    }
}
