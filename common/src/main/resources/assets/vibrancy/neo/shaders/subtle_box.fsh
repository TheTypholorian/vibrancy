#version 430

#include "vibrancy:include/common"
#include "vibrancy:include/fragment"
#include "vibrancy:include/downsize"

struct Light {
    vec3 color;
    vec3 pos;
};

layout(std430, binding = 0) buffer LightBuffer {
    Light lights[];
};

uniform sampler2D VibrancyWorldPosSampler;

uniform mat4 IProjMat;
uniform mat4 IModelMat;

uniform vec2 ScreenSize;
uniform float LightRadius;
uniform vec3 CameraPos;

flat in uint id;

out vec4 fragColor;

void main() {
    vec3 pos = texture(VibrancyWorldPosSampler, getScreenUV(ScreenSize)).xyz;

    fragColor = sampleLight(ScreenSize, lights[id].pos, pos, LightRadius, lights[id].color);
}
