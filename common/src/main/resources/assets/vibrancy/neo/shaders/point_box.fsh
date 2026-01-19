#version 430

#include "vibrancy:include/common"
#include "vibrancy:include/fragment"
#include "vibrancy:include/downsize"

uniform sampler2D VibrancyShadowSampler;
uniform sampler2D VibrancyWorldPosSampler;
uniform sampler2D VibrancyNormalSampler;

uniform mat4 IProjMat;
uniform mat4 IModelMat;

uniform vec2 ScreenSize;
uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;
uniform vec3 CameraPos;

uniform bool SampleShadows;

out vec4 fragColor;

void main() {
    vec2 uv = getScreenUV(ScreenSize);
    vec3 pos = texture(VibrancyWorldPosSampler, uv).xyz;
    vec3 shadow = 1 - texture(VibrancyShadowSampler, uv).rgb;

    fragColor = sampleLight(VibrancyNormalSampler, uv, LightPos, pos, LightRadius, LightColor * shadow);
}
