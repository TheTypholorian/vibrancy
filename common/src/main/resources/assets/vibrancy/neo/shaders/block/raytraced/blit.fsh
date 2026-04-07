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
in flat uint index;

out vec3 fragColor;

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
    vec4 accum = vec4(0);
    float denom = 0;

    for (uint i = 0u; i < index; i++) {
        float dist;
        vec4 outColor;
        Quad quad = shadowQuads[i];

        if (sampleQuad(Sampler0, ray.pos, ray.dir, ray.len, 1e-3, quad, dist, outColor)) {
            discard;
        }

        if (outColor.a > 0) {
            accum += outColor;
            denom++;
        }
    }

    float lightStrength = attenuateNoCusp(distance(LightPos, vertexPos), LightRadius);

    if (denom > 0) {
        accum /= denom;
        fragColor = (LightColor * (1 - accum.a) + accum.rgb * accum.a) * lightStrength;
    } else {
        fragColor = LightColor * lightStrength;
    }
}
