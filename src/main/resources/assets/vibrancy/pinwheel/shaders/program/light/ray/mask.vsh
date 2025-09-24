#version 430

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;

out flat uint index;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1);
    index = uint(gl_VertexID / 24);
}
