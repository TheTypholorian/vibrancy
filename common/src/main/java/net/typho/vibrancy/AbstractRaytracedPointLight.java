package net.typho.vibrancy;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.light.PointLight;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockBox;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.Objects;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public abstract class AbstractRaytracedPointLight extends PointLight implements RaytracedLight {
    protected final VertexBuffer geomVBO = new VertexBuffer(VertexBuffer.Usage.DYNAMIC), boxVBO = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
    protected final int quadsSSBO = glGenBuffers();
    protected int shadowCount = 0;
    protected boolean anyShadows = false;
    protected float flicker = 0, flickerMin, flickerMax, flickerStart = (float) GLFW.glfwGetTime();

    public float getFlicker() {
        return flicker;
    }

    public AbstractRaytracedPointLight setFlicker(float flicker) {
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

    public BlockBox getBox() {
        Vector3d pos = getPosition();
        BlockPos lightBlockPos = new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
        int blockRadius = Vibrancy.capShadowDistance((int) Math.ceil(radius) - 2);
        BlockBox box = BlockBox.of(lightBlockPos);

        if (blockRadius > 1) {
            box = new BlockBox(
                    new BlockPos(box.min().getX() - blockRadius, box.min().getY() - blockRadius, box.min().getZ() - blockRadius),
                    new BlockPos(box.max().getX() + blockRadius, box.max().getY() + blockRadius, box.max().getZ() + blockRadius)
            );
        }

        return box;
    }

    protected void renderMask(boolean raytrace, Vector3f lightPos, Matrix4f view) {
        AdvancedFbo fbo = Objects.requireNonNull(VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(Vibrancy.id("shadow_mask")));
        fbo.bind(true);
        glClearColor(0f, 0f, 0f, 0f);
        glClearStencil(0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        if (anyShadows && raytrace) {
            glEnable(GL_STENCIL_TEST);
            glStencilMask(0xFF);
            glStencilFunc(GL_ALWAYS, 1, 0xFF);
            glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);

            RenderType stencilType = VeilRenderType.get(Vibrancy.id("shadow_stencil"));
            stencilType.setupRenderState();

            ShaderInstance shader = Objects.requireNonNull(RenderSystem.getShader());

            shader.safeGetUniform("LightPos").set(lightPos.x, lightPos.y, lightPos.z);
            shader.safeGetUniform("LightRadius").set(radius);

            Vibrancy.SCREEN_VBO.bind();
            Vibrancy.SCREEN_VBO.drawWithShader(view, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();

            stencilType.clearRenderState();

            RenderType type = VeilRenderType.get(Vibrancy.id("shadow"));
            type.setupRenderState();

            glEnable(GL_STENCIL_TEST);
            glStencilMask(0xFF);
            glStencilFunc(GL_EQUAL, 1, 0xFF);
            glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);

            shader = Objects.requireNonNull(RenderSystem.getShader());

            shader.safeGetUniform("LightPos").set(lightPos.x, lightPos.y, lightPos.z);
            shader.safeGetUniform("LightRadius").set(radius);

            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, quadsSSBO);

            geomVBO.bind();
            geomVBO.drawWithShader(view, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();

            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);

            type.clearRenderState();
            glDisable(GL_STENCIL_TEST);

            Vibrancy.SHADOW_COUNT += shadowCount;
        }
    }

    protected void renderLight(Vector3f lightPos, Matrix4f view) {
        Objects.requireNonNull(VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(Vibrancy.id("ray_light"))).bind(true);
        VeilRenderSystem.setShader(Vibrancy.id("light/ray/point"));
        ShaderInstance shader = Objects.requireNonNull(RenderSystem.getShader());

        float time = (float) GLFW.glfwGetTime();

        while (time - flickerStart > 0.25) {
            flickerStart += 0.25f;
            flickerMin = flickerMax;
            flickerMax = new java.util.Random().nextFloat(-1, 1);
        }

        float brightness = getBrightness() * (1 + flicker * Mth.lerp((time - flickerStart) * 4, flickerMin, flickerMax));

        shader.safeGetUniform("LightPos").set(lightPos.x, lightPos.y, lightPos.z);
        shader.safeGetUniform("LightColor").set(color.x * brightness, color.y * brightness, color.z * brightness);
        shader.safeGetUniform("LightRadius").set(radius);
        shader.safeGetUniform("AnyShadows").set(anyShadows ? 1 : 0);

        BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        AABB box = getBoundingBox();
        Vector3f[] vertices = {
                new Vector3f((float) box.maxX, (float) box.maxY, (float) box.maxZ),
                new Vector3f((float) box.minX, (float) box.maxY, (float) box.maxZ),
                new Vector3f((float) box.minX, (float) box.minY, (float) box.maxZ),
                new Vector3f((float) box.maxX, (float) box.minY, (float) box.maxZ),
                new Vector3f((float) box.maxX, (float) box.maxY, (float) box.minZ),
                new Vector3f((float) box.minX, (float) box.maxY, (float) box.minZ),
                new Vector3f((float) box.minX, (float) box.minY, (float) box.minZ),
                new Vector3f((float) box.maxX, (float) box.minY, (float) box.minZ),
        };

        builder.addVertex(vertices[0])
                .addVertex(vertices[1])
                .addVertex(vertices[2])
                .addVertex(vertices[3]);

        builder.addVertex(vertices[1])
                .addVertex(vertices[5])
                .addVertex(vertices[6])
                .addVertex(vertices[2]);

        builder.addVertex(vertices[5])
                .addVertex(vertices[4])
                .addVertex(vertices[7])
                .addVertex(vertices[6]);

        builder.addVertex(vertices[4])
                .addVertex(vertices[0])
                .addVertex(vertices[3])
                .addVertex(vertices[7]);

        builder.addVertex(vertices[1])
                .addVertex(vertices[0])
                .addVertex(vertices[4])
                .addVertex(vertices[5]);

        builder.addVertex(vertices[3])
                .addVertex(vertices[2])
                .addVertex(vertices[6])
                .addVertex(vertices[7]);

        RenderSystem.disableDepthTest();
        glCullFace(GL_FRONT);
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.blendEquation(GL_FUNC_ADD);

        boxVBO.bind();
        boxVBO.upload(builder.build());
        boxVBO.drawWithShader(view, RenderSystem.getProjectionMatrix(), shader);
        VertexBuffer.unbind();

        glCullFace(GL_BACK);
        RenderSystem.disableBlend();
    }

    @Override
    public AABB getBoundingBox() {
        Vector3d pos = getPosition();
        return new AABB(pos.x - radius, pos.y - radius, pos.z - radius, pos.x + radius, pos.y + radius, pos.z + radius);
    }

    @Override
    public void free() {
        geomVBO.close();
        glDeleteBuffers(quadsSSBO);
    }
}
