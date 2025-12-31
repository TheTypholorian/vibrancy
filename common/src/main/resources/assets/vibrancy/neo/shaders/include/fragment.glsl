vec4 getWorldPos(sampler2D depthSampler, vec2 screenSize, mat4 ProjMat, mat4 ModelViewMat, vec3 CameraPos) {
    vec2 uv = gl_FragCoord.xy / screenSize;
    float depth = texelFetch(depthSampler, ivec2(gl_FragCoord.xy), 0).r;
    vec4 pos = inverse(ProjMat) * (vec4(uv, depth, 1.0) * 2.0 - 1.0);
    return -vec4(CameraPos, 0.0) + inverse(ModelViewMat) * (pos / pos.w);
}

float getNormalDot(sampler2D normalSampler, vec3 lightDirection) {
    return clamp(dot(normalize(texelFetch(normalSampler, ivec2(gl_FragCoord.xy), 0).xyz), lightDirection), 0, 1);
}

float attenuateNoCusp(float distance, float radius) {
    float s = distance / radius;

    if (s >= 1.0) {
        return 0.0;
    }

    float oneMinusS = 1.0 - s;
    return oneMinusS * oneMinusS * oneMinusS;
}

vec4 sampleLight(sampler2D normalSampler, vec3 lightPos, vec3 fragPos, float radius, vec3 lightColor) {
    return vec4(
        getNormalDot(normalSampler, normalize(lightPos - fragPos))
        * attenuateNoCusp(distance(lightPos, fragPos), radius)
        * lightColor,
        1
    );
}