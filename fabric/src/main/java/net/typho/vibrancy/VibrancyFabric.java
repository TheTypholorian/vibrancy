package net.typho.vibrancy;

import com.google.common.collect.Maps;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Map;
import java.util.Objects;

public class VibrancyFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Vibrancy.BLOCK_STATE_PREDICATE = (definition, key) -> {
            Map<Property<?>, Comparable<?>> map = Maps.newHashMap();

            for (String entry : key.split(",")) {
                String[] parts = entry.split("=");
                Property<?> property = definition.getProperty(parts[0]);

                if (property != null && parts.length == 2) {
                    property.getValue(parts[1]).ifPresent(value -> map.put(property, value));
                }
            }

            return state -> {
                for (Map.Entry<Property<?>, Comparable<?>> e : map.entrySet()) {
                    if (!Objects.equals(state.getValue(e.getKey()), e.getValue())) {
                        return false;
                    }
                }

                return true;
            };
        };
        SimpleParticleType type = Registry.register(BuiltInRegistries.PARTICLE_TYPE, Vibrancy.id("steam"), new SimpleParticleType(false) {});
        Vibrancy.STEAM = () -> type;
        Vibrancy.init();
    }
}
