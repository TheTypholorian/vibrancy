#version 150

#include "vibrancy:include/shadow"

uniform vec3 LightPos;
uniform float LightRadius;

in vec3 Position;
in vec2 UV0;

out vec2 texCoord;

void main() {
    vec3 dir = Position - LightPos;
    gl_Position = vec4(directionToShadowCoords(normalize(dir)) * 2 - 1, (1 - length(dir) / LightRadius) * 2 - 1, 1);
    texCoord = UV0;
}
