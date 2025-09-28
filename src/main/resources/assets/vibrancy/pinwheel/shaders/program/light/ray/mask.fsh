#version 430

#include "veil:deferred_utils"
#include "vibrancy:quads"

uniform sampler2D BlockAtlasSampler;
uniform sampler2D WorldPosSampler;
uniform vec3 LightPos;
uniform vec2 ScreenSize;

uniform bool Detailed = false;

in flat Quad q;

out vec4 fragColor;

void main() {
    fragColor = vec4(1);

    // 40-80%
    if (Detailed) {
        vec3 Pos = texelFetch(WorldPosSampler, ivec2(gl_FragCoord.xy), 0).rgb;

        vec3 delta = Pos - LightPos;
        float len = length(delta);
        vec3 dir = delta / len;

        vec2 uv;

        if (raycastQuad(LightPos, dir, len, q, uv)) {
            if (q.doSample == 1) {
                fragColor = texture(BlockAtlasSampler, uv);

                if (fragColor.a == 0) {
                    discard;
                }
            }
        } else {
            discard;
        }
    }
}
