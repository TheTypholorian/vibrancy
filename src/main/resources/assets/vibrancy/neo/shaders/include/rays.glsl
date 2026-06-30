struct AABB {
    vec3 min;
    vec3 max;
};

bool raycastAABB(vec3 origin, vec3 invDir, float len, AABB aabb) {
    vec3 t0 = (aabb.min - origin) * invDir;
    vec3 t1 = (aabb.max - origin) * invDir;

    vec3 ts = min(t0, t1);
    vec3 tb = max(t0, t1);

    float tmin = max(max(ts.x, ts.y), ts.z);
    float tmax = min(min(tb.x, tb.y), tb.z);

    return tmax >= 0 && tmin <= tmax && tmin <= len;
}

struct Quad {
    vec3 vert1; uint uv1;
    vec3 vert2; uint uv2;
    vec3 vert3; uint uv3;
    vec3 vert4; uint uv4;
};
struct ColoredQuad {
    vec3 vert1; uint color1; vec2 uv1;
    vec3 vert2; uint color2; vec2 uv2;
    vec3 vert3; uint color3; vec2 uv3;
    vec3 vert4; uint color4; vec2 uv4;
};
struct ComplexQuad {
    vec3 vert1; float u1; float v1; uint overlay1; uint color1; uint normal1;
    vec3 vert2; float u2; float v2; uint overlay2; uint color2; uint normal2;
    vec3 vert3; float u3; float v3; uint overlay3; uint color3; uint normal3;
    vec3 vert4; float u4; float v4; uint overlay4; uint color4; uint normal4;
};

ivec2 unpackUV(uint uv) {
    return ivec2(uv >> 16, uv & 0xFFFFu);
}

bool raycastQuad(bool checkDir, vec3 origin, vec3 dir, float len, float margin, vec3 v1, vec3 v2, vec3 v3, vec3 v4, out vec2 uv, out float tt) {
    vec3 normal = normalize(cross(v2 - v1, v4 - v1));

    float denom = dot(dir, normal);
    if (checkDir && denom <= 0.0) return false;

    float d = dot(normal, v1);

    tt = (d - dot(origin, normal)) / denom;
    if (tt < margin * sign(denom) || tt > len - margin) return false;

    vec3 p = origin + tt * dir;
    vec3 vp = p - v1;

    vec3 diagonal1 = v2 - v1;
    vec3 diagonal2 = v4 - v1;

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

    if (a < -margin || b < -margin || a > 1 + margin || b > 1 + margin) return false;

    uv = clamp(vec2(a, b), margin, 1 - margin);

    return true;
}

bool sampleQuad(bool checkDir, sampler2D Sampler0, ivec2 Sampler0Size, vec3 origin, vec3 dir, float len, float margin, Quad q, out float dist, out vec4 outColor) {
    vec2 uv;

    if (raycastQuad(checkDir, origin, dir, len, margin, q.vert1, q.vert2, q.vert3, q.vert4, uv, dist)) {
        ivec2 texUv = ivec2(mix(mix(unpackUV(q.uv1), unpackUV(q.uv2), uv.x), mix(unpackUV(q.uv4), unpackUV(q.uv3), uv.x), uv.y));
        vec4 pixel = texelFetch(Sampler0, texUv, 0);
        outColor = pixel;

        return true;
    } else {
        outColor = vec4(0);
        return false;
    }
}

bool sampleComplexQuad(bool checkDir, sampler2D Sampler0, ivec2 Sampler0Size, vec3 origin, vec3 dir, float len, float margin, ComplexQuad q, out float dist, out vec4 outColor) {
    vec2 uv;

    if (raycastQuad(checkDir, origin, dir, len, margin, q.vert1, q.vert2, q.vert3, q.vert4, uv, dist)) {
        vec2 texUv = mix(mix(vec2(q.u1, q.v1), vec2(q.u2, q.v2), uv.x), mix(vec2(q.u4, q.v4), vec2(q.u3, q.v3), uv.x), uv.y);
        vec4 pixel = texture(Sampler0, texUv);
        outColor = pixel;

        return true;
    } else {
        outColor = vec4(0);
        return false;
    }
}

bool sampleColoredQuad(bool checkDir, sampler2D Sampler0, ivec2 Sampler0Size, vec3 origin, vec3 dir, float len, float margin, ColoredQuad q, out float dist, out vec4 outColor) {
    vec2 uv;

    if (raycastQuad(checkDir, origin, dir, len, margin, q.vert1, q.vert2, q.vert3, q.vert4, uv, dist)) {
        vec2 texUv = mix(mix(q.uv1, q.uv2, uv.x), mix(q.uv4, q.uv3, uv.x), uv.y);
        vec4 color = mix(mix(unpackUnorm4x8(q.color1), unpackUnorm4x8(q.color2), uv.x), mix(unpackUnorm4x8(q.color4), unpackUnorm4x8(q.color3), uv.x), uv.y);
        vec4 pixel = texture(Sampler0, texUv) * color;
        outColor = pixel;

        return true;
    } else {
        outColor = vec4(0);
        return false;
    }
}
