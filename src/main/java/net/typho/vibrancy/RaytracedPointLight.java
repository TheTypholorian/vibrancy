package net.typho.vibrancy;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

public class RaytracedPointLight extends AbstractRaytracedLight {
    protected final List<BlockPos> dirty = new LinkedList<>();
    protected boolean remove = false;
    protected List<ShadowVolume> volumes = new LinkedList<>();
    protected CompletableFuture<List<ShadowVolume>> fullRebuildTask;

    public void regenQuads(ClientWorld world, BlockPos pos, Consumer<ShadowVolume> out, BlockPos lightBlockPos, Vector3f lightPos) {
        volumes.removeIf(v -> v.caster().blockPos().equals(pos));
        getVolumes(world, pos, out, pos.getSquaredDistance(lightBlockPos), lightBlockPos, lightPos, radius, true);
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
    }

    @Override
    public boolean render(boolean raytrace) {
        ClientWorld world = MinecraftClient.getInstance().world;

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
                        BlockPos pos1 = pos.offset(dir);

                        if (!pos1.equals(lightBlockPos)) {
                            regenQuads(world, pos1, volumes::add, lightBlockPos, lightPos);
                        }
                    }
                }

                dirty.clear();

                BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

                for (ShadowVolume volume : volumes) {
                    volume.render(builder);
                }

                upload(builder, volumes);
            }

            if (fullRebuildTask != null && fullRebuildTask.isDone()) {
                try {
                    volumes = fullRebuildTask.get();
                    fullRebuildTask = null;
                    BufferBuilder builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);

                    for (ShadowVolume volume : volumes) {
                        volume.render(builder);
                    }

                    upload(builder, volumes);
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

                            for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
                                for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
                                    for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                                        BlockPos pos = new BlockPos(x, y, z);

                                        if (!pos.equals(lightBlockPos)) {
                                            double sqDist = pos.getSquaredDistance(lightBlockPos);

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
}
