uniform int DownsizeFactor = 1;

ivec2 getScreenUV() {
    return ivec2(gl_FragCoord.xy * DownsizeFactor);
}

void upsizeHelper(sampler2D colorSampler, sampler2D normalSampler, sampler2D posSampler, ivec2 sourceUV, int scale, vec3 targetNormal, vec3 targetPos, inout float accum, inout vec4 resultColor) {
    ivec2 upscaledUV = sourceUV * scale;
    vec3 normal = texelFetch(normalSampler, upscaledUV, 0).xyz;
    vec3 pos = texelFetch(posSampler, upscaledUV, 0).xyz;
    vec4 color = texelFetch(colorSampler, sourceUV, 0);

    if (dot(targetNormal, normal) > 0.99) {
        float d = 1 / (distance(pos, targetPos) + 1);
        accum += d;
        resultColor += color * d;
    }
}

vec4 upsize(sampler2D colorSampler, sampler2D normalSampler, sampler2D posSampler, ivec2 upscaledUV, int scale) {
    vec3 targetNormal = texelFetch(normalSampler, upscaledUV, 0).xyz;
    vec3 targetPos = texelFetch(posSampler, upscaledUV, 0).xyz;
    ivec2 sourceUV = upscaledUV / scale;

    float accum = 0;
    vec4 resultColor = vec4(0);

    upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV, scale, targetNormal, targetPos, accum, resultColor);
    upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV + ivec2(1, 0), scale, targetNormal, targetPos, accum, resultColor);
    upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV + ivec2(1, 1), scale, targetNormal, targetPos, accum, resultColor);
    upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV + ivec2(0, 1), scale, targetNormal, targetPos, accum, resultColor);

    return resultColor / accum;
}