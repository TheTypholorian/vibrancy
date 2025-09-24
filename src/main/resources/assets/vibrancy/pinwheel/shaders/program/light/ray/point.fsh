#version 150

#include "veil:deferred_utils"

uniform sampler2D DiffuseDepthSampler;
uniform sampler2D ShadowMaskBackSampler;
uniform sampler2D ShadowMaskBackDepthSampler;
uniform sampler2D ShadowMaskFrontSampler;
uniform sampler2D ShadowMaskFrontDepthSampler;

uniform vec2 ScreenSize;
uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;

out vec4 fragColor;

void main() {
    vec2 screenUv = gl_FragCoord.xy / ScreenSize;

    float depth = texture(DiffuseDepthSampler, screenUv).r;
    vec3 pos = viewToWorldSpace(viewPosFromDepth(depth, screenUv));

    fragColor = vec4(LightColor * clamp(1 - distance(pos, LightPos) / LightRadius, 0, 1) / 2, 1);

    float maskBackDepth = texture(ShadowMaskBackDepthSampler, screenUv).r;
    float maskFrontDepth = texture(ShadowMaskFrontDepthSampler, screenUv).r;

    float worldDepth = depthSampleToWorldDepth(depth);

    if (worldDepth > depthSampleToWorldDepth(maskFrontDepth) - 1e-3 && worldDepth < depthSampleToWorldDepth(maskBackDepth) + 1e-3) {
        fragColor.rgb *= 1 - texture(ShadowMaskBackSampler, screenUv).a;
    }
}
