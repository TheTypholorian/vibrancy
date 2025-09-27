#version 430

#include "veil:deferred_utils"

uniform sampler2D DiffuseDepthSampler;
uniform vec2 ScreenSize;

out vec3 fragPos;

void main() {
    fragPos = viewToWorldSpace(viewPosFromDepth(texelFetch(DiffuseDepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.xy / ScreenSize));
}
