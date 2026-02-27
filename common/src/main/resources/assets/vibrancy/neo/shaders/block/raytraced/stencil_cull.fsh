#version 430

uniform sampler2D VibrancyWorldPosSampler;

uniform vec3 LightPos;
uniform float LightRadius;

in vec2 texCoord0;

void main() {
    vec3 pos = texture(VibrancyWorldPosSampler, texCoord0).xyz;

    if (distance(LightPos, pos) < LightRadius) {
        discard;
    }
}
