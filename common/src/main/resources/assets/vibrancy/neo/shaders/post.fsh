#version 430

#include "big_shot_lib:fog"
#include "vibrancy:include/fragment"

uniform sampler2D DiffuseSampler0;
uniform sampler2D VibrancyWorldPosSampler;
uniform sampler2D VibrancyOutputSampler;
uniform sampler2D VibrancyAlbedoSampler;

uniform mat4 IProjMat;
uniform mat4 IModelMat;

uniform vec2 ScreenSize;
uniform vec3 CameraPos;

in vec2 uv;

out vec4 fragColor;

void main() {
    vec3 outputColor = texture(VibrancyOutputSampler, gl_FragCoord.xy / ScreenSize).rgb;
    float outputScale = min(1, 1 / max(outputColor.r, max(outputColor.g, outputColor.b)));
    vec3 pos = texelFetch(VibrancyWorldPosSampler, ivec2(gl_FragCoord.xy), 0).xyz;
    vec4 finalColor = bigShotFog(vec4(1), pos - CameraPos) * vec4(outputColor * outputScale, 1);

    fragColor = texelFetch(DiffuseSampler0, ivec2(gl_FragCoord.xy), 0) + finalColor * texelFetch(VibrancyAlbedoSampler, ivec2(gl_FragCoord.xy), 0);
}
