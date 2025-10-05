package net.typho.vibrancy;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
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

import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL43.GL_SHADER_STORAGE_BUFFER;

public interface RaytracedLight extends NativeResource {
    Set<BlockPos> DIRTY = new HashSet<>();

    default boolean isVisible() {
        return true;
    }

    void updateDirty(Iterable<BlockPos> it);

    boolean render(boolean raytrace);

    Vector3d getPosition();

    default void getQuads(Iterable<BakedQuad> bakedQuads, BlockPos pos, Consumer<Quad> out, RenderLayer layer) {
        for (BakedQuad quad : bakedQuads) {
            Vector3f[] positions = new Vector3f[4];
            Vector2f[] uvs = new Vector2f[4];

            int[] data = quad.getVertexData();
            int len = data.length / 8;

            for (int i = 0, j = 0; i < len; i++, j += 8) {
                positions[i] = new Vector3f(
                        Float.intBitsToFloat(data[j]) + pos.getX(),
                        Float.intBitsToFloat(data[j + 1]) + pos.getY(),
                        Float.intBitsToFloat(data[j + 2]) + pos.getZ()
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
                    layer.isTranslucent() || layer != RenderLayer.getSolid()
            ));
        }
    }

    default void getQuads(ClientWorld world, BlockPos pos, Consumer<Quad> out, double sqDist, BlockPos lightBlockPos, Vector3f lightPos, boolean normalTest) {
        BlockState state = world.getBlockState(pos);

        if (!Vibrancy.TRANSPARENCY_TEST.getValue() && state.isTransparent(world, pos)) {
            return;
        }

        //List<Quad> quads = new LinkedList<>();

        RenderLayer layer = RenderLayers.getBlockLayer(state);
        BakedModel model = MinecraftClient.getInstance().getBlockRenderManager().getModel(state);
        Random random = Random.create(lightBlockPos.hashCode());

        for (Direction direction : Direction.values()) {
            if (Block.shouldDrawSide(state, world, pos, direction, pos.offset(direction)) || sqDist <= 1) {
                //if (!normalTest || (sqDist == 1 || Vibrancy.pointsToward(pos, direction, lightBlockPos))) {
                getQuads(model.getQuads(state, direction, random), pos, out, layer);
                //}
            }
        }

        getQuads(model.getQuads(state, null, random), pos, out, layer);
    }

    default void getVolumes(ClientWorld world, BlockPos pos, Consumer<ShadowVolume> out, double sqDist, BlockPos lightBlockPos, Vector3f lightPos, float radius, boolean normalTest) {
        getQuads(world, pos, quad -> out.accept(quad.toVolume(lightPos, radius * 2)), sqDist, lightBlockPos, lightPos, normalTest);
    }

    default void upload(BufferBuilder builder, Collection<ShadowVolume> volumes, VertexBuffer geomVBO, int quadsSSBO, int usage) {
        geomVBO.bind();
        geomVBO.upload(builder.end());
        VertexBuffer.unbind();

        ByteBuffer buf = MemoryUtil.memAlloc(volumes.size() * Quad.BYTES);

        for (ShadowVolume v : volumes) {
            v.caster().put(buf);
        }

        glBindBuffer(GL_SHADER_STORAGE_BUFFER, quadsSSBO);
        glBufferData(GL_SHADER_STORAGE_BUFFER, buf.flip(), usage);
        glBindBuffer(GL_SHADER_STORAGE_BUFFER, 0);

        MemoryUtil.memFree(buf);
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

        public ShadowVolume toVolume(Vector3f origin, float distance) {
            Vector3f[] vertices = {v1, v2, v3, v4, null, null, null, null};

            for (int i = 0; i < 4; i++) {
                Vector3f vertex = new Vector3f(vertices[i]);
                Vector3f off = vertex.sub(origin, new Vector3f());
                vertices[i + 4] = vertex.add(off.normalize(distance));
            }

            return new ShadowVolume(
                    this,
                    vertices
            );
        }
    }

    record ShadowVolume(Quad caster, Vector3f[] vertices) {
        public void render(VertexConsumer consumer) {
            consumer.vertex(vertices()[0])
                    .vertex(vertices()[1])
                    .vertex(vertices()[2])
                    .vertex(vertices()[3]);

            consumer.vertex(vertices()[1])
                    .vertex(vertices()[5])
                    .vertex(vertices()[6])
                    .vertex(vertices()[2]);

            consumer.vertex(vertices()[5])
                    .vertex(vertices()[4])
                    .vertex(vertices()[7])
                    .vertex(vertices()[6]);

            consumer.vertex(vertices()[4])
                    .vertex(vertices()[0])
                    .vertex(vertices()[3])
                    .vertex(vertices()[7]);

            consumer.vertex(vertices()[1])
                    .vertex(vertices()[0])
                    .vertex(vertices()[4])
                    .vertex(vertices()[5]);

            consumer.vertex(vertices()[3])
                    .vertex(vertices()[2])
                    .vertex(vertices()[6])
                    .vertex(vertices()[7]);
        }
    }
}
