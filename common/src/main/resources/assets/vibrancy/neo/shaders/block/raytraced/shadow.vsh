#version 430

#include "vibrancy:block/raytraced/shadow_utils"

uniform uint Face;
uniform vec3 LightPos;
uniform float LightRadius;

in vec3 Position;
in vec2 UV0;

out vec2 texCoord;

void main() {
    vec3 delta = Position - LightPos;
    gl_Position = vec4(faceAndDirectionToShadowCoords(delta, Face), length(delta) / LightRadius, 1);
    texCoord = UV0;
}
