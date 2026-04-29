#version 430

in vec3 Position;
in vec2 UV0;

out vec3 vertexPos;

void main() {
    gl_Position = vec4(UV0 * 2 - 1, 0.0, 1.0);
    vertexPos = Position;
}
