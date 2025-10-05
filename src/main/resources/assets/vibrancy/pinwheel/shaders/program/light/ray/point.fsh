#version 430

#include "veil:common"
#include "veil:deferred_utils"
#include "veil:color_utilities"
#include "veil:light"

uniform sampler2D DiffuseDepthSampler;
uniform sampler2D ShadowMaskSampler;
uniform sampler2D VeilDynamicNormalSampler;

uniform vec2 ScreenSize;
uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;
uniform bool AnyShadows;

out vec4 fragColor;

void applyShadowColor(vec4 color) {
    if (color.a == 1) {
        discard;
    } else {
        // wip code for blocks tinting light
        //fragColor = vec4(fragColor.rgb * mix(color.rgb, vec3(0), color.a), fragColor.a);
    }
}

void main() {
    vec3 pos = viewToWorldSpace(viewPosFromDepth(texelFetch(DiffuseDepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.xy / ScreenSize));

    fragColor = vec4(clamp(dot(normalize(texelFetch(VeilDynamicNormalSampler, ivec2(gl_FragCoord.xy), 0).xyz), normalize((VeilCamera.ViewMat * vec4(LightPos - pos, 0.0)).xyz)), 0, 1) * attenuate_no_cusp(length(LightPos - pos), LightRadius) * LightColor, 1);

    if (AnyShadows) {
        applyShadowColor(texelFetch(ShadowMaskSampler, ivec2(gl_FragCoord.xy), 0));
    }
}
