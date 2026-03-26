#version 150

#include "big_shot_lib:fog"

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 texCoord0;

out vec4 fragColor;

float luminance(vec3 rgb) {
    return 0.2126 * rgb.r + 0.7152 * rgb.g + 0.0722 * rgb.b;
}

void main() {
    vec4 light = texture(Sampler0, texCoord0);
    vec4 src = texture(Sampler1, texCoord0);

    float lum = luminance((src + light).rgb);

    fragColor = src + light;//vec4(1 - exp(-light.rgb * (0.25 + lum * 0.5)), 0);
}
