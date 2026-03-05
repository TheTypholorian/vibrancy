float attenuateNoCusp(float distance, float radius) {
    float s = distance / radius;

    if (s >= 1.0) {
        return 0.0;
    }

    float oneMinusS = 1.0 - s;
    return oneMinusS * oneMinusS * oneMinusS;
}

vec4 samplePointLight(vec3 lightPos, vec3 fragPos, float radius, vec3 lightColor) {
    return vec4(
        attenuateNoCusp(distance(lightPos, fragPos), radius) *
        lightColor,
        1
    );
}

vec4 sampleCubeLight(vec3 lightPos, vec3 fragPos, float startRadius, float endRadius, vec3 lightColor) {
    float dist = max(abs(lightPos.x - fragPos.x), max(abs(lightPos.y - fragPos.y), abs(lightPos.z - fragPos.z)));
    return vec4(
        clamp((endRadius - dist) / (endRadius - startRadius), 0, 1) *
        lightColor,
        1
    );
}