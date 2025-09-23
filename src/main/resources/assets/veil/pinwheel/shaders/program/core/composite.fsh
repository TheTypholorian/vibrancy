#include "veil:deferred_utils"

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;
uniform sampler2D AlbedoSampler;
uniform sampler2D NormalSampler;
uniform sampler2D LightSampler;

uniform vec4 ColorModulator;

uniform int Blur = 3;
uniform int LightScale = 1;

in vec2 texCoord;

out vec4 fragColor;

bool pixelMatchesNormal(vec3 target, float depth, ivec2 lightPos) {
    const float margin = 1e-2;
    ivec2 lightPos2 = lightPos * LightScale;

    return distance(target, texelFetch(NormalSampler, lightPos2, 0).rgb) < margin &&
    distance(target, texelFetch(NormalSampler, lightPos2 + ivec2(0, LightScale), 0).rgb) < margin &&
    distance(target, texelFetch(NormalSampler, lightPos2 + ivec2(LightScale, LightScale), 0).rgb) < margin &&
    distance(target, texelFetch(NormalSampler, lightPos2 + ivec2(LightScale, 0), 0).rgb) < margin &&
    abs(depth - depthSampleToWorldDepth(texelFetch(DiffuseDepthSampler, lightPos2 + ivec2(LightScale, 0), 0).r)) < 0.2;
}

vec4 sampleLight(float depth) {
    vec3 normal = texture(NormalSampler, texCoord).rgb;
    ivec2 lightBufSize = textureSize(LightSampler, 0);
    ivec2 lightPixelPos = ivec2(texCoord * lightBufSize);
    vec4 lightColor = texelFetch(LightSampler, lightPixelPos, 0);

    if (LightScale != 1) {
        for (int rad = 0; rad <= 4; rad++) {
            for (int x = -rad; x <= rad; x++) {
                for (int y = -rad; y <= rad; y++) {
                    ivec2 lightPos = lightPixelPos + ivec2(x, y);

                    if (lightPos.x >= 0 && lightPos.y >= 0 && lightPos.x <= lightBufSize.x && lightPos.y <= lightBufSize.y) {
                        if (pixelMatchesNormal(normal, depth, lightPos)) {
                            vec4 color = vec4(0);
                            float denom = 0;

                            for (int sx = -Blur; sx <= Blur; sx++) {
                                for (int sy = -Blur; sy <= Blur; sy++) {
                                    ivec2 samplePos = lightPos + ivec2(sx, sy);
                                    float fallOff = clamp(1 - length(vec2(texCoord * lightBufSize - samplePos) / Blur), 0, 1);

                                    if (fallOff > 0 && pixelMatchesNormal(normal, depth, samplePos)) {
                                        color += texelFetch(LightSampler, samplePos, 0) * fallOff;
                                        denom += fallOff;
                                    }
                                }
                            }

                            return color / denom;
                        }
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
