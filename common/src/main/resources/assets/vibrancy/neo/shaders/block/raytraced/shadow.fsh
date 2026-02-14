#version 430

#include "vibrancy:block/raytraced/shadow_utils"

uniform sampler2D Sampler0;

in vec2 texCoord;

void main() {
    if (texture(Sampler0, texCoord).a < 1) {
        discard;
    }
}
