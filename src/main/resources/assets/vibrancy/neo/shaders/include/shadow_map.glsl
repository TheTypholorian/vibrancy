vec3 fisheyeShadowMapCoords(vec4 glPosition, float power) {
    vec3 ndc = glPosition.xyz / glPosition.w;
    float r = length(ndc.xy);

    if (r > 0.00001) {
        ndc.xy *= atan(r * power) / atan(power) / r;
    }

    ndc.z = sign(ndc.z) * atan(abs(ndc.z) * power) / atan(power);

    return ndc;
}

vec4 fisheyeShadowMap(vec4 glPosition, float power) {
    return vec4(fisheyeShadowMapCoords(glPosition, power) * glPosition.w, glPosition.w);
}