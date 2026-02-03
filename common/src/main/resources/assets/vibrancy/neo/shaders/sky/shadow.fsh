#version 430

uniform sampler2D Sampler0;

in vec2 texCoord;
in float vertexDistance;

out float fragDistance;

void main() {
    if (texture(Sampler0, texCoord).a < 0.1) {
        discard;
    }

    fragDistance = 0.5; // TODO vertexDistance
}
