vec4 getWorldPos(sampler2D depthSampler, vec2 screenSize, mat4 iProjMat, mat4 iModelMat, vec3 camera, vec2 uv) {
    float depth = texelFetch(depthSampler, ivec2(gl_FragCoord.xy), 0).r;
    vec4 pos = iProjMat * (vec4(uv, depth, 1.0) * 2.0 - 1.0);
    return vec4(camera, 0) + iModelMat * (pos / pos.w);
}

float getNormalDot(sampler2D normalSampler, vec3 lightDirection, vec2 uv) {
    return clamp(dot(texture(normalSampler, uv).xyz, lightDirection), 0, 1);
}

float attenuateNoCusp(float distance, float radius) {
    float s = distance / radius;

    if (s >= 1.0) {
        return 0.0;
    }

    float oneMinusS = 1.0 - s;
    return oneMinusS * oneMinusS * oneMinusS;
}

vec4 sampleSkyLight(sampler2D normalSampler, vec2 uv, vec3 lightDirection, vec3 lightColor) {
    return vec4(
        getNormalDot(normalSampler, lightDirection, uv) *
        lightColor,
        1
    );
}

vec4 samplePointLight(sampler2D normalSampler, vec2 uv, vec3 lightPos, vec3 fragPos, float radius, vec3 lightColor) {
    return vec4(
        getNormalDot(normalSampler, normalize(lightPos - fragPos), uv) *
        attenuateNoCusp(distance(lightPos, fragPos), radius) *
        lightColor,
        1
    );
}

vec4 samplePointLight(vec2 screenSize, vec3 lightPos, vec3 fragPos, float radius, vec3 lightColor) {
    return vec4(
        attenuateNoCusp(distance(lightPos, fragPos), radius) *
        lightColor,
        1
    );
}