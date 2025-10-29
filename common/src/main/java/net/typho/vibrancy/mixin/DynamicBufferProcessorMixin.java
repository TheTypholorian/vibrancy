package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.glsl.GlslInjectionPoint;
import foundry.veil.api.glsl.GlslParser;
import foundry.veil.api.glsl.GlslSyntaxException;
import foundry.veil.api.glsl.node.GlslNodeList;
import foundry.veil.api.glsl.node.GlslTree;
import foundry.veil.api.glsl.node.expression.GlslAssignmentNode;
import foundry.veil.api.glsl.node.variable.GlslNewNode;
import foundry.veil.api.glsl.node.variable.GlslVariableNode;
import foundry.veil.impl.client.render.dynamicbuffer.DynamicBufferProcessor;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(value = DynamicBufferProcessor.class, remap = false)
public class DynamicBufferProcessorMixin {
    @Shadow
    @Final
    private Object2IntMap<String> validBuffers;

    @Inject(
            method = "modify",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lfoundry/veil/api/client/render/shader/processor/ShaderPreProcessor$MinecraftContext;shaderInstance()Ljava/lang/String;"
            )
    )
    private void modify(ShaderPreProcessor.Context ctx, GlslTree tree, CallbackInfo ci, @Local(ordinal = 2) LocalBooleanRef modified, @Local VertexFormat format, @Local(ordinal = 0) GlslNodeList mainFunctionBody, @Local(ordinal = 1) GlslNodeList treeBody, @Local DynamicBufferType type, @Local(ordinal = 1) String output, @Local(ordinal = 2) String shaderName) throws GlslSyntaxException {
        if (type == Vibrancy.POSITION_BUFFER_TYPE) {
            if (format.contains(VertexFormatElement.POSITION)) {
                if (ctx.isVertex()) {
                    Optional<GlslNewNode> position = tree.field(format.getElementName(VertexFormatElement.POSITION));

                    if (position.isPresent()) {
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec3 Pass" + type.getSourceName()));
                        mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode("Pass" + type.getSourceName()), GlslParser.parseExpression("(ModelViewMat * vec4(" + position.get().getName() + ", 1.0)).xyz"), GlslAssignmentNode.Operand.EQUAL));
                        modified.set(true);
                        validBuffers.computeInt(shaderName, (unused, mask) -> (mask != null ? mask : 0) | type.getMask());
                    }
                } else if (ctx.isFragment()) {
                    if ((validBuffers.getInt(shaderName) & type.getMask()) != 0) {
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec3 Pass" + type.getSourceName()));
                        treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression(output));
                        mainFunctionBody.add(new GlslAssignmentNode(new GlslVariableNode(type.getSourceName()), GlslParser.parseExpression("Pass" + type.getSourceName()), GlslAssignmentNode.Operand.EQUAL));
                        modified.set(true);
                    }
                }
            }
        }
    }
}
