#version 430

#include "vibrancy:common"
#include "vibrancy:fragment"
#include "veil:common"
#include "veil:space_helper"
#include "veil:light"

uniform sampler2D AtlasSampler;
uniform sampler2D DiffuseDepthSampler;
uniform vec3 LightPos;
uniform float LightRadius;
uniform vec2 ScreenSize;

//in flat Quad quad;

out vec4 fragColor;

void main() {
    fragColor = vec4(1, 0, 0, 1);

    /*
    vec3 Pos = getWorldPos(DiffuseDepthSampler, ScreenSize);

    vec3 delta = LightPos - Pos.xyz;
    float len = length(delta);

    vec3 dir = delta / len;

    // max((Pos.w - 16) / 128, 1e-3)

    if (sampleQuad(AtlasSampler, Pos, dir, len, 1e-3, false, quad)) {
        discard;
    }
    */
}
