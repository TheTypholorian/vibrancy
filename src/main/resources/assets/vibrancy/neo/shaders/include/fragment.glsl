float attenuateNoCusp(float distance, float radius) {
    float s = distance / radius;

    if (s >= 1.0) {
        return 0.0;
    }

    float oneMinusS = 1.0 - s;
    return oneMinusS * oneMinusS * oneMinusS;
}

vec3 samplePointLight(vec3 lightPos, vec3 fragPos, float radius, vec3 lightColor) {
    return attenuateNoCusp(distance(lightPos, fragPos), radius) * lightColor;
}

vec3 sampleCubeLight(vec3 lightPos, vec3 fragPos, float startRadius, float endRadius, vec3 lightColor) {
    float dist = max(abs(lightPos.x - fragPos.x), max(abs(lightPos.y - fragPos.y), abs(lightPos.z - fragPos.z)));
    return clamp((endRadius - dist) / (endRadius - startRadius), 0, 1) * lightColor;
}

vec3 specularReflection(vec3 baseColor, vec3 lightColor, vec3 lightDir, vec3 cameraPos, vec3 vertexPos, vec3 normal, float strength, float exponent, sampler2D reflectionSampler, vec2 texCoord0) {
    vec3 inputNormal = lightDir;
    vec3 outputNormal = normalize(cameraPos - vertexPos);
    vec3 reflectedNormal = 2 * dot(inputNormal, normal) * normal - inputNormal;
    float multiplier = pow(clamp(dot(outputNormal, reflectedNormal), 0, 1) * (1 - (dot(inputNormal, outputNormal) / 2 + 0.5)), exponent) * strength;

    return baseColor + lightColor * texture(reflectionSampler, texCoord0).r * multiplier;
}

vec3 applyLight(vec3 lightColor, vec4 blockColor, vec3 pos) {
    vec3 mixedColor = mix(lightColor, lightColor * blockColor.rgb, blockColor.a) * blockColor.a;
    return mixedColor * fogFade(pos);
}