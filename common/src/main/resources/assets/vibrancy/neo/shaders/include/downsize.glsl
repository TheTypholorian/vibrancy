uniform int DownsizeFactor = 1;

ivec2 getScreenUV() {
    return ivec2(gl_FragCoord.xy * DownsizeFactor);
}

void upsizeHelper(sampler2D colorSampler, sampler2D normalSampler, sampler2D posSampler, ivec2 originUV, ivec2 sourceUV, int scale, vec3 targetNormal, vec3 targetPos, inout vec4 result, inout float minDistance) {
    ivec2 upscaledUV = sourceUV * scale;
    vec3 checkNormal = texelFetch(normalSampler, upscaledUV, 0).xyz;

    if (dot(checkNormal, targetNormal) > 0.99) {
        vec3 checkPos = texelFetch(posSampler, upscaledUV, 0).xyz;
        float d = distance(targetPos, checkPos);

        if (d < minDistance) {
            ivec2 blockOrigin = (upscaledUV / scale) * scale;
            result = texelFetch(colorSampler, blockOrigin / scale, 0);
            minDistance = d;
        }
    }
}

vec4 upsize(sampler2D colorSampler, sampler2D normalSampler, sampler2D posSampler, ivec2 upscaledUV, int scale) {
    vec3 targetNormal = texelFetch(normalSampler, upscaledUV, 0).xyz;
    vec3 targetPos = texelFetch(posSampler, upscaledUV, 0).xyz;
    ivec2 sourceUV = upscaledUV / scale;

    vec4 result = vec4(0.5);
    float minDistance = 100;

    upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV, scale, targetNormal, targetPos, result, minDistance);

    upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(1, 0), scale, targetNormal, targetPos, result, minDistance);
    upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(-1, 0), scale, targetNormal, targetPos, result, minDistance);
    upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(0, 1), scale, targetNormal, targetPos, result, minDistance);
    upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(0, -1), scale, targetNormal, targetPos, result, minDistance);

    upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(1, 1), scale, targetNormal, targetPos, result, minDistance);
    upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(-1, 1), scale, targetNormal, targetPos, result, minDistance);
    upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(1, -1), scale, targetNormal, targetPos, result, minDistance);
    upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(-1, -1), scale, targetNormal, targetPos, result, minDistance);

    upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(2, 0), scale, targetNormal, targetPos, result, minDistance);
    upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(-2, 0), scale, targetNormal, targetPos, result, minDistance);
    upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(0, 2), scale, targetNormal, targetPos, result, minDistance);
    upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(0, -2), scale, targetNormal, targetPos, result, minDistance);

    return result;
}