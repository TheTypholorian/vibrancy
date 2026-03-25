#version 150

#include "big_shot_lib:fog"

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform vec3 CameraPos;
uniform vec3 LightPos;
uniform float LightRadius;

in vec2 texCoord0;
in vec2 texCoord1;
in vec4 vertexColor;
in vec3 vertexPosition;
in vec3 vertexNormal;

out vec4 fragColor;

void main() {
    vec4 lightColor = texelFetch(Sampler1, ivec2(texCoord1), 0);

    vec3 inputNormal = normalize(LightPos - vertexPosition);
    vec3 outputNormal = normalize(CameraPos - vertexPosition);
    vec3 reflectedNormal = 2 * dot(inputNormal, vertexNormal) * vertexNormal - inputNormal;
    float multiplier = clamp(dot(outputNormal, reflectedNormal), 0, 1);
    multiplier = multiplier * multiplier * multiplier * 3;

    lightColor.rgb *= 0.75 + multiplier;

    fragColor = texelFetch(Sampler0, ivec2(texCoord0 * textureSize(Sampler0, 0)), 0) * lightColor * vertexColor;//, vertexPosition - CameraPos);
}
