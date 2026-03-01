#version 150

#include "big_shot_lib:fog"
#include "vibrancy:include/fragment"

uniform sampler2D DiffuseSampler0;
uniform sampler2D VibrancyWorldPosSampler;
uniform sampler2D VibrancyOutputSampler;

uniform vec2 ScreenSize;
uniform vec3 CameraPos;
uniform float LightBrightnessLimit;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec3 outputColor = texelFetch(VibrancyOutputSampler, ivec2(gl_FragCoord.xy), 0).rgb;
    float outputScale = min(LightBrightnessLimit, LightBrightnessLimit / max(outputColor.r, max(outputColor.g, outputColor.b)));
    vec3 pos = texelFetch(VibrancyWorldPosSampler, ivec2(gl_FragCoord.xy), 0).xyz;
    vec4 finalColor = bigShotFog(vec4(outputColor * outputScale, 1), pos - CameraPos, vec4(0));

    // TODO
    fragColor = texelFetch(DiffuseSampler0, ivec2(gl_FragCoord.xy), 0) + finalColor;// * texelFetch(VibrancyAlbedoSampler, ivec2(gl_FragCoord.xy), 0);
}
