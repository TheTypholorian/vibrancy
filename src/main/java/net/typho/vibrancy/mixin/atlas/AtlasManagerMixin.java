package net.typho.vibrancy.mixin.atlas;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import kotlin.collections.CollectionsKt;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import net.typho.vibrancy.Vibrancy;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(AtlasManager.class)
public class AtlasManagerMixin {
    @SuppressWarnings("unchecked")
    @WrapOperation(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/List;of([Ljava/lang/Object;)Ljava/util/List;"
            )
    )
    private static <E> List<E> clinit(@NotNull E[] elements, Operation<List<E>> original) {
        return (List<E>) CollectionsKt.plus(
                original.call((Object) elements),
                new AtlasManager.AtlasConfig(Vibrancy.transmissionAtlasLocation, Vibrancy.transmissionAtlasId, true)
        );
    }
}
