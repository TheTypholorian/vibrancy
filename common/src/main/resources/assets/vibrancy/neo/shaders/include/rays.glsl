struct Quad {
    vec3 v1; vec2 uv1;
    vec3 v2; vec2 uv2;
    vec3 v3; vec2 uv3;
    vec3 v4; vec2 uv4;
};

bool raycastQuad(vec3 origin, vec3 dir, float len, float margin, Quad q, out vec2 uv, out float tt) {
    vec3 normal = normalize(cross(q.v2 - q.v1, q.v4 - q.v1));

    float denom = dot(dir, normal);
    //if (denom >= 0.0 == front) return false;

    float d = dot(normal, q.v1);

    tt = (d - dot(origin, normal)) / denom;
    if (tt < margin || tt > len - margin) return false;

    vec3 p = origin + tt * dir;
    vec3 vp = p - q.v1;

    vec3 diagonal1 = q.v2 - q.v1;
    vec3 diagonal2 = q.v4 - q.v1;

    float d1p = dot(diagonal1, vp);
    float d2p = dot(diagonal2, vp);

    float d11 = dot(diagonal1, diagonal1);
    float d12 = dot(diagonal1, diagonal2);
    float d22 = dot(diagonal2, diagonal2);
    float invDet = 1 / (d11 * d22 - d12 * d12);

    float inv11 = d22 * invDet;
    float inv12 = -d12 * invDet;
    float inv22 = d11 * invDet;

    float a = inv11 * d1p + inv12 * d2p;
    float b = inv12 * d1p + inv22 * d2p;

    if (a < 0 || b < 0 || a > 1 || b > 1) return false;

    uv = vec2(a, b);

    return true;
}

bool sampleQuad(sampler2D AtlasSampler, vec3 origin, vec3 dir, float len, float margin, Quad q, inout vec2 uv, out float t) {
    if (raycastQuad(origin, dir, len, margin, q, uv, t)) {
        vec2 texUv = mix(mix(q.uv1, q.uv2, uv.x), mix(q.uv4, q.uv3, uv.x), uv.y);
        return texture(AtlasSampler, texUv).a < 1;
    } else {
        return true;
    }
}
