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
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.typho.vibrancy.Vibrancy;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.system.NativeResource;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.GL_FUNC_ADD;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.glBindBufferBase;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public abstract class SkyLight implements RaytracedLight {
    protected class Node implements NativeResource {
        protected final VertexBuffer vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
        protected final int ssbo = glGenBuffers();
        protected final List<BlockPos> dirty = new LinkedList<>();
        protected List<Quad> quads = new LinkedList<>();
        protected CompletableFuture<List<Quad>> fullRebuildTask;
        protected final ChunkPos pos;
        protected boolean isDirty = true;

        public void markDirty() {
            isDirty = true;
        }

        public void clean() {
            isDirty = false;
        }

        public boolean isDirty() {
            return isDirty;
        }

        protected Node(ChunkPos pos) {
            this.pos = pos;
        }

        @Override
        public void free() {
            vbo.close();
            glDeleteBuffers(ssbo);
        }

        protected void upload(BufferBuilder builder, Collection<? extends IQuad> volumes) {
            SkyLight.this.upload(builder, volumes, vbo, ssbo, GL_STATIC_DRAW);
        }

        protected void regenQuads(ClientLevel level, BlockPos pos, Consumer<Quad> out) {
            if (shouldCastBlock(level, pos)) {
                quads.removeIf(quad -> quad.blockPos().equals(pos));
                getQuads(level, pos, out, false, direction, false, dir -> true);
            }
        }

        protected void init(ClientLevel level, boolean raytrace) {
            if (fullRebuildTask != null) {
                Vibrancy.NUM_LIGHT_TASKS++;
            }

            for (BlockPos pos : dirty) {
                regenQuads(level, pos, quads::add);

                for (Direction dir : Direction.values()) {
                    regenQuads(level, pos.relative(dir), quads::add);
                }
            }

            dirty.clear();

            if (fullRebuildTask != null && fullRebuildTask.isDone()) {
                try {
                    quads = fullRebuildTask.get();
                    fullRebuildTask = null;
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            } else if (raytrace) {
                if (isDirty() || SkyLight.this.isDirty()) {
                    if (isDirty()) {
                        clean();
                    }

                    fullRebuildTask = CompletableFuture.supplyAsync(() -> {
                        List<Quad> quads = new LinkedList<>();

                        if (level.hasChunk(pos.x, pos.z)) {
                            LevelChunk chunk = level.getChunk(pos.x, pos.z);

                            for (int i = chunk.getMinSection(); i < chunk.getMaxSection(); i++) {
                                LevelChunkSection section = chunk.getSection(chunk.getSectionIndexFromSectionY(i));
                                BlockPos minPos = SectionPos.of(chunk.getPos(), i).origin();

                                for (int x = 0; x < 16; x++) {
                                    for (int y = 0; y < 16; y++) {
                                        for (int z = 0; z < 16; z++) {
                                            BlockPos blockPos = new BlockPos(minPos.getX() + x, minPos.getY() + y, minPos.getZ() + z);

                                            if (shouldCastBlock(level, blockPos)) {
                                                BlockState state = section.getBlockState(x, y, z);

                                                BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
                                                RandomSource random = RandomSource.create();
                                                Vec3 offset = state.getOffset(level, blockPos);

                                                for (Direction direction : Direction.values()) {
                                                    if (Block.shouldRenderFace(state, level, blockPos, direction, blockPos.relative(direction))) {
                                                        getQuads(model.getQuads(state, direction, random), blockPos, quads::add, offset, direction);
                                                    }
                                                }

                                                getQuads(model.getQuads(state, null, random), blockPos, quads::add, offset, null);
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        return quads;
                    });
                }
            }

            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

            List<Quad> newQuads = new LinkedList<>();

            for (Quad quad : quads) {
                if (quad.direction() == null || Vibrancy.pointsToward(quad.direction(), direction)) {
                    quad.toVolumeSky(direction, distance).render(builder);
                    newQuads.add(quad);
                }
            }

            upload(builder, newQuads);
        }

        protected void renderMask(Matrix4f view) {
            RenderType stencilType = VeilRenderType.get(Vibrancy.id("sky_stencil_node"));
            stencilType.setupRenderState();

            glEnable(GL_STENCIL_TEST);
            glStencilMask(2);
            glStencilFunc(GL_ALWAYS, 2, 2);
            glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);

            stencilType.clearRenderState();

            RenderType type = VeilRenderType.get(Vibrancy.id("sky_shadow"));
            type.setupRenderState();

            glEnable(GL_STENCIL_TEST);
            glStencilMask(0xFF);
            glStencilFunc(GL_EQUAL, 3, 3);
            glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP);
            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, ssbo);

            ShaderInstance shader = Objects.requireNonNull(RenderSystem.getShader());

            shader.safeGetUniform("MaxLength").set(distance);
            shader.safeGetUniform("LightDirection").set(direction);
            shader.setSampler("AtlasSampler", Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS));

            vbo.bind();
            vbo.drawWithShader(view, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();

            glBindBufferBase(GL_SHADER_STORAGE_BUFFER, 0, 0);

            type.clearRenderState();

            RenderType stencilClearType = VeilRenderType.get(Vibrancy.id("sky_stencil_clear"));
            stencilClearType.setupRenderState();

            glEnable(GL_STENCIL_TEST);
            glStencilMask(2);
            glStencilFunc(GL_ALWAYS, 0, 2);
            glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);

            Vibrancy.SCREEN_VBO.bind();
            Vibrancy.SCREEN_VBO.drawWithShader(view, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());

            stencilClearType.clearRenderState();
        }
    }

    public static final Vector3f SUN_COLOR = new Vector3f(0.5f, 0.5f, 0.425f);
    public static @Nullable SkyLight INSTANCE;

    protected final Map<ChunkPos, Node> nodes = new LinkedHashMap<>();
    protected Vector3f direction;
    protected float distance = 2048;
    protected boolean isDirty = true;

    public void markDirty() {
        isDirty = true;
    }

    public void clean() {
        isDirty = false;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void appendDebugInfo(Consumer<String> out) {
        out.accept("Sky Light Nodes: " + nodes.size());
        out.accept("Sky Light Quads: " + nodes.values().stream().mapToInt(node -> node.quads.size()).sum());
    }

    @Override
    public void updateDirty(Iterable<BlockPos> it) {
        for (BlockPos pos : it) {
            nodes.computeIfAbsent(new ChunkPos(pos), Node::new).dirty.add(pos);
        }
    }

    @Override
    public void init() {
    }

    public abstract Vector3f getDirection(ClientLevel level);

    public abstract Vector3f getColor(ClientLevel level);

    protected boolean shouldCastBlock(ClientLevel level, BlockPos pos) {
        for (Direction direction : new Direction[]{Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
            if (level.getBrightness(LightLayer.SKY, pos.relative(direction)) == 15) {
                return true;
            }
        }

        return false;
    }

    protected void renderMask(boolean raytrace, Matrix4f view) {
        AdvancedFbo fbo = Objects.requireNonNull(VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(Vibrancy.id("shadow_mask")));
        fbo.bind(true);
        glClearColor(0f, 0f, 0f, 0f);
        glClearStencil(0);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT | GL_STENCIL_BUFFER_BIT);

        if (raytrace) {
            glEnable(GL_STENCIL_TEST);
            glStencilMask(1);
            glStencilFunc(GL_ALWAYS, 1, 1);
            glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE);

            RenderType stencilType = VeilRenderType.get(Vibrancy.id("sky_stencil"));
            stencilType.setupRenderState();

            Vibrancy.SCREEN_VBO.bind();
            Vibrancy.SCREEN_VBO.drawWithShader(view, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();

            stencilType.clearRenderState();

            for (Node node : nodes.values()) {
                node.renderMask(view);
            }

            glDisable(GL_STENCIL_TEST);
        }
    }

    protected void renderLight(ClientLevel level) {
        Objects.requireNonNull(VeilRenderSystem.renderer().getFramebufferManager().getFramebuffer(Vibrancy.id("ray_light"))).bind(true);
        VeilRenderSystem.setShader(Vibrancy.id("light/ray/sky"));
        ShaderInstance shader = Objects.requireNonNull(RenderSystem.getShader());

        shader.safeGetUniform("LightDirection").set(direction);
        shader.safeGetUniform("LightColor").set(getColor(level));
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
            direction = getColor(level);

            for (Node node : nodes.values()) {
                node.init(level, raytrace);
            }

            clean();

            Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
            Matrix4f view = new Matrix4f()
                    .rotate(camera.rotation().invert(new Quaternionf()))
                    .translate((float) -camera.getPosition().x, (float) -camera.getPosition().y, (float) -camera.getPosition().z);

            renderMask(raytrace, view);
            renderLight(level);

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
        for (Node node : nodes.values()) {
            node.free();
        }

        nodes.clear();
    }

    public static class Overworld extends SkyLight {
        @Override
        public Vector3f getDirection(ClientLevel level) {
            float sunAngle = level.getSunAngle(0);
            return new Vector3f((float) -Math.sin(sunAngle), (float) Math.cos(sunAngle), 0);
        }

        @Override
        public Vector3f getColor(ClientLevel level) {
            return SUN_COLOR;
        }
    }
}
