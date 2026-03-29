#version 150

#include "big_shot_lib:fog"

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
uniform ivec2 Sampler0Size;

in vec2 texCoord0;
in vec2 texCoord1;
in vec4 vertexColor;

out vec3 fragColor;

void main() {
    vec4 block = texelFetch(Sampler0, ivec2(texCoord0 * Sampler0Size), 0) * vertexColor;

    if (block.a == 0) {
        discard;
    }

    fragColor = block.rgb * texelFetch(Sampler1, ivec2(texCoord1), 0).rgb * block.a;
}
