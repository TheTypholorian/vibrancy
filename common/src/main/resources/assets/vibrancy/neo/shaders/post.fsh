#version 430

uniform sampler2D DiffuseSampler0;
uniform sampler2D VibrancyOutputSampler;
uniform sampler2D VibrancyAlbedoSampler;

in vec2 uv;

out vec4 fragColor;

void main() {
    fragColor = texture(DiffuseSampler0, uv);

    vec3 outputColor = texture(VibrancyOutputSampler, uv).rgb;
    float outputScale = min(1, 1 / max(outputColor.r, max(outputColor.g, outputColor.b)));

    fragColor.rgb += outputColor * outputScale * texture(VibrancyAlbedoSampler, uv).rgb;
}
