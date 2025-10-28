package net.typho.vibrancy.light;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.framebuffer.AdvancedFbo;
import foundry.veil.api.client.render.rendertype.VeilRenderType;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.typho.vibrancy.Vibrancy;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public class SkyLight implements RaytracedLight {
    public static final Vector3f SUN_COLOR = new Vector3f(1, 1, 0.85f);
    public static @Nullable SkyLight INSTANCE = new SkyLight();

    protected final VertexBuffer geomVBO = new VertexBuffer(VertexBuffer.Usage.STATIC);
    protected final int quadsSSBO = glGenBuffers();
    protected final List<BlockPos> dirty = new LinkedList<>();
    protected List<Quad> quads = new LinkedList<>();
    protected Vector3f direction;
    protected float distance = 2048;

    @Override
    public void updateDirty(Iterable<BlockPos> it) {
        for (BlockPos pos : it) {
            dirty.add(pos);
        }
    }

    @Override
    public void init() {
    }

    protected void upload(BufferBuilder builder, Collection<? extends IQuad> volumes) {
        upload(builder, volumes, geomVBO, quadsSSBO, GL_STATIC_DRAW);
    }

    protected void regenQuads(ClientLevel world, BlockPos pos, Consumer<Quad> out) {
        quads.removeIf(quad -> quad.blockPos().equals(pos));
        getQuads(world, pos, out, false, direction, false);
    }

    protected void renderMask(boolean raytrace, Matrix4f view) {
        AdvancedFbo fbo = Objects.requireNonNull(VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(Vibrancy.id("shadow_mask")));
        fbo.bind(true);
        glClearColor(0f, 0f, 0f, 0f);
        glClearStencil(0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        if (raytrace && !quads.isEmpty()) {
            glEnable(GL_STENCIL_TEST);
            glStencilMask(0xFF);
            glStencilFunc(GL_ALWAYS, 1, 0xFF);
            glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);

            RenderType stencilType = VeilRenderType.get(Vibrancy.id("sky_stencil"));
            stencilType.setupRenderState();

            Vibrancy.SCREEN_VBO.bind();
            Vibrancy.SCREEN_VBO.drawWithShader(view, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();

            stencilType.clearRenderState();

            RenderType type = VeilRenderType.get(Vibrancy.id("sky_shadow"));
            type.setupRenderState();

            glEnable(GL_STENCIL_TEST);
            glStencilMask(0xFF);
            glStencilFunc(GL_EQUAL, 1, 0xFF);
            glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, quadsSSBO);

            ShaderInstance shader = Objects.requireNonNull(RenderSystem.getShader());

            shader.safeGetUniform("MaxLength").set(distance);
            shader.safeGetUniform("LightDirection").set(direction);
            shader.setSampler("AtlasSampler", Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS));

            geomVBO.bind();
            geomVBO.drawWithShader(view, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();

            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);

            type.clearRenderState();
            glDisable(GL_STENCIL_TEST);
        }
    }

    protected void renderLight() {
        Objects.requireNonNull(VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(Vibrancy.id("ray_light"))).bind(true);
        VeilRenderSystem.setShader(Vibrancy.id("light/ray/sky"));
        ShaderInstance shader = Objects.requireNonNull(RenderSystem.getShader());

        shader.safeGetUniform("LightDirection").set(direction);
        shader.safeGetUniform("LightColor").set(SUN_COLOR);
        //shader.safeGetUniform("LightColor").set(color.x * brightness, color.y * brightness, color.z * brightness);

        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
        RenderSystem.blendEquation(GL_FUNC_ADD);

        Vibrancy.SCREEN_VBO.bind();
        Vibrancy.SCREEN_VBO.drawWithShader(null, null, shader);
        VertexBuffer.unbind();

        RenderSystem.disableBlend();
    }

    @Override
    public boolean render(boolean raytrace) {
        ClientLevel level = Minecraft.getInstance().level;

        if (level != null) {
            float sunAngle = level.getSunAngle(0);
            direction = new Vector3f((float) -Math.sin(sunAngle), (float) Math.cos(sunAngle), 0);

            for (BlockPos pos : dirty) {
                regenQuads(level, pos, quads::add);

                for (Direction dir : Direction.values()) {
                    regenQuads(level, pos.relative(dir), quads::add);
                }
            }

            dirty.clear();

            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

            for (Quad quad : quads) {
                quad.toVolumeSky(direction, distance).render(builder);
            }

            upload(builder, quads);

            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            Matrix4f view = new Matrix4f()
                    .rotate(camera.rotation().invert(new Quaternionf()))
                    .translate((float) -camera.getPosition().x, (float) -camera.getPosition().y, (float) -camera.getPosition().z);

            renderMask(raytrace, view);
            renderLight();

            return true;
        }

        return false;
    }

    @Override
    public Vector3d getPosition() {
        return null;
    }

    @Override
    public boolean shouldRender() {
        return true;
    }

    @Override
    public void free() {
    }
}
