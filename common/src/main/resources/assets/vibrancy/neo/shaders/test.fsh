#version 430

in vec2 uv;

out vec4 fragColor;

void main() {
    fragColor = vec4(uv, 1, 0.25);
}
