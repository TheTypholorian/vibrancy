#version 430

#include "vibrancy:include/common"
#include "vibrancy:include/fragment"
#include "vibrancy:include/downsize"

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
    vec2 uv = getScreenUV(ScreenSize);
    vec3 pos = texture(VibrancyWorldPosSampler, uv).xyz;

    fragColor = sampleLight(VibrancyNormalSampler, uv, LightPos, pos, LightRadius, LightColor);
}
