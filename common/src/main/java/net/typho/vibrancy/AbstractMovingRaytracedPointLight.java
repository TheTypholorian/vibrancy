package net.typho.vibrancy;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL15.GL_STREAM_DRAW;

public abstract class AbstractMovingRaytracedPointLight extends AbstractRaytracedPointLight {
    protected boolean hasLight = false;
    protected Map<BlockPos, List<Quad>> quads = new LinkedHashMap<>();
    protected final List<BlockPos> dirty = new LinkedList<>();
    protected BlockBox quadBox;
    protected CompletableFuture<Map<BlockPos, List<Quad>>> fullRebuildTask;

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

    public void regenQuadsSync(ClientLevel world, BlockPos pos, Consumer<Quad> out, BlockPos lightBlockPos) {
        quads.remove(pos);
        regenQuadsAsync(world, pos, out, lightBlockPos);
    }

    public void regenQuadsAsync(ClientLevel world, BlockPos pos, Consumer<Quad> out, BlockPos lightBlockPos) {
        getQuads(world, pos, out, pos.distSqr(lightBlockPos), lightBlockPos, false);
    }

    public void regenAll(ClientLevel world, BlockBox box, BlockPos lightBlockPos) {
        fullRebuildTask = CompletableFuture.supplyAsync(() -> {
            Map<BlockPos, List<Quad>> quads = new LinkedHashMap<>();

            for (int x = box.min().getX(); x <= box.max().getX(); x++) {
                for (int y = box.min().getY(); y <= box.max().getY(); y++) {
                    for (int z = box.min().getZ(); z <= box.max().getZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        List<Quad> existing = this.quads.get(pos);

                        if (existing != null && !existing.isEmpty()) {
                            quads.put(pos, existing);
                        } else {
                            List<Quad> list = new LinkedList<>();
                            regenQuadsAsync(world, pos, list::add, lightBlockPos);

                            if (!list.isEmpty()) {
                                quads.put(pos, list);
                            }
                        }
                    }
                }
            }

            return quads;
        });
    }

    @Override
    public void init() {
    }

    public abstract boolean shouldRegenAll();

    @Override
    public boolean render(boolean raytrace) {
        ClientLevel world = Minecraft.getInstance().level;

        if (world != null) {
            BlockPos lightBlockPos = new BlockPos((int) Math.floor(getPosition().x), (int) Math.floor(getPosition().y), (int) Math.floor(getPosition().z));
            Vector3f lightPos = new Vector3f((float) getPosition().x, (float) getPosition().y, (float) getPosition().z);
            int blockRadius = Vibrancy.capShadowDistance((int) Math.ceil(radius) - 2);
            BlockBox box = getBox();

            List<ShadowVolume> volumes = new LinkedList<>();
            BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);

            if (isVisible()) {
                if (fullRebuildTask != null && fullRebuildTask.isDone()) {
                    try {
                        quads = fullRebuildTask.get();
                        fullRebuildTask = null;
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                } else if (hasLight) {
                    if (quadBox == null || shouldRegenAll()) {
                        dirty.clear();
                        regenAll(world, blockRadius > 1 ? new BlockBox(
                                new BlockPos(box.min().getX() - blockRadius, box.min().getY() - blockRadius, box.min().getZ() - blockRadius),
                                new BlockPos(box.max().getX() + blockRadius, box.max().getY() + blockRadius, box.max().getZ() + blockRadius)
                        ) : box, lightBlockPos);
                        quadBox = box;
                    } else if (!dirty.isEmpty()) {
                        for (BlockPos pos : dirty) {
                            List<Quad> list = new LinkedList<>();
                            regenQuadsSync(world, pos, list::add, lightBlockPos);

                            for (Direction dir : Direction.values()) {
                                regenQuadsSync(world, pos.relative(dir), list::add, lightBlockPos);
                            }

                            if (!list.isEmpty()) {
                                quads.put(pos, list);
                            } else {
                                quads.remove(pos);
                            }
                        }
                    }
                }

                quads.forEach((pos, list) -> {
                    if (box.contains(pos)) {
                        for (Quad quad : list) {
                            ShadowVolume volume = quad.toVolume(lightPos, radius);
                            volume.render(builder);
                            volumes.add(volume);
                        }
                    }
                });

                upload(builder, volumes);

                shadowCount = volumes.size();

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
}
