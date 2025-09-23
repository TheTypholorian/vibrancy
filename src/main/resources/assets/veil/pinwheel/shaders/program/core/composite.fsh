#include "veil:deferred_utils"

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D AlbedoSampler;
uniform sampler2D NormalSampler;
uniform sampler2D LightSampler;

uniform vec4 ColorModulator;

uniform int Blur = 8;

in vec2 texCoord;

out vec4 fragColor;

bool pixelMatchesNormal(vec3 target, float depth, ivec2 lightPos) {
    const float norm_margin = 1e-2, depth_margin = 0.5;

    return distance(target, texelFetch(NormalSampler, lightPos * 8, 0).rgb) < norm_margin &&
    distance(target, texelFetch(NormalSampler, lightPos * 8 + ivec2(0, 8), 0).rgb) < norm_margin &&
    distance(target, texelFetch(NormalSampler, lightPos * 8 + ivec2(8, 8), 0).rgb) < norm_margin &&
    distance(target, texelFetch(NormalSampler, lightPos * 8 + ivec2(8, 0), 0).rgb) < norm_margin &&

    abs(depth - depthSampleToWorldDepth(texelFetch(DiffuseDepthSampler, lightPos * 8, 0).r)) < depth_margin &&
    abs(depth - depthSampleToWorldDepth(texelFetch(DiffuseDepthSampler, lightPos * 8 + ivec2(0, 8), 0).r)) < depth_margin &&
    abs(depth - depthSampleToWorldDepth(texelFetch(DiffuseDepthSampler, lightPos * 8 + ivec2(8, 8), 0).r)) < depth_margin &&
    abs(depth - depthSampleToWorldDepth(texelFetch(DiffuseDepthSampler, lightPos * 8 + ivec2(8, 0), 0).r)) < depth_margin;
}

vec4 sampleLight(float depth) {
    vec3 normal = texture(NormalSampler, texCoord).rgb;
    ivec2 lightBufSize = textureSize(LightSampler, 0);
    ivec2 lightPixelPos = ivec2(texCoord * lightBufSize);
    vec4 lightColor = texelFetch(LightSampler, lightPixelPos, 0);

    for (int rad = 0; rad <= 5; rad++) {
        for (int x = -rad; x <= rad; x++) {
            for (int y = -rad; y <= rad; y++) {
                ivec2 lightPos = lightPixelPos + ivec2(x, y);

                if (lightPos.x >= 0 && lightPos.y >= 0 && lightPos.x <= lightBufSize.x && lightPos.y <= lightBufSize.y) {
                    if (pixelMatchesNormal(normal, depth, lightPos)) {
                        return texelFetch(LightSampler, lightPos, 0);
                    }
                }
            }
        }
    }

    return lightColor;
}

void main() {
    vec4 main = texture(DiffuseSampler0, texCoord);
    float mainDepth = texture(DiffuseDepthSampler, texCoord).r;
    vec4 lightColor = sampleLight(depthSampleToWorldDepth(mainDepth));

    fragColor = main;
    fragColor.rgb += texture(AlbedoSampler, texCoord).rgb * lightColor.rgb * lightColor.a;

    gl_FragDepth = mainDepth;
}
