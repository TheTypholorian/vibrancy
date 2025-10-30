package net.typho.vibrancy.mixin;

import net.minecraft.client.resources.model.BlockStateModelLoader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.function.Predicate;

@Mixin(BlockStateModelLoader.class)
public interface BlockStateModelLoaderAccessor {
    @Invoker("predicate")
    static Predicate<BlockState> predicate(StateDefinition<Block, BlockState> def, String properties) {
        throw new IllegalStateException();
    }
}
