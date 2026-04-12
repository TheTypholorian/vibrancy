#version 430

#include "vibrancy:include/fragment.glsl"
#include "vibrancy:include/rays.glsl"

layout(std430, binding = 0) buffer ShadowQuadBuffer {
    Quad shadowQuads[];
};

uniform sampler2D Sampler0;
uniform ivec2 Sampler0Size;
uniform vec3 LightPos;

in float denom;
in vec3 fragPos;
flat in uint index;

out vec4 fragColor;

struct Ray {
    vec3 pos;
    vec3 dir;
    float len;
};

void main() {
    Quad q = shadowQuads[index];
    vec2 texCoord0;

    if (!raycastQuad((q.d - denom) / (dot(LightPos, q.normal) - denom), fragPos, LightPos - fragPos, 1e-3, q, texCoord0)) {
        discard;
    }

    vec2 texUv = mix(mix(q.uv1, q.uv2, texCoord0.x), mix(q.uv4, q.uv3, texCoord0.x), texCoord0.y);
    vec4 color = mix(mix(unpackUnorm4x8(q.color1), unpackUnorm4x8(q.color2), texCoord0.x), mix(unpackUnorm4x8(q.color4), unpackUnorm4x8(q.color3), texCoord0.x), texCoord0.y);
    fragColor = texelFetch(Sampler0, ivec2(texUv * Sampler0Size), 0) * color;
}
