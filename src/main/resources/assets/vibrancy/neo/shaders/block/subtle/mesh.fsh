#version 430

#include "vibrancy:fragment"
#include "vibrancy:subtle"

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform ivec2 Sampler0Size;
uniform float LightBrightness;

uniform bool SpecularReflectionsEnabled;
uniform float SpecularReflectionStrength;
uniform float SpecularReflectionExponent;

uniform float FlickerStrength;
uniform float GLFWTime;

uniform vec3 CameraPos;

in vec2 texCoord0;
flat in SubtleLight light;
//in vec4 vertexColor;
in vec3 vertexPosition;
in vec3 fogPosition;
//in vec3 vertexNormal;

out vec3 fragColor;

void main() {
    vec4 block = texture(Sampler0, texCoord0);// * vertexColor;

    if (block.a == 0) {
        discard;
    }

    vec3 lightColor;

    if (light.shape == 0) {
        lightColor = sampleCubeLight(light.pos, vertexPosition, 0.5, 1.5, light.color);
    } else if (light.shape == 1) {
        lightColor = samplePointLight(light.pos, vertexPosition, 1.5, light.color);

        //if (SpecularReflectionsEnabled) {
        //    lightColor = specularReflection(lightColor, lightColor, normalize(light.pos - vertexPosition), CameraPos, vertexPosition, vertexNormal, SpecularReflectionStrength, SpecularReflectionExponent, Sampler1, texCoord0);
        //}
    }

    fragColor = applyLight(lightColor, block, fogPosition, light.pos, light.flicker * FlickerStrength, GLFWTime);
}
