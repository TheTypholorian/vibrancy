#version 430

uniform sampler2D DiffuseSampler0;
uniform sampler2D VibrancyOutputSampler;
uniform sampler2D VibrancyNormalSampler;
uniform sampler2D VibrancyAlbedoSampler;

in vec2 uv;

out vec4 fragColor;

void main() {
    fragColor = texture(DiffuseSampler0, uv);

    vec3 outputColor = texture(VibrancyOutputSampler, uv).rgb;

    if (outputColor.r > 1) {
        outputColor /= outputColor.r;
    }

    if (outputColor.g > 1) {
        outputColor /= outputColor.g;
    }

    if (outputColor.b > 1) {
        outputColor /= outputColor.b;
    }

    fragColor.rgb += outputColor * texture(VibrancyAlbedoSampler, uv).rgb;
}
