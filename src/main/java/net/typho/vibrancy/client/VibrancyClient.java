package net.typho.vibrancy.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import foundry.veil.api.client.registry.LightTypeRegistry;
import foundry.veil.platform.registry.RegistrationProvider;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.typho.vibrancy.Vibrancy;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

public class VibrancyClient implements ClientModInitializer {
    public static final SimpleOption<Boolean> DYNAMIC_LIGHTMAP = SimpleOption.ofBoolean("options.vibrancy.dynamic_lightmap", true);
    public static final SimpleOption<Boolean> RAYTRACE_LIGHTS = SimpleOption.ofBoolean("options.vibrancy.raytrace_lights", true);
    public static final KeyBinding SAVE_LIGHTMAP = !FabricLoader.getInstance().isDevelopmentEnvironment() ? null : KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.vibrancy.debug.save_lightmap",
            GLFW.GLFW_KEY_F9,
            "key.categories.misc"
    ));
    public static final RegistrationProvider<LightTypeRegistry.LightType<?>> LIGHT_TYPE_PROVIDER = RegistrationProvider.get(LightTypeRegistry.REGISTRY_KEY, Vibrancy.MOD_ID);
    public static final Supplier<LightTypeRegistry.LightType<RaytracedPointLight>> RAY_POINT_LIGHT = LIGHT_TYPE_PROVIDER.register("ray_point", () -> new LightTypeRegistry.LightType<>(RaytracedPointLightRenderer::new, (level, camera) -> new RaytracedPointLight().setTo(camera).setRadius(15)));

    @Override
    public void onInitializeClient() {
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
                        DataResult<Pair<Map<RegistryKey<Block>, Either<DynamicLightInfo, RegistryKey<Block>>>, JsonElement>> result = DynamicLightInfo.FILE_CODEC.decode(JsonOps.INSTANCE, JsonParser.parseReader(reader));

                        if (result.isSuccess()) {
                            result.getOrThrow().getFirst().forEach((k, v) -> DynamicLightInfo.MAP.put(k, v.map(
                                    info -> state -> info,
                                    key -> state -> DynamicLightInfo.get(key, state)
                            )));
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
        ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of(Vibrancy.MOD_ID, "emissive_ores"), FabricLoader.getInstance().getModContainer(Vibrancy.MOD_ID).orElseThrow(), Text.translatable("pack.name.vibrancy.emissive_ores"), ResourcePackActivationType.DEFAULT_ENABLED);
        ResourceManagerHelper.registerBuiltinResourcePack(Identifier.of(Vibrancy.MOD_ID, "vibrant_textures"), FabricLoader.getInstance().getModContainer(Vibrancy.MOD_ID).orElseThrow(), Text.translatable("pack.name.vibrancy.textures"), ResourcePackActivationType.DEFAULT_ENABLED);
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
