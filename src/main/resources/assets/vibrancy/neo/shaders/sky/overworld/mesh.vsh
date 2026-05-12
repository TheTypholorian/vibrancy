#version 430

#include "vibrancy:shadow_map"

uniform float ShadowMapPower = 8;

float fog_distance(vec3 pos, int shape) {
    if (shape == 0) {
        return length(pos);
    } else {
        float distXZ = length(pos.xz);
        float distY = abs(pos.y);
        return max(distXZ, distY);
    }
}

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat4 SableMat;
uniform mat4 SpecularMat;
uniform mat4 ShadowMat;

in vec3 Position;
in vec2 UV0;
in ivec2 UV2;
in vec4 Color;
in vec3 Normal;

out vec2 texCoord0;
out vec3 texCoord1;
out vec2 texCoord2;
out vec4 vertexColor;
out vec3 vertexPosition;
out vec3 fogPosition;
out vec3 vertexNormal;

void main() {
    vec4 pos = ModelViewMat * vec4(Position, 1);
    gl_Position = ProjMat * pos;
    texCoord0 = UV0;
    vec4 sablePos = SableMat * vec4(Position, 1);
    texCoord1 = fisheyeShadowMapCoords(ShadowMat * sablePos, ShadowMapPower) / 2 + 0.5;
    texCoord2 = vec2(UV2) / 240;
    vertexColor = Color;
    vertexPosition = (SpecularMat * vec4(Position, 1)).xyz;
    fogPosition = sablePos.xyz;
    vertexNormal = normalize(mat3(SableMat) * Normal);
}
