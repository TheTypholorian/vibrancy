#version 430

#include "vibrancy:common"

layout(std430, binding = 0) buffer Quads {
    Quad quads[];
};

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;

void main() {
    // ProjMat * ModelViewMat *
    gl_Position = vec4(Position / 100, 1);
    //quad = quads[gl_VertexID / 24];
}
