#version 150

#include "big_shot_lib:fog"

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform sampler2D Sampler2;
uniform vec3 CameraPos;
uniform vec3 LightPos;
uniform float LightRadius;

in vec2 texCoord0;
in vec2 texCoord1;
in vec4 vertexColor;
in vec3 vertexPosition;
in vec3 vertexNormal;

out vec4 fragColor;

float luminance(vec3 rgb) {
    return 0.2126 * rgb.r + 0.7152 * rgb.g + 0.0722 * rgb.b;
}

void main() {
    vec4 block = texelFetch(Sampler0, ivec2(texCoord0 * textureSize(Sampler0, 0)), 0) * vertexColor;

    vec3 lightColor = texelFetch(Sampler1, ivec2(texCoord1), 0).rgb;

    vec3 inputNormal = normalize(LightPos - vertexPosition);
    vec3 outputNormal = normalize(CameraPos - vertexPosition);
    vec3 reflectedNormal = 2 * dot(inputNormal, vertexNormal) * vertexNormal - inputNormal;
    float multiplier = clamp(dot(outputNormal, reflectedNormal), 0, 1);
    multiplier = multiplier * multiplier * multiplier * 2.5;

    lightColor *= 1 + multiplier * texelFetch(Sampler2, ivec2(texCoord0 * textureSize(Sampler2, 0)), 0).r;

    fragColor = vec4(block.rgb * lightColor, luminance(lightColor)) * block.a;//, vertexPosition - CameraPos);
}
