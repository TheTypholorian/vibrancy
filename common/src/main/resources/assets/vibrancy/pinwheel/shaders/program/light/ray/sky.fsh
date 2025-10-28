#version 430

#include veil:common
#include veil:deferred_utils
#include veil:color_utilities
#include veil:light

uniform sampler2D DiffuseDepthSampler;
uniform sampler2D ShadowMaskSampler;
uniform sampler2D VeilDynamicNormalSampler;
uniform sampler2D VeilDynamicUVSampler;

uniform vec2 ScreenSize;
uniform vec3 LightColor;
uniform vec3 LightDirection;

out vec4 fragColor;

void main() {
    float scale = texelFetch(VeilDynamicUVSampler, ivec2(gl_FragCoord.xy), 0).g;

    vec3 pos = viewToWorldSpace(viewPosFromDepth(texelFetch(DiffuseDepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.xy / ScreenSize));

    vec4 shadow = texelFetch(ShadowMaskSampler, ivec2(gl_FragCoord.xy), 0);

    if (shadow.a == 1) {
        discard;
    }

    fragColor = vec4(clamp(dot(normalize(texelFetch(VeilDynamicNormalSampler, ivec2(gl_FragCoord.xy), 0).xyz), (VeilCamera.ViewMat * vec4(LightDirection, 0)).xyz), 0, 1) * scale * LightColor, 1);
}
