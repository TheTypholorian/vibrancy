#version 430

#include "vibrancy:include/rays"
#include "vibrancy:block/raytraced/shadow_texture_utils"

layout(std430, binding = 0) buffer Quads {
    Quad quads[];
};

uniform sampler2D Sampler0;

uniform vec3 LightPos;
uniform float LightRadius;

in vec2 texCoord;

out float fragDistance;

void main() {
    uint face = uint(floor(texCoord.x * 6.0));
    vec3 dir = faceAndShadowCoordsToDirection(texCoord, face);
    fragDistance = LightRadius;

    for (uint i = 0u; i < quads.length(); i++) {
        Quad quad = quads[i];
        float dist;

        if (!sampleQuad(Sampler0, LightPos, dir, fragDistance, 1e-3, quad, dist)) {
            fragDistance = dist;
        }
    }

    fragDistance /= LightRadius;
}
