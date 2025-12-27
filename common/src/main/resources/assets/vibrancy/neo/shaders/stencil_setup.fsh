#version 430

uniform sampler2D VeilDynamicLightSampler;

in vec2 uv;

out vec4 fragColor;

void main() {
    fragColor = vec4(1);

    vec2 uv = texture(VeilDynamicLightSampler, uv).rg;

    if (uv.r * 255 <= 8) {
        discard;
    }
}
