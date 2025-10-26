#version 430

#include "veil:deferred_utils"

uniform sampler2D BlockAtlasSampler;
uniform sampler2D WorldPosSampler;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D VeilDynamicNormalSampler;
uniform vec3 LightPos;
uniform float LightRadius;
uniform vec2 ScreenSize;

out vec4 fragColor;

void main() {
    fragColor = vec4(1);

    vec3 Pos = viewToWorldSpace(viewPosFromDepth(texelFetch(DiffuseDepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.xy / ScreenSize));
    vec3 Normal = normalize(texelFetch(VeilDynamicNormalSampler, ivec2(gl_FragCoord.xy), 0).xyz);

    vec3 delta = LightPos - Pos;
    float d = dot(Normal, normalize((VeilCamera.ViewMat * vec4(delta, 0)).xyz));

    if (d <= 0) {
        discard;
    }

    float len = length(delta);

    if (len > LightRadius) {
        discard;
    }
}
