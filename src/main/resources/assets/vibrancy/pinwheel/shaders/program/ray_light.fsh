uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D RayLightSampler;
uniform sampler2D VeilDynamicAlbedoSampler;

uniform vec4 ColorModulator;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 main = texture(DiffuseSampler0, texCoord);
    float mainDepth = texture(DiffuseDepthSampler, texCoord).r;
    vec3 rayLight = texture(RayLightSampler, texCoord).rgb;
    vec4 albedo = texture(VeilDynamicAlbedoSampler, texCoord);
    fragColor = vec4(main.rgb + (albedo.rgb * rayLight), main.a);
    gl_FragDepth = mainDepth;
}
