package net.typho.vibrancy.light;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.typho.vibrancy.DynamicLightInfo;

public class EntityPointLight extends AbstractMovingPointLight {
    public final LivingEntity entity;

    public EntityPointLight(net.minecraft.world.entity.LivingEntity entity) {
        this.entity = entity;
    }

    @Override
    public boolean shouldRemove() {
        return entity.isRemoved();
    }

    @Override
    public boolean shouldRender() {
        return super.shouldRender() || entity == Minecraft.getInstance().cameraEntity;
    }

    @Override
    public void init() {
        float tickDelta = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        Vec3 entityPos = entity.getEyePosition(tickDelta)
                .add(entity.getViewVector(tickDelta));
        setPosition(entityPos.x, entityPos.y, entityPos.z);
    }

    @Override
    public boolean shouldRegenAll() {
        return !quadBox.contains(entity.blockPosition());
    }

    public boolean init(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem block) {
            BlockState state = block.getBlock().defaultBlockState();
            DynamicLightInfo info = DynamicLightInfo.get(state);

            if (info != null) {
                info.initLight(this, state);

                //NativeImage lightmap = Minecraft.getInstance().gameRenderer.lightTexture().lightPixels;
                //Color color = new Color(lightmap.getPixelRGBA(15, 0));
                //this.color.set(color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f);

                hasLight = true;
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean render(boolean raytrace) {
        if (!(init(entity.getMainHandItem()) || init(entity.getOffhandItem()))) {
            return false;
        }

        return super.render(raytrace);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[" + entity + "]";
    }
}
