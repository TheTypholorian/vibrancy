#version 430

#include "vibrancy:include/fragment"
#include "vibrancy:include/rays"

struct Light {
    vec3 color;
    vec3 pos;
};

layout(std430, binding = 0) buffer LightQuadBuffer {
    Quad lightQuads[];
};
layout(std430, binding = 1) buffer LightBuffer {
    Light lights[];
};

uniform float LightBrightness;

in vec3 vertexPos;

out vec4 fragColor;

void main() {
    /*
    Quad self = lightQuads[index];

    vec3 pos = interpolateQuadPos(self, mappedUV);

    fragColor = vec4(0);

    for (uint i = 0u; i < lights.length(); i++) {
        Light light = lights[i];

        fragColor = max(fragColor, sampleCubeLight(light.pos, pos, 0.5, 1.5, light.color * LightBrightness));
    }
    */
}
