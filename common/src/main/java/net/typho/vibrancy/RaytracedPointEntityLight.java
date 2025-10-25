package net.typho.vibrancy;

import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3;

import java.awt.*;

public class RaytracedPointEntityLight extends AbstractMovingRaytracedLight {
    public final LivingEntity entity;

    public RaytracedPointEntityLight(LivingEntity entity) {
        this.entity = entity;
    }

    @Override
    public void init() {
        float tickDelta = Minecraft.getInstance().getRenderTickCounter().getTickDelta(true);
        Vec3 entityPos = entity.getCameraPosVec(tickDelta)
                .add(entity.getRotationVec(tickDelta));
        setPosition(entityPos.x, entityPos.y, entityPos.z);
    }

    @Override
    public boolean shouldRegenAll() {
        return !quadBox.contains(entity.getBlockPos());
    }

    public boolean init(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem block) {
            BlockState state = block.getBlock().getDefaultState();
            DynamicLightInfo info = DynamicLightInfo.get(state);

            if (info != null) {
                info.initLight(this, state);

                NativeImage lightmap = Minecraft.getInstance().gameRenderer.getLightmapTextureManager().image;
                Color color = new Color(lightmap.getColor(15, 0));
                this.color.set(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);

                hasLight = true;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean render(boolean raytrace) {
        if (!(init(entity.getMainHandStack()) || init(entity.getOffHandStack()))) {
            return false;
        }

        return super.render(raytrace);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + entity + "]";
    }
}
