#version 430

uniform vec2 TextureSize;

in vec3 Position;
in ivec2 UV1;

out vec3 vertexPos;

void main() {
    gl_Position = vec4((vec2(UV1) / TextureSize) * 2 - 1, 0.0, 1.0);
    vertexPos = Position;
}
