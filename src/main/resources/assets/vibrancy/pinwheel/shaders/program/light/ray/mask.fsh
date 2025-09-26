#version 430

#include "veil:deferred_utils"

struct Quad {
    vec3 v1; uint doSample;
    vec3 v2; float _p2;
    vec3 v3; float _p3;
    vec3 v4; float _p4;

    vec2 uv1, uv2, uv3, uv4;

    vec3 n; float d;
    vec3 e1; float lenE1;
    vec3 e2; float lenE2;
};

layout(std430, binding = 0) buffer Quads {
    Quad quads[];
};

uniform sampler2D BlockAtlasSampler;
uniform sampler2D DiffuseDepthSampler;
uniform vec3 LightPos;
uniform vec2 ScreenSize;

uniform bool Detailed = false;

in flat uint index;

out vec4 fragColor;

bool raycastQuad(vec3 origin, vec3 dir, float len, Quad q, out vec2 uv) {
    float denom = dot(dir, q.n);
    if (denom >= 0.0) return false;

    float tt = (q.d - dot(origin, q.n)) / denom;
    if (tt < 1e-3 || tt > len - 1e-3) return false;

    vec3 p = origin + tt * dir;
    vec3 vp = p - q.v1;

    float d11 = q.lenE1;
    float d12 = dot(q.e1, q.e2);
    float d22 = q.lenE2;
    float d1p = dot(q.e1, vp);
    float d2p = dot(q.e2, vp);

    float invDenom = 1.0 / (d11 * d22 - d12 * d12);
    float a = (d22 * d1p - d12 * d2p) * invDenom;
    float b = (d11 * d2p - d12 * d1p) * invDenom;

    if (a < 0.0 || b < 0.0 || a > 1.0 || b > 1.0) return false;

    if (q.doSample == 1) {
        uv = mix(mix(q.uv1, q.uv2, a), mix(q.uv4, q.uv3, a), b);
        vec4 col = texture(BlockAtlasSampler, uv);

        fragColor = col;

        if (col.a == 0) {
            discard;
        }
    }

    return true;
}

void main() {
    fragColor = vec4(1);

    if (Detailed) {
        vec2 screenUv = gl_FragCoord.xy / ScreenSize;

        float depth = texelFetch(DiffuseDepthSampler, ivec2(gl_FragCoord.xy), 0).r;
        vec3 Pos = viewToWorldSpace(viewPosFromDepth(depth, screenUv));

        vec3 delta = Pos - LightPos;
        float len = length(delta);
        vec3 dir = delta / len;

        Quad q = quads[index];
        vec2 uv;

        // 35%
        if (!raycastQuad(LightPos, dir, len, q, uv)) {
            discard;
        }
    }
}
