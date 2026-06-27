package net.typho.vibrancy.mixin;

//? neoforge {
/*import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Pseudo
@Mixin(targets = "net.neoforged.neoforge.client.model.data.ModelDataManager")
public class ModelDataManagerMixin {
    @Shadow
    @Final
    @Mutable
    private Long2ObjectMap<Set<BlockPos>> needModelDataRefresh;

    @Shadow
    @Final
    @Mutable
    private Long2ObjectMap<Long2ObjectMap<ModelData>> modelDataCache;

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void init(Level level, CallbackInfo ci) {
        needModelDataRefresh = Long2ObjectMaps.synchronize(needModelDataRefresh);
        modelDataCache = Long2ObjectMaps.synchronize(modelDataCache);
    }
}
*///? }