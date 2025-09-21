package net.typho.vibrancy.mixin.client;

import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "me.pepperbell.continuity.client.model.EmissiveBakedModel$EmissiveBlockQuadTransform")
public class EmissiveBakedModelMixin {
    @Redirect(
            method = "transform",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/fabricmc/fabric/api/renderer/v1/mesh/QuadEmitter;material(Lnet/fabricmc/fabric/api/renderer/v1/material/RenderMaterial;)Lnet/fabricmc/fabric/api/renderer/v1/mesh/QuadEmitter;"
            )
    )
    private QuadEmitter transform(QuadEmitter instance, RenderMaterial renderMaterial) {
        /*
        instance.material(new RenderMaterial() {
            @Override
            public BlendMode blendMode() {
                return renderMaterial.blendMode();
            }

            @Override
            public boolean disableColorIndex() {
                return renderMaterial.disableColorIndex();
            }

            @Override
            public boolean emissive() {
                return false;
            }

            @Override
            public boolean disableDiffuse() {
                return renderMaterial.disableDiffuse();
            }

            @Override
            public TriState ambientOcclusion() {
                return renderMaterial.ambientOcclusion();
            }

            @Override
            public TriState glint() {
                return renderMaterial.glint();
            }
        });
         */

        for (int i = 0; i < 4; i++) {
            int l = instance.lightmap(i);
            int block = LightmapTextureManager.getBlockLightCoordinates(l);
            instance.lightmap(i, LightmapTextureManager.MAX_LIGHT_COORDINATE);//LightmapTextureManager.pack(block + (7 - block), LightmapTextureManager.getSkyLightCoordinates(l)));
        }

        return instance;
    }
}
