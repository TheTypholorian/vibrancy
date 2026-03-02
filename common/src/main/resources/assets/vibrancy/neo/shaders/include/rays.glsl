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

    if (a < -margin || b < -margin || a > (1 + margin) || b > (1 + margin)) return false;

    uv = clamp(vec2(a, b), 0, 1);

    return true;
}

vec4 sampleQuad(sampler2D AtlasSampler, vec3 origin, vec3 dir, float len, float margin, Quad q, out float dist) {
    vec2 uv;

    if (raycastQuad(origin, dir, len, margin, q, uv, dist)) {
        vec2 texUv = mix(mix(q.uv1, q.uv2, uv.x), mix(q.uv4, q.uv3, uv.x), uv.y);
        vec4 pixel = texelFetch(AtlasSampler, ivec2(texUv * textureSize(AtlasSampler, 0)), 0);

        if (pixel.a == 0) {
            return vec4(1);
        }

        if (pixel.a == 1) {
            return vec4(0);
        }

        return vec4(pixel.rgb, 1);
    } else {
        return vec4(1);
    }
}

vec3 interpolateQuadPos(Quad q, vec2 uv) {
    vec3 a = mix(q.v1, q.v2, uv.x);
    vec3 b = mix(q.v4, q.v3, uv.x);
    return mix(a, b, uv.y);
}

struct Triangle {
    vec3 v1; vec2 uv1;
    vec3 v2; vec2 uv2;
    vec3 v3; vec2 uv3;
};

bool raycastTriangle(vec3 origin, vec3 dir, float len, float margin, Triangle t, out vec2 uv, out float tt) {
    vec3 edge1 = t.v2 - t.v1;
    vec3 edge2 = t.v3 - t.v1;

    vec3 pvec = cross(dir, edge2);
    float det = dot(edge1, pvec);

    float invDet = 1.0 / det;
    vec3 tvec = origin - t.v1;

    float u = dot(tvec, pvec) * invDet;
    if (u < 0.0 || u > 1.0) return false;

    vec3 qvec = cross(tvec, edge1);
    float v = dot(dir, qvec) * invDet;
    if (v < 0.0 || u + v > 1.0) return false;

    tt = dot(edge2, qvec) * invDet;
    if (tt < margin || tt > len - margin) return false;

    uv = vec2(u, v);

    return true;
}

bool sampleTriangle(sampler2D AtlasSampler, vec3 origin, vec3 dir, float len, float margin, Triangle t, out float dist) {
    vec2 uv;

    if (raycastTriangle(origin, dir, len, margin, t, uv, dist)) {
        vec2 texUv = t.uv1 * (1.0 - uv.x - uv.y) + t.uv2 * uv.x + t.uv3 * uv.y;
        return texture(AtlasSampler, texUv).a < 1;
    } else {
        return true;
    }
}