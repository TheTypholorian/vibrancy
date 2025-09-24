package net.typho.vibrancy.client;

import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.registry.LightTypeRegistry;
import foundry.veil.api.client.render.CullFrustum;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.VeilFramebuffers;
import foundry.veil.api.client.render.light.PointLight;
import foundry.veil.api.client.render.light.renderer.LightRenderer;
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
import net.minecraft.util.math.Box;
import net.minecraft.util.math.random.Random;
import net.typho.vibrancy.Vibrancy;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.*;

public class RaytracedPointLight extends PointLight implements RaytracedLight {
    public static final VertexBuffer SCREEN_VBO = new VertexBuffer(VertexBuffer.Usage.STATIC);
    protected final VertexBuffer geomVBO = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
    protected boolean render = true;

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

    @Override
    public LightTypeRegistry.LightType<?> getType() {
        return VibrancyClient.RAY_POINT_LIGHT.get();
    }

    @Override
    public void prepare(LightRenderer renderer, CullFrustum frustum) {
        BlockPos lightBlockPos = new BlockPos((int) Math.floor(getPosition().x), (int) Math.floor(getPosition().y), (int) Math.floor(getPosition().z));

        render = frustum.testAab(new Box(lightBlockPos).expand(radius + 2));

        if (isDirty() && render) {
            clean();

            ClientWorld world = MinecraftClient.getInstance().world;

            if (world != null) {
                int quads = 0;
                Vector3f lightPos = new Vector3f((float) getPosition().x, (float) getPosition().y, (float) getPosition().z);
                BlockBox box = new BlockBox(lightBlockPos).expand(15);
                MatrixStack stack = new MatrixStack();
                Random random = Random.create();
                BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);

                for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
                    for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
                        for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                            BlockPos pos = new BlockPos(x, y, z);
                            BlockState state = world.getBlockState(pos);

                            stack.push();
                            stack.translate(pos.getX(), pos.getY(), pos.getZ());

                            List<Vector3f> flatVertices = new LinkedList<>(), normals = new LinkedList<>();
                            List<Vector2f> flatTexCoords = new LinkedList<>();

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
                                    true,
                                    random
                            );

                            if (flatVertices.size() % 4 != 0) {
                                System.err.println("[Vibrancy] Block " + state + " doesn't use quads for rendering, skipping it for raytracing");
                            } else {
                                for (int j = 0; j < flatVertices.size(); j += 4) {
                                    for (int k = j; k < j + 4; k++) {
                                        if (normals.get(k).dot(lightPos.sub(flatVertices.get(k), new Vector3f())) > 0) {
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
                                            Vector2f[] texCoords = new Vector2f[]{
                                                    flatTexCoords.get(j),
                                                    flatTexCoords.get(j + 1),
                                                    flatTexCoords.get(j + 2),
                                                    flatTexCoords.get(j + 3)
                                            };

                                            for (int i = 0; i < 4; i++) {
                                                Vector3f vertex = new Vector3f(vertices[i]);
                                                vertices[i + 4] = vertex.add(vertex.sub(lightPos, new Vector3f()).normalize(radius * 2));
                                            }

                                            builder.vertex(vertices[0]).texture(texCoords[0].x, texCoords[0].y)
                                                    .vertex(vertices[1]).texture(texCoords[1].x, texCoords[1].y)
                                                    .vertex(vertices[2]).texture(texCoords[2].x, texCoords[2].y)
                                                    .vertex(vertices[3]).texture(texCoords[3].x, texCoords[3].y);

                                            builder.vertex(vertices[1]).texture(texCoords[1].x, texCoords[1].y)
                                                    .vertex(vertices[5]).texture(texCoords[1].x, texCoords[1].y)
                                                    .vertex(vertices[6]).texture(texCoords[2].x, texCoords[2].y)
                                                    .vertex(vertices[2]).texture(texCoords[2].x, texCoords[2].y);

                                            builder.vertex(vertices[5]).texture(texCoords[1].x, texCoords[1].y)
                                                    .vertex(vertices[4]).texture(texCoords[0].x, texCoords[0].y)
                                                    .vertex(vertices[7]).texture(texCoords[3].x, texCoords[3].y)
                                                    .vertex(vertices[6]).texture(texCoords[2].x, texCoords[2].y);

                                            builder.vertex(vertices[4]).texture(texCoords[0].x, texCoords[0].y)
                                                    .vertex(vertices[0]).texture(texCoords[0].x, texCoords[0].y)
                                                    .vertex(vertices[3]).texture(texCoords[3].x, texCoords[3].y)
                                                    .vertex(vertices[7]).texture(texCoords[3].x, texCoords[3].y);

                                            builder.vertex(vertices[1]).texture(texCoords[1].x, texCoords[1].y)
                                                    .vertex(vertices[0]).texture(texCoords[0].x, texCoords[0].y)
                                                    .vertex(vertices[4]).texture(texCoords[0].x, texCoords[0].y)
                                                    .vertex(vertices[5]).texture(texCoords[1].x, texCoords[1].y);

                                            builder.vertex(vertices[3]).texture(texCoords[3].x, texCoords[3].y)
                                                    .vertex(vertices[2]).texture(texCoords[2].x, texCoords[2].y)
                                                    .vertex(vertices[6]).texture(texCoords[2].x, texCoords[2].y)
                                                    .vertex(vertices[7]).texture(texCoords[3].x, texCoords[3].y);
                                            quads += 6;

                                            break;
                                        }
                                    }
                                }
                            }

                            stack.pop();
                        }
                    }
                }

                geomVBO.bind();
                geomVBO.upload(builder.end());
                VertexBuffer.unbind();

                VibrancyClient.DYNAMIC_LIGHT_INFOS.add(new DynamicLightInfo(lightBlockPos, quads));
            }
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
    public void render(LightRenderer renderer) {
        if (render) {
            Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
            Matrix4f view = new Matrix4f()
                    .rotate(camera.getRotation().invert(new Quaternionf()))
                    .translate((float) -camera.getPos().x, (float) -camera.getPos().y, (float) -camera.getPos().z);

            VeilRenderSystem.setShader(Identifier.of(Vibrancy.MOD_ID, "light/ray/mask"));
            ShaderProgram shader = Objects.requireNonNull(RenderSystem.getShader());

            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();

            shader.getUniformOrDefault("Back").set(1);
            glCullFace(GL_FRONT);
            glDepthFunc(GL_GEQUAL);
            renderMask(Identifier.of(Vibrancy.MOD_ID, "shadow_mask_back"), view, 0);

            shader.getUniformOrDefault("Back").set(0);
            glCullFace(GL_BACK);
            glDepthFunc(GL_LEQUAL);
            renderMask(Identifier.of(Vibrancy.MOD_ID, "shadow_mask_front"), view, 1);

            Objects.requireNonNull(VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(VeilFramebuffers.LIGHT)).bind(true);
            VeilRenderSystem.setShader(Identifier.of(Vibrancy.MOD_ID, "light/ray/point"));
            RenderSystem.enableBlend();

            shader = Objects.requireNonNull(RenderSystem.getShader());

            shader.getUniformOrDefault("LightPos").set((float) position.x, (float) position.y, (float) position.z);
            shader.getUniformOrDefault("LightColor").set(color.x, color.y, color.z);
            shader.getUniformOrDefault("LightRadius").set(15f);

            SCREEN_VBO.bind();
            SCREEN_VBO.draw(view, RenderSystem.getProjectionMatrix(), shader);
            VertexBuffer.unbind();
        }
    }

    @Override
    public void free() {
        geomVBO.close();
    }
}
