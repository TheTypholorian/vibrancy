uniform int DownsizeFactor = 1;

vec2 getScreenUV(vec2 screen) {
    return gl_FragCoord.xy / screen;
}

vec2 upsizeHelper(sampler2D colorSampler, sampler2D normalSampler, sampler2D posSampler, ivec2 originUV, ivec2 sourceUV, int scale, vec3 targetNormal, vec3 targetPos) {
    ivec2 upscaledUV = sourceUV * scale;
    vec3 checkNormal = texelFetch(normalSampler, upscaledUV, 0).xyz;

    if (dot(checkNormal, targetNormal) > 0.99) {
        vec3 checkPos = texelFetch(posSampler, upscaledUV, 0).xyz;
        float d = distance(originUV, upscaledUV); //distance(targetPos, checkPos) + 1;
        vec4 resultColor = texelFetch(colorSampler, sourceUV, 0);

        if (resultColor.a < 0.5) {
            return vec2(1 / d, 0);
        } else {
            return vec2(0, 1 / d);
        }
    }

    return vec2(0);
}

vec4 upsize(sampler2D colorSampler, sampler2D normalSampler, sampler2D posSampler, ivec2 upscaledUV, int scale) {
    vec3 targetNormal = texelFetch(normalSampler, upscaledUV, 0).xyz;
    vec3 targetPos = texelFetch(posSampler, upscaledUV, 0).xyz;
    ivec2 sourceUV = upscaledUV / scale;

    vec2 result = vec2(0);

    //result += upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV, scale, targetNormal, targetPos);

    result += upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(1, 0), scale, targetNormal, targetPos);
    result += upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(-1, 0), scale, targetNormal, targetPos);
    result += upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(0, 1), scale, targetNormal, targetPos);
    result += upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(0, -1), scale, targetNormal, targetPos);

    result += upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(1, 1), scale, targetNormal, targetPos);
    result += upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(-1, 1), scale, targetNormal, targetPos);
    result += upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(1, -1), scale, targetNormal, targetPos);
    result += upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(-1, -1), scale, targetNormal, targetPos);

    //result += upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(2, 0), scale, targetNormal, targetPos);
    //result += upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(-2, 0), scale, targetNormal, targetPos);
    //result += upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(0, 2), scale, targetNormal, targetPos);
    //result += upsizeHelper(colorSampler, normalSampler, posSampler, upscaledUV, sourceUV + ivec2(0, -2), scale, targetNormal, targetPos);

    return vec4(normalize(result), 1, 1);//result.y >= result.x ? vec4(1) : vec4(0);
}