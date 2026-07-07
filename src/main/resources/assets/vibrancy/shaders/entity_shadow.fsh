#version 430 core

#include "vibrancy:config"
#include "vibrancy:rays"
#include "vibrancy:pixel_alignment"

layout(std140) uniform u_ShadowRange {
    uint shadowRangeStart;
    uint shadowRangeLength;
};
layout(std430, binding = 0) readonly buffer u_Shadows {
    EntityQuad shadows[];
};

uniform sampler2D u_BlockTex;
uniform sampler2D u_TransmissionTex;

in vec3 vertexPosition;
in vec2 texCoord0;
in float vertexLight;

out vec4 fragColor;

bool raycastEntityShadowQuad(vec3 shadowPos, float margin, EntityQuad quad, out float denom, out vec2 uv, out float tt) {
    vec3 normal = cross(quad.vert2 - quad.vert1, quad.vert4 - quad.vert1);

    denom = normal.y;

    if (abs(denom) < margin) return false;

    float d = dot(normal, quad.vert1);

    tt = (d - dot(shadowPos, normal)) / denom;
    if (tt < margin * sign(denom)) return false;

    vec3 vp;
    vp.x = shadowPos.x - quad.vert1.x;
    vp.z = shadowPos.z - quad.vert1.z;
    vp.y = tt + shadowPos.y - quad.vert1.y;

    vec3 diagonal1 = quad.vert2 - quad.vert1;
    vec3 diagonal2 = quad.vert4 - quad.vert1;

    float d1p = dot(diagonal1, vp);
    float d2p = dot(diagonal2, vp);

    float d11 = dot(diagonal1, diagonal1);
    float d12 = dot(diagonal1, diagonal2);
    float d22 = dot(diagonal2, diagonal2);
    float invDet = 1 / (d11 * d22 - d12 * d12);

    float inv11 = d22 * invDet;
    float inv12 = -d12 * invDet;
    float inv22 = d11 * invDet;

    float a = inv11 * d1p + inv12 * d2p;
    float b = inv12 * d1p + inv22 * d2p;

    if (a < -margin || b < -margin || a > 1 + margin || b > 1 + margin) return false;

    uv = clamp(vec2(a, b), margin, 1 - margin);

    return true;
}

void main() {
    fragColor = vec4(0);

    vec3 shadowPos = getShadowPosition(textureSize(u_BlockTex, 0), texCoord0, vertexPosition);
    shadowPos.y = vertexPosition.y;
    float minHit = -1;

    for (uint i = shadowRangeStart; i < shadowRangeStart + shadowRangeLength; i++) {
        EntityQuad quad = shadows[i];
        float denom;
        vec2 uv;
        float tt;

        if (raycastEntityShadowQuad(shadowPos, 1e-3, quad, denom, uv, tt) && tt > 0) {
            if (tt < minHit || minHit == -1) {
                vec2 texUv = interpolateQuadUV(quad, uv);
                vec4 color = texture(u_TransmissionTex, texUv) * interpolateQuadColor(quad, uv);

                if (color.a > 0) {
                    if (color.a == 1) {
                        color.rgb = vec3(0);
                    }

                    /*
                    if (config.visuals.alignPixels) {
                        vec2 quadSize = abs(vec2(quad.u1 - quad.u3, quad.v1 - quad.v3)) * textureSize(u_TransmissionTex, 0);
                        vec2 pixelUv = uv * quadSize;
                        float smoothing = min(min(clamp(pixelUv.x, 0, 1), clamp(quadSize.x - pixelUv.x, 0, 1)), min(clamp(pixelUv.y, 0, 1), clamp(quadSize.y - pixelUv.y, 0, 1)));

                        color.a *= smoothing;
                    }
                    */

                    minHit = tt;
                    fragColor = color;

                    if (color.a == 1) {
                        break;
                    }
                }
            }
        }
    }

    if (minHit != -1) {
        fragColor.a *= clamp(2 - minHit / 2, 0, 1) * vertexLight * texture(u_BlockTex, texCoord0).a;
    }
}
