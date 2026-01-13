#version 150

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;

out flat uint id;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1);
    id = gl_VertexID / 24;
}
