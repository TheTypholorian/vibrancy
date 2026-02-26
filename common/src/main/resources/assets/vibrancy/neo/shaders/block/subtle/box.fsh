#version 430

#include "vibrancy:include/fragment"

struct Light {
    vec3 color;
    vec3 pos;
};

layout(std430, binding = 0) buffer LightBuffer {
    Light lights[];
};

uniform sampler2D VibrancyWorldPosSampler;

uniform vec2 ScreenSize;
uniform float LightRadius;
uniform float LightBrightness;
uniform vec3 CameraPos;

flat in uint id;

out vec4 fragColor;

void main() {
    vec3 pos = texelFetch(VibrancyWorldPosSampler, ivec2(gl_FragCoord.xy), 0).xyz;

    fragColor = sampleLight(ScreenSize, lights[id].pos, pos, LightRadius, lights[id].color * LightBrightness);
}
