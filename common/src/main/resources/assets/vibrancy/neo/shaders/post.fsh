#version 430

uniform sampler2D DiffuseSampler0;
uniform sampler2D VibrancyOutputSampler;
uniform sampler2D VibrancyAlbedoSampler;

in vec2 uv;

out vec4 fragColor;

void main() {
    fragColor = texture(DiffuseSampler0, uv);
    fragColor.rgb += texture(VibrancyOutputSampler, uv).rgb * texture(VibrancyAlbedoSampler, uv).rgb;
}
