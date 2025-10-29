vec4 getWorldPos(sampler2D DiffuseDepthSampler, ivec2 fragCoord, vec2 screenSize) {
    vec3 view = viewPosFromDepth(texelFetch(DiffuseDepthSampler, fragCoord, 0).r, vec2(fragCoord) / screenSize);
    return vec4(viewToWorldSpace(view), length(view));
}

bool testMask(sampler2D AtlasSampler, vec3 origin, vec3 dir, float len, float margin, bool front, Quad q) {
    vec2 uv;

    if (raycastQuad(origin, dir, len, margin, front, q, uv)) {
        vec4 color = texture(AtlasSampler, uv);

        if (color.a == 0) {
            return true;
        }
    } else {
        return true;
    }

    return false;
}