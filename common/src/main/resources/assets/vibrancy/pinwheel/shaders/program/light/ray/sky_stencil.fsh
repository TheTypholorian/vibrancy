#version 430

#include veil:deferred_utils

uniform sampler2D DiffuseDepthSampler;
uniform sampler2D VeilDynamicNormalSampler;
uniform sampler2D VeilDynamicUVSampler;
uniform vec3 LightDirection;
uniform vec2 ScreenSize;

out vec4 fragColor;

void main() {
    fragColor = vec4(0, 0, 1, 1);

    vec3 Normal = texelFetch(VeilDynamicNormalSampler, ivec2(gl_FragCoord.xy), 0).xyz;

    float d = dot(Normal, normalize((VeilCamera.ViewMat * vec4(LightDirection, 0)).xyz));

    if (d <= 0) {
        discard;
    }

    vec2 uv = texelFetch(VeilDynamicUVSampler, ivec2(gl_FragCoord.xy), 0).xy;

    if (uv.g <= 0) {
        discard;
    }
}
