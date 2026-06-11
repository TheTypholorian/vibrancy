#version 430

uniform vec2 TextureSize;

in vec3 Position;
in vec2 UV0;
in ivec2 UV1;
in vec4 Color;
in vec3 Normal;

out vec3 vertexPos;

void main() {
    gl_Position = vec4((vec2(UV1) / TextureSize) * 2 - 1, 0.0, 1.0);
    vertexPos = Position;
}
