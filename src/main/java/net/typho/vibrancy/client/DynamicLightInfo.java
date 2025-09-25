package net.typho.vibrancy.client;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public record DynamicLightInfo(Vector3f color, Optional<Float> radius, Optional<Float> brightness, Optional<Float> flicker) {
    public static final Map<RegistryKey<Block>, Function<BlockState, DynamicLightInfo>> MAP = new LinkedHashMap<>();
    public static final Codec<DynamicLightInfo> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.listOf(3, 3).xmap(list -> new Vector3f(list.getFirst(), list.get(1), list.get(2)), color -> List.of(color.x, color.y, color.z)).fieldOf("color").forGetter(DynamicLightInfo::color),
            Codec.FLOAT.optionalFieldOf("radius").forGetter(DynamicLightInfo::radius),
            Codec.FLOAT.optionalFieldOf("brightness").forGetter(DynamicLightInfo::brightness),
            Codec.FLOAT.optionalFieldOf("flicker").forGetter(DynamicLightInfo::flicker)
    ).apply(instance, DynamicLightInfo::new));
    public static final Codec<Map<RegistryKey<Block>, Either<DynamicLightInfo, RegistryKey<Block>>>> FILE_CODEC = Codec.unboundedMap(RegistryKey.createCodec(RegistryKeys.BLOCK), Codec.either(CODEC, RegistryKey.createCodec(RegistryKeys.BLOCK)));

    public static DynamicLightInfo get(RegistryKey<Block> key, BlockState state) {
        return MAP.getOrDefault(key, k -> null).apply(state);
    }

    @SuppressWarnings("deprecation")
    public static DynamicLightInfo get(BlockState state) {
        return get(state.getBlock().getRegistryEntry().registryKey(), state);
    }
}
