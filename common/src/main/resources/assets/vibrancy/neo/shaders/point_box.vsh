#version 150

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1);
}
