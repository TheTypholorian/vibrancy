uniform int DownsizeFactor = 1;

vec2 getScreenUV(vec2 screen) {
    return gl_FragCoord.xy / screen;
}

void upsizeHelper(sampler2D colorSampler, sampler2D normalSampler, sampler2D posSampler, ivec2 sourceUV, int scale, vec3 targetNormal, vec3 targetPos, inout float resultDistance, inout vec4 resultColor) {
    ivec2 upscaledUV = sourceUV * scale;
    vec3 checkNormal = texelFetch(normalSampler, upscaledUV, 0).xyz;

    if (dot(checkNormal, targetNormal) > 0.99) {
        vec3 checkPos = texelFetch(posSampler, upscaledUV, 0).xyz;
        float d = distance(targetPos, checkPos);

        if (d < resultDistance) {
            resultColor = texelFetch(colorSampler, sourceUV, 0);
            resultDistance = d;
        }
    }
}

vec4 upsize(sampler2D colorSampler, sampler2D normalSampler, sampler2D posSampler, ivec2 upscaledUV, int scale) {
    vec3 targetNormal = texelFetch(normalSampler, upscaledUV, 0).xyz;
    vec3 targetPos = texelFetch(posSampler, upscaledUV, 0).xyz;
    ivec2 sourceUV = upscaledUV / scale;

    vec4 resultColor = vec4(0);
    float resultDistance = 1000;

    upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV, scale, targetNormal, targetPos, resultDistance, resultColor);

    upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV + ivec2(1, 0), scale, targetNormal, targetPos, resultDistance, resultColor);
    upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV + ivec2(-1, 0), scale, targetNormal, targetPos, resultDistance, resultColor);
    upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV + ivec2(0, 1), scale, targetNormal, targetPos, resultDistance, resultColor);
    upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV + ivec2(0, -1), scale, targetNormal, targetPos, resultDistance, resultColor);

    upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV + ivec2(1, 1), scale, targetNormal, targetPos, resultDistance, resultColor);
    upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV + ivec2(-1, 1), scale, targetNormal, targetPos, resultDistance, resultColor);
    upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV + ivec2(1, -1), scale, targetNormal, targetPos, resultDistance, resultColor);
    upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV + ivec2(-1, -1), scale, targetNormal, targetPos, resultDistance, resultColor);

    //upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV + ivec2(2, 0), scale, targetNormal, targetPos, resultDistance, resultColor);
    //upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV + ivec2(-2, 0), scale, targetNormal, targetPos, resultDistance, resultColor);
    //upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV + ivec2(0, 2), scale, targetNormal, targetPos, resultDistance, resultColor);
    //upsizeHelper(colorSampler, normalSampler, posSampler, sourceUV + ivec2(0, -2), scale, targetNormal, targetPos, resultDistance, resultColor);

    return resultColor;
}