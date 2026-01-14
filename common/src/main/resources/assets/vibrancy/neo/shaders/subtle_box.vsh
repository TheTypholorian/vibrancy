#version 150

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;

flat out uint id;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1);
    id = uint(gl_VertexID / 24);
}
