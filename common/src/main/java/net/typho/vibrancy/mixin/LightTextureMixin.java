package net.typho.vibrancy.mixin;

import net.minecraft.client.renderer.LightTexture;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(LightTexture.class)
public class LightTextureMixin {
    @ModifyVariable(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LightTexture;getBrightness(Lnet/minecraft/world/level/dimension/DimensionType;I)F",
                    ordinal = 1
            ),
            print = true,
            ordinal = 0
    )
    private float getBrightness(float value) {
        return value * Vibrancy.BLOCK_LIGHT_MULTIPLIER.get().floatValue();
    }

    /*
    @Shadow
    @Final
    private NativeImage image;

    @Shadow
    private boolean dirty;

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    @Final
    private NativeImageBackedTexture texture;

    @Unique
    private float temp, humid;

    @WrapMethod(
            method = "update"
    )
    private void update(float delta, Operation<Void> original) {
        if (Vibrancy.DYNAMIC_LIGHTMAP.getValue()) {
            if (client.world != null && client.player != null) {
                Biome biome = client.world.getBiome(client.player.getBlockPos()).value();

                temp = MathHelper.lerp(delta / 50, temp, biome.getTemperature());
                humid = MathHelper.lerp(delta / 50, humid, biome.weather.downfall());

                if (dirty) {
                    dirty = false;

                    client.getProfiler().push("lightTex");

                    Vibrancy.createLightmap(client.world, client.player, client.options, image, temp, humid, delta);

                    texture.upload();
                    client.getProfiler().pop();

                    if (Vibrancy.SAVE_LIGHTMAP != null && Vibrancy.SAVE_LIGHTMAP.isPressed()) {
                        try (NativeImage big = new NativeImage(1024, 1024, false)) {
                            Vibrancy.createLightmap(client.world, client.player, client.options, big, temp, humid, delta);
                            File file = new File("lightmap.png").getAbsoluteFile();
                            big.writeTo(file);
                            client.player.sendMessage(Text.translatable("debug.vibrancy.save_lightmap", file));
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        } else {
            original.call(delta);

            if (client.player != null && Vibrancy.SAVE_LIGHTMAP != null && Vibrancy.SAVE_LIGHTMAP.isPressed()) {
                try {
                    File file = new File("lightmap.png").getAbsoluteFile();
                    image.writeTo(file);
                    client.player.sendMessage(Text.translatable("debug.vibrancy.save_lightmap", file));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
    */
}
