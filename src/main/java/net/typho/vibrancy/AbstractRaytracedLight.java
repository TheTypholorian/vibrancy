package net.typho.vibrancy;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexFormats;
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

public abstract class AbstractRaytracedLight implements RaytracedLight {
    protected final int geomVAO = glGenVertexArrays(), geomVBO = glGenBuffers(), boxVBO = glGenBuffers(), quadsSSBO = glGenBuffers();
    protected int numVertices = 0;
    protected boolean anyShadows = false, dirty = false;
    protected float flicker = 0, flickerMin, flickerMax, flickerStart = (float) GLFW.glfwGetTime();
    protected Vector3f position = new Vector3f(), color = new Vector3f();
    protected float brightness = 1, radius = 1;

    public void markDirty() {
        dirty = true;
    }

    public void clean() {
        dirty = false;
    }

    public float getFlicker() {
        return flicker;
    }

    public AbstractRaytracedLight setFlicker(float flicker) {
        this.flicker = flicker;
        markDirty();
        return this;
    }

    @Override
    public Vector3f getPosition() {
        return position;
    }

    public AbstractRaytracedLight setPosition(Vector3f position) {
        this.position = new Vector3f(position);
        return this;
    }

    public AbstractRaytracedLight setPosition(Vector3d position) {
        this.position = position.get(new Vector3f());
        return this;
    }

    public AbstractRaytracedLight setPosition(double x, double y, double z) {
        this.position = new Vector3f((float) x, (float) y, (float) z);
        return this;
    }

    public Vector3f getColor() {
        return color;
    }

    public AbstractRaytracedLight setColor(Vector3f color) {
        this.color = new Vector3f(color);
        return this;
    }

    public AbstractRaytracedLight setColor(Vector3d position) {
        this.color = position.get(new Vector3f());
        return this;
    }

    public AbstractRaytracedLight setColor(double r, double g, double b) {
        this.color = new Vector3f((float) r, (float) g, (float) b);
        return this;
    }

    public float getBrightness() {
        return brightness;
    }

    public AbstractRaytracedLight setBrightness(float brightness) {
        this.brightness = brightness;
        return this;
    }

    public float getRadius() {
        return radius;
    }

    public AbstractRaytracedLight setRadius(float radius) {
        this.radius = radius;
        return this;
    }

    public void upload(Collection<Vector3f> vertices, Collection<ShadowVolume> volumes) {
        if (volumes.isEmpty()) {
            anyShadows = false;
        } else {
            anyShadows = true;
            numVertices = upload(vertices, volumes, geomVBO, quadsSSBO, GL_DYNAMIC_DRAW);
        }
    }

    public BlockBox getBox() {
        Vector3f pos = getPosition();
        BlockPos lightBlockPos = new BlockPos((int) Math.floor(pos.x), (int) Math.floor(pos.y), (int) Math.floor(pos.z));
        int blockRadius = Vibrancy.capShadowDistance((int) Math.ceil(radius) - 2);
        BlockBox box = new BlockBox(lightBlockPos);

        if (blockRadius > 1) {
            box = box.expand(blockRadius);
        }

        return box;
    }

    protected void renderMask(int fbo, Matrix4f view) {
        glBindFramebuffer(GL_FRAMEBUFFER, fbo);
        glClearColor(0f, 0f, 0f, 0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        glBindVertexArray(geomVAO);
        glDrawArrays(GL_QUADS, 0, numVertices);
        geomVBO.draw(view, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
        VertexBuffer.unbind();
    }

    protected void renderMask(boolean raytrace, Vector3f lightPos, Camera camera, Matrix4f view) {
        if (anyShadows && raytrace) {
            VeilRenderSystem.setShader(Vibrancy.id("light/ray/mask"));
            ShaderProgram shader = Objects.requireNonNull(RenderSystem.getShader());

            shader.getUniformOrDefault("LightPos").set(lightPos.x, lightPos.y, lightPos.z);
            shader.getUniformOrDefault("LightRadius").set(radius);
            shader.getUniformOrDefault("Detailed").set(1);

            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
            RenderSystem.blendEquation(GL_FUNC_ADD);
            RenderSystem.disableCull();

            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, quadsSSBO);

            renderMask(Vibrancy.id("shadow_mask"), view);

            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
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
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE);
        RenderSystem.blendEquation(GL_FUNC_ADD);

        boxVBO.bind();
        boxVBO.upload(builder.end());
        boxVBO.draw(view, RenderSystem.getProjectionMatrix(), shader);
        VertexBuffer.unbind();

        glCullFace(GL_BACK);
        RenderSystem.disableBlend();
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
