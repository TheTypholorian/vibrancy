#version 430

#include "vibrancy:shadow_map"

uniform float ShadowMapPower = 8;

uniform mat4 SableMat;
uniform mat4 ShadowMat;

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 texCoord0;
out vec4 vertexColor;

void main() {
    gl_Position = fisheyeShadowMap(ShadowMat * (SableMat * vec4(Position, 1.0)), ShadowMapPower);
    texCoord0 = UV0;
    vertexColor = Color;
}
