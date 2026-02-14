#version 430

#include "vibrancy:include/fragment"
#include "vibrancy:block/raytraced/shadow_utils"

uniform samplerCubeShadow VibrancyShadowSampler;
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
uniform ivec2 ShadowTextureSize;

out vec4 fragColor;

void main() {
    vec3 pos = texelFetch(VibrancyWorldPosSampler, ivec2(gl_FragCoord.xy), 0).xyz;
    vec3 color = LightColor;

    if (SampleShadows) {
        vec3 delta = pos - LightPos;
        //vec2 shadowUV = directionToShadowCoords(normalize(delta), vec2(ShadowTextureSize));
        float shadow = texture(VibrancyShadowSampler, vec4(normalize(delta), length(delta) / LightRadius)).r;

        if (shadow == 0) {
            discard;
        }

        color *= shadow;
    }

    fragColor = sampleLight(VibrancyNormalSampler, gl_FragCoord.xy / ScreenSize, LightPos, pos, LightRadius, color);
}
