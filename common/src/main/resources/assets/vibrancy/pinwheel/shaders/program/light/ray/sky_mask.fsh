#version 430

layout(early_fragment_tests) in;

#include vibrancy:mask_utils
#include veil:deferred_utils
#include vibrancy:quads

uniform sampler2D AtlasSampler;
uniform sampler2D WorldPosSampler;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D VeilDynamicNormalSampler;
uniform vec3 LightDirection;
uniform vec2 ScreenSize;
uniform float MaxLength;

in flat Quad quad;

out vec4 fragColor;

void main() {
    fragColor = vec4(1);

    vec3 Pos = getWorldPos(DiffuseDepthSampler, ivec2(gl_FragCoord.xy), ScreenSize);
4
    if (testMask(AtlasSampler, Pos, LightDirection, MaxLength, quad)) {
        discard;
    }
}