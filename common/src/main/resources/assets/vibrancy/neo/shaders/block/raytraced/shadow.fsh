#version 430

#include "vibrancy:include/rays"
#include "vibrancy:block/raytraced/shadow_utils"

uniform sampler2D Sampler0;

uniform vec3 LightPos;
uniform float LightRadius;

in vec3 delta;
in flat Quad quad;

void main() {
    float t;

    if (sampleQuad(Sampler0, LightPos, normalize(delta), LightRadius, 1e-3, quad, t)) {
        discard;
    }
}
