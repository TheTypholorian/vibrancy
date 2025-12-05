vec3 getWorldPos(sampler2D depthSampler, vec2 screenSize) {
    return screenToWorldSpace(
        gl_FragCoord.xy / screenSize,
        texelFetch(depthSampler, ivec2(gl_FragCoord.xy), 0).r
    ).xyz;
}

float getNormalDot(sampler2D normalSampler, vec3 lightDirection) {
    return clamp(dot(normalize(texelFetch(normalSampler, ivec2(gl_FragCoord.xy), 0).xyz), lightDirection), 0, 1);
}

vec4 sampleLight(sampler2D normalSampler, vec3 lightPos, vec3 fragPos, float radius, vec3 lightColor) {
    return vec4(
        getNormalDot(normalSampler, normalize((VeilCamera.ViewMat * vec4(lightPos - fragPos, 0.0)).xyz))
        * attenuate_no_cusp(length(lightPos - fragPos), radius)
        * lightColor,
        1
    );
}