#version 430

#include veil:deferred_utils

uniform sampler2D DiffuseDepthSampler;
uniform vec2 ScreenSize;

out vec4 fragColor;

void main() {
    vec3 pos = viewPosFromDepth(texelFetch(DiffuseDepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.xy / ScreenSize);
    fragColor = vec4(viewToWorldSpace(pos), length(pos));
}
