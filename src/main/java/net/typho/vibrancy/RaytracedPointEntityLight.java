package net.typho.vibrancy;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import static org.lwjgl.opengl.GL30.GL_STREAM_DRAW;

public class RaytracedPointEntityLight extends AbstractRaytracedLight {
    public final LivingEntity entity;
    protected boolean hasLight = false;
    @ApiStatus.Internal
    public boolean render = false;
    protected Map<BlockPos, List<Quad>> quads = new LinkedHashMap<>();
    protected final List<BlockPos> dirty = new LinkedList<>();
    protected BlockBox quadBox;
    protected CompletableFuture<Map<BlockPos, List<Quad>>> fullRebuildTask;

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
        quads.remove(pos);
        regenQuadsAsync(world, pos, out, lightBlockPos, lightPos);
    }

    public void regenQuadsAsync(ClientWorld world, BlockPos pos, Consumer<Quad> out, BlockPos lightBlockPos, Vector3f lightPos) {
        getQuads(world, pos, out, pos.getSquaredDistance(lightBlockPos), lightBlockPos, lightPos, false);
    }

    public void regenAll(ClientWorld world, BlockBox box, BlockPos lightBlockPos, Vector3f lightPos) {
        fullRebuildTask = CompletableFuture.supplyAsync(() -> {
            Map<BlockPos, List<Quad>> quads = new LinkedHashMap<>();

            for (int x = box.getMinX(); x <= box.getMaxX(); x++) {
                for (int y = box.getMinY(); y <= box.getMaxY(); y++) {
                    for (int z = box.getMinZ(); z <= box.getMaxZ(); z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        List<Quad> existing = this.quads.get(pos);

                        if (existing != null && !existing.isEmpty()) {
                            quads.put(pos, existing);
                        } else {
                            List<Quad> list = new LinkedList<>();
                            regenQuadsAsync(world, pos, list::add, lightBlockPos, lightPos);

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

    public boolean init(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem block) {
            BlockState state = block.getBlock().getDefaultState();
            DynamicLightInfo info = DynamicLightInfo.get(state);

            if (info != null) {
                info.initLight(this, state);
                hasLight = true;
                render = true;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean render(boolean raytrace) {
        if (!render) {
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
                        regenAll(world, blockRadius > 1 ? box.expand(blockRadius) : box, lightBlockPos, lightPos);
                        quadBox = box;
                    } else if (!dirty.isEmpty()) {
                        for (BlockPos pos : dirty) {
                            List<Quad> list = new LinkedList<>();
                            regenQuadsSync(world, pos, list::add, lightBlockPos, lightPos);

                            for (Direction dir : Direction.values()) {
                                regenQuadsSync(world, pos.offset(dir), list::add, lightBlockPos, lightPos);
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
    public String toString() {
        return getClass().getSimpleName() + "[" + entity + "]";
    }
}
