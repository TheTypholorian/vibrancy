#version 430

#include "vibrancy:include/common"
#include "vibrancy:include/fragment"
//#include "veil:common"
//#include "veil:space_helper"
//#include "veil:light"

uniform sampler2D DiffuseDepthSampler;
uniform sampler2D VeilDynamicNormalSampler;

uniform vec2 ScreenSize;
uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;

out vec4 fragColor;

void main() {
    fragColor = vec4(LightColor, 1);
    //vec3 pos = getWorldPos(DiffuseDepthSampler, ScreenSize);

    //fragColor = sampleLight(VeilDynamicNormalSampler, LightPos, pos, LightRadius, LightColor);
}
