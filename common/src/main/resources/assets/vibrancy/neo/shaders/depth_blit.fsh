#version 430

uniform sampler2D Sampler0;

in vec2 texCoord0;

void main() {
    gl_FragDepth = texture(Sampler0, texCoord0).r;
}
