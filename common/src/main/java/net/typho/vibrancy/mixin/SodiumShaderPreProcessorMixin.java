package net.typho.vibrancy.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import foundry.veil.api.client.render.dynamicbuffer.DynamicBufferType;
import foundry.veil.api.client.render.shader.processor.ShaderPreProcessor;
import foundry.veil.api.glsl.GlslInjectionPoint;
import foundry.veil.api.glsl.GlslParser;
import foundry.veil.api.glsl.GlslSyntaxException;
import foundry.veil.api.glsl.node.GlslNode;
import foundry.veil.api.glsl.node.GlslNodeList;
import foundry.veil.api.glsl.node.GlslTree;
import foundry.veil.api.glsl.node.expression.GlslAssignmentNode;
import foundry.veil.api.glsl.node.variable.GlslVariableNode;
import foundry.veil.impl.compat.sodium.SodiumShaderPreProcessor;
import net.typho.vibrancy.Vibrancy;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = SodiumShaderPreProcessor.class, remap = false)
public class SodiumShaderPreProcessorMixin {
    @Inject(
            method = "modify",
            at = @At(
                    value = "INVOKE_ASSIGN",
                    target = "Lfoundry/veil/api/glsl/grammar/GlslTypeSpecifier$BuiltinType;getSourceString()Ljava/lang/String;"
            )
    )
    private void modify(ShaderPreProcessor.Context ctx, GlslTree tree, CallbackInfo ci, @Local DynamicBufferType type, @Local GlslNodeList treeBody, @Local List<GlslNode> mainBody, @Local LocalBooleanRef modified, @Local(ordinal = 0) String sourceName, @Local(ordinal = 1) int i) throws GlslSyntaxException {
        if (type == Vibrancy.POSITION_BUFFER_TYPE) {
            if (ctx.isVertex()) {
                treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("out vec3 PassVibrancyPosition"));
                mainBody.add(new GlslAssignmentNode(new GlslVariableNode("PassVibrancyPosition"), GlslParser.parseExpression("(ModelViewMat * vec4(_vert_position, 1.0)).xyz"), GlslAssignmentNode.Operand.EQUAL));
                modified.set(true);
            }

            if (ctx.isFragment()) {
                treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("in vec3 PassVibrancyPosition"));
                treeBody.add(GlslInjectionPoint.BEFORE_MAIN, GlslParser.parseExpression("layout(location = " + (1 + i) + ") out " + type.getType().getSourceString() + " " + sourceName));
                mainBody.add(GlslParser.parseExpression(sourceName + " = PassVibrancyPosition"));
                modified.set(true);
            }
        }
    }
}
