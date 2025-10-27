package net.typho.vibrancy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.typho.vibrancy.light.SkyLight;
import org.joml.Vector3f;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record DimensionLightInfo(Vector3f color, Optional<Float> brightness, Optional<Float> flicker) {
    public static final Map<ResourceKey<Level>, DimensionLightInfo> MAP = new LinkedHashMap<>();

    public static DimensionLightInfo get(ResourceKey<Level> key) {
        return MAP.get(key);
    }

    public SkyLight createSkyLight() {
        SkyLight light = new SkyLight();
        //light.setFlicker(flicker().orElse(0f));
        //light.setBrightness(brightness.orElse(1f));
        //light.setColor(color().x, color().y, color().z);
        return light;
    }

    public static class Builder {
        public Vector3f color;
        public Optional<Float> brightness = Optional.empty(), flicker = Optional.empty();

        public Builder color(Vector3f color) {
            this.color = color;
            return this;
        }

        public Builder brightness(Optional<Float> brightness) {
            Optional<Float> brightness1 = this.brightness;
            this.brightness = brightness.or(() -> brightness1);
            return this;
        }

        public Builder flicker(Optional<Float> flicker) {
            Optional<Float> flicker1 = this.flicker;
            this.flicker = flicker.or(() -> flicker1);
            return this;
        }

        public Builder copy(DimensionLightInfo info) {
            return color(info.color)
                    .brightness(info.brightness)
                    .flicker(info.flicker);
        }

        public Builder then(String key, JsonElement value) {
            return switch (key) {
                case "color" -> {
                    if (value.isJsonArray()) {
                        JsonArray array = value.getAsJsonArray();

                        if (array.size() != 3) {
                            throw new JsonParseException("Expected a 3-element array for color while parsing dimension light info, got " + array.size() + " elements");
                        }

                        yield color(new Vector3f(
                                array.get(0).getAsJsonPrimitive().getAsFloat(),
                                array.get(1).getAsJsonPrimitive().getAsFloat(),
                                array.get(2).getAsJsonPrimitive().getAsFloat()
                        ));
                    } else {
                        throw new JsonParseException("Expected a 3-element array for color while parsing dimension light info, got " + value);
                    }
                }
                case "brightness" -> {
                    if (value.isJsonNull()) {
                        brightness = Optional.empty();
                        yield this;
                    } else if (value.isJsonPrimitive()) {
                        yield brightness(Optional.of(value.getAsJsonPrimitive().getAsFloat()));
                    } else {
                        throw new JsonParseException("Expected a float for brightness while parsing dimension light info, got " + value);
                    }
                }
                case "flicker" -> {
                    if (value.isJsonNull()) {
                        flicker = Optional.empty();
                        yield this;
                    } else if (value.isJsonPrimitive()) {
                        yield flicker(Optional.of(value.getAsJsonPrimitive().getAsFloat()));
                    } else {
                        throw new JsonParseException("Expected a float for flicker while parsing dimension light info, got " + value);
                    }
                }
                default -> this;
            };
        }

        public DimensionLightInfo build() {
            return new DimensionLightInfo(color, brightness, flicker);
        }
    }
}
