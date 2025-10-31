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