#version 440

uniform bool Back;

in vec2 tc;

out vec4 fragColor;

void main() {
    fragColor = vec4(tc, 0, 1);
}
