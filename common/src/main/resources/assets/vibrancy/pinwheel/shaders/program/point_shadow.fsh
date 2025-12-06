#version 430

layout(early_fragment_tests) in;

#include "vibrancy:common"
#include "vibrancy:fragment"
#include "veil:common"
#include "veil:space_helper"
#include "veil:light"

uniform sampler2D AtlasSampler;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D VeilDynamicNormalSampler;
uniform vec3 LightPos;
uniform float LightRadius;
uniform vec2 ScreenSize;

//in flat Quad quad;

out vec4 fragColor;

void main() {
    fragColor = vec4(1);

    /*
    vec3 Pos = getWorldPos(DiffuseDepthSampler, ScreenSize);

    vec3 delta = LightPos - Pos.xyz;
    float len = length(delta);

    vec3 dir = delta / len;

    if (sampleQuad(AtlasSampler, Pos.xyz, dir, len, max((Pos.w - 16) / 128, 1e-3), false, quad)) {
        discard;
    }
    */
}
