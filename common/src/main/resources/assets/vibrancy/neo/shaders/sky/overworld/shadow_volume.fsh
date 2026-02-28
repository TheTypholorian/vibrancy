#version 430

#include "vibrancy:include/rays"

uniform sampler2D Sampler0;
uniform sampler2D VibrancyWorldPosSampler;

uniform vec3 LightDirection;
uniform float LightLength;
uniform vec2 ScreenSize;
uniform vec3 CameraPos;

in flat Triangle triangle;

out vec4 fragColor;

void main() {
    vec3 Pos = texelFetch(VibrancyWorldPosSampler, ivec2(gl_FragCoord.xy), 0).xyz;

    float dist;

    // max((Pos.w - 16) / 128, 1e-3)

    if (sampleTriangle(Sampler0, Pos, LightDirection, LightLength, 4e-3, triangle, dist)) {
        discard;
    }
}