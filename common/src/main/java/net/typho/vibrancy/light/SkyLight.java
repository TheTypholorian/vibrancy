package net.typho.vibrancy.light;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import foundry.veil.api.client.render.VeilRenderSystem;
import net.minecraft.core.BlockPos;
import net.typho.vibrancy.Vibrancy;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

public class SkyLight implements RaytracedLight {
    public static @Nullable SkyLight INSTANCE = new SkyLight();

    @Override
    public void updateDirty(Iterable<BlockPos> it) {
    }

    @Override
    public void init() {
    }

    @Override
    public boolean render(boolean raytrace) {
        VeilRenderSystem.setShader(Vibrancy.id("light/ray/sky"));

        Vibrancy.SCREEN_VBO.bind();
        Vibrancy.SCREEN_VBO.drawWithShader(null, null, RenderSystem.getShader());
        VertexBuffer.unbind();

        return true;
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
