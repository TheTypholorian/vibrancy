#version 150

#include "veil:deferred_utils"

uniform sampler2D DiffuseDepthSampler;
uniform sampler2D ShadowMaskBackSampler;
uniform sampler2D ShadowMaskBackDepthSampler;
uniform sampler2D ShadowMaskFrontSampler;
uniform sampler2D ShadowMaskFrontDepthSampler;
uniform sampler2D BlockAtlasSampler;

uniform vec2 ScreenSize;
uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;

out vec4 fragColor;

void main() {
    vec2 screenUv = gl_FragCoord.xy / ScreenSize;

    float depth = texelFetch(DiffuseDepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    vec3 pos = viewToWorldSpace(viewPosFromDepth(depth, screenUv));

    fragColor = vec4(LightColor * clamp(1 - distance(pos, LightPos) / LightRadius, 0, 1) / 2, 1);

    float maskBackDepth = depthSampleToWorldDepth(texelFetch(ShadowMaskBackDepthSampler, ivec2(gl_FragCoord.xy), 0).r);
    float maskFrontDepth = depthSampleToWorldDepth(texelFetch(ShadowMaskFrontDepthSampler, ivec2(gl_FragCoord.xy), 0).r);

    float worldDepth = depthSampleToWorldDepth(depth);

    if (worldDepth > maskFrontDepth + 1e-3 && worldDepth < maskBackDepth - 1e-3) {
        vec4 backColor = texelFetch(ShadowMaskBackSampler, ivec2(gl_FragCoord.xy), 0);
        vec4 frontColor = texelFetch(ShadowMaskFrontSampler, ivec2(gl_FragCoord.xy), 0);

        if (backColor.a > 0) {
            float delta = (worldDepth - maskFrontDepth) / maskBackDepth;
            vec2 tc = (backColor.rg - frontColor.rg) * delta + frontColor.rg;
            vec4 sampleColor = texture(BlockAtlasSampler, tc);

            //if (sampleColor.a > 0) {
                fragColor.rgb *= 1 - backColor.a;
            //}
        }
    }
}
