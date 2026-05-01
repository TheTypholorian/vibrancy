#version 430

#include "vibrancy:fragment"
//#include "big_shot_lib:fog"

float linear_fog_fade(float vertexDistance, float fogStart, float fogEnd) {
    if (vertexDistance <= fogStart) {
        return 1.0;
    } else if (vertexDistance >= fogEnd) {
        return 0.0;
    }

    return smoothstep(fogEnd, fogStart, vertexDistance);
}

struct Light {
    vec3 pos;
    vec3 color;
};

layout(std430, binding = 0) buffer LightBuffer {
    Light lights[];
};

uniform float FogStart;
uniform float FogEnd;

uniform sampler2D Sampler0;
//uniform sampler2D Sampler1;
uniform ivec2 Sampler0Size;
uniform float LightBrightness;

uniform vec3 CameraPos;

in vec2 texCoord0;
//in vec2 texCoord1;
in uint lightIndex;
in vec4 vertexColor;
in vec3 vertexPosition;
in float vertexDistance;

out vec3 fragColor;

void main() {
    vec4 block = texelFetch(Sampler0, ivec2(texCoord0 * Sampler0Size), 0) * vertexColor;

    if (block.a == 0) {
        discard;
    }

    Light light = lights[lightIndex];
    fragColor = block.rgb * block.a * LightBrightness * sampleCubeLight(light.pos, vertexPosition, 0.5, 1.5, light.color) * linear_fog_fade(vertexDistance, FogStart, FogEnd);
}
