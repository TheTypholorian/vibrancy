package net.typho.vibrancy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface BlockStateFunction<T> {
    T apply(BlockState state);

    static <T> BlockStateFunction<T> parse(Map<Predicate<BlockState>, Supplier<T>> map, Supplier<T> def) {
        return state -> map.entrySet().stream()
                .filter(entry -> entry.getKey().test(state))
                .map(Map.Entry::getValue)
                .findAny()
                .orElse(def)
                .get();
    }

    static <T> BlockStateFunction<T> parseJson(Block block, JsonElement json, Function<JsonElement, T> toT, Supplier<T> def) {
        if (json.isJsonObject()) {
            JsonObject obj = json.getAsJsonObject();
            JsonElement defElement = obj.get("default");

            if (defElement != null) {
                T t = toT.apply(defElement);
                def = () -> t;
            }

            return parse(
                    obj.asMap().entrySet().stream()
                            .filter(entry -> !entry.getKey().equals("default"))
                            .map(entry -> {
                                T value = toT.apply(entry.getValue());
                                return new ImmutablePair<Predicate<BlockState>, Supplier<T>>(Vibrancy.BLOCK_STATE_PREDICATE.apply(block.getStateDefinition(), entry.getKey()), () -> value);
                            })
                            .collect(
                                    LinkedHashMap::new,
                                    (map, value) -> map.put(value.getLeft(), value.getRight()),
                                    HashMap::putAll
                            ),
                    def
            );
        } else {
            T t = toT.apply(json);
            return state -> t;
        }
    }
}
