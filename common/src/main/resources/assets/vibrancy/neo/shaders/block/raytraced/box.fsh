#version 430

#include "vibrancy:include/fragment"
#include "vibrancy:block/raytraced/shadow_utils"

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

out vec4 fragColor;

void main() {
    vec3 pos = texelFetch(VibrancyWorldPosSampler, ivec2(gl_FragCoord.xy), 0).xyz;

    vec3 delta = pos - LightPos;
    vec2 shadowUV = directionToShadowCoords(normalize(delta));
    float shadow = texture(VibrancyShadowSampler, shadowUV).r;

    // TODO fix margin
    if (shadow * LightRadius + 1e-1 <= length(delta)) {
        discard;
    }

    fragColor = sampleLight(VibrancyNormalSampler, gl_FragCoord.xy / ScreenSize, LightPos, pos, LightRadius, LightColor);
}
