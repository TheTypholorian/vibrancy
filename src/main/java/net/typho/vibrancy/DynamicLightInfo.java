package net.typho.vibrancy;

import com.google.gson.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public record DynamicLightInfo(Vector3f color, BlockStateFunction<Optional<Float>> radius, BlockStateFunction<Optional<Float>> brightness, BlockStateFunction<Optional<Float>> flicker, BlockStateFunction<Optional<Vec3d>> offset) {
    public static final Map<Predicate<BlockState>, Function<BlockState, DynamicLightInfo>> MAP = new LinkedHashMap<>();

    public static DynamicLightInfo get(BlockState state) {
        return MAP.entrySet().stream()
                .filter(entry -> entry.getKey().test(state))
                .findAny()
                .map(entry -> entry.getValue().apply(state))
                .orElse(null);
    }

    public RaytracedPointBlockLight createLight(BlockPos pos, BlockState state) {
        float brightness = brightness().apply(state).orElse(1f);
        float radius = radius().apply(state).orElse((float) state.getLuminance());

        if (brightness <= 0 || radius <= 0) {
            return null;
        }

        return (RaytracedPointBlockLight) new RaytracedPointBlockLight(
                pos,
                state.getBlock(),
                this,
                offset().apply(state).orElse(new Vec3d(0.5, 0.5, 0.5))
        )
                .setFlicker(flicker().apply(state).orElse(0f))
                .setBrightness(brightness)
                .setColor(color().x, color().y, color().z)
                .setRadius(radius);
    }

    public void addLight(BlockPos pos, BlockState state) {
        RaytracedPointBlockLight light = createLight(pos, state);

        if (light != null) {
            RaytracedPointBlockLight old = RaytracedPointBlockLightRenderer.INSTANCE.lights.put(pos, light);

            if (old != null) {
                old.free();
            }
        }
    }

    public static class Builder {
        public Vector3f color;
        public BlockStateFunction<Optional<Float>> radius = state -> Optional.empty(), brightness = state -> Optional.empty(), flicker = state -> Optional.empty();
        public BlockStateFunction<Optional<Vec3d>> offset = state -> Optional.empty();

        public Builder color(Vector3f color) {
            this.color = color;
            return this;
        }

        public Builder radius(BlockStateFunction<Optional<Float>> radius) {
            BlockStateFunction<Optional<Float>> radius1 = this.radius;
            this.radius = state -> radius.apply(state).or(() -> radius1.apply(state));
            return this;
        }

        public Builder brightness(BlockStateFunction<Optional<Float>> brightness) {
            BlockStateFunction<Optional<Float>> brightness1 = this.brightness;
            this.brightness = state -> brightness.apply(state).or(() -> brightness1.apply(state));
            return this;
        }

        public Builder flicker(BlockStateFunction<Optional<Float>> flicker) {
            BlockStateFunction<Optional<Float>> flicker1 = this.flicker;
            this.flicker = state -> flicker.apply(state).or(() -> flicker1.apply(state));
            return this;
        }

        public Builder offset(BlockStateFunction<Optional<Vec3d>> offset) {
            BlockStateFunction<Optional<Vec3d>> offset1 = this.offset;
            this.offset = state -> offset.apply(state).or(() -> offset1.apply(state));
            return this;
        }

        public Builder copy(DynamicLightInfo info) {
            return color(info.color)
                    .radius(info.radius)
                    .brightness(info.brightness)
                    .flicker(info.flicker)
                    .offset(info.offset);
        }

        public Builder then(Block block, String key, JsonElement value) {
            return switch (key) {
                case "color" -> {
                    if (value.isJsonArray()) {
                        JsonArray array = value.getAsJsonArray();

                        if (array.size() != 3) {
                            throw new JsonParseException("Expected a 3-element array for color while parsing dynamic light info while parsing " + block + ", got " + array.size() + " elements");
                        }

                        yield color(new Vector3f(
                                array.get(0).getAsJsonPrimitive().getAsFloat(),
                                array.get(1).getAsJsonPrimitive().getAsFloat(),
                                array.get(2).getAsJsonPrimitive().getAsFloat()
                        ));
                    } else {
                        throw new JsonParseException("Expected a 3-element array for color while parsing dynamic light info while parsing " + block + ", got " + value);
                    }
                }
                case "offset" -> {
                    if (value.isJsonNull()) {
                        offset = state -> Optional.empty();
                        yield this;
                    } else if (value.isJsonObject() || value.isJsonArray()) {
                        yield offset(BlockStateFunction.parseJson(block, value, json -> {
                            if (json.isJsonArray()) {
                                JsonArray array = json.getAsJsonArray();

                                if (array.size() != 3) {
                                    throw new JsonParseException("Expected a 3-element array for offset while parsing dynamic light info while parsing " + block + ", got " + array.size() + " elements");
                                }

                                return Optional.of(new Vec3d(
                                        array.get(0).getAsJsonPrimitive().getAsDouble(),
                                        array.get(1).getAsJsonPrimitive().getAsDouble(),
                                        array.get(2).getAsJsonPrimitive().getAsDouble()
                                ));
                            } else {
                                throw new JsonParseException("Expected a 3-element array for offset while parsing dynamic light info while parsing " + block + ", got " + json);
                            }
                        }, Optional::empty));
                    } else {
                        throw new JsonParseException("Expected a 3-element array for offset while parsing dynamic light info while parsing " + block + ", got " + value);
                    }
                }
                case "radius" -> {
                    if (value.isJsonNull()) {
                        radius = state -> Optional.empty();
                        yield this;
                    } else if (value.isJsonObject() || value.isJsonPrimitive()) {
                        yield radius(BlockStateFunction.parseJson(block, value, json -> {
                            if (json.isJsonPrimitive()) {
                                return Optional.of(json.getAsJsonPrimitive().getAsFloat());
                            } else {
                                throw new JsonParseException("Expected a float for radius while parsing dynamic light info while parsing " + block + ", got " + json);
                            }
                        }, Optional::empty));
                    } else {
                        throw new JsonParseException("Expected a float for radius while parsing dynamic light info while parsing " + block + ", got " + value);
                    }
                }
                case "brightness" -> {
                    if (value.isJsonNull()) {
                        brightness = state -> Optional.empty();
                        yield this;
                    } else if (value.isJsonObject() || value.isJsonPrimitive()) {
                        yield brightness(BlockStateFunction.parseJson(block, value, json -> {
                            if (json.isJsonPrimitive()) {
                                return Optional.of(json.getAsJsonPrimitive().getAsFloat());
                            } else {
                                throw new JsonParseException("Expected a float for brightness while parsing dynamic light info while parsing " + block + ", got " + json);
                            }
                        }, Optional::empty));
                    } else {
                        throw new JsonParseException("Expected a float for brightness while parsing dynamic light info while parsing " + block + ", got " + value);
                    }
                }
                case "flicker" -> {
                    if (value.isJsonNull()) {
                        flicker = state -> Optional.empty();
                        yield this;
                    } else if (value.isJsonObject() || value.isJsonPrimitive()) {
                        yield flicker(BlockStateFunction.parseJson(block, value, json -> {
                            if (json.isJsonPrimitive()) {
                                return Optional.of(json.getAsJsonPrimitive().getAsFloat());
                            } else {
                                throw new JsonParseException("Expected a float for flicker while parsing dynamic light info while parsing " + block + ", got " + json);
                            }
                        }, Optional::empty));
                    } else {
                        throw new JsonParseException("Expected a float for flicker while parsing dynamic light info while parsing " + block + ", got " + value);
                    }
                }
                default -> this;
            };
        }

        public Builder load(Block block, JsonElement json) {
            if (json.isJsonObject()) {
                JsonObject jsonObj = json.getAsJsonObject();

                JsonElement copy = jsonObj.get("copy");

                if (copy != null) {
                    if (copy.isJsonPrimitive()) {
                        JsonPrimitive primitive = copy.getAsJsonPrimitive();

                        if (primitive.isString()) {
                            String s = primitive.getAsString();

                            if (s.startsWith("#")) {
                                throw new IllegalArgumentException("Can't copy the dynamic light info of a tag (for technical reasons)");
                            }

                            copy(get(Registries.BLOCK.get(Identifier.of(s)).getDefaultState()));
                        } else {
                            throw new JsonParseException("Expected a string for copy while parsing dynamic light info for \"" + block + "\", got " + primitive);
                        }
                    } else {
                        throw new JsonParseException("Expected a string for copy while parsing dynamic light info for \"" + block + "\", got " + copy);
                    }
                }

                jsonObj.asMap().forEach((key1, value) -> then(block, key1, value));
                return this;
            } else if (json.isJsonPrimitive()) {
                JsonPrimitive primitive = json.getAsJsonPrimitive();

                if (primitive.isString()) {
                    String s = primitive.getAsString();

                    if (s.startsWith("#")) {
                        throw new IllegalArgumentException("Can't copy the dynamic light info of a tag (for technical reasons)");
                    }

                    return copy(get(Registries.BLOCK.get(Identifier.of(s)).getDefaultState()));
                }
            }

            throw new JsonParseException("Expected either a string or object while parsing dynamic light info for \"" + block + "\", got " + json);
        }

        public DynamicLightInfo build() {
            return new DynamicLightInfo(color, radius, brightness, flicker, offset);
        }
    }
}
