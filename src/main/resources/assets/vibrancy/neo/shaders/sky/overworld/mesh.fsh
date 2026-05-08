#version 430

//#include "big_shot_lib:fog"
#include "vibrancy:fragment"

uniform float FogStart;
uniform float FogEnd;

uniform sampler2D Sampler0; // material
uniform ivec2 Sampler0Size;
uniform sampler2DShadow Sampler1; // solid shadow (depth)
uniform sampler2D Sampler2; // translucent shadow
uniform sampler2DShadow Sampler3; // translucent shadow (depth)
uniform sampler2D Sampler4; //

uniform vec3 CameraPos;
uniform vec3 LightColor;
uniform vec3 LightDirection;

uniform float ShadowBias;

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

    vec2 texelPos = texCoord0 * Sampler0Size;
    vec2 d = (floor(texelPos) + 0.5 - texelPos) / Sampler0Size;

    vec2 stepA = dFdx(texCoord0);
    vec2 stepB = dFdy(texCoord0);
    float det = stepA.x * stepB.y - stepA.y * stepB.x;
    float a = (d.x * stepB.y - d.y * stepB.x) / det;
    float b = (-d.x * stepA.y + d.y * stepA.x) / det;

    // discard here to not mess up derivatives
    if (block.a < 0.01) {
        discard;
    }

    vec3 uv = texCoord1 + dFdx(texCoord1) * a + dFdy(texCoord1) * b;
    vec3 biasUV = vec3(uv.xy, max(uv.z, 0) + ShadowBias);

    vec4 translucentColor = texture(Sampler2, uv.xy);
    vec3 lightColor = LightColor * texCoord2.y * clamp(dot(vertexNormal, LightDirection), 0, 1);

    if (uv.x >= 0 && uv.x <= 1 && uv.y >= 0 && uv.y <= 1) {
        lightColor *= texture(Sampler1, biasUV) * mix(translucentColor.rgb * translucentColor.a, vec3(1), texture(Sampler3, biasUV));
    }

    if (SpecularReflectionsEnabled) {
        lightColor = specularReflection(lightColor, LightDirection, CameraPos, vertexPosition, vertexNormal, SpecularReflectionStrength, SpecularReflectionExponent, Sampler4, texCoord0);
    }

    fragColor = applyLight(lightColor, block, vertexDistance, FogStart, FogEnd);
}