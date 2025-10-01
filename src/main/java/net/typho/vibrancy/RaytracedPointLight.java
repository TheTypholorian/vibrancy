package net.typho.vibrancy;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public class RaytracedPointLight extends PointLight implements RaytracedLight {
    public static final Set<BlockPos> DIRTY = new HashSet<>();
    public static final VertexBuffer SCREEN_VBO = new VertexBuffer(VertexBuffer.Usage.STATIC);
    protected final List<BlockPos> dirty = new LinkedList<>();
    protected final VertexBuffer geomVBO = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
    protected final int quadsSSBO = glGenBuffers();
    protected boolean remove = false, anyShadows = false;
    protected float flicker = 0, flickerMin, flickerMax, flickerStart = (float) GLFW.glfwGetTime();
    protected List<ShadowVolume> volumes = new LinkedList<>();
    protected CompletableFuture<List<ShadowVolume>> fullRebuildTask;

    static {
        BufferBuilder bufferBuilder = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION);
        bufferBuilder.vertex(-1, 1, 0);
        bufferBuilder.vertex(-1, -1, 0);
        bufferBuilder.vertex(1, 1, 0);
        bufferBuilder.vertex(1, -1, 0);

        SCREEN_VBO.bind();
        SCREEN_VBO.upload(bufferBuilder.end());
        VertexBuffer.unbind();
    }

    public float getFlicker() {
        return flicker;
    }

    public RaytracedPointLight setFlicker(float flicker) {
        this.flicker = flicker;
        markDirty();
        return this;
    }

    @Override
    public double lazyDistance(Vec3d vec) {
        return getPosition().distanceSquared(vec.x, vec.y, vec.z);
    }

    public void getQuads(ClientWorld world, BlockPos pos, Consumer<ShadowVolume> out, MatrixStack stack, double sqDist, BlockPos lightBlockPos, Vector3f lightPos) {
        BlockState state = world.getBlockState(pos);

        if (Vibrancy.TRANSPARENCY_TEST.getValue() && state.isTransparent(world, pos)) {
            return;
        }

        stack.push();
        stack.translate(pos.getX(), pos.getY(), pos.getZ());

        List<Vector3f> flatVertices = new LinkedList<>(), normals = new LinkedList<>();
        List<Vector2f> flatTexCoords = new LinkedList<>();

        RenderLayer layer = RenderLayers.getBlockLayer(state);

        MinecraftClient.getInstance().getBlockRenderManager().renderBlock(
                state,
                pos,
                world,
                stack,
                new VertexConsumer() {
                    @Override
                    public VertexConsumer vertex(float x, float y, float z) {
                        flatVertices.add(new Vector3f(x, y, z));
                        return this;
                    }

                    @Override
                    public VertexConsumer color(int red, int green, int blue, int alpha) {
                        return this;
                    }

                    @Override
                    public VertexConsumer texture(float u, float v) {
                        flatTexCoords.add(new Vector2f(u, v));
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
                        normals.add(new Vector3f(x, y, z));
                        return this;
                    }
                },
                sqDist != 1,
                Random.create(lightBlockPos.hashCode())
        );

        if (flatVertices.size() % 4 != 0) {
            System.err.println("[Vibrancy] Block " + state + " doesn't use quads for rendering, skipping it for raytracing");
        } else {
            for (int j = 0; j < flatVertices.size(); j += 4) {
                for (int k = j; k < j + 4; k++) {
                    if (normals.get(k).dot(lightPos.sub(flatVertices.get(k), new Vector3f())) > 0 || flatVertices.get(k).distanceSquared(lightPos) < 4) {

                        Vector3f[] vertices = new Vector3f[]{
                                flatVertices.get(j),
                                flatVertices.get(j + 1),
                                flatVertices.get(j + 2),
                                flatVertices.get(j + 3),
                                null,
                                null,
                                null,
                                null
                        };

                        for (int i = 0; i < 4; i++) {
                            Vector3f vertex = new Vector3f(vertices[i]);
                            Vector3f off = vertex.sub(lightPos, new Vector3f());
                            vertices[i + 4] = vertex.add(off.normalize(radius * 2));
                        }

                        out.accept(new ShadowVolume(
                                new Quad(pos, vertices[0], vertices[1], vertices[2], vertices[3], flatTexCoords.get(j), flatTexCoords.get(j + 1), flatTexCoords.get(j + 2), flatTexCoords.get(j + 3), layer.isTranslucent() || layer != RenderLayer.getSolid()),
                                vertices
                        ));

                        break;
                    }
                }
            }
        }

        stack.pop();
    }

    public void upload(BufferBuilder builder, Collection<ShadowVolume> volumes) {
        if (volumes.isEmpty()) {
            anyShadows = false;
        } else {
            anyShadows = true;
            geomVBO.bind();
            geomVBO.upload(builder.end());
            VertexBuffer.unbind();

            ByteBuffer buf = MemoryUtil.memAlloc(volumes.size() * Quad.BYTES);

            for (ShadowVolume v : volumes) {
                v.caster().put(buf);
            }

            glBindBuffer(GL_SHADER_STORAGE_BUFFER, quadsSSBO);
            glBufferData(GL_SHADER_STORAGE_BUFFER, buf.flip(), GL_DYNAMIC_DRAW);
            glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

            MemoryUtil.memFree(buf);
        }
    }

    public void regenQuads(ClientWorld world, BlockPos pos, Consumer<ShadowVolume> out, MatrixStack stack, BlockPos lightBlockPos, Vector3f lightPos) {
        volumes.removeIf(v -> v.caster().blockPos().equals(pos));
        getQuads(world, pos, out, stack, pos.getSquaredDistance(lightBlockPos), lightBlockPos, lightPos);
    }

    @Override
    public void render(boolean raytrace) {
        BlockPos lightBlockPos = new BlockPos((int) Math.floor(getPosition().x), (int) Math.floor(getPosition().y), (int) Math.floor(getPosition().z));
        Vector3f lightPos = new Vector3f((float) getPosition().x, (float) getPosition().y, (float) getPosition().z);
        int blockRadius = Vibrancy.capShadowDistance((int) Math.ceil(radius) - 2);
        BlockBox box = new BlockBox(lightBlockPos).expand(blockRadius);

        for (BlockPos pos : DIRTY) {
            if (box.contains(pos)) {
                dirty.add(pos);
            }
        }

        if (fullRebuildTask != null) {
            Vibrancy.NUM_LIGHT_TASKS++;
        }

        if (fullRebuildTask != null && fullRebuildTask.isDone()) {
            try {
                volumes = fullRebuildTask.get();
                BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

                for (ShadowVolume volume : volumes) {
                    volume.render(builder);
                }

                upload(builder, volumes);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        } else if (raytrace) {
            ClientWorld world = MinecraftClient.getInstance().world;

            if (world != null) {
                if (blockRadius < 1) {
                    raytrace = false;
                } else {
                    if (isDirty()) {
                        clean();
                        fullRebuildTask = CompletableFuture.supplyAsync(() -> {
                            MatrixStack stack = new MatrixStack();
                            List<ShadowVolume> volumes = new LinkedList<>();

                            for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
                                for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
                                    for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                                        BlockPos pos = new BlockPos(x, y, z);
                                        double sqDist = pos.getSquaredDistance(lightBlockPos);

                                        if (sqDist != 0 && sqDist < blockRadius * blockRadius) {
                                            getQuads(world, pos, volumes::add, stack, sqDist, lightBlockPos, lightPos);
                                        }
                                    }
                                }
                            }

                            return volumes;
                        });
                    } else if (!dirty.isEmpty()) {
                        MatrixStack stack = new MatrixStack();
                        Consumer<ShadowVolume> out = volumes::add;

                        for (BlockPos pos : dirty) {
                            regenQuads(world, pos, out, stack, lightBlockPos, lightPos);

                            for (Direction dir : Direction.values()) {
                                regenQuads(world, pos.offset(dir), out, stack, lightBlockPos, lightPos);
                            }
                        }

                        BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

                        for (ShadowVolume volume : volumes) {
                            volume.render(builder);
                        }

                        upload(builder, volumes);
                    }
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
                VeilRenderSystem.setShader(Identifier.of(Vibrancy.MOD_ID, "light/ray/mask"));
                shader = Objects.requireNonNull(RenderSystem.getShader());

                shader.getUniformOrDefault("LightPos").set((float) position.x, (float) position.y, (float) position.z);
                shader.getUniformOrDefault("Detailed").set(position.distanceSquared(camera.getPos().x, camera.getPos().y, camera.getPos().z) < MathHelper.square(Vibrancy.RAYTRACE_DISTANCE.getValue() * 16) ? 1 : 0);

                RenderSystem.depthMask(true);
                RenderSystem.disableDepthTest();
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
                RenderSystem.blendEquation(GL_FUNC_ADD);

                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, quadsSSBO);

                glCullFace(GL_FRONT);
                glDepthFunc(GL_GEQUAL);
                renderMask(Identifier.of(Vibrancy.MOD_ID, "shadow_mask"), view);

                glCullFace(GL_BACK);
                glDepthFunc(GL_LEQUAL);
                RenderSystem.depthMask(false);
                RenderSystem.enableBlend();
                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);
            }

            Objects.requireNonNull(VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(VeilFramebuffers.LIGHT)).bind(true);
            VeilRenderSystem.setShader(Identifier.of(Vibrancy.MOD_ID, "light/ray/point"));
            shader = Objects.requireNonNull(RenderSystem.getShader());

            float time = (float) GLFW.glfwGetTime();

            while (time - flickerStart > 0.25) {
                flickerStart += 0.25f;
                flickerMin = flickerMax;
                flickerMax = new java.util.Random().nextFloat(-1, 1);
            }

            float brightness = getBrightness() * (1 + flicker * MathHelper.lerp((time - flickerStart) * 4, flickerMin, flickerMax));

            shader.getUniformOrDefault("LightPos").set((float) position.x, (float) position.y, (float) position.z);
            shader.getUniformOrDefault("LightColor").set(color.x * brightness, color.y * brightness, color.z * brightness);
            shader.getUniformOrDefault("LightRadius").set(radius);
            shader.getUniformOrDefault("AnyShadows").set(anyShadows ? 1 : 0);

            SCREEN_VBO.bind();
            SCREEN_VBO.draw(view, RenderSystem.getProjectionMatrix(), shader);
            VertexBuffer.unbind();
        }
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
