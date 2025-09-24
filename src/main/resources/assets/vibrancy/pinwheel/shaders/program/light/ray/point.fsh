#version 430

#include "veil:deferred_utils"

uniform sampler2D DiffuseDepthSampler;
uniform sampler2D ShadowMaskBackSampler;
uniform sampler2D ShadowMaskBackDepthSampler;
uniform sampler2D ShadowMaskFrontDepthSampler;
uniform sampler2D VeilDynamicNormalSampler;

uniform vec2 ScreenSize;
uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;

out vec4 fragColor;

void main() {
    vec2 screenUv = gl_FragCoord.xy / ScreenSize;

    float depth = texelFetch(DiffuseDepthSampler, ivec2(gl_FragCoord.xy), 0).r;

    float maskBackDepth = depthSampleToWorldDepth(texelFetch(ShadowMaskBackDepthSampler, ivec2(gl_FragCoord.xy), 0).r);
    float maskFrontDepth = depthSampleToWorldDepth(texelFetch(ShadowMaskFrontDepthSampler, ivec2(gl_FragCoord.xy), 0).r);

    float worldDepth = depthSampleToWorldDepth(depth);

    if (maskFrontDepth > maskBackDepth) {
        if (worldDepth < maskBackDepth - 1e-3) {
            discard;
        }
    } else {
        if (worldDepth > maskFrontDepth + 1e-3 && worldDepth < maskBackDepth - 1e-3) {
            discard;
        }
    }

    vec3 pos = viewToWorldSpace(viewPosFromDepth(depth, screenUv));

    fragColor = vec4(LightColor * clamp(1 - distance(pos, LightPos) / LightRadius, 0, 1) / 2, 1);
}
