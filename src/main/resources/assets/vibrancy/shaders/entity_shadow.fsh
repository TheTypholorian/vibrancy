#version 430 core

#include "vibrancy:config"
#include "vibrancy:rays"
#include "vibrancy:pixel_alignment"

layout(std430, binding = 0) readonly buffer u_Shadows {
    EntityQuad shadows[];
};

uniform sampler2D u_BlockTex;
uniform sampler2D u_TransmissionTex;

in vec3 vertexPosition;
in vec2 texCoord0;
in float vertexLight;

out vec4 fragColor;

void main() {
    fragColor = vec4(0);

    vec3 shadowPos = getShadowPosition(textureSize(u_BlockTex, 0), texCoord0, vertexPosition);
    EndlessRay ray = createEndlessRay(shadowPos, vec3(0, 1, 0));
    float minHit = -1;

    for (uint i = 0u; i < shadows.length(); i++) {
        EntityQuad quad = shadows[i];
        float denom;
        vec2 uv;
        float tt;

        if (raycastQuad(ray, 1e-3, quad, denom, uv, tt) && denom < 1e-3 && tt > 0) {
            if (tt < minHit || minHit == -1) {
                vec2 texUv = interpolateQuadUV(quad, uv);

                if (clamp(texUv, vec2(0), vec2(1)) == texUv) {
                    vec4 color = texture(u_TransmissionTex, texUv) * interpolateQuadColor(quad, uv);

                    if (color.a > 0) {
                        if (color.a == 1) {
                            color.rgb = vec3(0);
                        }

                        minHit = tt;
                        fragColor = color;
                    }
                }
            }
        }
    }

    if (minHit != -1) {
        fragColor.a *= clamp(2 - minHit, 0, 1) * vertexLight * texture(u_BlockTex, texCoord0).a;
    }
}
