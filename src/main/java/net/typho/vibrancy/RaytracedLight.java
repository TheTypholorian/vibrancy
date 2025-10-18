package net.typho.vibrancy;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.NativeResource;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public interface RaytracedLight extends NativeResource {
    Set<BlockPos> DIRTY = new HashSet<>();

    default boolean isVisible() {
        return true;
    }

    void updateDirty(Iterable<BlockPos> it);

    void init();

    boolean render(boolean raytrace);

    Vector3f getPosition();

    Box getBoundingBox();

    default int upload(int vbo, Collection<Vector3f> vertices, int usage) {
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        ByteBuffer vertexBuf = MemoryUtil.memAlloc(vertices.size() * Float.BYTES * 3);
        int i = 0;

        for (Vector3f vertex : vertices) {
            vertex.get(vertexBuf);
            i += Float.BYTES * 3;
            vertexBuf.position(i);
        }

        glBufferData(GL_ARRAY_BUFFER, vertexBuf.flip(), usage);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        return vertices.size();
    }

    default void getQuads(Iterable<BakedQuad> quads, BlockPos pos, Consumer<Quad> out, BlockRenderLayer layer, Vec3d offset) {
        for (BakedQuad quad : quads) {
            Vector3f[] positions = new Vector3f[4];
            Vector2f[] uvs = new Vector2f[4];

            int[] data = quad.vertexData();
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
                    uvs[3],
                    layer != BlockRenderLayer.SOLID
            ));
        }
    }

    default void getQuads(ClientWorld world, BlockPos pos, Consumer<Quad> out, double sqDist, BlockPos lightBlockPos, boolean normalTest) {
        BlockState state = world.getBlockState(pos);

        if (!Vibrancy.TRANSPARENCY_TEST.getValue() && state.isTransparent()) {
            return;
        }

        BlockRenderLayer layer = RenderLayers.getBlockLayer(state);
        BlockStateModel model = MinecraftClient.getInstance().getBlockRenderManager().getModel(state);
        Random random = Random.create();
        Vec3d offset = state.getModelOffset(pos);
        List<BlockModelPart> parts = model.getParts(random);

        for (BlockModelPart part : parts) {
            for (Direction direction : Direction.values()) {
                if (sqDist <= 4 || (Block.shouldDrawSide(state, world.getBlockState(pos.offset(direction)), direction) && (!normalTest || Vibrancy.pointsToward(pos, direction, lightBlockPos)))) {
                    getQuads(part.getQuads(direction), pos, out, layer, offset);
                }
            }

            getQuads(part.getQuads(null), pos, out, layer, offset);
        }
    }

    default void getVolumes(ClientWorld world, BlockPos pos, Consumer<ShadowVolume> out, double sqDist, BlockPos lightBlockPos, Vector3f lightPos, float radius, boolean normalTest) {
        getQuads(world, pos, quad -> out.accept(quad.toVolume(lightPos, radius)), sqDist, lightBlockPos, normalTest);
    }

    default int upload(Collection<Vector3f> vertices, Collection<ShadowVolume> volumes, int geomVBO, int quadsSSBO, int usage) {
        int i = upload(geomVBO, vertices, usage);

        ByteBuffer buf = MemoryUtil.memAlloc(volumes.size() * Quad.BYTES);

        for (ShadowVolume v : volumes) {
            v.caster().put(buf);
        }

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, quadsSSBO);
        glBufferData(GL_SHADER_STORAGE_BUFFER, buf.flip(), usage);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        MemoryUtil.memFree(buf);
        return i;
    }

    record Quad(
            BlockPos blockPos,
            Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
            Vector2f uv1, Vector2f uv2, Vector2f uv3, Vector2f uv4,
            Vector3f n, float d,
            Vector3f e1, Vector3f e2,
            boolean sample
    ) {
        public static final int BYTES = 40 * Float.BYTES;

        public Quad(BlockPos blockPos, Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
                    Vector2f uv1, Vector2f uv2, Vector2f uv3, Vector2f uv4, boolean sample) {
            this(
                    blockPos,
                    v1, v2, v3, v4, uv1, uv2, uv3, uv4,
                    new Vector3f(v2).sub(v1).cross(new Vector3f(v4).sub(v1)).normalize(),
                    new Vector3f(v2).sub(v1).cross(new Vector3f(v4).sub(v1)).normalize().dot(v1),
                    new Vector3f(v2).sub(v1),
                    new Vector3f(v4).sub(v1),
                    sample
            );
        }

        public void put(ByteBuffer buf) {
            buf.putFloat(v1.x).putFloat(v1.y).putFloat(v1.z).putInt(sample ? 1 : 0);
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

        public ShadowVolume toVolume(Vector3f origin, float radius) {
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
    }

    record ShadowVolume(Quad caster, Vector3f[] vertices) {
        public void render(Consumer<Vector3f> consumer) {
            consumer.accept(vertices()[0]);
            consumer.accept(vertices()[1]);
            consumer.accept(vertices()[2]);
            consumer.accept(vertices()[3]);

            consumer.accept(vertices()[1]);
            consumer.accept(vertices()[5]);
            consumer.accept(vertices()[6]);
            consumer.accept(vertices()[2]);

            consumer.accept(vertices()[5]);
            consumer.accept(vertices()[4]);
            consumer.accept(vertices()[7]);
            consumer.accept(vertices()[6]);

            consumer.accept(vertices()[4]);
            consumer.accept(vertices()[0]);
            consumer.accept(vertices()[3]);
            consumer.accept(vertices()[7]);

            consumer.accept(vertices()[1]);
            consumer.accept(vertices()[0]);
            consumer.accept(vertices()[4]);
            consumer.accept(vertices()[5]);

            consumer.accept(vertices()[3]);
            consumer.accept(vertices()[2]);
            consumer.accept(vertices()[6]);
            consumer.accept(vertices()[7]);
        }
    }
}
