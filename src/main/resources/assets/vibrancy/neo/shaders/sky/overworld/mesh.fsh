#version 430

//#include "big_shot_lib:fog"
#include "vibrancy:fragment"

uniform float FogStart;
uniform float FogEnd;

uniform sampler2D Sampler0;
uniform ivec2 Sampler0Size;

uniform sampler2DArray ColorTextures;
uniform sampler2DArrayShadow DepthTextures;

uniform uint ShadowDistance;
uniform uint NumShadowLODs = 3;

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

bool isInBounds(vec2 uv, float size) {
    return uv.x >= -size && uv.x <= size && uv.y >= -size && uv.y <= size;
}

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

    float lodWidth = 1;

    for (int l = 0; l < NumShadowLODs; l++) {
        if (isInBounds(uv.xy, lodWidth)) {
            vec3 uv1 = uv / lodWidth / 2 + 0.5;
            fragColor *= texture(DepthTextures, vec4(uv1.xy, l, uv1.z + 1e-3)); // TODO
            break;
        }

        lodWidth *= 2;
    }
}
