#version 430

layout(early_fragment_tests) in;

#include vibrancy:mask_utils
#include veil:deferred_utils
#include vibrancy:quads

uniform sampler2D AtlasSampler;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D VeilDynamicNormalSampler;
uniform sampler2D WorldPositionSampler;
uniform vec3 LightPos;
uniform float LightRadius;
uniform vec2 ScreenSize;

in flat Quad quad;

out vec4 fragColor;

void main() {
    fragColor = vec4(1);

    vec4 Pos = texelFetch(WorldPositionSampler, ivec2(gl_FragCoord.xy), 0);

    vec3 delta = LightPos - Pos.xyz;
    float len = length(delta);

    vec3 dir = delta / len;

    if (testMask(AtlasSampler, Pos.xyz, dir, len, Pos.w / 128, true, quad)) {
        discard;
    }
}
