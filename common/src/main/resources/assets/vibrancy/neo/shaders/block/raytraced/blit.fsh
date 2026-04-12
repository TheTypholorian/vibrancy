#version 430

#include "vibrancy:include/fragment.glsl"
#include "vibrancy:include/rays.glsl"

layout(std430, binding = 0) buffer ShadowQuadBuffer {
    Quad shadowQuads[];
};

uniform sampler2D Sampler0;
uniform ivec2 Sampler0Size;

in vec2 texCoord0;
flat in uint index;

out vec4 fragColor;

struct Ray {
    vec3 pos;
    vec3 dir;
    float len;
};

void main() {
    Quad q = shadowQuads[index];

    vec2 texUv = mix(mix(q.uv1, q.uv2, texCoord0.x), mix(q.uv4, q.uv3, texCoord0.x), texCoord0.y);
    vec4 color = mix(mix(unpackUnorm4x8(q.color1), unpackUnorm4x8(q.color2), texCoord0.x), mix(unpackUnorm4x8(q.color4), unpackUnorm4x8(q.color3), texCoord0.x), texCoord0.y);

    if (texCoord0.x < 0 || texCoord0.x > 1 || texCoord0.y < 0 || texCoord0.y > 1) {
        fragColor = vec4(texCoord0, 1, 1);
    } else {
        fragColor = vec4(texCoord0, 0, 1);
    }

     //vec4(0); //texelFetch(Sampler0, ivec2(texUv * Sampler0Size), 0) * color;
}
