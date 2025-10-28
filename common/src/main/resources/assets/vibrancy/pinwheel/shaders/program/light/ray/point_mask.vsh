#version 430

#include vibrancy:quads

layout(std430, binding = 0) buffer Quads {
    Quad quads[];
};

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;

out flat Quad quad;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1);
    quad = quads[gl_VertexID / 24];
}
