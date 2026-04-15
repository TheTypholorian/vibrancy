#version 150

//#include "big_shot_lib:fog"
#include "vibrancy:include/fragment.glsl"

uniform sampler2D Sampler0;
uniform ivec2 Sampler0Size;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform sampler2D Sampler3;

uniform vec3 CameraPos;
uniform vec3 LightColor;
uniform vec3 LightPos;
uniform float LightRadius;

uniform bool SpecularReflectionsEnabled;
uniform float SpecularReflectionStrength;
uniform float SpecularReflectionExponent;

in vec2 texCoord0;
in vec2 texCoord1;
in vec4 vertexColor;
in vec3 vertexPosition;
in vec3 vertexNormal;

out vec3 fragColor;

void main() {
    vec4 block = texelFetch(Sampler0, ivec2(texCoord0 * Sampler0Size), 0) * vertexColor;

    if (block.a == 0) {
        discard;
    }

    vec3 lightColor = texelFetch(Sampler1, ivec2(texCoord1), 0).rgb * texelFetch(Sampler2, ivec2(texCoord1), 0).rgb * LightColor * attenuateNoCusp(distance(LightPos, vertexPosition), LightRadius);

    if (SpecularReflectionsEnabled) {
        vec3 inputNormal = normalize(LightPos - vertexPosition);
        vec3 outputNormal = normalize(CameraPos - vertexPosition);
        vec3 reflectedNormal = 2 * dot(inputNormal, vertexNormal) * vertexNormal - inputNormal;
        float multiplier = clamp(dot(outputNormal, reflectedNormal), 0, 1);
        multiplier = pow(multiplier, SpecularReflectionExponent) * SpecularReflectionStrength;

        lightColor += lightColor * multiplier * texelFetch(Sampler3, ivec2(texCoord0 * Sampler0Size), 0).r;
    }

    fragColor = block.rgb * block.a * lightColor;
}
