#version 330

#include "minecraft:dynamictransforms"

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, texCoord0) * vertexColor;

    if (color.a == 0.0) {
        discard;
    }

    fragColor = color * ColorModulator;
    fragColor.rgb = vec3((fragColor.r + fragColor.g + fragColor.b) / 3);
}
