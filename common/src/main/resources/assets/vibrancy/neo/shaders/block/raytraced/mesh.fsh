#version 150

uniform sampler2D Sampler0;
uniform sampler2D Sampler1;

in vec2 texCoord0;
in vec2 texCoord1;

out vec4 fragColor;

void main() {
    fragColor = texture(Sampler0, texCoord0) * texelFetch(Sampler1, ivec2(texCoord1), 0);
}
