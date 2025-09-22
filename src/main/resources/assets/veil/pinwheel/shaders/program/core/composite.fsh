uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D VeilDynamicAlbedoSampler;
uniform sampler2D LightSampler;
uniform sampler2D LightDepthSampler;

uniform vec4 ColorModulator;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 main = texture(DiffuseSampler0, texCoord);
    float mainDepth = texture(DiffuseDepthSampler, texCoord).r;
    float lightDepth = texture(LightDepthSampler, texCoord).r;

    if (abs(mainDepth - lightDepth) < 1) {
        vec4 light = texture(LightSampler, texCoord)/** texture(VeilDynamicAlbedoSampler, texCoord)*/;
        fragColor = vec4(main.rgb + (light.rgb * light.a), main.a);
    } else {
        fragColor = main;
    }

    gl_FragDepth = mainDepth;
}
