package net.typho.vibrancy;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public class RaytracedPointLight extends PointLight implements RaytracedLight {
    protected final List<BlockPos> dirty = new LinkedList<>();
    protected final VertexBuffer geomVBO = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
    protected final int quadsSSBO = glGenBuffers();
    protected boolean remove = false, anyShadows = false;
    protected float flicker = 0, flickerMin, flickerMax, flickerStart = (float) GLFW.glfwGetTime();
    protected List<ShadowVolume> volumes = new LinkedList<>();
    protected CompletableFuture<List<ShadowVolume>> fullRebuildTask;

    public float getFlicker() {
        return flicker;
    }

    public RaytracedPointLight setFlicker(float flicker) {
        this.flicker = flicker;
        markDirty();
        return this;
    }

    public void upload(BufferBuilder builder, Collection<ShadowVolume> volumes) {
        if (volumes.isEmpty()) {
            anyShadows = false;
        } else {
            anyShadows = true;
            upload(builder, volumes, geomVBO, quadsSSBO, GL_DYNAMIC_DRAW);
        }
    }

    public void regenQuads(ClientWorld world, BlockPos pos, Consumer<ShadowVolume> out, BlockPos lightBlockPos, Vector3f lightPos) {
        volumes.removeIf(v -> v.caster().blockPos().equals(pos));
        getVolumes(world, pos, out, pos.getSquaredDistance(lightBlockPos), lightBlockPos, lightPos, radius, true);
    }

    @Override
    public void updateDirty(Iterable<BlockPos> it) {
        BlockPos lightBlockPos = new BlockPos((int) Math.floor(getPosition().x), (int) Math.floor(getPosition().y), (int) Math.floor(getPosition().z));
        int blockRadius = Vibrancy.capShadowDistance((int) Math.ceil(radius) - 2);
        BlockBox box = new BlockBox(lightBlockPos).expand(blockRadius);

        for (BlockPos pos : it) {
            if (box.contains(pos)) {
                dirty.add(pos);
            }
        }
    }

    @Override
    public boolean render(boolean raytrace) {
        ClientWorld world = MinecraftClient.getInstance().world;

        if (world != null) {
            BlockPos lightBlockPos = new BlockPos((int) Math.floor(getPosition().x), (int) Math.floor(getPosition().y), (int) Math.floor(getPosition().z));
            Vector3f lightPos = new Vector3f((float) getPosition().x, (float) getPosition().y, (float) getPosition().z);
            int blockRadius = Vibrancy.capShadowDistance((int) Math.ceil(radius) - 2);
            BlockBox box = new BlockBox(lightBlockPos).expand(blockRadius);

            if (fullRebuildTask != null) {
                Vibrancy.NUM_LIGHT_TASKS++;
            }

            if (!dirty.isEmpty()) {
                for (BlockPos pos : dirty) {
                    if (!pos.equals(lightBlockPos)) {
                        regenQuads(world, pos, volumes::add, lightBlockPos, lightPos);
                    }

                    for (Direction dir : Direction.values()) {
                        BlockPos pos1 = pos.offset(dir);

                        if (!pos1.equals(lightBlockPos)) {
                            regenQuads(world, pos1, volumes::add, lightBlockPos, lightPos);
                        }
                    }
                }

                dirty.clear();

                BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

                for (ShadowVolume volume : volumes) {
                    volume.render(builder);
                }

                upload(builder, volumes);
            }

            if (fullRebuildTask != null && fullRebuildTask.isDone()) {
                try {
                    volumes = fullRebuildTask.get();
                    fullRebuildTask = null;
                    BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

                    for (ShadowVolume volume : volumes) {
                        volume.render(builder);
                    }

                    upload(builder, volumes);
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            } else if (raytrace) {
                if (blockRadius < 1) {
                    raytrace = false;
                } else {
                    if (isDirty()) {
                        clean();
                        fullRebuildTask = CompletableFuture.supplyAsync(() -> {
                            List<ShadowVolume> volumes = new LinkedList<>();

                            for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
                                for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
                                    for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                                        BlockPos pos = new BlockPos(x, y, z);

                                        if (!pos.equals(lightBlockPos)) {
                                            double sqDist = pos.getSquaredDistance(lightBlockPos);

                                            if (sqDist != 0 && sqDist < blockRadius * blockRadius) {
                                                getVolumes(world, pos, volumes::add, sqDist, lightBlockPos, lightPos, radius, true);
                                            }
                                        }
                                    }
                                }
                            }

                            return volumes;
                        });
                    }
                }
            }

            if (isVisible()) {
                Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
                Matrix4f view = new Matrix4f()
                        .rotate(camera.getRotation().invert(new Quaternionf()))
                        .translate((float) -camera.getPos().x, (float) -camera.getPos().y, (float) -camera.getPos().z);
                ShaderProgram shader;

                if (anyShadows && raytrace) {
                    VeilRenderSystem.setShader(Vibrancy.id("light/ray/mask"));
                    shader = Objects.requireNonNull(RenderSystem.getShader());

                    shader.getUniformOrDefault("LightPos").set(lightPos.x, lightPos.y, lightPos.z);
                    shader.getUniformOrDefault("Detailed").set(getPosition().distanceSquared(camera.getPos().x, camera.getPos().y, camera.getPos().z) < MathHelper.square(Vibrancy.RAYTRACE_DISTANCE.getValue() * 16) ? 1 : 0);

                    RenderSystem.depthMask(true);
                    RenderSystem.disableDepthTest();
                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
                    RenderSystem.blendEquation(GL_FUNC_ADD);

                    glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, quadsSSBO);

                    glCullFace(GL_FRONT);
                    glDepthFunc(GL_GEQUAL);
                    renderMask(Vibrancy.id("shadow_mask"), view);

                    glCullFace(GL_BACK);
                    glDepthFunc(GL_LEQUAL);
                    RenderSystem.depthMask(false);
                    RenderSystem.enableBlend();
                    glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);
                } else {
                    Objects.requireNonNull(VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(Vibrancy.id("shadow_mask"))).bind(false);
                    glClearColor(0f, 0f, 0f, 0f);
                    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
                }

                Objects.requireNonNull(VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(Vibrancy.id("ray_light"))).bind(true);
                VeilRenderSystem.setShader(Vibrancy.id("light/ray/point"));
                shader = Objects.requireNonNull(RenderSystem.getShader());

                float time = (float) GLFW.glfwGetTime();

                while (time - flickerStart > 0.25) {
                    flickerStart += 0.25f;
                    flickerMin = flickerMax;
                    flickerMax = new java.util.Random().nextFloat(-1, 1);
                }

                float brightness = getBrightness() * (1 + flicker * MathHelper.lerp((time - flickerStart) * 4, flickerMin, flickerMax));

                shader.getUniformOrDefault("LightPos").set(lightPos.x, lightPos.y, lightPos.z);
                shader.getUniformOrDefault("LightColor").set(color.x * brightness, color.y * brightness, color.z * brightness);
                shader.getUniformOrDefault("LightRadius").set(radius);
                shader.getUniformOrDefault("AnyShadows").set(anyShadows ? 1 : 0);

                Vibrancy.SCREEN_VBO.bind();
                Vibrancy.SCREEN_VBO.draw(view, RenderSystem.getProjectionMatrix(), shader);
                VertexBuffer.unbind();

                return true;
            }
        }

        return false;
    }

    protected void renderMask(Identifier fbo, Matrix4f view) {
        AdvancedFbo main = Objects.requireNonNull(VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(Identifier.of("main")));
        AdvancedFbo to = Objects.requireNonNull(VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(fbo));
        glBindFramebuffer(GL_READ_FRAMEBUFFER, main.getId());
        glBindFramebuffer(GL_DRAW_FRAMEBUFFER, to.getId());

        glBlitFramebuffer(
                0, 0, main.getWidth(), main.getHeight(),
                0, 0, to.getWidth(), to.getHeight(),
                GL_DEPTH_BUFFER_BIT,
                GL_NEAREST
        );

        to.bind(true);
        glClearColor(0f, 0f, 0f, 0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        geomVBO.bind();
        geomVBO.draw(view, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind();
    }

    @Override
    public void free() {
        geomVBO.close();
        glDeleteBuffers(quadsSSBO);
    }
}
