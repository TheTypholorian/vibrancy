#version 430

#include "vibrancy:include/fragment"
#include "vibrancy:include/rays"

layout(std430, binding = 0) buffer ShadowQuadBuffer {
    Quad shadowQuads[];
};

uniform sampler2D Sampler0;

uniform vec3 LightPos;
uniform vec3 LightColor;
uniform float LightRadius;
uniform float LightBrightness;

in vec3 vertexPos;

out vec4 fragColor;

struct Ray {
    vec3 pos;
    vec3 dir;
    float len;
};

vec4 test(Quad q, Ray check) {
    float dist;

    return sampleQuad(Sampler0, check.pos, check.dir, check.len, 1e-3, q, dist);
}

void main() {
    //vec2 step = 1 / (vec2(sprite.width, sprite.height) * 3);

    vec3 delta = LightPos - vertexPos;
    vec3 dir = normalize(delta);
    float len = length(delta);

    Ray ray = Ray(vertexPos, dir, len);

    fragColor = samplePointLight(LightPos, vertexPos, LightRadius, LightColor);

    for (uint i = 0u; i < shadowQuads.length(); i++) {
        fragColor *= test(shadowQuads[i], ray);
    }
}
