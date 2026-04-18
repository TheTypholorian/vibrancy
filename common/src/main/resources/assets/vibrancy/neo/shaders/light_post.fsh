#version 430

//#include "big_shot_lib:fog"

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 light = texture(Sampler0, texCoord0);
    vec4 src = texture(Sampler1, texCoord0);

    fragColor = src + light;
}
