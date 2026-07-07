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
    vec2 edge1 = quad.vert2.xz - quad.vert1.xz;
    vec2 edge2 = quad.vert4.xz - quad.vert1.xz;
    vec2 delta = shadowPos.xz - quad.vert1.xz;

    float det = edge1.x * edge2.y - edge1.y * edge2.x;
    if (abs(det) < margin) return false;

    float invDet = 1.0 / det;

    float a = (delta.x * edge2.y - delta.y * edge2.x) * invDet;
    float b = (edge1.x * delta.y - edge1.y * delta.x) * invDet;

    if (a < -margin || b < -margin || a > 1.0 + margin || b > 1.0 + margin) return false;

    uv = clamp(vec2(a, b), margin, 1.0 - margin);

    float y = quad.vert1.y
    + a * (quad.vert2.y - quad.vert1.y)
    + b * (quad.vert4.y - quad.vert1.y);

    tt = y - shadowPos.y;
    denom = det;

    if (tt < margin) return false;

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
                        minHit = tt;
                        fragColor = vec4(0, 0, 0, 1);
                        break;
                    }

                    minHit = tt;
                    fragColor = color;
                }
            }
        }
    }

    if (minHit != -1) {
        fragColor.a *= clamp(2 - minHit, 0, 1) * vertexLight * texture(u_BlockTex, texCoord0).a;
    }
}
