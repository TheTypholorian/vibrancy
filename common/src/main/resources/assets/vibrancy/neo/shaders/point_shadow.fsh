#version 430

#include "vibrancy:include/common"
#include "vibrancy:include/fragment"
#include "vibrancy:include/downsize"

uniform sampler2D Sampler0;
uniform sampler2D VibrancyWorldPosSampler;

uniform mat4 IProjMat;
uniform mat4 IModelMat;

uniform vec3 LightPos;
uniform float LightRadius;
uniform vec2 ScreenSize;
uniform vec3 CameraPos;

in flat Triangle triangle;

out vec4 fragColor;

void main() {
    vec3 Pos = texelFetch(VibrancyWorldPosSampler, getScreenUV(), 0).xyz;

    vec3 delta = LightPos - Pos;
    float len = length(delta);

    vec3 dir = normalize(delta);

    // max((Pos.w - 16) / 128, 1e-3)

    if (sampleTriangle(Sampler0, Pos, dir, len, 1e-3, triangle)) {
        fragColor = vec4(0);
    } else {
        fragColor = vec4(1);
    }
}
