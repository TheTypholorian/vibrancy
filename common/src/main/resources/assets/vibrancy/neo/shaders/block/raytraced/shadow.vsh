#version 430

#include "vibrancy:include/rays"
#include "vibrancy:block/raytraced/shadow_utils"

layout(std430, binding = 0) buffer Quads {
    Quad quads[];
};

uniform uint Face;
uniform vec3 LightPos;
uniform float LightRadius;
uniform uint QuadStride;

in vec3 Position;
in vec2 UV0;

out vec3 delta;
out flat Quad quad;

void main() {
    delta = Position - LightPos;
    gl_Position = vec4(faceAndDirectionToShadowCoords(delta, Face), length(delta) / LightRadius, 1);
    gl_Layer = int(Face);
    quad = quads[gl_Vertex / QuadStride];
}
