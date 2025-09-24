#version 440

in vec2 tc;

out vec4 fragColor;

void main() {
    fragColor = vec4(tc, 0, 1);
}
