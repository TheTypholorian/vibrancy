#version 430

layout(early_fragment_tests) in;

#include vibrancy:mask_utils
#include veil:deferred_utils
#include vibrancy:quads

uniform sampler2D AtlasSampler;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D VeilDynamicNormalSampler;
uniform sampler2D WorldPositionSampler;
uniform vec3 LightDirection;
uniform vec2 ScreenSize;
uniform float MaxLength;

in flat Quad quad;

out vec4 fragColor;

void main() {
    fragColor = vec4(1);

    vec4 Pos = texelFetch(WorldPositionSampler, ivec2(gl_FragCoord.xy), 0);

    if (testMask(AtlasSampler, Pos.xyz, LightDirection, MaxLength, max((Pos.w - 16) / 128, 1e-3), false, quad)) {
        discard;
    }
}