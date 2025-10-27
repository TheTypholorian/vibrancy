package net.typho.vibrancy;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.light.PointLight;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3dc;
import org.joml.Vector3f;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class RaytracedPointBlockLight extends AbstractRaytracedPointLight {
    protected final List<BlockPos> dirty = new LinkedList<>();
    protected boolean remove = false;
    protected List<ShadowVolume> volumes = new LinkedList<>();
    protected CompletableFuture<List<ShadowVolume>> fullRebuildTask;
    public final BlockPos blockPos;
    protected boolean render = false;

    public RaytracedPointBlockLight(BlockPos blockPos) {
        this.blockPos = blockPos;
        markDirty();
    }

    public void regenQuads(ClientLevel world, BlockPos pos, Consumer<ShadowVolume> out, BlockPos lightBlockPos, Vector3f lightPos) {
        volumes.removeIf(v -> v.caster().blockPos().equals(pos));
        getVolumes(world, pos, out, pos.distSqr(lightBlockPos), lightBlockPos, lightPos, radius, true);
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
    public void init() {
        render = false;

        boolean dirty = isDirty();

        ClientLevel world = Minecraft.getInstance().level;

        if (world != null) {
            BlockState state = world.getBlockState(blockPos);
            DynamicLightInfo info = DynamicLightInfo.get(state);

            if (info != null) {
                Vec3 offset = info.offset().apply(state).orElse(new Vec3(0.5, 0.5, 0.5));
                position.set(blockPos.getX() + offset.x, blockPos.getY() + offset.y, blockPos.getZ() + offset.z);
                info.initLight(this, state);
                render = true;
            }
        }

        if (!dirty) {
            clean();
        }
    }

    @Override
    public boolean render(boolean raytrace) {
        if (!render) {
            return false;
        }

        ClientLevel world = Minecraft.getInstance().level;

        if (world != null) {
            BlockPos lightBlockPos = new BlockPos((int) Math.floor(getPosition().x), (int) Math.floor(getPosition().y), (int) Math.floor(getPosition().z));
            Vector3f lightPos = new Vector3f((float) getPosition().x, (float) getPosition().y, (float) getPosition().z);
            int blockRadius = Vibrancy.capShadowDistance((int) Math.ceil(radius) - 2);
            BlockBox box = getBox();

            if (fullRebuildTask != null) {
                Vibrancy.NUM_LIGHT_TASKS++;
            }

            if (!dirty.isEmpty()) {
                for (BlockPos pos : dirty) {
                    if (!pos.equals(lightBlockPos)) {
                        regenQuads(world, pos, volumes::add, lightBlockPos, lightPos);
                    }

                    for (Direction dir : Direction.values()) {
                        BlockPos pos1 = pos.relative(dir);

                        if (!pos1.equals(lightBlockPos)) {
                            regenQuads(world, pos1, volumes::add, lightBlockPos, lightPos);
                        }
                    }
                }

                dirty.clear();

                BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

                for (ShadowVolume volume : volumes) {
                    volume.render(builder);
                }

                upload(builder, volumes);

                shadowCount = volumes.size();
            }

            if (fullRebuildTask != null && fullRebuildTask.isDone()) {
                try {
                    volumes = fullRebuildTask.get();
                    fullRebuildTask = null;
                    BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

                    for (ShadowVolume volume : volumes) {
                        volume.render(builder);
                    }

                    upload(builder, volumes);

                    shadowCount = volumes.size();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            } else if (raytrace) {
                if (blockRadius < 1) {
                    raytrace = false;
                } else {
                    if (isDirty()) {
                        clean();
                        fullRebuildTask = CompletableFuture.supplyAsync(() -> {
                            List<ShadowVolume> volumes = new LinkedList<>();

                            for (int x = box.min().getX(); x <= box.max().getX(); x++) {
                                for (int y = box.min().getY(); y <= box.max().getY(); y++) {
                                    for (int z = box.min().getZ(); z <= box.max().getZ(); z++) {
                                        BlockPos pos = new BlockPos(x, y, z);

                                        if (!pos.equals(lightBlockPos)) {
                                            double sqDist = pos.distSqr(lightBlockPos);

                                            if (sqDist != 0 && sqDist < blockRadius * blockRadius) {
                                                getVolumes(world, pos, volumes::add, sqDist, lightBlockPos, lightPos, radius, true);
                                            }
                                        }
                                    }
                                }
                            }

                            return volumes;
                        });
                    }
                }
            }

            if (isVisible()) {
                Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
                Matrix4f view = new Matrix4f()
                        .rotate(camera.rotation().invert(new Quaternionf()))
                        .translate((float) -camera.getPosition().x, (float) -camera.getPosition().y, (float) -camera.getPosition().z);

                renderMask(raytrace, lightPos, view);
                renderLight(lightPos, view);

                return true;
            }
        }

        return false;
    }

    @Override
    public final PointLight setPosition(Vector3dc position) {
        throw new UnsupportedOperationException("Can't move a block light");
    }

    @Override
    public final PointLight setPosition(double x, double y, double z) {
        throw new UnsupportedOperationException("Can't move a block light");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + blockPos.getX() + ", " + blockPos.getY() + ", " + blockPos.getZ() + "]";
    }
}
