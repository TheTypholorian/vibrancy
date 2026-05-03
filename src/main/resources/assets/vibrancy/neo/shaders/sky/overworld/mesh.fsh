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
uniform vec3 LightPos;
uniform float LightRadius;

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
    vec2 screenOffset = inverse(mat2(dFdx(texCoord0), dFdy(texCoord0))) * (floor(texelPos) + 0.5 - texelPos) / Sampler0Size;
    vec3 uv = texCoord1 + dFdx(texCoord1) * screenOffset.x + dFdy(texCoord1) * screenOffset.y;

    if (uv.x >= 0 && uv.x <= 1 && uv.y >= 0 && uv.y <= 1) {
        vec4 shadow = texture(Sampler1, uv.xy);
        float depth = texture(Sampler2, uv.xy).r;

        if (shadow.a == 1 && uv.z < depth - 2e-3) {
            discard;
        }
    }

    fragColor = block.rgb * block.a * vec3(1, 1, 0.59) * 0.5; // TODO

    /*
    vec3 lightColor = texelFetch(Sampler1, ivec2(texCoord1), 0).rgb * texelFetch(Sampler2, ivec2(texCoord1), 0).rgb * LightColor * attenuateNoCusp(distance(LightPos, vertexPosition), LightRadius);

    if (SpecularReflectionsEnabled) {
        lightColor = specularReflection(lightColor, LightPos, CameraPos, vertexPosition, vertexNormal, SpecularReflectionStrength, SpecularReflectionExponent, Sampler3, texCoord0);
    }

    fragColor = applyLight(lightColor, block, vertexDistance, FogStart, FogEnd);
    */
}
