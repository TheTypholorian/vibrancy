#version 430

#include "vibrancy:include/light_blit"

struct Light {
    vec3 color;
    vec3 pos;
};

layout(std430, binding = 2) buffer LightBuffer {
    Light lights[];
};

uniform float LightBrightness;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    Quad self;
    vec2 mappedUV;

    lightBlitInit(self, mappedUV);

    vec3 pos = interpolateQuadPos(self, mappedUV);

    fragColor = vec4(0);

    for (uint i = 0u; i < lights.length(); i++) {
        Light light = lights[i];

        fragColor = max(fragColor, sampleCubeLight(light.pos, pos, 0.5, 1.5, light.color * LightBrightness));
    }
}
