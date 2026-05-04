#version 430

//#include "big_shot_lib:fog"
#include "vibrancy:fragment"

uniform float FogStart;
uniform float FogEnd;

uniform sampler2D Sampler0;
uniform ivec2 Sampler0Size;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;

uniform vec3 CameraPos;
uniform vec3 LightColor;
uniform vec3 LightDirection;

uniform bool SpecularReflectionsEnabled;
uniform float SpecularReflectionStrength;
uniform float SpecularReflectionExponent;

in vec2 texCoord0;
in vec3 texCoord1;
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

    fragColor = block.rgb * block.a * LightColor;// * clamp(dot(vertexNormal, LightDirection), 0, 1);

    if (uv.x >= 0 && uv.x <= 1 && uv.y >= 0 && uv.y <= 1) {
        vec4 shadow = texture(Sampler1, uv.xy);
        float depth = texture(Sampler2, uv.xy).r;

        if (shadow.a > 0.9 && uv.z < depth - 2e-4) {
            discard;
        }
    }
}

/*
vec3 lightColor = texelFetch(Sampler1, ivec2(texCoord1), 0).rgb * texelFetch(Sampler2, ivec2(texCoord1), 0).rgb * LightColor * attenuateNoCusp(distance(LightPos, vertexPosition), LightRadius);

if (SpecularReflectionsEnabled) {
    lightColor = specularReflection(lightColor, LightPos, CameraPos, vertexPosition, vertexNormal, SpecularReflectionStrength, SpecularReflectionExponent, Sampler3, texCoord0);
}

fragColor = applyLight(lightColor, block, vertexDistance, FogStart, FogEnd);
*/
