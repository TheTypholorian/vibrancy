#version 430

#include "veil:deferred_utils"

uniform sampler2D BlockAtlasSampler;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D VeilDynamicNormalSampler;
uniform vec2 ScreenSize;

in vec2 tc;
in vec3 n;

out vec4 fragColor;

void main() {
    //fragColor = vec4(texture(VeilDynamicNormalSampler, gl_FragCoord.xy / ScreenSize).rgb, 1);
    //if (texture(VeilDynamicNormalSampler, gl_FragCoord.xy / ScreenSize).rgb != n) {
    //    discard;
    //}

    if (abs(depthSampleToWorldDepth(texture(DiffuseDepthSampler, gl_FragCoord.xy / ScreenSize).r) - depthSampleToWorldDepth(gl_FragCoord.z)) > 1e-2) {
        discard;
    }

    fragColor = texture(BlockAtlasSampler, tc);

    if (fragColor.a == 0) {
        discard;
    }
}
