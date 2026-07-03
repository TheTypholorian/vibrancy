#include "sodium:globals"
#include "sodium:fog"
#include "vibrancy:config"

#ifdef USE_FOG
in vec2 v_FragDistance;
in float v_FadeFactor;
#endif

vec3 sampleCubeLight(vec3 lightPos, vec3 fragPos, float startRadius, float endRadius, vec3 lightColor) {
    float dist = max(abs(lightPos.x - fragPos.x), max(abs(lightPos.y - fragPos.y), abs(lightPos.z - fragPos.z)));
    return clamp((endRadius - dist) / (endRadius - startRadius), 0, 1) * lightColor;
}

vec3 specularReflection(SpecularConfig config, vec3 baseColor, vec3 lightColor, vec3 lightDir, vec3 cameraPos, vec3 vertexPos, vec3 normal, sampler2D reflectionSampler, vec2 texCoord0) {
    vec3 inputNormal = lightDir;
    vec3 outputNormal = normalize(cameraPos - vertexPos);
    vec3 reflectedNormal = 2 * dot(inputNormal, normal) * normal - inputNormal;
    float multiplier = pow(clamp(dot(outputNormal, reflectedNormal), 0, 1) * (1 - (dot(inputNormal, outputNormal) / 2 + 0.5)), config.exponent) * config.strength;

    return baseColor + lightColor * texture(reflectionSampler, texCoord0).r * multiplier;
}

vec4 sampleNearest(sampler2D source, vec2 uv, vec2 pixelSize, vec2 du, vec2 dv, vec2 texelScreenSize) {
    vec2 uvTexelCoords = uv / pixelSize;
    vec2 texelCenter = round(uvTexelCoords) - 0.5f;
    vec2 texelOffset = uvTexelCoords - texelCenter;

    texelOffset = (texelOffset - 0.5f) * pixelSize / texelScreenSize + 0.5f;
    texelOffset = clamp(texelOffset, 0.0f, 1.0f);

    uv = (texelCenter + texelOffset) * pixelSize;
    return textureGrad(source, uv, du, dv);
}

vec4 sampleNearest(sampler2D source, vec2 uv, vec2 pixelSize) {
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);
    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    return sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);
}

vec4 sampleRGSS(sampler2D source, vec2 uv, vec2 pixelSize) {
    vec2 du = dFdx(uv);
    vec2 dv = dFdy(uv);

    vec2 texelScreenSize = sqrt(du * du + dv * dv);
    float maxTexelSize = max(texelScreenSize.x, texelScreenSize.y);

    float minPixelSize = min(pixelSize.x, pixelSize.y);

    float transitionStart = minPixelSize * 1.0;
    float transitionEnd = minPixelSize * 2.0;
    float blendFactor = smoothstep(transitionStart, transitionEnd, maxTexelSize);

    float duLength = length(du);
    float dvLength = length(dv);
    float minDerivative = min(duLength, dvLength);
    float maxDerivative = max(duLength, dvLength);

    float effectiveDerivative = sqrt(minDerivative * maxDerivative);

    float mipLevelExact = max(0.0, log2(effectiveDerivative / minPixelSize));

    const vec2 offsets[4] = vec2[](
    vec2(0.125, 0.375),
    vec2(-0.125, -0.375),
    vec2(0.375, -0.125),
    vec2(-0.375, 0.125)
    );

    vec4 rgssColor = vec4(0.0);
    for (int i = 0; i < 4; ++i) {
        vec2 sampleUV = uv + offsets[i] * pixelSize;
        rgssColor += textureLod(source, sampleUV, mipLevelExact);
    }
    rgssColor *= 0.25;

    vec4 nearestColor = sampleNearest(source, uv, pixelSize, du, dv, texelScreenSize);

    return mix(nearestColor, rgssColor, blendFactor);
}

vec4 sampleTexture(sampler2D source, vec2 uv, vec2 pixelSize) {
    return u_UseRGSS ? sampleRGSS(source, uv, pixelSize) : sampleNearest(source, uv, pixelSize);
}

float getFogScale() {
    #ifdef FOG
    return v_FadeFactor * (1 - total_fog_value(v_FragDistance.y, v_FragDistance.x, u_EnvironmentFog.x, u_EnvironmentFog.y, u_RenderFog.x, u_RenderFog.y));
    #else
    return 1;
    #endif
}