#version 430

#include "vibrancy:include/rays"
#include "vibrancy:include/shadow"

layout(std430, binding = 0) buffer Quads {
    Quad quads[];
};

uniform sampler2D Sampler0;

uniform vec3 LightPos;
uniform float LightRadius;

in vec2 texCoord;

out float fragDistance;

void main() {
    vec3 dir = shadowCoordsToDirection(texCoord);
    fragDistance = 1;

    for (uint i = 0u; i < quads.length(); i++) {
        Quad quad = quads[i];
        float t;

        if (!sampleQuad(Sampler0, LightPos, dir, LightRadius, 1e-3, quad, t)) {
            fragDistance = min(fragDistance, t / LightRadius);
        }
    }
}
