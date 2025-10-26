package net.typho.vibrancy;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.deferred.light.PointLight;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.ShaderInstance;
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

    protected void renderMask(boolean raytrace, Vector3f lightPos, Camera camera, Matrix4f view) {
        Objects.requireNonNull(VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(Vibrancy.id("shadow_mask"))).bind(true);
        glClearColor(0f, 0f, 0f, 0f);
        glClear(GL_COLOR_BUFFER_BIT);

        if (anyShadows && raytrace) {
            VeilRenderSystem.setShader(Vibrancy.id("light/ray/mask"));
            ShaderInstance shader = Objects.requireNonNull(RenderSystem.getShader());

            shader.safeGetUniform("LightPos").set(lightPos.x, lightPos.y, lightPos.z);
            shader.safeGetUniform("LightRadius").set(radius);
            shader.safeGetUniform("Detailed").set(1);

            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            RenderSystem.blendEquation(GL_FUNC_ADD);
            RenderSystem.disableCull();

            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, quadsSSBO);

            geomVBO.bind();
            geomVBO.drawWithShader(view, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();

            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);
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

        BufferBuilder builder = RenderSystem.renderThreadTesselator().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
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

        builder.vertex(vertices[0].x, vertices[0].y, vertices[0].z)
                .vertex(vertices[1].x, vertices[0].y, vertices[0].z)
                .vertex(vertices[2].x, vertices[0].y, vertices[0].z)
                .vertex(vertices[3].x, vertices[0].y, vertices[0].z);

        builder.vertex(vertices[1].x, vertices[1].y, vertices[1].z)
                .vertex(vertices[5].x, vertices[5].y, vertices[5].z)
                .vertex(vertices[6].x, vertices[6].y, vertices[6].z)
                .vertex(vertices[2].x, vertices[2].y, vertices[2].z);

        builder.vertex(vertices[5].x, vertices[5].y, vertices[5].z)
                .vertex(vertices[4].x, vertices[4].y, vertices[4].z)
                .vertex(vertices[7].x, vertices[7].y, vertices[7].z)
                .vertex(vertices[6].x, vertices[6].y, vertices[6].z);

        builder.vertex(vertices[4].x, vertices[4].y, vertices[4].z)
                .vertex(vertices[0].x, vertices[0].y, vertices[0].z)
                .vertex(vertices[3].x, vertices[3].y, vertices[3].z)
                .vertex(vertices[7].x, vertices[7].y, vertices[7].z);

        builder.vertex(vertices[1].x, vertices[1].y, vertices[1].z)
                .vertex(vertices[0].x, vertices[0].y, vertices[0].z)
                .vertex(vertices[4].x, vertices[4].y, vertices[4].z)
                .vertex(vertices[5].x, vertices[5].y, vertices[5].z);

        builder.vertex(vertices[3].x, vertices[3].y, vertices[3].z)
                .vertex(vertices[2].x, vertices[2].y, vertices[2].z)
                .vertex(vertices[6].x, vertices[6].y, vertices[6].z)
                .vertex(vertices[7].x, vertices[7].y, vertices[7].z);

        RenderSystem.disableDepthTest();
        glCullFace(GL_FRONT);
        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.blendEquation(GL_FUNC_ADD);

        boxVBO.bind();
        boxVBO.upload(builder.end());
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
