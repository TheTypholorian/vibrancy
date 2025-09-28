package net.typho.vibrancy;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.light.PointLight;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public class RaytracedPointLight extends PointLight implements RaytracedLight {
    public static final VertexBuffer SCREEN_VBO = new VertexBuffer(VertexBuffer.Usage.STATIC);
    protected final VertexBuffer geomVBO = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
    protected final int quadsSSBO = glGenBuffers();
    protected boolean remove = false, anyShadows = false;
    protected float flicker = 0, flickerMin, flickerMax, flickerStart = (float) GLFW.glfwGetTime();

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

    @Override
    public void render(boolean raytrace) {
        BlockPos lightBlockPos = new BlockPos((int) Math.floor(getPosition().x), (int) Math.floor(getPosition().y), (int) Math.floor(getPosition().z));

        if (isDirty() && raytrace) {
            clean();

            ClientWorld world = MinecraftClient.getInstance().world;

            if (world != null) {
                int numQuads = 0;
                Vector3f lightPos = new Vector3f((float) getPosition().x, (float) getPosition().y, (float) getPosition().z);
                int blockRadius = Vibrancy.capShadowDistance((int) Math.ceil(radius) - 4);

                if (blockRadius < 1) {
                    raytrace = false;
                } else {
                    BlockBox box = new BlockBox(lightBlockPos).expand(blockRadius);
                    MatrixStack stack = new MatrixStack();
                    BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
                    List<Quad> quads = new LinkedList<>();
                    PrintWriter out;

                    try {
                        out = FabricLoader.getInstance().isDevelopmentEnvironment() && new File("mesh.obj").exists() ? null : new PrintWriter("mesh.obj");
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }

                    for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
                        for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
                            for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                                BlockPos pos = new BlockPos(x, y, z);
                                double sqDist = pos.getSquaredDistance(lightBlockPos);

                                if (sqDist != 0 && sqDist < blockRadius * blockRadius) {
                                    BlockState state = world.getBlockState(pos);

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
                                                        vertices[i + 4] = vertex.add(off.normalize((radius) - off.length() + 1));
                                                    }

                                                    quads.add(new Quad(vertices[0], vertices[1], vertices[2], vertices[3], flatTexCoords.get(j), flatTexCoords.get(j + 1), flatTexCoords.get(j + 2), flatTexCoords.get(j + 3), layer.isTranslucent() || layer != RenderLayer.getSolid()));

                                                    builder.vertex(vertices[0])
                                                            .vertex(vertices[1])
                                                            .vertex(vertices[2])
                                                            .vertex(vertices[3]);

                                                    builder.vertex(vertices[1])
                                                            .vertex(vertices[5])
                                                            .vertex(vertices[6])
                                                            .vertex(vertices[2]);

                                                    builder.vertex(vertices[5])
                                                            .vertex(vertices[4])
                                                            .vertex(vertices[7])
                                                            .vertex(vertices[6]);

                                                    builder.vertex(vertices[4])
                                                            .vertex(vertices[0])
                                                            .vertex(vertices[3])
                                                            .vertex(vertices[7]);

                                                    builder.vertex(vertices[1])
                                                            .vertex(vertices[0])
                                                            .vertex(vertices[4])
                                                            .vertex(vertices[5]);

                                                    builder.vertex(vertices[3])
                                                            .vertex(vertices[2])
                                                            .vertex(vertices[6])
                                                            .vertex(vertices[7]);

                                                    if (out != null) {
                                                        int i = numQuads * 4 + 1;

                                                        out.println("v " + vertices[0].x + " " + vertices[0].y + " " + vertices[0].z);
                                                        out.println("v " + vertices[1].x + " " + vertices[1].y + " " + vertices[1].z);
                                                        out.println("v " + vertices[2].x + " " + vertices[2].y + " " + vertices[2].z);
                                                        out.println("v " + vertices[3].x + " " + vertices[3].y + " " + vertices[3].z);
                                                        out.println("v " + vertices[1].x + " " + vertices[1].y + " " + vertices[1].z);
                                                        out.println("v " + vertices[5].x + " " + vertices[5].y + " " + vertices[5].z);
                                                        out.println("v " + vertices[6].x + " " + vertices[6].y + " " + vertices[6].z);
                                                        out.println("v " + vertices[2].x + " " + vertices[2].y + " " + vertices[2].z);
                                                        out.println("v " + vertices[5].x + " " + vertices[5].y + " " + vertices[5].z);
                                                        out.println("v " + vertices[4].x + " " + vertices[4].y + " " + vertices[4].z);
                                                        out.println("v " + vertices[7].x + " " + vertices[7].y + " " + vertices[7].z);
                                                        out.println("v " + vertices[6].x + " " + vertices[6].y + " " + vertices[6].z);
                                                        out.println("v " + vertices[4].x + " " + vertices[4].y + " " + vertices[4].z);
                                                        out.println("v " + vertices[0].x + " " + vertices[0].y + " " + vertices[0].z);
                                                        out.println("v " + vertices[3].x + " " + vertices[3].y + " " + vertices[3].z);
                                                        out.println("v " + vertices[7].x + " " + vertices[7].y + " " + vertices[7].z);
                                                        out.println("v " + vertices[1].x + " " + vertices[1].y + " " + vertices[1].z);
                                                        out.println("v " + vertices[0].x + " " + vertices[0].y + " " + vertices[0].z);
                                                        out.println("v " + vertices[4].x + " " + vertices[4].y + " " + vertices[4].z);
                                                        out.println("v " + vertices[5].x + " " + vertices[5].y + " " + vertices[5].z);
                                                        out.println("v " + vertices[3].x + " " + vertices[3].y + " " + vertices[3].z);
                                                        out.println("v " + vertices[2].x + " " + vertices[2].y + " " + vertices[2].z);
                                                        out.println("v " + vertices[6].x + " " + vertices[6].y + " " + vertices[6].z);
                                                        out.println("v " + vertices[7].x + " " + vertices[7].y + " " + vertices[7].z);

                                                        out.println("f " + i++ + " " + i++ + " " + i++ + " " + i++);
                                                        out.println("f " + i++ + " " + i++ + " " + i++ + " " + i++);
                                                        out.println("f " + i++ + " " + i++ + " " + i++ + " " + i++);
                                                        out.println("f " + i++ + " " + i++ + " " + i++ + " " + i++);
                                                        out.println("f " + i++ + " " + i++ + " " + i++ + " " + i++);
                                                        out.println("f " + i++ + " " + i++ + " " + i++ + " " + i++);
                                                    }

                                                    numQuads += 6;

                                                    break;
                                                }
                                            }
                                        }
                                    }

                                    stack.pop();
                                }
                            }
                        }
                    }

                    if (numQuads == 0) {
                        anyShadows = false;
                    } else {
                        anyShadows = true;
                        geomVBO.bind();
                        geomVBO.upload(builder.end());
                        VertexBuffer.unbind();

                        ByteBuffer buf = MemoryUtil.memAlloc(quads.size() * Quad.BYTES);

                        for (Quad quad : quads) {
                            quad.put(buf);
                        }

                        glBindBuffer(GL_SHADER_STORAGE_BUFFER, quadsSSBO);
                        glBufferData(GL_SHADER_STORAGE_BUFFER, buf.flip(), GL_DYNAMIC_DRAW);
                        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

                        MemoryUtil.memFree(buf);
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
                RenderSystem.enableDepthTest();
                RenderSystem.disableBlend();

                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, quadsSSBO);

                glCullFace(GL_FRONT);
                glDepthFunc(GL_GEQUAL);
                renderMask(Identifier.of(Vibrancy.MOD_ID, "shadow_mask_back"), view, 0);

                glCullFace(GL_BACK);
                glDepthFunc(GL_LEQUAL);
                renderMask(Identifier.of(Vibrancy.MOD_ID, "shadow_mask_front"), view, 1);

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

    protected void renderMask(Identifier fbo, Matrix4f view, double depthClear) {
        Objects.requireNonNull(VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(fbo)).bind(true);
        glClearColor(0f, 0f, 0f, 0f);
        glClearDepth(depthClear);
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
