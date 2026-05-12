#version 430

#include "vibrancy:shadow_map"

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 SpecularMat;

in vec3 Position;
in vec2 UV0;
in ivec2 UV1;
in vec4 Color;
in vec3 Normal;

out vec2 texCoord0;
out vec2 texCoord1;
out vec4 vertexColor;
out vec3 vertexPosition;
out vec3 fogPosition;
out vec3 vertexNormal;

void main() {
    vec4 pos = ModelViewMat * vec4(Position, 1);
    gl_Position = ProjMat * pos;
    texCoord0 = UV0;
    texCoord1 = vec2(UV1);
    vertexColor = Color;
    vertexPosition = (SpecularMat * vec4(Position, 1)).xyz;
    fogPosition = pos.xyz;
    vertexNormal = Normal;
}
