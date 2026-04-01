#version 430

#include "vibrancy:include/fragment.glsl"
#include "vibrancy:include/rays.glsl"

struct Light {
    vec3 pos;
    vec3 color;
};

layout(std430, binding = 0) buffer LightBuffer {
    Light lights[];
};

uniform float LightBrightness;

in vec3 vertexPos;

out vec4 fragColor;

void main() {
    fragColor = vec4(0);

    for (uint i = 0u; i < lights.length(); i++) {
        Light light = lights[i];

        fragColor = max(fragColor, sampleCubeLight(light.pos, vertexPos, 0.5, 1.5, light.color * LightBrightness));
    }
}
