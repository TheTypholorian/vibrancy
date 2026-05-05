#version 430

//#include "big_shot_lib:fog"
#include "vibrancy:fragment"

uniform float FogStart;
uniform float FogEnd;

uniform sampler2D Sampler0;
uniform ivec2 Sampler0Size;
uniform sampler2DShadow Sampler1;

uniform vec3 CameraPos;
uniform vec3 LightColor;
uniform vec3 LightDirection;

uniform bool SpecularReflectionsEnabled;
uniform float SpecularReflectionStrength;
uniform float SpecularReflectionExponent;

in vec2 texCoord0;
in vec3 texCoord1;
in vec2 texCoord2;
in vec4 vertexColor;
in vec3 vertexPosition;
in float vertexDistance;
in vec3 vertexNormal;

out vec3 fragColor;

void main() {
    vec4 block = texture(Sampler0, texCoord0) * vertexColor;

    if (block.a == 0) {
        discard;
    }

    vec2 texelPos = texCoord0 * Sampler0Size;
    vec2 d = (floor(texelPos) + 0.5 - texelPos) / Sampler0Size;

    vec2 stepA = dFdx(texCoord0);
    vec2 stepB = dFdy(texCoord0);
    float det = stepA.x * stepB.y - stepA.y * stepB.x;
    float a = (d.x * stepB.y - d.y * stepB.x) / det;
    float b = (-d.x * stepA.y + d.y * stepA.x) / det;

    vec3 uv = texCoord1 + dFdx(texCoord1) * a + dFdy(texCoord1) * b;

    fragColor = block.rgb * block.a * LightColor * texture(Sampler1, vec3(uv.xy, uv.z + 2e-4)) * texCoord2.y * clamp(dot(vertexNormal, LightDirection), 0, 1);
}