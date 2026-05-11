#version 430

#include "vibrancy:shadow_map"

uniform float ShadowMapPower = 8;

uniform mat4 ShadowMat;
uniform vec3 ChunkOffset;
uniform vec3 CameraPos;

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 texCoord0;
out vec4 vertexColor;

void main() {
    gl_Position = fisheyeShadowMap(ShadowMat * vec4(Position + ChunkOffset - CameraPos, 1.0), ShadowMapPower);
    texCoord0 = UV0;
    vertexColor = Color;
}
