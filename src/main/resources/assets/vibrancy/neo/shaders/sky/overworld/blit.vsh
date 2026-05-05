#version 430

uniform mat4 ShadowMat;
uniform vec3 CameraPos;

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 texCoord0;
out vec4 vertexColor;

void main() {
    gl_Position = ShadowMat * vec4(Position - floor(CameraPos), 1.0);
    texCoord0 = UV0;
    vertexColor = Color;
}
