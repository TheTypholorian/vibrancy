package net.typho.vibrancy;

import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.*;

import java.lang.Math;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL30.GL_STREAM_DRAW;

public class RaytracedPointEntityLight extends AbstractRaytracedLight {
    public final LivingEntity entity;
    protected boolean hasLight = false;
    protected List<Quad> quads = new LinkedList<>();
    protected final List<BlockPos> dirty = new LinkedList<>();
    protected BlockBox quadBox;
    protected CompletableFuture<List<Quad>> fullRebuildTask;

    public RaytracedPointEntityLight(LivingEntity entity) {
        this.entity = entity;
    }

    @Override
    public void updateDirty(Iterable<BlockPos> it) {
        BlockBox box = getBox();

        for (BlockPos pos : it) {
            if (box.contains(pos)) {
                dirty.add(pos);
            }
        }
    }

    @Override
    public void upload(BufferBuilder builder, Collection<ShadowVolume> volumes) {
        if (volumes.isEmpty()) {
            anyShadows = false;
        } else {
            anyShadows = true;
            upload(builder, volumes, geomVBO, quadsSSBO, GL_STREAM_DRAW);
        }
    }

    public void regenQuadsSync(ClientWorld world, BlockPos pos, Consumer<Quad> out, BlockPos lightBlockPos, Vector3f lightPos) {
        quads.removeIf(q -> q.blockPos().equals(pos));
        regenQuadsAsync(world, pos, out, lightBlockPos, lightPos);
    }

    public void regenQuadsAsync(ClientWorld world, BlockPos pos, Consumer<Quad> out, BlockPos lightBlockPos, Vector3f lightPos) {
        getQuads(world, pos, out, pos.getSquaredDistance(lightBlockPos), lightBlockPos, lightPos, false);
    }

    public void regenAll(ClientWorld world, BlockBox box, BlockPos lightBlockPos, Vector3f lightPos) {
        fullRebuildTask = CompletableFuture.supplyAsync(() -> {
            List<Quad> quads = new LinkedList<>();

            for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
                for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
                    for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                        regenQuadsAsync(world, new BlockPos(x, y, z), quads::add, lightBlockPos, lightPos);
                    }
                }
            }

            return quads;
        });
    }

    @Override
    public boolean render(boolean raytrace) {
        hasLight = false;

        if (entity.getMainHandStack().getItem() instanceof BlockItem block) {
            BlockState state = block.getBlock().getDefaultState();
            DynamicLightInfo info = DynamicLightInfo.get(state);

            if (info == null) {
                return false;
            } else {
                info.initLight(this, state);
                hasLight = true;
            }
        } else {
            return false;
        }

        ClientWorld world = MinecraftClient.getInstance().world;

        if (world != null) {
            BlockPos lightBlockPos = new BlockPos((int) Math.floor(getPosition().x), (int) Math.floor(getPosition().y), (int) Math.floor(getPosition().z));
            Vector3f lightPos = new Vector3f((float) getPosition().x, (float) getPosition().y, (float) getPosition().z);
            int blockRadius = Vibrancy.capShadowDistance((int) Math.ceil(radius) - 2);
            BlockBox box = getBox();

            List<ShadowVolume> volumes = new LinkedList<>();
            BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

            if (isVisible()) {
                if (fullRebuildTask != null && fullRebuildTask.isDone()) {
                    try {
                        quads = fullRebuildTask.get();
                        fullRebuildTask = null;
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                } else if (hasLight) {
                    if (quadBox == null || !quadBox.contains(entity.getBlockPos())) {
                        dirty.clear();
                        quadBox = box;
                        regenAll(world, blockRadius > 1 ? box.expand(blockRadius) : box, lightBlockPos, lightPos);
                    } else if (!dirty.isEmpty()) {
                        for (BlockPos pos : dirty) {
                            regenQuadsSync(world, pos, quads::add, lightBlockPos, lightPos);

                            for (Direction dir : Direction.values()) {
                                regenQuadsSync(world, pos.offset(dir), quads::add, lightBlockPos, lightPos);
                            }
                        }
                    }
                }

                for (Quad quad : quads) {
                    if (box.contains(quad.blockPos())) {
                        ShadowVolume volume = quad.toVolume(lightPos, radius * 2);
                        volume.render(builder);
                        volumes.add(volume);
                    }
                }

                upload(builder, volumes);

                Camera camera = MinecraftClient.getInstance().gameRenderer.getCamera();
                Matrix4f view = new Matrix4f()
                        .rotate(camera.getRotation().invert(new Quaternionf()))
                        .translate((float) -camera.getPos().x, (float) -camera.getPos().y, (float) -camera.getPos().z);

                renderMask(raytrace, lightPos, camera, view);
                renderLight(lightPos, view);

                return true;
            }
        }

        return false;
    }

    @Override
    public final PointLight setPosition(Vector3dc position) {
        throw new UnsupportedOperationException("Can't move an entity light");
    }

    @Override
    public final PointLight setPosition(double x, double y, double z) {
        throw new UnsupportedOperationException("Can't move an entity light");
    }

    @Override
    public Vector3d getPosition() {
        float tickDelta = MinecraftClient.getInstance().getRenderTickCounter().getTickDelta(true);
        Vec3d pos = entity.getCameraPosVec(tickDelta)
                .add(entity.getRotationVec(tickDelta).multiply(0.5));
        return new Vector3d(pos.x, pos.y, pos.z);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + entity + "]";
    }
}
