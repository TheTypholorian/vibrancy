#version 430

#include "vibrancy:include/common"
#include "vibrancy:include/fragment"
//#include "veil:common"
//#include "veil:space_helper"
//#include "veil:light"

uniform sampler2D VibrancyWorldPosSampler;
uniform sampler2D VibrancyNormalSampler;

uniform mat4 IProjMat;
uniform mat4 IModelMat;

uniform vec2 ScreenSize;
uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;
uniform vec3 CameraPos;

out vec4 fragColor;

void main() {
    vec3 pos = texelFetch(VibrancyWorldPosSampler, ivec2(gl_FragCoord.xy), 0).xyz;

    fragColor = sampleLight(VibrancyNormalSampler, ScreenSize, LightPos, pos, LightRadius, LightColor);
}
