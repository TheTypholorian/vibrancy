vec4 getWorldPos(sampler2D depthSampler, vec2 screenSize, mat4 iProjMat, mat4 iModelMat, vec3 camera) {
    vec2 uv = gl_FragCoord.xy / screenSize;
    float depth = texture(depthSampler, uv).r;
    vec4 pos = iProjMat * (vec4(uv, depth, 1.0) * 2.0 - 1.0);
    return vec4(camera, 0) + iModelMat * (pos / pos.w);
}

float getNormalDot(sampler2D normalSampler, vec3 lightDirection, vec2 screenSize) {
    return clamp(dot(texture(normalSampler, gl_FragCoord.xy / screenSize).xyz, lightDirection), 0, 1);
}

float attenuateNoCusp(float distance, float radius) {
    float s = distance / radius;

    if (s >= 1.0) {
        return 0.0;
    }

    float oneMinusS = 1.0 - s;
    return oneMinusS * oneMinusS * oneMinusS;
}

vec4 sampleLight(sampler2D normalSampler, vec2 screenSize, vec3 lightPos, vec3 fragPos, float radius, vec3 lightColor) {
    return vec4(
        //getNormalDot(normalSampler, normalize(lightPos - fragPos), screenSize)
        attenuateNoCusp(distance(lightPos, fragPos), radius)
        * lightColor,
        1
    );
}