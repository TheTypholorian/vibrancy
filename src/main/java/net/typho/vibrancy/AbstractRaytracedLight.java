package net.typho.vibrancy;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.Objects;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public abstract class AbstractRaytracedLight extends PointLight implements RaytracedLight {
    protected final VertexBuffer geomVBO = new VertexBuffer(VertexBuffer.Usage.DYNAMIC), boxVBO = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
    protected final int quadsSSBO = glGenBuffers();
    protected boolean anyShadows = false;
    protected float flicker = 0, flickerMin, flickerMax, flickerStart = (float) GLFW.glfwGetTime();

    public float getFlicker() {
        return flicker;
    }

    public AbstractRaytracedLight setFlicker(float flicker) {
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
        BlockBox box = new BlockBox(lightBlockPos);

        if (blockRadius > 1) {
            box = box.expand(blockRadius);
        }

        return box;
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

    protected void renderMask(boolean raytrace, Vector3f lightPos, Camera camera, Matrix4f view) {
        if (anyShadows && raytrace) {
            VeilRenderSystem.setShader(Vibrancy.id("light/ray/mask"));
            ShaderProgram shader = Objects.requireNonNull(RenderSystem.getShader());

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
    }

    protected void renderLight(Vector3f lightPos, Matrix4f view) {
        Objects.requireNonNull(VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(Vibrancy.id("ray_light"))).bind(true);
        VeilRenderSystem.setShader(Vibrancy.id("light/ray/point"));
        ShaderProgram shader = Objects.requireNonNull(RenderSystem.getShader());

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

        BufferBuilder builder = RenderSystem.renderThreadTesselator().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        Box box = getBoundingBox();
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

        RenderSystem.disableDepthTest();
        glCullFace(GL_FRONT);
        RenderSystem.enableCull();

        boxVBO.bind();
        boxVBO.upload(builder.end());
        boxVBO.draw(view, RenderSystem.getProjectionMatrix(), shader);
        VertexBuffer.unbind();

        glCullFace(GL_BACK);
    }

    @Override
    public Box getBoundingBox() {
        int rad = Math.max(Vibrancy.capShadowDistance((int) Math.ceil(radius) - 2), 0);
        Vector3d pos = getPosition();
        return new Box(pos.x - rad, pos.y - rad, pos.z - rad, pos.x + rad, pos.y + rad, pos.z + rad);
    }

    @Override
    public void free() {
        geomVBO.close();
        glDeleteBuffers(quadsSSBO);
    }
}
