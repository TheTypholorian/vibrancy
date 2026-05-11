vec3 fisheyeShadowMapCoords(vec4 glPosition, float power) {
    vec3 ndc = glPosition.xyz / glPosition.w;
    float r = length(ndc.xyz);

    if (r > 0.00001) {
        ndc.xyz *= atan(r * power) / atan(power) / r;
    }

    return ndc;
}

vec4 fisheyeShadowMap(vec4 glPosition, float power) {
    return vec4(fisheyeShadowMapCoords(glPosition, power) * glPosition.w, glPosition.w);
}