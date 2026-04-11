#version 430

#include "vibrancy:include/rays.glsl"

layout(std430, binding = 0) buffer ShadowQuadBuffer {
    Quad shadowQuads[];
};

uniform vec3 LightPos;
uniform float LightRadius;

in vec3 Position;
in vec2 UV0;

out vec2 texCoord0;
flat out uint index;

void main() {
    gl_Position = vec4(UV0 * 2 - 1, 0.0, 1.0);
    index = gl_InstanceID;

    vec3 delta = LightPos - Position;
    vec3 dir = normalize(delta);
    float len = length(delta);
    Quad quad = shadowQuads[gl_InstanceID];
    float dist;

    raycastQuad(Position, dir, len, 1e-3, quad, texCoord0, dist);
}
