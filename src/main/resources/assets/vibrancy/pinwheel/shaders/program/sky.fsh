#version 430

#include "veil:fog"

uniform vec3 SkyColor;

in vec3 pos;

out vec4 fragColor;

void main() {
    fragColor = vec4(SkyColor, 1);
}
