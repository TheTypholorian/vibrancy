#version 430

#include "vibrancy:include/common"
#include "vibrancy:include/fragment"
//#include "veil:common"
//#include "veil:space_helper"
//#include "veil:light"

uniform sampler2D AtlasSampler;
uniform sampler2D DiffuseDepthSampler;

uniform mat4 IProjMat;
uniform mat4 IModelMat;

uniform vec3 LightPos;
uniform float LightRadius;
uniform vec2 ScreenSize;
uniform vec3 CameraPos;

in flat Triangle triangle;

out vec4 fragColor;

void main() {
    vec3 Pos = getWorldPos(DiffuseDepthSampler, ScreenSize, IProjMat, IModelMat, CameraPos).xyz;

    vec3 delta = LightPos - Pos;
    float len = length(delta);

    vec3 dir = normalize(delta);

    // max((Pos.w - 16) / 128, 1e-3)

    if (sampleTriangle(AtlasSampler, Pos, dir, len, 1e-3, triangle)) {
        discard;
    }
}
