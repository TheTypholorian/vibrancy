#version 430

#include "vibrancy:subtle"

layout(std430) readonly buffer LightBuffer {
    SubtleLight lights[];
};

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

in vec3 Position;
in uvec3 UV0;
in uint LightIndex;

out vec2 texCoord0;
flat out SubtleLight light;
//out vec4 vertexColor;
out vec3 vertexPosition;
out vec3 fogPosition;
//out vec3 vertexNormal;

void main() {
    texCoord0 = vec2((UV0.x << 4) | (UV0.y >> 4), (UV0.y & 0xFFu) | UV0.z) / 4095;
    light = lights[LightIndex];
    vec3 xyz = Position * 4 - 2 + light.pos;
    vec4 pos = ModelViewMat * vec4(xyz, 1);
    gl_Position = ProjMat * pos;
    fogPosition = pos.xyz;
    vertexPosition = xyz;
}
