package net.typho.vibrancy.mixin;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.vertex.VertexFormat;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferManger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.server.packs.resources.ResourceProvider;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderInstance.class)
public abstract class ShaderProgramMixin {
    @Shadow
    @Nullable
    public abstract Uniform getUniform(String p_173349_);

    @Shadow
    public abstract void setSampler(String p_173351_, Object p_173352_);

    @Unique
    private Uniform camPos;
    @Unique
    private Uniform renderTime;
    @Unique
    private Uniform skyAngle;

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void init(ResourceProvider resourceProvider, String name, VertexFormat vertexFormat, CallbackInfo ci) {
        camPos = getUniform("CameraPos");
        renderTime = getUniform("RenderTime");
        skyAngle = getUniform("SkyAngle");
    }

    @Inject(
            method = "setDefaultUniforms",
            at = @At("TAIL")
    )
    private void setDefaultUniforms(VertexFormat.Mode mode, Matrix4f projectionMatrix, Matrix4f frustrumMatrix, Window window, CallbackInfo ci) {
        if (camPos != null) {
            Vector3f vec = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition().toVector3f();
            camPos.set(vec.z, vec.y, vec.x);
        }

        if (renderTime != null) {
            renderTime.set((float) GLFW.glfwGetTime());
        }

        if (skyAngle != null) {
            skyAngle.set(Minecraft.getInstance().level.getSunAngle(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false)));
        }

        DynamicBufferManger bufferManger = VeilRenderSystem.renderer().getDynamicBufferManger();
        for (DynamicBufferType dynamicBuffer : DynamicBufferType.values()) {
            setSampler(dynamicBuffer.getSourceName() + "Sampler", bufferManger.getBufferTexture(dynamicBuffer));
        }
    }
}
