#version 430

layout(early_fragment_tests) in;

#include "veil:deferred_utils"
#include "vibrancy:quads"

uniform sampler2D BlockAtlasSampler;
uniform sampler2D WorldPosSampler;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D VeilDynamicNormalSampler;
uniform vec3 LightPos;
uniform float LightRadius;
uniform vec2 ScreenSize;

in flat Quad q;

out vec4 fragColor;

void main() {
    fragColor = vec4(1);

    vec3 Pos = viewToWorldSpace(viewPosFromDepth(texelFetch(DiffuseDepthSampler, ivec2(gl_FragCoord.xy), 0).r, gl_FragCoord.xy / ScreenSize));

    vec3 delta = Pos - LightPos;
    float len = length(delta);

    vec3 dir = delta / len;

    vec2 uv;

    if (raycastQuad(LightPos, dir, len, q, uv)) {
        fragColor = texture(BlockAtlasSampler, uv);

        if (fragColor.a == 0) {
            discard;
        }
    } else {
        discard;
    }
}
