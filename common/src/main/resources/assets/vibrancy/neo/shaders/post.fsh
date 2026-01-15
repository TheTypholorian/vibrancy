#version 430

#include "minecraft:fog"
#include "vibrancy:include/fragment"

uniform sampler2D DiffuseSampler0;
uniform sampler2D VibrancyWorldPosSampler;
uniform sampler2D VibrancyOutputSampler;
uniform sampler2D VibrancyAlbedoSampler;

uniform mat4 IProjMat;
uniform mat4 IModelMat;

uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

uniform vec2 ScreenSize;
uniform vec3 CameraPos;

in vec2 uv;

out vec4 fragColor;

void main() {
    vec3 outputColor = texelFetch(VibrancyOutputSampler, ivec2(gl_FragCoord.xy), 0).rgb;
    float outputScale = min(1, 1 / max(outputColor.r, max(outputColor.g, outputColor.b)));
    vec3 pos = texelFetch(VibrancyWorldPosSampler, ivec2(gl_FragCoord.xy), 0).xyz;
    vec4 finalColor = linear_fog(vec4(outputColor * outputScale, 1), distance(pos, CameraPos), FogStart, FogEnd, FogColor);

    fragColor = texelFetch(DiffuseSampler0, ivec2(gl_FragCoord.xy), 0) + finalColor * texelFetch(VibrancyAlbedoSampler, ivec2(gl_FragCoord.xy), 0);
}
