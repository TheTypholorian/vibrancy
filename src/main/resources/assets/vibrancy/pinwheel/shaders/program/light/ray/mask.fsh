#version 430

#include "veil:deferred_utils"

struct Quad {
    vec3 v1, v2, v3, v4;
    vec2 uv1, uv2, uv3, uv4;
};

layout(std430, binding = 0) buffer Quads {
    Quad quads[];
};

uniform sampler2D BlockAtlasSampler;
uniform sampler2D DiffuseDepthSampler;
uniform vec3 LightPos;
uniform vec2 ScreenSize;

in flat uint index;

out vec4 fragColor;

bool raycastTriangle(vec3 ro, vec3 rd, vec3 v0, vec3 v1, vec3 v2, float m, out float t, out float u, out float v) {
    vec3 v1v0 = v1 - v0;
    vec3 v2v0 = v2 - v0;
    vec3 rov0 = ro - v0;
    vec3 n = cross(v1v0, v2v0);
    vec3 q = cross(rov0, rd);
    float d = 1.0 / dot(rd, n);
    u = d * dot(-q, v2v0);

    if (u < 0) {
        return false;
    }

    v = d * dot(q, v1v0);

    if (v < 0 || u + v > 1) {
        return false;
    }

    t = d * dot(-n, rov0);

    return t > 1e-3 && t < m - 1e-3;
}

bool raycastQuad(vec3 origin, vec3 dir, float len, Quad q, out float t, out vec3 bary, out vec2 uv0, out vec2 uv1, out vec2 uv2) {
    float tu, uu, vv;

    if (raycastTriangle(origin, dir, q.v1, q.v2, q.v3, len, tu, uu, vv)) {
        t = tu;
        bary = vec3(1.0 - uu - vv, uu, vv);
        uv0 = q.uv1;
        uv1 = q.uv2;
        uv2 = q.uv3;
        return true;
    }

    if (raycastTriangle(origin, dir, q.v1, q.v3, q.v4, len, tu, uu, vv)) {
        t = tu;
        bary = vec3(1.0 - uu - vv, uu, vv);
        uv0 = q.uv1;
        uv1 = q.uv3;
        uv2 = q.uv4;
        return true;
    }

    return false;
}

void main() {
    fragColor = vec4(1);

    vec2 screenUv = gl_FragCoord.xy / ScreenSize;

    float depth = texelFetch(DiffuseDepthSampler, ivec2(gl_FragCoord.xy), 0).r;
    vec3 Pos = viewToWorldSpace(viewPosFromDepth(depth, screenUv));

    vec3 delta = Pos - LightPos;
    float len = length(delta);
    vec3 dir = delta / len;

    float t;
    vec3 bary;
    vec2 uv0, uv1, uv2;

    if (!raycastQuad(LightPos, dir, len, quads[index], t, bary, uv0, uv1, uv2)) {
        discard;
    } else {
        vec2 uv = uv0 * bary.x + uv1 * bary.y + uv2 * bary.z;
        vec4 col = texture(BlockAtlasSampler, uv);

        fragColor = col;

        if (col.a == 0) {
            discard;
        }
    }
}
