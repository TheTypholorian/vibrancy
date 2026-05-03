#version 430

uniform mat4 ShadowMat;

in vec3 Position;
in vec2 UV0;

out vec2 texCoord0;

void main() {
    gl_Position = ShadowMat * vec4(Position, 1.0);
    texCoord0 = UV0;
}
