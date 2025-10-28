package net.typho.vibrancy.light;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexConsumer;
import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.typho.vibrancy.Vibrancy;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public interface RaytracedLight extends NativeResource {
    Set<BlockPos> DIRTY = new HashSet<>();

    default boolean isVisible() {
        return true;
    }

    void updateDirty(Iterable<BlockPos> it);

    void init();

    boolean render(boolean raytrace);

    @Nullable Vector3d getPosition();

    default boolean shouldRemove() {
        return false;
    }

    default boolean shouldRender() {
        AABB box = getBoundingBox();

        if (box == null) {
            return true;
        }

        return VeilRenderSystem.getCullingFrustum().testAab(box);
    }

    default @Nullable AABB getBoundingBox() {
        return null;
    }

    default void getQuads(Iterable<BakedQuad> bakedQuads, BlockPos pos, Consumer<Quad> out, Vec3 offset) {
        for (BakedQuad quad : bakedQuads) {
            Vector3f[] positions = new Vector3f[4];
            Vector2f[] uvs = new Vector2f[4];

            int[] data = quad.getVertices();
            int len = data.length / 8;

            for (int i = 0, j = 0; i < len; i++, j += 8) {
                positions[i] = new Vector3f(
                        Float.intBitsToFloat(data[j]) + pos.getX() + (float) offset.x,
                        Float.intBitsToFloat(data[j + 1]) + pos.getY() + (float) offset.y,
                        Float.intBitsToFloat(data[j + 2]) + pos.getZ() + (float) offset.z
                );
                uvs[i] = new Vector2f(
                        Float.intBitsToFloat(data[j + 4]),
                        Float.intBitsToFloat(data[j + 5])
                );
            }

            out.accept(new Quad(
                    pos,
                    positions[0],
                    positions[1],
                    positions[2],
                    positions[3],
                    uvs[0],
                    uvs[1],
                    uvs[2],
                    uvs[3]
            ));
        }
    }

    default void getQuads(ClientLevel world, BlockPos pos, Consumer<Quad> out, boolean close, BlockPos blockPos, boolean normalTest, Predicate<Direction> predicate) {
        getQuads(world, pos, out, close, new Vector3f(blockPos.getX() - pos.getX(), blockPos.getY() - pos.getY(), blockPos.getZ() - pos.getZ()), normalTest, predicate);
    }

    default void getQuads(ClientLevel world, BlockPos pos, Consumer<Quad> out, boolean close, Vector3f lightDirection, boolean normalTest, Predicate<Direction> predicate) {
        BlockState state = world.getBlockState(pos);

        if (!Vibrancy.TRANSPARENCY_TEST.get() && state.propagatesSkylightDown(world, pos)) {
            return;
        }

        BakedModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
        RandomSource random = RandomSource.create();
        Vec3 offset = state.getOffset(world, pos);

        for (Direction direction : Direction.values()) {
            if (predicate.test(direction) && (close || (Block.shouldRenderFace(state, world, pos, direction, pos.relative(direction)) && (!normalTest || Vibrancy.pointsToward(direction, lightDirection))))) {
                getQuads(model.getQuads(state, direction, random), pos, out, offset);
            }
        }

        if (predicate.test(null)) {
            getQuads(model.getQuads(state, null, random), pos, out, offset);
        }
    }

    default void upload(BufferBuilder builder, Collection<? extends IQuad> quads, VertexBuffer geomVBO, int quadsSSBO, int usage) {
        MeshData mesh = builder.build();

        if (mesh == null) {
            return;
        }

        geomVBO.bind();
        geomVBO.upload(mesh);
        VertexBuffer.unbind();

        ByteBuffer buf = MemoryUtil.memAlloc(quads.size() * Quad.BYTES);

        for (IQuad quad : quads) {
            quad.toQuad().put(buf);
        }

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, quadsSSBO);
        glBufferData(GL_SHADER_STORAGE_BUFFER, buf.flip(), usage);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        MemoryUtil.memFree(buf);
    }

    interface IQuad {
        Quad toQuad();
    }

    record Quad(
            BlockPos blockPos,
            Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
            Vector2f uv1, Vector2f uv2, Vector2f uv3, Vector2f uv4,
            Vector3f n, float d,
            Vector3f e1, Vector3f e2
    ) implements IQuad {
        public static final int BYTES = 40 * Float.BYTES;

        public Quad(BlockPos blockPos, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
                    Vector2f uv1, Vector2f uv2, Vector2f uv3, Vector2f uv4) {
            this(
                    blockPos,
                    v1, v2, v3, v4, uv1, uv2, uv3, uv4,
                    new Vector3f(v2).sub(v1).cross(new Vector3f(v4).sub(v1)).normalize(),
                    new Vector3f(v2).sub(v1).cross(new Vector3f(v4).sub(v1)).normalize().dot(v1),
                    new Vector3f(v2).sub(v1),
                    new Vector3f(v4).sub(v1)
            );
        }

        @Override
        public Quad toQuad() {
            return this;
        }

        public void put(ByteBuffer buf) {
            buf.putFloat(v1.x).putFloat(v1.y).putFloat(v1.z).putFloat(0);
            buf.putFloat(v2.x).putFloat(v2.y).putFloat(v2.z).putFloat(0);
            buf.putFloat(v3.x).putFloat(v3.y).putFloat(v3.z).putFloat(0);
            buf.putFloat(v4.x).putFloat(v4.y).putFloat(v4.z).putFloat(0);

            buf.putFloat(uv1.x).putFloat(uv1.y);
            buf.putFloat(uv2.x).putFloat(uv2.y);
            buf.putFloat(uv3.x).putFloat(uv3.y);
            buf.putFloat(uv4.x).putFloat(uv4.y);

            buf.putFloat(n.x).putFloat(n.y).putFloat(n.z).putFloat(d);
            buf.putFloat(e1.x).putFloat(e1.y).putFloat(e1.z).putFloat(e1.dot(e1));
            buf.putFloat(e2.x).putFloat(e2.y).putFloat(e2.z).putFloat(e2.dot(e2));

            float d11 = e1.dot(e1);
            float d12 = e1.dot(e2);
            float d22 = e2.dot(e2);
            float invDet = 1.0f / (d11 * d22 - d12 * d12);

            float inv11 =  d22 * invDet;
            float inv12 = -d12 * invDet;
            float inv21 = -d12 * invDet;
            float inv22 =  d11 * invDet;

            buf.putFloat(inv11).putFloat(inv12).putFloat(inv21).putFloat(inv22);
        }

        public ShadowVolume toVolumeSphere(Vector3f origin, float radius) {
            float d0 = n.dot(v1.sub(origin, new Vector3f()));
            float t = radius - d0;

            Vector3f[] vertices = {v1, v2, v3, v4, null, null, null, null};

            for (int i = 0; i < 4; i++) {
                Vector3f vertex = new Vector3f(vertices[i]);
                Vector3f off = vertex.sub(origin, new Vector3f());
                vertices[i + 4] = vertex.add(off.normalize(t));
            }

            return new ShadowVolume(
                    this,
                    vertices
            );
        }

        public ShadowVolume toVolumeSky(Vector3f direction, float distance) {
            Vector3f add = direction.mul(-distance, new Vector3f());
            Vector3f[] vertices = {v1, v2, v3, v4, null, null, null, null};

            for (int i = 0; i < 4; i++) {
                vertices[i + 4] = new Vector3f(vertices[i]).add(add);
            }

            return new ShadowVolume(
                    this,
                    vertices
            );
        }
    }

    record ShadowVolume(Quad caster, Vector3f[] vertices) implements IQuad {
        @Override
        public Quad toQuad() {
            return caster;
        }

        public void render(VertexConsumer consumer) {
            consumer.addVertex(vertices()[0])
                    .addVertex(vertices()[1])
                    .addVertex(vertices()[2])
                    .addVertex(vertices()[3]);

            consumer.addVertex(vertices()[1])
                    .addVertex(vertices()[5])
                    .addVertex(vertices()[6])
                    .addVertex(vertices()[2]);

            consumer.addVertex(vertices()[5])
                    .addVertex(vertices()[4])
                    .addVertex(vertices()[7])
                    .addVertex(vertices()[6]);

            consumer.addVertex(vertices()[4])
                    .addVertex(vertices()[0])
                    .addVertex(vertices()[3])
                    .addVertex(vertices()[7]);

            consumer.addVertex(vertices()[1])
                    .addVertex(vertices()[0])
                    .addVertex(vertices()[4])
                    .addVertex(vertices()[5]);

            consumer.addVertex(vertices()[3])
                    .addVertex(vertices()[2])
                    .addVertex(vertices()[6])
                    .addVertex(vertices()[7]);
        }
    }
}
