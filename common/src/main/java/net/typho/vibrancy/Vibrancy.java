package net.typho.vibrancy;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;

public class Vibrancy {
    public static final String MOD_ID = "vibrancy";

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static final ResourceLocation LOGO_TEXTURE = id("textures/gui/title/vibrancy.png");
    //public static final OptionInstance<Boolean> DYNAMIC_LIGHTMAP = OptionInstance.createBoolean("options.vibrancy.dynamic_lightmap", value -> Tooltip.create(Component.translatable("options.vibrancy.dynamic_lightmap.tooltip")), true);
    public static final OptionInstance<Boolean> TRANSPARENCY_TEST = OptionInstance.createBoolean("options.vibrancy.transparency_test", value -> Tooltip.create(Component.translatable("options.vibrancy.transparency_test.tooltip")), true);
    //public static final OptionInstance<Boolean> BETTER_SKY = OptionInstance.createBoolean("options.vibrancy.better_sky", value -> Tooltip.create(Component.translatable("options.vibrancy.better_sky.tooltip")), true);
    public static final OptionInstance<Boolean> BETTER_FOG = OptionInstance.createBoolean("options.vibrancy.better_fog", value -> Tooltip.create(Component.translatable("options.vibrancy.better_fog.tooltip")), true);
    public static final OptionInstance<Boolean> ELYTRA_TRAILS = OptionInstance.createBoolean("options.vibrancy.elytra_trails", value -> Tooltip.create(Component.translatable("options.vibrancy.elytra_trails.tooltip")), true);
    public static final OptionInstance<Integer> RAYTRACE_DISTANCE = new OptionInstance<>(
            "options.vibrancy.raytrace_distance",
            value -> Tooltip.create(Component.translatable("options.vibrancy.raytrace_distance.tooltip")),
            (text, value) -> Options.genericValueLabel(text, Component.translatable("options.vibrancy.raytrace_distance.value", value * 16)),
            new OptionInstance.IntRange(1, 32, false),
            16,
            value -> {}
    );
    public static final OptionInstance<Integer> LIGHT_CULL_DISTANCE = new OptionInstance<>(
            "options.vibrancy.light_cull_distance",
            value -> Tooltip.create(Component.translatable("options.vibrancy.light_cull_distance.tooltip")),
            (text, value) -> Options.genericValueLabel(text, Component.translatable("options.vibrancy.light_cull_distance.value", value * 16)),
            new OptionInstance.IntRange(1, 32, false),
            32,
            value -> {}
    );
    public static final OptionInstance<Integer> MAX_RAYTRACED_LIGHTS = new OptionInstance<>(
            "options.vibrancy.max_raytraced_lights",
            value -> Tooltip.create(Component.translatable("options.vibrancy.max_raytraced_lights.tooltip")),
            (text, value) -> Options.genericValueLabel(text, value > 100 ? Component.translatable("options.vibrancy.max_raytraced_lights.max") : Component.translatable("options.vibrancy.max_raytraced_lights.value", value)),
            new OptionInstance.IntRange(5, 105, false),
            60,
            value -> {}
    );
    public static final OptionInstance<Integer> MAX_SHADOW_DISTANCE = new OptionInstance<>(
            "options.vibrancy.max_shadow_distance",
            value -> Tooltip.create(Component.translatable("options.vibrancy.max_shadow_distance.tooltip")),
            (text, value) -> Options.genericValueLabel(text, value > 15 ? Component.translatable("options.vibrancy.max_shadow_distance.max") : Component.translatable("options.vibrancy.max_shadow_distance.value", value)),
            new OptionInstance.IntRange(1, 16, false),
            6,
            value -> {}
    );
    public static final OptionInstance<Integer> MAX_LIGHT_RADIUS = new OptionInstance<>(
            "options.vibrancy.max_light_radius",
            value -> Tooltip.create(Component.translatable("options.vibrancy.max_light_radius.tooltip")),
            (text, value) -> Options.genericValueLabel(text, value > 15 ? Component.translatable("options.vibrancy.max_light_radius.max") : Component.translatable("options.vibrancy.max_light_radius.value", value)),
            new OptionInstance.IntRange(1, 16, false),
            15,
            value -> {}
    );
    public static final OptionInstance<Double> BLOCK_LIGHT_MULTIPLIER = new OptionInstance<>(
            "options.vibrancy.block_light_multiplier",
            value -> Tooltip.create(Component.translatable("options.vibrancy.block_light_multiplier.tooltip")),
            (text, value) -> Options.genericValueLabel(text, Component.translatable("options.vibrancy.block_light_multiplier.value", (int) (value * 100))),
            OptionInstance.UnitDouble.INSTANCE,
            0.5,
            value -> {}
    );
    public static boolean SEEN_ALPHA_TEXT = false;
    public static Supplier<SimpleParticleType> STEAM;
    public static final Map<ResourceKey<Block>, BlockStateFunction<Boolean>> EMISSIVE_OVERRIDES = new LinkedHashMap<>();
    public static final Map<BlockPos, RaytracedPointBlockLight> BLOCK_LIGHTS = new LinkedHashMap<>();
    public static final Map<LivingEntity, RaytracedPointEntityLight> ENTITY_LIGHTS = new LinkedHashMap<>();
    public static int NUM_LIGHT_TASKS = 0, NUM_RAYTRACED_LIGHTS = 0, NUM_VISIBLE_LIGHTS = 0, SHADOW_COUNT = 0;
    public static BiFunction<StateDefinition<Block, BlockState>, String, Predicate<BlockState>> BLOCK_STATE_PREDICATE = (def, properties) -> {
        throw new IllegalStateException();
    };

    public static int maxLights() {
        int v = MAX_RAYTRACED_LIGHTS.get();
        return v > 100 ? Integer.MAX_VALUE : v;
    }

    public static int capShadowDistance(int distance) {
        int v = MAX_SHADOW_DISTANCE.get();
        return v > 15 ? distance : Math.min(distance, v);
    }

    public static boolean shouldRenderLight(RaytracedLight light) {
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        boolean b = light.getPosition().distanceSquared(cam.x, cam.y, cam.z) / 16 < Vibrancy.LIGHT_CULL_DISTANCE.get() * Vibrancy.LIGHT_CULL_DISTANCE.get() &&
                (VeilRenderSystem.getCullingFrustum().testAab(light.getBoundingBox()) || (light instanceof RaytracedPointEntityLight entity && entity.entity == Minecraft.getInstance().cameraEntity));

        if (b) {
            NUM_VISIBLE_LIGHTS++;
        }

        return b;
    }

    public static double getLightDistance(RaytracedLight light) {
        Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
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

    public static boolean pointsToward(BlockPos from, Direction dir, BlockPos to) {
        return switch (dir.getAxis()) {
            case X -> dir.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? from.getX() <= to.getX()
                    : from.getX() >= to.getX();
            case Y -> dir.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? from.getY() <= to.getY()
                    : from.getY() >= to.getY();
            case Z -> dir.getAxisDirection() == Direction.AxisDirection.POSITIVE
                    ? from.getZ() <= to.getZ()
                    : from.getZ() >= to.getZ();
        };
    }

    public static void elytraTrail(LivingEntity entity) {
        if (Math.random() < Math.min(entity.getDeltaMovement().length() - 0.75, (entity.getY() - 80) / 40)) {
            entity.level().addParticle(STEAM.get(), entity.getX(), entity.getY(), entity.getZ(), 0, 0, 0);
        }
    }

    public static void updateBlock(BlockPos pos, BlockState state) {
        DynamicLightInfo info = DynamicLightInfo.get(state);

        if (info != null) {
            info.addBlockLight(pos, state);
        }
    }

    @SuppressWarnings("deprecation")
    public static void registerReloadListeners(ReloadableResourceManager resourceManager) {
        resourceManager.registerReloadListener((ResourceManagerReloadListener) manager -> {
            DynamicLightInfo.MAP.clear();

            for (Resource resource : manager.getResourceStack(id("dynamic_lights.json"))) {
                try (BufferedReader reader = resource.openAsReader()) {
                    JsonParser.parseReader(reader).getAsJsonObject().asMap().forEach((key, value) -> {
                        if (key.startsWith("#")) {
                            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, ResourceLocation.parse(key.substring(1)));
                            DynamicLightInfo.MAP.put(state -> state.is(tagKey), Util.memoize(state -> new DynamicLightInfo.Builder().load(state.getBlock().builtInRegistryHolder().value(), value).build()));
                        } else {
                            ResourceKey<Block> regKey = ResourceKey.create(Registries.BLOCK, ResourceLocation.parse(key));
                            DynamicLightInfo info = new DynamicLightInfo.Builder().load(BuiltInRegistries.BLOCK.get(regKey), value).build();
                            DynamicLightInfo.MAP.put(state -> state.is(regKey), state -> info);
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            EMISSIVE_OVERRIDES.clear();

            for (Resource resource : manager.getResourceStack(id("emissive_blocks.json"))) {
                try (BufferedReader reader = resource.openAsReader()) {
                    JsonParser.parseReader(reader).getAsJsonObject().asMap().forEach((key, value) -> {
                        ResourceKey<Block> regKey = ResourceKey.create(Registries.BLOCK, ResourceLocation.parse(key));
                        EMISSIVE_OVERRIDES.put(regKey, BlockStateFunction.parseJson(BuiltInRegistries.BLOCK.get(regKey), value, JsonElement::getAsBoolean, () -> false));
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public static void onChunkLoad(LevelChunk chunk) {
        onChunkUnload(chunk);

        for(int i = chunk.getMinSection(); i < chunk.getMaxSection(); i++) {
            LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(i));

            if (section.maybeHas(state -> DynamicLightInfo.MAP.keySet()
                    .stream()
                    .anyMatch(p -> p.test(state)))) {
                BlockPos minPos = SectionPos.of(chunk.getPos(), i).origin();

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
    }

    public static void onChunkUnload(LevelChunk chunk) {
        BLOCK_LIGHTS.values().removeIf(light -> {
            boolean b = new ChunkPos(light.blockPos).equals(chunk.getPos());

            if (b) {
                light.free();
            }

            return b;
        });
    }

    public static void render() {
        NUM_LIGHT_TASKS = 0;
        ResourceLocation id = id("ray_light");

        VeilRenderSystem.renderer().enableBuffers(id, DynamicBufferType.NORMAL, DynamicBufferType.ALBEDO);

        VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(id).bind(true);
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
        SHADOW_COUNT = 0;

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

    public static void afterClientLevelChange(ClientLevel world) {
        BLOCK_LIGHTS.values().forEach(RaytracedPointBlockLight::free);
        BLOCK_LIGHTS.clear();
        ENTITY_LIGHTS.values().forEach(RaytracedPointEntityLight::free);
        ENTITY_LIGHTS.clear();

        for (AbstractClientPlayer player : world.players()) {
            ENTITY_LIGHTS.put(player, new RaytracedPointEntityLight(player));
        }
    }

    public static void init() {
    }
}
