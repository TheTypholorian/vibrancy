#version 430

uniform sampler2D Sampler0;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    fragColor = texture(Sampler0, texCoord);

    if (fragColor.a < 0.1) {
        discard;
    }
}
