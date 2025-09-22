#include "veil:deferred_utils"

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D AlbedoSampler;
uniform sampler2D LightSampler;
uniform sampler2D LightDepthSampler;

uniform vec4 ColorModulator;

uniform int Blur = 2;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 main = texture(DiffuseSampler0, texCoord);
    float mainDepth = texture(DiffuseDepthSampler, texCoord).r;

    ivec2 texSize = textureSize(LightSampler, 0);
    ivec2 pos = ivec2(texCoord * texSize);
    vec4 lightColor = texelFetch(LightSampler, pos, 0);
    float lightDepth = texelFetch(LightDepthSampler, pos, 0).r;

    for (int x = pos.x - Blur; x <= pos.x + Blur; x++) {
        for (int y = pos.y - Blur; y <= pos.y + Blur; y++) {
            ivec2 nPos = ivec2(x, y);
		    float nLightDepth = texelFetch(LightDepthSampler, nPos, 0).r;

            if (abs(nLightDepth - mainDepth) < abs(lightDepth - mainDepth)) {
                lightDepth = nLightDepth;
                lightColor = texelFetch(LightSampler, nPos, 0);
            }
        }
    }

    //lightDepth /= (Blur * 2 + 1) * (Blur * 2 + 1);
    //lightColor /= (Blur * 2 + 1) * (Blur * 2 + 1);

    fragColor = main;

    //if (depthSampleToWorldDepth(lightLength) < 0.1) {
        fragColor.rgb += texture(AlbedoSampler, texCoord).rgb * lightColor.rgb * lightColor.a;
    //}

    gl_FragDepth = mainDepth;
}
