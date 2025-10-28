vec3 getWorldPos(sampler2D DiffuseDepthSampler, ivec2 fragCoord, vec2 screenSize) {
    return viewToWorldSpace(viewPosFromDepth(texelFetch(DiffuseDepthSampler, fragCoord, 0).r, vec2(fragCoord) / screenSize));
}

bool testMask(sampler2D AtlasSampler, vec3 origin, vec3 dir, float len, Quad q) {
    vec2 uv;

    if (raycastQuad(origin, dir, len, q, uv)) {
        vec4 color = texture(AtlasSampler, uv);

        if (color.a == 0) {
            return true;
        }
    } else {
        return true;
    }

    return false;
}