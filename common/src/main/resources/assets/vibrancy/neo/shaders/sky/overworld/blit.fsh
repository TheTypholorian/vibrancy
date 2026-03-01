#version 430

#include "vibrancy:include/fragment"

//uniform sampler2D VibrancyShadowSampler;
uniform sampler2D VibrancyWorldPosSampler;
uniform sampler2D VibrancyNormalSampler;

uniform vec2 ScreenSize;
uniform vec3 LightColor;
uniform vec3 LightDirection;
uniform float LightLength;
uniform vec3 CameraPos;

//uniform float ShadowMultiplier = 0;
//uniform ivec2 ShadowTextureSize;

out vec4 fragColor;

void main() {
    //vec3 pos = texelFetch(VibrancyWorldPosSampler, ivec2(gl_FragCoord.xy), 0).xyz;

    fragColor = sampleSkyLight(VibrancyNormalSampler, gl_FragCoord.xy / ScreenSize, LightDirection, LightColor);

    //vec3 delta = pos - LightPos;
    //uint face;
    //vec2 shadowUV = directionToShadowCoords(normalize(delta), vec2(ShadowTextureSize), face);
    //float shadow = texture(VibrancyShadowSampler, shadowUV).r;

    //if (shadow * LightRadius + 2e-2 <= length(delta)) {
    //    fragColor *= ShadowMultiplier;
    //}
}
