#version 330

#include "vibrancy:config"
#include "vibrancy:pixel_alignment"

uniform sampler2D u_BaseTex;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 color = texture(u_BaseTex, texCoord0);

    if (color.a == 0.0) {
        discard;
    }

    fragColor = color;
}
