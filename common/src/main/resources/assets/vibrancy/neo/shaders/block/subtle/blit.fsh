#version 430

#include "vibrancy:include/light_blit"

struct Light {
    vec3 color;
    vec3 pos;
};

layout(std140, binding = 2) buffer LightBuffer {
    Light lights[];
};

uniform float LightRadius;
uniform float LightBrightness;
uniform vec2 ScreenSize;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    Quad self;
    vec2 mappedUV;
    vec2 step;

    lightBlitInit(self, mappedUV, step);

    vec3 pos = interpolateQuadPos(self, mappedUV);

    fragColor = vec4(0);

    for (uint i = 0u; i < lights.length(); i++) {
        Light light = lights[i];

        fragColor += samplePointLight(ScreenSize, light.pos, pos, LightRadius, light.color * LightBrightness);
    }
}
