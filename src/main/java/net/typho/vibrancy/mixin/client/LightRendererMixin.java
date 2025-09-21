package net.typho.vibrancy.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import foundry.veil.api.client.registry.LightTypeRegistry;
import foundry.veil.api.client.render.light.Light;
import foundry.veil.api.client.render.light.PointLight;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.typho.vibrancy.client.RaytracedLight;
import net.typho.vibrancy.client.VibrancyClient;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

@Mixin(value = LightRenderer.class, remap = false)
public abstract class LightRendererMixin {
    @Shadow
    public abstract <T extends Light> List<T> getLights(LightTypeRegistry.LightType<? extends T> type);

    @Unique
    private int quadSSBO, rangesSSBO;

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void init(CallbackInfo ci) {
        quadSSBO = glGenBuffers();
        rangesSSBO = glGenBuffers();
    }

    @Inject(
            method = "applyShader",
            at = @At("TAIL")
    )
    private void applyShader(CallbackInfoReturnable<Boolean> cir, @Local ShaderProgram shader) {
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, quadSSBO);

        List<PointLight> lights = getLights(LightTypeRegistry.POINT.get());

        if (!lights.isEmpty()) {
            List<RaytracedLight.Quad> quads = new LinkedList<>();
            int[] ranges = new int[lights.size() * 2];
            int i = 0;

            ClientWorld world = MinecraftClient.getInstance().world;

            if (world != null) {
                for (PointLight light : lights) {
                    ranges[i++] = quads.size();
                    BlockBox box = new BlockBox(new BlockPos((int) Math.floor(light.getPosition().x), (int) Math.floor(light.getPosition().y), (int) Math.floor(light.getPosition().z))).expand(5);//(int) Math.ceil(light.getRadius()) + 1);
                    MatrixStack stack = new MatrixStack();
                    Random random = Random.create();

                    for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
                        for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
                            for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                                BlockPos pos = new BlockPos(x, y, z);
                                BlockState state = world.getBlockState(pos);

                                stack.push();
                                stack.translate(pos.getX(), pos.getY(), pos.getZ());

                                List<Vector3f> vertices = new LinkedList<>();

                                MinecraftClient.getInstance().getBlockRenderManager().renderBlock(
                                        state,
                                        pos,
                                        world,
                                        stack,
                                        new VertexConsumer() {
                                            @Override
                                            public VertexConsumer vertex(float x, float y, float z) {
                                                vertices.add(new Vector3f(x, y, z));
                                                return this;
                                            }

                                            @Override
                                            public VertexConsumer color(int red, int green, int blue, int alpha) {
                                                return this;
                                            }

                                            @Override
                                            public VertexConsumer texture(float u, float v) {
                                                return this;
                                            }

                                            @Override
                                            public VertexConsumer overlay(int u, int v) {
                                                return this;
                                            }

                                            @Override
                                            public VertexConsumer light(int u, int v) {
                                                return this;
                                            }

                                            @Override
                                            public VertexConsumer normal(float x, float y, float z) {
                                                return this;
                                            }
                                        },
                                        false,
                                        random
                                );

                                if (vertices.size() % 4 != 0) {
                                    System.err.println("[Vibrancy] Block " + state + " doesn't use quads for rendering, skipping it for raytracing");
                                } else {
                                    for (int j = 0; j < vertices.size(); j += 4) {
                                        quads.add(new RaytracedLight.Quad(
                                                vertices.get(j),
                                                vertices.get(j + 1),
                                                vertices.get(j + 2),
                                                vertices.get(j + 3)
                                        ));
                                    }
                                }

                                stack.pop();
                            }
                        }
                    }

                    ranges[i++] = quads.size();
                }
            }

            if (VibrancyClient.SAVE_LIGHTMAP.isPressed()) {
                System.out.println("uploading " + quads.size() + " quads");

                for (int j = 0; j < 50; j++) {
                    System.err.println(quads.get(j));
                }
            }

            ByteBuffer buf = MemoryUtil.memAlloc(quads.size() * 16 * Float.BYTES);

            for (RaytracedLight.Quad quad : quads) {
                quad.put(buf);
            }

            buf.flip();
            glBufferData(GL_SHADER_STORAGE_BUFFER, buf, GL_DYNAMIC_DRAW);

            MemoryUtil.memFree(buf);

            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, quadSSBO);

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, rangesSSBO);
            glBufferData(GL_SHADER_STORAGE_BUFFER, ranges, GL_DYNAMIC_DRAW);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 1, rangesSSBO);

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);
        }
    }
}
