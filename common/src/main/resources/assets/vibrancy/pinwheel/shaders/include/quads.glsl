struct Quad {
    vec3 v1; float _p1;
    vec3 v2; float _p2;
    vec3 v3; float _p3;
    vec3 v4; float _p4;

    vec2 uv1, uv2, uv3, uv4;

    vec3 n; float d;
    vec3 e1; float _p5;
    vec3 e2; float _p6;

    float inv11; float inv12;
    float inv21; float inv22;
};

bool raycastQuad(vec3 origin, vec3 dir, float len, float margin, bool front, Quad q, out vec2 uv) {
    float denom = dot(dir, q.n);
    if (denom >= 0.0 == front) return false;

    float tt = (q.d - dot(origin, q.n)) / denom;
    if (tt < margin || tt > len - margin) return false;

    vec3 p = origin + tt * dir;
    vec3 vp = p - q.v1;

    float d1p = dot(q.e1, vp);
    float d2p = dot(q.e2, vp);

    float a = q.inv11 * d1p + q.inv12 * d2p;
    float b = q.inv21 * d1p + q.inv22 * d2p;

    if (a < 0 || b < 0 || a > 1 || b > 1) return false;

    uv = mix(mix(q.uv1, q.uv2, a), mix(q.uv4, q.uv3, a), b);

    return true;
}
