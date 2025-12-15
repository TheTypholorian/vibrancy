#version 430

//uniform sampler2D DiffuseSampler0;
uniform sampler2D VibrancyOutputSampler;
//uniform sampler2D VeilDynamicAlbedoSampler;

in vec2 uv;

out vec4 fragColor;

void main() {
    fragColor = texture(VibrancyOutputSampler, uv);
    //fragColor = texture(DiffuseSampler0, texCoord);
    //fragColor.rgb += texture(VibrancyOutputSampler, texCoord).rgb * texture(VeilDynamicAlbedoSampler, texCoord).rgb;
}
