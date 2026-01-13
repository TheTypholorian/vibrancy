#version 430

#include "vibrancy:include/common"
#include "vibrancy:include/fragment"
//#include "veil:common"
//#include "veil:space_helper"
//#include "veil:light"

uniform sampler2D DiffuseDepthSampler;

uniform mat4 IProjMat;
uniform mat4 IModelMat;

uniform vec2 ScreenSize;
uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;
uniform vec3 CameraPos;

out vec4 fragColor;

void main() {
    vec3 pos = getWorldPos(DiffuseDepthSampler, ScreenSize, IProjMat, IModelMat, CameraPos).xyz;

    fragColor = sampleLight(ScreenSize, LightPos, pos, LightRadius, LightColor);
}
