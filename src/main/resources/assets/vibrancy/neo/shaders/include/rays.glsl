struct Ray {
    vec3 pos;
    vec3 dir;
    vec3 invDir;
    float len;
};

struct EndlessRay {
    vec3 pos;
    vec3 dir;
    vec3 invDir;
};

Ray createRay(vec3 pos, vec3 dir, float len) {
    return Ray(pos, dir, 1 / dir, len);
}

Ray createRayTo(vec3 from, vec3 to) {
    vec3 delta = to - from;
    return createRay(from, normalize(delta), length(delta));
}

EndlessRay createEndlessRay(vec3 pos, vec3 dir) {
    return EndlessRay(pos, dir, 1 / dir);
}

EndlessRay createEndlessRayTo(vec3 from, vec3 to) {
    vec3 delta = to - from;
    return createEndlessRay(from, normalize(delta));
}

EndlessRay createEndlessRay(Ray ray) {
    return EndlessRay(ray.pos, ray.dir, ray.invDir);
}

struct AABB {
    vec3 min;
    vec3 max;
};

bool testRayAABB(Ray ray, AABB aabb) { // TODO length checking stuff
    vec3 t0 = (aabb.min - ray.pos) * ray.invDir;
    vec3 t1 = (aabb.max - ray.pos) * ray.invDir;

    vec3 ts = min(t0, t1);
    vec3 tb = max(t0, t1);

    float tmin = max(max(ts.x, ts.y), ts.z);
    float tmax = min(min(tb.x, tb.y), tb.z);

    return tmax >= 0 && tmin <= tmax && tmin <= ray.len;
}

struct Quad {
    vec3 vert1; uint uv1;
    vec3 vert2; uint uv2;
    vec3 vert3; uint uv3;
    vec3 vert4; uint uv4;
};
struct ColoredQuad {
    vec3 vert1; uint color1; uint uv1;
    vec3 vert2; uint color2; uint uv2;
    vec3 vert3; uint color3; uint uv3;
    vec3 vert4; uint color4; uint uv4;
};
struct EntityQuad {
    vec3 vert1; float u1; float v1; uint color1; uvec2 padding1;
    vec3 vert2; float u2; float v2; uint color2; uvec2 padding2;
    vec3 vert3; float u3; float v3; uint color3; uvec2 padding3;
    vec3 vert4; float u4; float v4; uint color4; uvec2 padding4;
};

bool raycastQuad(Ray ray, float margin, vec3 v1, vec3 v2, vec3 v3, vec3 v4, out float denom, out vec2 uv, out float tt) {
    vec3 normal = normalize(cross(v2 - v1, v4 - v1));

    denom = dot(ray.dir, normal);

    if (abs(denom) < margin) return false;

    float d = dot(normal, v1);

    tt = (d - dot(ray.pos, normal)) / denom;
    if (tt < margin * sign(denom) || tt > ray.len - margin) return false;

    vec3 p = ray.pos + tt * ray.dir;
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

bool raycastQuad(Ray ray, float margin, Quad quad, out float denom, out vec2 uv, out float tt) {
    return raycastQuad(ray, margin, quad.vert1, quad.vert2, quad.vert3, quad.vert4, denom, uv, tt);
}

bool raycastQuad(Ray ray, float margin, ColoredQuad quad, out float denom, out vec2 uv, out float tt) {
    return raycastQuad(ray, margin, quad.vert1, quad.vert2, quad.vert3, quad.vert4, denom, uv, tt);
}

bool raycastQuad(Ray ray, float margin, EntityQuad quad, out float denom, out vec2 uv, out float tt) {
    return raycastQuad(ray, margin, quad.vert1, quad.vert2, quad.vert3, quad.vert4, denom, uv, tt);
}

bool raycastQuad(EndlessRay ray, float margin, vec3 v1, vec3 v2, vec3 v3, vec3 v4, out float denom, out vec2 uv, out float tt) {
    vec3 normal = normalize(cross(v2 - v1, v4 - v1));

    denom = dot(ray.dir, normal);

    if (abs(denom) < margin) return false;

    float d = dot(normal, v1);

    tt = (d - dot(ray.pos, normal)) / denom;
    if (tt < margin * sign(denom)) return false;

    vec3 p = ray.pos + tt * ray.dir;
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

bool raycastQuad(EndlessRay ray, float margin, Quad quad, out float denom, out vec2 uv, out float tt) {
    return raycastQuad(ray, margin, quad.vert1, quad.vert2, quad.vert3, quad.vert4, denom, uv, tt);
}

bool raycastQuad(EndlessRay ray, float margin, ColoredQuad quad, out float denom, out vec2 uv, out float tt) {
    return raycastQuad(ray, margin, quad.vert1, quad.vert2, quad.vert3, quad.vert4, denom, uv, tt);
}

bool raycastQuad(EndlessRay ray, float margin, EntityQuad quad, out float denom, out vec2 uv, out float tt) {
    return raycastQuad(ray, margin, quad.vert1, quad.vert2, quad.vert3, quad.vert4, denom, uv, tt);
}

vec2 interpolateQuadUV(Quad quad, vec2 uv) {
    return mix(mix(unpackUnorm2x16(quad.uv1), unpackUnorm2x16(quad.uv2), uv.x), mix(unpackUnorm2x16(quad.uv4), unpackUnorm2x16(quad.uv3), uv.x), uv.y);
}

vec2 interpolateQuadUV(ColoredQuad quad, vec2 uv) {
    return mix(mix(unpackUnorm2x16(quad.uv1), unpackUnorm2x16(quad.uv2), uv.x), mix(unpackUnorm2x16(quad.uv4), unpackUnorm2x16(quad.uv3), uv.x), uv.y);
}

vec2 interpolateQuadUV(EntityQuad quad, vec2 uv) {
    return mix(mix(vec2(quad.u1, quad.v1), vec2(quad.u2, quad.v2), uv.x), mix(vec2(quad.u4, quad.v4), vec2(quad.u3, quad.v3), uv.x), uv.y);
}

vec4 interpolateQuadColor(ColoredQuad quad, vec2 uv) {
    return mix(mix(unpackUnorm4x8(quad.color1), unpackUnorm4x8(quad.color2), uv.x), mix(unpackUnorm4x8(quad.color4), unpackUnorm4x8(quad.color3), uv.x), uv.y);
}

vec4 interpolateQuadColor(EntityQuad quad, vec2 uv) {
    return mix(mix(unpackUnorm4x8(quad.color1), unpackUnorm4x8(quad.color2), uv.x), mix(unpackUnorm4x8(quad.color4), unpackUnorm4x8(quad.color3), uv.x), uv.y);
}

struct DDAState {
    ivec3 voxel;
    ivec3 step;
    vec3 nextPos;
    vec3 tMax;
    vec3 tDelta;
    float tEnter;
    float tExit;
};

DDAState createDDA(EndlessRay ray, vec3 pos, ivec3 voxel) {
    DDAState dda;

    dda.voxel = voxel;
    dda.step = ivec3(sign(ray.dir));
    dda.nextPos.x = ray.dir.x > 0 ? float(dda.voxel.x + 1) : float(dda.voxel.x);
    dda.nextPos.y = ray.dir.y > 0 ? float(dda.voxel.y + 1) : float(dda.voxel.y);
    dda.nextPos.z = ray.dir.z > 0 ? float(dda.voxel.z + 1) : float(dda.voxel.z);
    dda.tMax = (dda.nextPos - pos) * ray.invDir;
    dda.tDelta = abs(ray.invDir);
    dda.tEnter = 0;
    dda.tExit = min(dda.tMax.x, min(dda.tMax.y, dda.tMax.z));

    return dda;
}

DDAState createDDA(EndlessRay ray, vec3 pos) {
    return createDDA(ray, pos, ivec3(floor(pos)));
}

DDAState createDDA(EndlessRay ray) {
    return createDDA(ray, ray.pos);
}

DDAState createDDA(Ray ray, vec3 pos, ivec3 voxel) {
    return createDDA(createEndlessRay(ray), pos, voxel);
}

DDAState createDDA(Ray ray, vec3 pos) {
    return createDDA(createEndlessRay(ray), pos);
}

DDAState createDDA(Ray ray) {
    return createDDA(createEndlessRay(ray));
}

void stepDDA(inout DDAState dda) {
    if (dda.tMax.x < dda.tMax.y) {
        if (dda.tMax.x < dda.tMax.z) {
            dda.voxel.x += dda.step.x;
            dda.tMax.x += dda.tDelta.x;
        } else {
            dda.voxel.z += dda.step.z;
            dda.tMax.z += dda.tDelta.z;
        }
    } else {
        if (dda.tMax.y < dda.tMax.z) {
            dda.voxel.y += dda.step.y;
            dda.tMax.y += dda.tDelta.y;
        } else {
            dda.voxel.z += dda.step.z;
            dda.tMax.z += dda.tDelta.z;
        }
    }

    dda.tEnter = dda.tExit;
    dda.tExit = min(dda.tMax.x, min(dda.tMax.y, dda.tMax.z));
}

void stepDDA(inout DDAState dda, inout uint gridIndex, ivec3 indexStep) {
    if (dda.tMax.x < dda.tMax.y) {
        if (dda.tMax.x < dda.tMax.z) {
            dda.voxel.x += dda.step.x;
            gridIndex += indexStep.x;
            dda.tMax.x += dda.tDelta.x;
        } else {
            dda.voxel.z += dda.step.z;
            gridIndex += indexStep.z;
            dda.tMax.z += dda.tDelta.z;
        }
    } else {
        if (dda.tMax.y < dda.tMax.z) {
            dda.voxel.y += dda.step.y;
            gridIndex += indexStep.y;
            dda.tMax.y += dda.tDelta.y;
        } else {
            dda.voxel.z += dda.step.z;
            gridIndex += indexStep.z;
            dda.tMax.z += dda.tDelta.z;
        }
    }

    dda.tEnter = dda.tExit;
    dda.tExit = min(dda.tMax.x, min(dda.tMax.y, dda.tMax.z));
}