#version 430

#include "vibrancy:shadow_map"

uniform mat4 ModelViewMat;
uniform mat4 ShadowMat;

in vec3 Position;
in vec2 UV0;
in vec4 Color;

out vec2 texCoord0;
out vec4 vertexColor;

void main() {
    gl_Position = fisheyeShadowMap(ShadowMat * ModelViewMat * vec4(Position, 1.0), 8.0);
    texCoord0 = UV0;
    vertexColor = Color;
}
