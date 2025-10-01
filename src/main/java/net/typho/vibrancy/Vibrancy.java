package net.typho.vibrancy;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

public class Vibrancy implements ClientModInitializer {
    public static final String MOD_ID = "vibrancy";

    public static final Identifier LOGO_TEXTURE = Identifier.of(MOD_ID, "textures/gui/title/vibrancy.png");
    public static final SimpleOption<Boolean> DYNAMIC_LIGHTMAP = SimpleOption.ofBoolean("options.vibrancy.dynamic_lightmap", true);
    public static final SimpleOption<Boolean> TRANSPARENCY_TEST = SimpleOption.ofBoolean("options.vibrancy.transparency_test", value -> Tooltip.of(Text.translatable("options.vibrancy.transparency_test.tooltip")), true);
    public static final SimpleOption<Integer> RAYTRACE_DISTANCE = new SimpleOption<>(
            "options.vibrancy.raytrace_distance",
            value -> Tooltip.of(Text.translatable("options.vibrancy.raytrace_distance.tooltip")),
            (text, value) -> GameOptions.getGenericValueText(text, Text.translatable("options.chunks", value)),
            new SimpleOption.ValidatingIntSliderCallbacks(1, 16, false),
            4,
            value -> {}
    );
    public static final SimpleOption<Integer> LIGHT_CULL_DISTANCE = new SimpleOption<>(
            "options.vibrancy.light_cull_distance",
            value -> Tooltip.of(Text.translatable("options.vibrancy.light_cull_distance.tooltip")),
            (text, value) -> GameOptions.getGenericValueText(text, Text.translatable("options.chunks", value)),
            new SimpleOption.ValidatingIntSliderCallbacks(1, 16, false),
            8,
            value -> {}
    );
    public static final SimpleOption<Integer> MAX_RAYTRACED_LIGHTS = new SimpleOption<>(
            "options.vibrancy.max_raytraced_lights",
            value -> Tooltip.of(Text.translatable("options.vibrancy.max_raytraced_lights.tooltip")),
            (text, value) -> GameOptions.getGenericValueText(text, value > 100 ? Text.translatable("options.vibrancy.max_raytraced_lights.max") : Text.translatable("options.vibrancy.max_raytraced_lights.value", value)),
            new SimpleOption.ValidatingIntSliderCallbacks(5, 105, false),
            50,
            value -> {}
    );
    public static final SimpleOption<Integer> MAX_SHADOW_DISTANCE = new SimpleOption<>(
            "options.vibrancy.max_shadow_distance",
            value -> Tooltip.of(Text.translatable("options.vibrancy.max_shadow_distance.tooltip")),
            (text, value) -> GameOptions.getGenericValueText(text, value > 15 ? Text.translatable("options.vibrancy.max_shadow_distance.max") : Text.translatable("options.vibrancy.max_shadow_distance.value", value)),
            new SimpleOption.ValidatingIntSliderCallbacks(1, 16, false),
            8,
            value -> {}
    );
    public static final SimpleOption<Integer> MAX_LIGHT_RADIUS = new SimpleOption<>(
            "options.vibrancy.max_light_radius",
            value -> Tooltip.of(Text.translatable("options.vibrancy.max_light_radius.tooltip")),
            (text, value) -> GameOptions.getGenericValueText(text, value > 15 ? Text.translatable("options.vibrancy.max_light_radius.max") : Text.translatable("options.vibrancy.max_light_radius.value", value)),
            new SimpleOption.ValidatingIntSliderCallbacks(1, 16, false),
            15,
            value -> {}
    );
    public static boolean SEEN_ALPHA_TEXT = false;
    public static final KeyBinding SAVE_LIGHTMAP = !FabricLoader.getInstance().isDevelopmentEnvironment() ? null : KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.vibrancy.debug.save_lightmap",
            GLFW.GLFW_KEY_F9,
            "key.categories.misc"
    ));
    public static final Map<RegistryKey<Block>, BlockStateFunction<Boolean>> EMISSIVE_OVERRIDES = new LinkedHashMap<>();
    public static VertexBuffer SCREEN_VBO;
    public static final Map<BlockPos, RaytracedPointBlockLight> BLOCK_LIGHTS = new LinkedHashMap<>();
    public static final Map<LivingEntity, RaytracedPointEntityLight> ENTITY_LIGHTS = new LinkedHashMap<>();
    public static int NUM_LIGHT_TASKS = 0, NUM_RAYTRACED_LIGHTS = 0, NUM_VISIBLE_LIGHTS = 0;

    static {
        RenderSystem.recordRenderCall(() -> {
            SCREEN_VBO = new VertexBuffer(VertexBuffer.Usage.STATIC);

            BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION);
            builder.vertex(-1, 1, 0);
            builder.vertex(-1, -1, 0);
            builder.vertex(1, 1, 0);
            builder.vertex(1, -1, 0);

            SCREEN_VBO.bind();
            SCREEN_VBO.upload(builder.end());
            VertexBuffer.unbind();
        });
    }

    public static int maxLights() {
        int v = MAX_RAYTRACED_LIGHTS.getValue();
        return v > 100 ? Integer.MAX_VALUE : v;
    }

    public static int capShadowDistance(int distance) {
        int v = MAX_SHADOW_DISTANCE.getValue();
        return v > 15 ? distance : Math.min(distance, v);
    }

    public static boolean shouldRenderLight(RaytracedLight light) {
        boolean b = light.lazyDistance(MinecraftClient.getInstance().gameRenderer.getCamera().getPos()) / 16 < Vibrancy.LIGHT_CULL_DISTANCE.getValue() * Vibrancy.LIGHT_CULL_DISTANCE.getValue();

        if (b) {
            NUM_VISIBLE_LIGHTS++;
        }

        return b;
    }

    public static double getLightDistance(RaytracedLight light) {
        return light.lazyDistance(MinecraftClient.getInstance().gameRenderer.getCamera().getPos());
    }

    public static void renderLight(RaytracedLight light, int[] cap) {
        boolean raytrace = cap[0] < Vibrancy.maxLights();

        if (light.render(raytrace)) {
            if (raytrace) {
                NUM_RAYTRACED_LIGHTS++;
            }

            cap[0]++;
        }
    }

    public static void renderLights() {
        BLOCK_LIGHTS.values().removeIf(light -> light == null || light.remove);
        int[] cap = {0};
        NUM_RAYTRACED_LIGHTS = 0;
        NUM_VISIBLE_LIGHTS = 0;

        for (RaytracedPointBlockLight light : BLOCK_LIGHTS.values()) {
            light.updateDirty(RaytracedLight.DIRTY);
        }

        for (RaytracedPointEntityLight light : ENTITY_LIGHTS.values()) {
            light.updateDirty(RaytracedLight.DIRTY);
        }

        ENTITY_LIGHTS.values().stream()
                .sorted(Comparator.comparingDouble(Vibrancy::getLightDistance))
                .filter(Vibrancy::shouldRenderLight)
                .forEachOrdered(light -> renderLight(light, cap));
        BLOCK_LIGHTS.values().stream()
                .sorted(Comparator.comparingDouble(Vibrancy::getLightDistance))
                .filter(Vibrancy::shouldRenderLight)
                .forEachOrdered(light -> renderLight(light, cap));

        RaytracedLight.DIRTY.clear();
    }

    @Override
    public void onInitializeClient() {
        ClientChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> {
            BLOCK_LIGHTS.values().removeIf(light -> {
                boolean b = new ChunkPos(light.blockPos).equals(chunk.getPos());

                if (b) {
                    light.free();
                }

                return b;
            });
            ENTITY_LIGHTS.values().removeIf(light -> {
                boolean b = light.entity.getChunkPos().equals(chunk.getPos());

                if (b) {
                    light.free();
                }

                return b;
            });
        });
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, world) -> {
            BLOCK_LIGHTS.values().forEach(RaytracedPointBlockLight::free);
            BLOCK_LIGHTS.clear();
            ENTITY_LIGHTS.values().forEach(RaytracedPointEntityLight::free);
            ENTITY_LIGHTS.clear();
        });
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return Identifier.of(Vibrancy.MOD_ID, "dynamic_lights");
            }

            @Override
            public void reload(ResourceManager manager) {
                DynamicLightInfo.MAP.clear();

                for (Resource resource : manager.getAllResources(Identifier.of(Vibrancy.MOD_ID, "dynamic_lights.json"))) {
                    try (BufferedReader reader = resource.getReader()) {
                        JsonParser.parseReader(reader).getAsJsonObject().asMap().forEach((key, value) -> {
                            if (key.startsWith("#")) {
                                TagKey<Block> tagKey = TagKey.of(RegistryKeys.BLOCK, Identifier.of(key.substring(1)));
                                DynamicLightInfo.MAP.put(state -> state.isIn(tagKey), Util.memoize(state -> new DynamicLightInfo.Builder().load(state.getRegistryEntry().value(), value).build()));
                            } else {
                                RegistryKey<Block> regKey = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(key));
                                DynamicLightInfo info = new DynamicLightInfo.Builder().load(Registries.BLOCK.get(regKey), value).build();
                                DynamicLightInfo.MAP.put(state -> state.matchesKey(regKey), state -> info);
                            }
                        });
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                EMISSIVE_OVERRIDES.clear();

                for (Resource resource : manager.getAllResources(Identifier.of(Vibrancy.MOD_ID, "emissive_blocks.json"))) {
                    try (BufferedReader reader = resource.getReader()) {
                        JsonParser.parseReader(reader).getAsJsonObject().asMap().forEach((key, value) -> {
                            RegistryKey<Block> regKey = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(key));
                            EMISSIVE_OVERRIDES.put(regKey, BlockStateFunction.parseJson(Registries.BLOCK.get(regKey), value, JsonElement::getAsBoolean, () -> false));
                        });
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of(Vibrancy.MOD_ID, "vibrant_textures"), FabricLoader.getInstance().getModContainer(Vibrancy.MOD_ID).orElseThrow(), Text.translatable("pack.name.vibrancy.textures"), ResourcePackActivationType.NORMAL);
        ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of(Vibrancy.MOD_ID, "ripple"), FabricLoader.getInstance().getModContainer(Vibrancy.MOD_ID).orElseThrow(), Text.translatable("pack.name.vibrancy.ripple"), ResourcePackActivationType.DEFAULT_ENABLED);
    }

    public static float[] getTempTint(DimensionLightInfo dimLight, float temp) {
        float[] skyScales = new float[]{1, 1, 1};

        if (dimLight.minTemp() == null || dimLight.maxTemp() == null) {
            return new float[]{1, 1, 1};
        } else {
            if (temp > 0.8f) {
                float blend = MathHelper.clamp(temp - 0.8f, 0, 1);
                skyScales = new float[]{
                        MathHelper.lerp(blend, skyScales[0], dimLight.maxTemp()[0]),
                        MathHelper.lerp(blend, skyScales[1], dimLight.maxTemp()[1]),
                        MathHelper.lerp(blend, skyScales[2], dimLight.maxTemp()[2])
                };
            } else {
                float blend = MathHelper.clamp(0.8f - temp, 0, 1);
                skyScales = new float[]{
                        MathHelper.lerp(blend, skyScales[0], dimLight.minTemp()[0]),
                        MathHelper.lerp(blend, skyScales[1], dimLight.minTemp()[1]),
                        MathHelper.lerp(blend, skyScales[2], dimLight.minTemp()[2])
                };
            }
        }

        if (dimLight.skyScale() == null) {
            return skyScales;
        } else {
            return new float[]{
                    skyScales[0] * dimLight.skyScale()[0],
                    skyScales[1] * dimLight.skyScale()[1],
                    skyScales[2] * dimLight.skyScale()[2]
            };
        }
    }

    public static float getDay(ClientWorld world, float delta) {
        return world.getSkyBrightness(delta);
    }

    public static void createLightmap(ClientWorld world, ClientPlayerEntity player, GameOptions options, NativeImage image, float temp, float humid, float delta) {
        DimensionLightInfo dimLight = DimensionLightInfo.get(world);
        float day = getDay(world, delta);
        float brightness = MathHelper.clamp(2 - options.getGamma().getValue().floatValue(), 1, 2);
        float[] tempTint = getTempTint(dimLight, temp);

        for (int sky = 0; sky < image.getHeight(); sky++) {
            float fSky = (float) sky / (image.getHeight() - 1);

            if (player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                fSky = 1;
                day = 1;
            } else {
                fSky = (float) Math.pow(fSky, brightness);
            }

            float[] skyTint = dimLight.hasDay() && dimLight.nightSky() != null ? new float[]{
                    fSky * fSky * MathHelper.lerp(day, dimLight.nightSky()[0], tempTint[0]),
                    fSky * fSky * MathHelper.lerp(day, dimLight.nightSky()[1], tempTint[1]),
                    fSky * fSky * MathHelper.lerp(day, dimLight.nightSky()[2], tempTint[2])
            } : tempTint;

            for (int block = 0; block < image.getWidth(); block++) {
                float fBlock = (float) Math.pow((float) block / (image.getWidth() - 1), brightness);

                float red = fBlock * dimLight.block()[0] + skyTint[0];
                float green = fBlock * dimLight.block()[1] + skyTint[1];
                float blue = fBlock * dimLight.block()[2] + skyTint[2];

                image.setColor(block, sky, 0xFF000000 | ((int) MathHelper.clamp(blue * 255, 0, 255) << 16) | ((int) MathHelper.clamp(green * 255, 0, 255) << 8) | (int) MathHelper.clamp(red * 255, 0, 255));
            }
        }
    }
}
