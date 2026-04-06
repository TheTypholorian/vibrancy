#version 430

#include "vibrancy:include/fragment.glsl"
#include "vibrancy:include/rays.glsl"

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

void main() {
    //vec2 step = 1 / (vec2(sprite.width, sprite.height) * 3);

    vec3 delta = LightPos - vertexPos;
    vec3 dir = normalize(delta);
    float len = length(delta);

    Ray ray = Ray(vertexPos, dir, len);

    fragColor = samplePointLight(LightPos, vertexPos, LightRadius, LightColor);
    vec3 accum = vec3(0);
    float denom = 0;

    for (uint i = 0u; i < shadowQuads.length(); i++) {
        float dist;
        vec4 outColor;
        Quad quad = shadowQuads[i];

        if (sampleQuad(Sampler0, ray.pos, ray.dir, ray.len, 1e-3, quad, dist, outColor)) {
            discard;
        }

        if (outColor.a > 0) {
            accum += outColor.rgb;
            denom++;
        }
    }

    if (denom > 0) {
        fragColor.rgb *= accum / denom;
    }
}
