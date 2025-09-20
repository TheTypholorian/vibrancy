package net.typho.vibrancy.mixin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourceFactory;
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

@Mixin(ShaderProgram.class)
public abstract class ShaderProgramMixin {
    @Shadow
    public abstract @Nullable GlUniform getUniform(String name);

    @Shadow
    public abstract void addSampler(String name, Object sampler);

    @Unique
    private GlUniform camPos;
    @Unique
    private GlUniform renderTime;

    @Inject(
            method = "<init>",
            at = @At("TAIL")
    )
    private void init(ResourceFactory factory, String name, VertexFormat format, CallbackInfo ci) {
        camPos = getUniform("CameraPos");
        renderTime = getUniform("RenderTime");
    }

    @Inject(
            method = "initializeUniforms",
            at = @At("TAIL")
    )
    private void initializeUniforms(VertexFormat.DrawMode drawMode, Matrix4f viewMatrix, Matrix4f projectionMatrix, Window window, CallbackInfo ci) {
        if (camPos != null) {
            Vector3f vec = MinecraftClient.getInstance().gameRenderer.getCamera().getPos().toVector3f();
            camPos.set(vec.z, vec.y, vec.x);
        }

        if (renderTime != null) {
            renderTime.set((float) GLFW.glfwGetTime());
        }
    }
}
