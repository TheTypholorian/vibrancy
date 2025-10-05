package net.typho.vibrancy;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import org.joml.*;
import org.lwjgl.glfw.GLFW;

import java.lang.Math;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public class RaytracedPointEntityLight extends PointLight implements RaytracedLight {
    public final LivingEntity entity;
    protected final VertexBuffer geomVBO = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
    protected final int quadsSSBO = glGenBuffers();
    protected boolean anyShadows = false, hasLight = false;
    protected float flicker = 0, flickerMin, flickerMax, flickerStart = (float) GLFW.glfwGetTime();
    protected List<Quad> quads = new LinkedList<>();
    protected final List<BlockPos> dirty = new LinkedList<>();
    protected BlockBox quadBox;
    protected CompletableFuture<List<Quad>> fullRebuildTask;

    public RaytracedPointEntityLight(LivingEntity entity) {
        this.entity = entity;
    }

    public BlockBox getBox() {
        BlockPos lightBlockPos = new BlockPos((int) Math.floor(getPosition().x), (int) Math.floor(getPosition().y), (int) Math.floor(getPosition().z));
        int blockRadius = Vibrancy.capShadowDistance((int) Math.ceil(radius) - 2);
        BlockBox box = new BlockBox(lightBlockPos);

        if (blockRadius > 1) {
            box = box.expand(blockRadius);
        }

        return box;
    }

    @Override
    public void updateDirty(Iterable<BlockPos> it) {
        BlockBox box = getBox();

        for (BlockPos pos : it) {
            if (box.contains(pos)) {
                dirty.add(pos);
            }
        }
    }

    public void upload(BufferBuilder builder, Collection<ShadowVolume> volumes) {
        if (volumes.isEmpty()) {
            anyShadows = false;
        } else {
            anyShadows = true;
            upload(builder, volumes, geomVBO, quadsSSBO, GL_STREAM_DRAW);
        }
    }

    public void regenQuadsSync(ClientWorld world, BlockPos pos, Consumer<Quad> out, BlockPos lightBlockPos, Vector3f lightPos) {
        quads.removeIf(q -> q.blockPos().equals(pos));
        regenQuadsAsync(world, pos, out, lightBlockPos, lightPos);
    }

    public void regenQuadsAsync(ClientWorld world, BlockPos pos, Consumer<Quad> out, BlockPos lightBlockPos, Vector3f lightPos) {
        getQuads(world, pos, out, pos.getSquaredDistance(lightBlockPos), lightBlockPos, lightPos, false);
    }

    public void regenAll(ClientWorld world, BlockBox box, BlockPos lightBlockPos, Vector3f lightPos) {
        fullRebuildTask = CompletableFuture.supplyAsync(() -> {
            List<Quad> quads = new LinkedList<>();

            for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
                for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
                    for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                        regenQuadsAsync(world, new BlockPos(x, y, z), quads::add, lightBlockPos, lightPos);
                    }
                }
            }

            return quads;
        });
    }

    @Override
    public boolean render(boolean raytrace) {
        hasLight = false;

        if (entity.getMainHandStack().getItem() instanceof BlockItem block) {
            BlockState state = block.getBlock().getDefaultState();
            DynamicLightInfo info = DynamicLightInfo.get(state);

            if (info == null) {
                return false;
            } else {
                info.initLight(this, state);
                hasLight = true;
            }
        } else {
            return false;
        }

        ClientWorld world = MinecraftClient.getInstance().world;

        if (world != null) {
            BlockPos lightBlockPos = new BlockPos((int) Math.floor(getPosition().x), (int) Math.floor(getPosition().y), (int) Math.floor(getPosition().z));
            Vector3f lightPos = new Vector3f((float) getPosition().x, (float) getPosition().y, (float) getPosition().z);
            int blockRadius = Vibrancy.capShadowDistance((int) Math.ceil(radius) - 2);
            BlockBox box = getBox();

            List<ShadowVolume> volumes = new LinkedList<>();
            BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

            if (isVisible()) {
                if (fullRebuildTask != null && fullRebuildTask.isDone()) {
                    try {
                        quads = fullRebuildTask.get();
                        fullRebuildTask = null;
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                } else if (hasLight) {
                    if (quadBox == null || !quadBox.contains(entity.getBlockPos())) {
                        dirty.clear();
                        quadBox = box;
                        regenAll(world, blockRadius > 1 ? box.expand(blockRadius) : box, lightBlockPos, lightPos);
                    } else if (!dirty.isEmpty()) {
                        for (BlockPos pos : dirty) {
                            regenQuadsSync(world, pos, quads::add, lightBlockPos, lightPos);

                            for (Direction dir : Direction.values()) {
                                regenQuadsSync(world, pos.offset(dir), quads::add, lightBlockPos, lightPos);
                            }
                        }
                    }
                }

                for (Quad quad : quads) {
                    if (box.contains(quad.blockPos())) {
                        ShadowVolume volume = quad.toVolume(lightPos, radius * 2);
                        volume.render(builder);
                        volumes.add(volume);
                    }
                }

                upload(builder, volumes);

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

    @Override
    public final PointLight setPosition(Vector3dc position) {
        throw new UnsupportedOperationException("Can't move an entity light");
    }

    @Override
    public final PointLight setPosition(double x, double y, double z) {
        throw new UnsupportedOperationException("Can't move an entity light");
    }

    @Override
    public Vector3d getPosition() {
        Vec3d pos = entity.getEyePos();
        return new Vector3d(pos.x, pos.y, pos.z);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + entity + "]";
    }
}
