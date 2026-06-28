//#include "big_shot_lib:fog"

vec3 samplePointLight(vec3 lightPos, vec3 fragPos, float radius, vec3 lightColor) {
    vec3 delta = lightPos - fragPos;
    float distSq = dot(delta, delta);
    float radiusSq = radius * radius;

    if (distSq >= radiusSq) {
        return vec3(0);
    }

    float s = sqrt(distSq) / radius;
    float a = 1 - s;
    return a * a * a * lightColor;
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

float hash4(vec4 v) {
    return fract(sin(dot(v, vec4(12.9898, 87.233, 37.719, 17.853))) * 43758.5454);
}

vec3 applyLight(vec3 lightColor, vec4 blockColor, vec3 pos) {
    vec3 mixedColor = mix(lightColor, lightColor * blockColor.rgb, blockColor.a) * blockColor.a;
    return mixedColor;// * fogFade(pos);
}

vec3 applyLight(vec3 lightColor, vec4 blockColor, vec3 pos, vec3 lightPos, float flicker, float time) {
    time *= 4;
    vec3 flickeredColor = lightColor * (1 + flicker * (mix(hash4(vec4(lightPos, floor(time))), hash4(vec4(lightPos, floor(time) + 1)), fract(time)) * 2 - 1));
    return applyLight(flickeredColor, blockColor, pos);
}