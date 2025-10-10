package net.typho.vibrancy;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.particle.CampfireSmokeParticle;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.*;
import net.minecraft.world.chunk.ChunkSection;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

public class Vibrancy implements ClientModInitializer {
    public static final String MOD_ID = "vibrancy";

    public static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }

    public static final Identifier LOGO_TEXTURE = id("textures/gui/title/vibrancy.png");
    //public static final SimpleOption<Boolean> DYNAMIC_LIGHTMAP = SimpleOption.ofBoolean("options.vibrancy.dynamic_lightmap", value -> Tooltip.of(Text.translatable("options.vibrancy.dynamic_lightmap.tooltip")), true);
    public static final SimpleOption<Boolean> TRANSPARENCY_TEST = SimpleOption.ofBoolean("options.vibrancy.transparency_test", value -> Tooltip.of(Text.translatable("options.vibrancy.transparency_test.tooltip")), true);
    //public static final SimpleOption<Boolean> BETTER_SKY = SimpleOption.ofBoolean("options.vibrancy.better_sky", value -> Tooltip.of(Text.translatable("options.vibrancy.better_sky.tooltip")), true);
    public static final SimpleOption<Boolean> BETTER_FOG = SimpleOption.ofBoolean("options.vibrancy.better_fog", value -> Tooltip.of(Text.translatable("options.vibrancy.better_fog.tooltip")), true);
    public static final SimpleOption<Boolean> ELYTRA_TRAILS = SimpleOption.ofBoolean("options.vibrancy.elytra_trails", value -> Tooltip.of(Text.translatable("options.vibrancy.elytra_trails.tooltip")), true);
    public static final SimpleOption<Integer> RAYTRACE_DISTANCE = new SimpleOption<>(
            "options.vibrancy.raytrace_distance",
            value -> Tooltip.of(Text.translatable("options.vibrancy.raytrace_distance.tooltip")),
            (text, value) -> GameOptions.getGenericValueText(text, Text.translatable("options.vibrancy.raytrace_distance.value", value * 16)),
            new SimpleOption.ValidatingIntSliderCallbacks(1, 32, false),
            4,
            value -> {}
    );
    public static final SimpleOption<Integer> LIGHT_CULL_DISTANCE = new SimpleOption<>(
            "options.vibrancy.light_cull_distance",
            value -> Tooltip.of(Text.translatable("options.vibrancy.light_cull_distance.tooltip")),
            (text, value) -> GameOptions.getGenericValueText(text, Text.translatable("options.vibrancy.light_cull_distance.value", value * 16)),
            new SimpleOption.ValidatingIntSliderCallbacks(1, 32, false),
            12,
            value -> {}
    );
    public static final SimpleOption<Integer> MAX_RAYTRACED_LIGHTS = new SimpleOption<>(
            "options.vibrancy.max_raytraced_lights",
            value -> Tooltip.of(Text.translatable("options.vibrancy.max_raytraced_lights.tooltip")),
            (text, value) -> GameOptions.getGenericValueText(text, value > 100 ? Text.translatable("options.vibrancy.max_raytraced_lights.max") : Text.translatable("options.vibrancy.max_raytraced_lights.value", value)),
            new SimpleOption.ValidatingIntSliderCallbacks(5, 105, false),
            30,
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
    public static final SimpleOption<Double> BLOCK_LIGHT_MULTIPLIER = new SimpleOption<>(
            "options.vibrancy.block_light_multiplier",
            value -> Tooltip.of(Text.translatable("options.vibrancy.block_light_multiplier.tooltip")),
            (text, value) -> GameOptions.getGenericValueText(text, Text.translatable("options.vibrancy.block_light_multiplier.value", (int) (value * 100))),
            SimpleOption.DoubleSliderCallbacks.INSTANCE,
            0.5,
            value -> {}
    );
    public static boolean SEEN_ALPHA_TEXT = false;
    public static final SimpleParticleType STEAM = Registry.register(Registries.PARTICLE_TYPE, id("steam"), new SimpleParticleType(false) {});
    public static final Map<RegistryKey<Block>, BlockStateFunction<Boolean>> EMISSIVE_OVERRIDES = new LinkedHashMap<>();
    public static final Map<BlockPos, RaytracedPointBlockLight> BLOCK_LIGHTS = new LinkedHashMap<>();
    public static final Map<LivingEntity, RaytracedPointEntityLight> ENTITY_LIGHTS = new LinkedHashMap<>();
    public static int NUM_LIGHT_TASKS = 0, NUM_RAYTRACED_LIGHTS = 0, NUM_VISIBLE_LIGHTS = 0;

    public static int maxLights() {
        int v = MAX_RAYTRACED_LIGHTS.getValue();
        return v > 100 ? Integer.MAX_VALUE : v;
    }

    public static int capShadowDistance(int distance) {
        int v = MAX_SHADOW_DISTANCE.getValue();
        return v > 15 ? distance : Math.min(distance, v);
    }

    public static boolean shouldRenderLight(RaytracedLight light) {
        Vec3d cam = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        boolean b = light.getPosition().distanceSquared(cam.x, cam.y, cam.z) / 16 < Vibrancy.LIGHT_CULL_DISTANCE.getValue() * Vibrancy.LIGHT_CULL_DISTANCE.getValue() &&
                (VeilRenderSystem.getCullingFrustum().testAab(light.getBoundingBox()) || (light instanceof RaytracedPointEntityLight entity && entity.entity == MinecraftClient.getInstance().cameraEntity));

        if (b) {
            NUM_VISIBLE_LIGHTS++;
        }

        return b;
    }

    public static double getLightDistance(RaytracedLight light) {
        Vec3d cam = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        return light.getPosition().distanceSquared(cam.x, cam.y, cam.z);
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

    public static void renderLightDebug(RaytracedLight light, VertexConsumer consumer) {
        Vec3d camera = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        WorldRenderer.drawBox(
                consumer,
                light.getPosition().x - 0.3 - camera.getX(), light.getPosition().y - 0.3 - camera.getY(), light.getPosition().z - 0.3 - camera.getZ(),
                light.getPosition().x + 0.3 - camera.getX(), light.getPosition().y + 0.3 - camera.getY(), light.getPosition().z + 0.3 - camera.getZ(),
                1, 1, 1, 1
        );
    }

    public static boolean pointsToward(BlockPos from, Direction dir, BlockPos to) {
        return switch (dir.getAxis()) {
            case X -> dir.getDirection() == Direction.AxisDirection.POSITIVE
                    ? from.getX() <= to.getX()
                    : from.getX() >= to.getX();
            case Y -> dir.getDirection() == Direction.AxisDirection.POSITIVE
                    ? from.getY() <= to.getY()
                    : from.getY() >= to.getY();
            case Z -> dir.getDirection() == Direction.AxisDirection.POSITIVE
                    ? from.getZ() <= to.getZ()
                    : from.getZ() >= to.getZ();
        };
    }

    public static void elytraTrail(LivingEntity entity) {
        if (Math.random() < Math.min(entity.getVelocity().length() - 0.75, (entity.getY() - 80) / 40)) {
            entity.getWorld().addParticle(STEAM, entity.getX(), entity.getY(), entity.getZ(), 0, 0, 0);
        }
    }

    public static void renderLights() {
        VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(id("ray_light")).bind(true);
        RenderSystem.clearColor(0, 0, 0, 0);
        glClear(GL_COLOR_BUFFER_BIT);

        BLOCK_LIGHTS.values().removeIf(light -> {
            boolean b = light == null || light.remove;

            if (b && light != null) {
                light.free();
            }

            return b;
        });
        int[] cap = {0};
        NUM_RAYTRACED_LIGHTS = 0;
        NUM_VISIBLE_LIGHTS = 0;

        for (RaytracedPointBlockLight light : BLOCK_LIGHTS.values()) {
            light.updateDirty(RaytracedLight.DIRTY);
            light.init();
        }

        for (RaytracedPointEntityLight light : ENTITY_LIGHTS.values()) {
            light.updateDirty(RaytracedLight.DIRTY);
            light.init();
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

            float[] skyTint = dimLight.nightSky() != null ? new float[]{
                    fSky * fSky * MathHelper.lerp(day, dimLight.nightSky()[0], tempTint[0]),
                    fSky * fSky * MathHelper.lerp(day, dimLight.nightSky()[1], tempTint[1]),
                    fSky * fSky * MathHelper.lerp(day, dimLight.nightSky()[2], tempTint[2])
            } : new float[]{
                tempTint[0] * fSky * fSky,
                tempTint[1] * fSky * fSky,
                tempTint[2] * fSky * fSky
            };

            for (int block = 0; block < image.getWidth(); block++) {
                float fBlock = (float) Math.pow((float) block / (image.getWidth() - 1), brightness) * BLOCK_LIGHT_MULTIPLIER.getValue().floatValue();

                float red = fBlock * dimLight.block()[0] + skyTint[0];
                float green = fBlock * dimLight.block()[1] + skyTint[1];
                float blue = fBlock * dimLight.block()[2] + skyTint[2];

                image.setColor(block, sky, 0xFF000000 | ((int) MathHelper.clamp(blue * 255, 0, 255) << 16) | ((int) MathHelper.clamp(green * 255, 0, 255) << 8) | (int) MathHelper.clamp(red * 255, 0, 255));
            }
        }
    }

    public static void updateBlock(BlockPos pos, BlockState oldBlock, BlockState newBlock) {
        DynamicLightInfo info = DynamicLightInfo.get(newBlock);

        if (info != null) {
            info.addBlockLight(pos, newBlock);
        }
    }

    @Override
    public void onInitializeClient() {
        ParticleFactoryRegistry.getInstance().register(STEAM, CampfireSmokeParticle.SignalSmokeFactory::new);
        WorldRenderEvents.LAST.register(context -> {
            if (MinecraftClient.getInstance().getDebugHud().shouldShowDebugHud() && FabricLoader.getInstance().isDevelopmentEnvironment()) {
                RenderSystem.disableBlend();
                RenderSystem.disableDepthTest();
                AdvancedFbo.unbind();
                ENTITY_LIGHTS.values().forEach(light -> renderLightDebug(light, context.consumers().getBuffer(RenderLayer.getLines())));
                BLOCK_LIGHTS.values().forEach(light -> renderLightDebug(light, context.consumers().getBuffer(RenderLayer.getLines())));
            }

            NUM_LIGHT_TASKS = 0;
            Identifier id = id("ray_light");

            VeilRenderSystem.renderer().enableBuffers(id, DynamicBufferType.NORMAL, DynamicBufferType.ALBEDO);

            renderLights();
        });
        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            for (int i = chunk.getBottomSectionCoord(); i < chunk.getTopSectionCoord(); i++) {
                ChunkSection section = chunk.getSection(chunk.sectionCoordToIndex(i));

                if (section.hasAny(state -> DynamicLightInfo.MAP.keySet()
                        .stream()
                        .anyMatch(p -> p.test(state)))) {
                    BlockPos minPos = ChunkSectionPos.from(chunk.getPos(), i).getMinPos();

                    for (int x = 0; x < 16; x++) {
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                BlockState state = section.getBlockState(x, y, z);
                                DynamicLightInfo info = DynamicLightInfo.get(state);

                                if (info != null) {
                                    info.addBlockLight(new BlockPos(x + minPos.getX(), y + minPos.getY(), z + minPos.getZ()), state);
                                }
                            }
                        }
                    }
                }
            }
        });
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

            for (AbstractClientPlayerEntity player : world.getPlayers()) {
                System.out.println(player);
                ENTITY_LIGHTS.put(player, new RaytracedPointEntityLight(player));
            }
        });
        ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener() {
            @Override
            public Identifier getFabricId() {
                return id("dynamic_lights");
            }

            @Override
            public void reload(ResourceManager manager) {
                DynamicLightInfo.MAP.clear();

                for (Resource resource : manager.getAllResources(id("dynamic_lights.json"))) {
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

                for (Resource resource : manager.getAllResources(id("emissive_blocks.json"))) {
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
        ModContainer mod = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
        ResourceManagerHelper.registerBuiltinResourcePack(id("vibrant_textures"), mod, Text.translatable("pack.name.vibrancy.textures"), ResourcePackActivationType.NORMAL);
        ResourceManagerHelper.registerBuiltinResourcePack(id("ripple"), mod, Text.translatable("pack.name.vibrancy.ripple"), ResourcePackActivationType.DEFAULT_ENABLED);
    }
}
