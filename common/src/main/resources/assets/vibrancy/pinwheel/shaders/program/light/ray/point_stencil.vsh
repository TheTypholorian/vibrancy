#version 430

in vec3 Position;

void main() {
    gl_Position = vec4(Position, 1);
}
